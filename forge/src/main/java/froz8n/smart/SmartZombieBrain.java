package froz8n.smart;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Baritone-style executor for smart zombies.
 *
 * <p>The zombie plans a route of <b>typed movements</b> with {@link DiggingPathfinder}
 * and then simply executes the current movement to completion before advancing to the
 * next one. It never "guesses" what to do from nearby blocks (which is what caused it
 * to pillar on flat ground or freeze one block below the player); each movement carries
 * exactly which blocks to dig / place and where to end up.
 */
public final class SmartZombieBrain {

    /** Detection radius in blocks. Configurable; the old hard-coded 128 saw across a whole map. */
    private static double followRange() { return froz8n.Config.detectionRange; }
    private static final double SPEED = 1.25;
    /** Vanilla zombies walk at 0.23 and a sprinting player moves at roughly 0.28. */
    public static double dayMoveSpeed() { return froz8n.Config.dayMoveSpeed; }
    public static double nightMoveSpeed() { return froz8n.Config.nightMoveSpeed; }
    private static final int SWING_INTERVAL = 5;
    private static final float IRON_HARDNESS = 3.0F;
    private static final double DIG_REACH = 4.5;
    private static final double JUMP_VELOCITY = 0.42;
    private static final int PILLAR_TIMEOUT = 40;
    private static final int WALK_PATH_RECHECK = 80;
    private static final int WATER_PICKUP_TICKS = 1;
    private static final int POWDER_SNOW_PICKUP_TICKS = 12;
    private static final int CLUTCH_TIMEOUT_TICKS = 80;
    private static long plannerBudgetTick = Long.MIN_VALUE;
    private static int plannersUsedThisTick;


    private static final Block[] BUILD_PALETTE = {
            Blocks.COBBLESTONE, Blocks.DIRT, Blocks.STONE, Blocks.COBBLED_DEEPSLATE, Blocks.TUFF,
    };

    private static final Map<UUID, BrainState> STATES = new ConcurrentHashMap<>();

    private SmartZombieBrain() {
    }

    private static final class BrainState {
        // Current plan and cursor.
        List<DiggingPathfinder.Move> plan;
        int planIndex;
        int replanCooldown;
        BlockPos planGoal;   // player pos the current plan was built for

        // Progressive digging.
        BlockPos digPos;
        double digProgress;
        int lastStage = -1;
        int swingTicks;
        boolean holdingPick;

        // Pillaring within a PILLAR move.
        int pillarPhase;   // 0 idle, 1 jumping
        int pillarTicks;
        int pillarBaseY;

        // Fast-path cache.
        boolean canWalkThere;
        int recheckTicks;

        // Stuck detection.
        double lastX, lastY, lastZ;
        int stuckTicks;
        double lastNodeDistance = Double.POSITIVE_INFINITY;

        // Miner-only bucket clutch.
        BlockPos clutchPos;
        boolean clutchWater;
        int clutchTicks;
        boolean clutchHadPick;

        // Collective navigation assignment. Recomputed infrequently and staggered.
        int groupTicks;
        int groupSize = 1;
        BlockPos groupGoal;
    }

    public static void forget(UUID id) {
        STATES.remove(id);
    }

    public static void cleanup(Zombie z) {
        BrainState st = STATES.remove(z.getUUID());
        if (st != null && z.level() instanceof ServerLevel level) {
            if (st.digPos != null) level.destroyBlockProgress(z.getId(), st.digPos, -1);
            cleanupClutch(level, z, st);
        }
        z.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        z.removeEffect(MobEffects.GLOWING);
    }

    public static void tickFallClutch(Zombie z) {
        if (!(z.level() instanceof ServerLevel level)) return;
        BrainState st = STATES.computeIfAbsent(z.getUUID(), k -> {
            BrainState fresh = new BrainState();
            fresh.recheckTicks = Math.floorMod(z.getId(), WALK_PATH_RECHECK);
            return fresh;
        });
        tickClutch(level, z, st);
    }

    public static void tick(Zombie z) {
        if (!(z.level() instanceof ServerLevel level)) {
            return;
        }
        // Vanilla already maintains a target. Reuse it instead of scanning every
        // player for every miner on every server tick.
        Player target = z.getTarget() instanceof Player p && p.isAlive() && !p.isSpectator()
                && !froz8n.combat.ZombieSerumSystem.isMasked(p)
                && z.distanceToSqr(p) <= followRange() * followRange() ? p : findTarget(level, z);
        if (target == null) {
            BrainState old = STATES.get(z.getUUID());
            if (old != null) {
                clearDig(z, level, old);
                putPickAway(z, old);
                z.removeEffect(MobEffects.GLOWING);
                z.setSprinting(false);
                z.getNavigation().stop();   // don't freeze holding a stale wanted-position
                if (old.clutchPos == null) STATES.remove(z.getUUID());
            }
            return;
        }

        z.setTarget(target);
        z.getLookControl().setLookAt(target, 30.0F, 30.0F);

        BrainState st = STATES.computeIfAbsent(z.getUUID(), k -> {
            BrainState fresh = new BrainState();
            fresh.recheckTicks = Math.floorMod(z.getId(), WALK_PATH_RECHECK);
            return fresh;
        });
        tickClutch(level, z, st);
        if (st.groupTicks-- <= 0) {
            updateGroupAssignment(level, z, target, st);
            st.groupTicks = 50 + Math.floorMod(z.getId(), 20);
        }
        boolean narrowTunnel=isNarrowTunnel(level,z.blockPosition());
        BlockPos pursuitGoal = !narrowTunnel && st.groupSize >= 3 && st.groupGoal != null
                && target.getY() - z.getY() > 1.25 ? st.groupGoal : target.blockPosition();
        if(!narrowTunnel)applyGroupSeparation(level, z, target, st.groupSize);

        // Busy states first: never interrupt an in-progress dig or pillar.
        if (st.digPos != null) {
            continueDig(z, level, st);
            return;
        }
        if (st.pillarPhase != 0) {
            continuePillar(z, level, st);
            return;
        }

        // Proactive head-block clear is only for cramped spaces. On open surface,
        // vanilla navigation gets the first chance so miners do not chip random leaves
        // or head-height blocks before they have actually proved to be stuck.
        BlockPos headBlock = narrowTunnel && ((z.tickCount + z.getId()) & 1) == 0
                ? smartmobs$headBlockToClear(z, level, pursuitGoal) : null;
        if (headBlock != null) {
            startDig(z, level, st, headBlock);
            return;
        }

        double dx = pursuitGoal.getX() + 0.5 - z.getX();
        double dz = pursuitGoal.getZ() + 0.5 - z.getZ();
        double dy = target.getY() - z.getY();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        if(z.isInWater()){
            if (tryExitWater(z, level, pursuitGoal)) {
                st.stuckTicks = 0;
                st.plan = null;
            } else {
                swimToward(z,target);
            }
            return;
        }

        // Close enough -> vanilla melee.
        if (horiz <= 1.7 && Math.abs(dy) < 2.0) {
            putPickAway(z, st);
            st.plan = null;
            moveToGoal(z, target, target.blockPosition());
            return;
        }

        // Fast path: plain walkable route (no digging) -> use vanilla navigation.
        if (st.recheckTicks-- <= 0) {
            Path p = z.getNavigation().createPath(pursuitGoal, 0);
            st.canWalkThere = p != null && p.canReach();
            st.recheckTicks = WALK_PATH_RECHECK;
        }
        if (st.canWalkThere) {
            putPickAway(z, st);
            st.plan = null;
            moveToGoal(z, target, pursuitGoal);
            return;
        }

        // VERTICAL SAFEGUARD: player is well above us and roughly overhead, and we
        // can't just walk there. Don't rely on the pathfinder finding stairs that
        // don't exist (that caused the "bounces off the canyon wall / runs below a
        // platform forever" bug) - come under the player and pillar straight up.
        if (dy >= 2.5 && horiz <= 1.75) {
            BlockPos above = z.blockPosition().above(2); // block blocking a jump
            if (!isOpen(level, above) && isBreakable(level, above)) {
                startDig(z, level, st, above);          // clear ceiling, then pillar next tick
            } else if (isOpen(level, above)) {
                st.plan = null;
                startPillar(z, level, st, null);        // build straight up toward the player
            } else {
                // ceiling is unbreakable right above: nudge aside to find a pillar spot
                z.getMoveControl().setWantedPosition(pursuitGoal.getX() + 0.5, z.getY(), pursuitGoal.getZ() + 0.5, 1.1);
            }
            return;
        }

        // Otherwise: execute a typed movement plan. Replan ONLY when we truly need to
        // (no plan / finished / player moved a lot / we're wedged) - re-planning every
        // tick was making the plan flicker and the zombie stutter.
        boolean needReplan = st.plan == null
                || st.planIndex >= st.plan.size()
                || st.stuckTicks > 40
                || (st.planGoal != null && st.planGoal.distManhattan(pursuitGoal) > 3.0);
        if (needReplan) {
            // A* is the expensive operation. At most two miners start it in one tick;
            // the others retain their current movement and get their turn next tick.
            // This removes 30-zombie server spikes without reducing path quality.
            if (!claimPlannerSlot(level.getGameTime())) {
                if (st.plan != null) executePlan(z, level, st, target, pursuitGoal);
                else moveToGoal(z, target, pursuitGoal);
                return;
            }
            st.plan = DiggingPathfinder.plan(level, z.blockPosition(), pursuitGoal);
            st.planIndex = 0;
            st.planGoal = pursuitGoal;
            st.stuckTicks = 0;
            st.lastNodeDistance = Double.POSITIVE_INFINITY;
        }
        executePlan(z, level, st, target, pursuitGoal);
    }

    /** Assigns every nearby miner a stable approach lane around the same player. */
    private static void updateGroupAssignment(ServerLevel level, Zombie self, Player target, BrainState st) {
        AABB area = self.getBoundingBox().inflate(18.0, 10.0, 18.0);
        List<Zombie> group = level.getEntitiesOfClass(Zombie.class, area, other ->
                other.isAlive() && froz8n.smart.SmartMobsEvents.isSmart(other)
                        && (other.getTarget() == target || other.distanceToSqr(target) < 24.0 * 24.0));
        group.sort(java.util.Comparator.comparingInt(net.minecraft.world.entity.Entity::getId));
        st.groupSize = group.size();
        if (group.size() < 3) {
            st.groupGoal = null;
            return;
        }
        int rank = group.indexOf(self);
        if (rank < 0) rank = Math.floorMod(self.getId(), group.size());
        // Eight independent columns/lanes. Extra mobs use a wider second ring, so a
        // 30-zombie horde does not try to occupy the same pillar footprint.
        int[][] ring = {{2,0},{2,2},{0,2},{-2,2},{-2,0},{-2,-2},{0,-2},{2,-2}};
        int[] slot = ring[rank & 7];
        int radiusScale = 1 + rank / 8;
        int ox = slot[0] * radiusScale;
        int oz = slot[1] * radiusScale;
        st.groupGoal = target.blockPosition().offset(ox, 0, oz);
    }

    /** Cheap close-range flock separation; prevents a crowd from pinning its builder. */
    private static void applyGroupSeparation(ServerLevel level, Zombie self, Player target, int groupSize) {
        if (groupSize < 3 || self.tickCount % 10 != Math.floorMod(self.getId(), 10)
                || !self.onGround()) return;
        List<Zombie> touching = level.getEntitiesOfClass(Zombie.class,
                self.getBoundingBox().inflate(0.55, 0.15, 0.55), other -> other != self
                        && froz8n.smart.SmartMobsEvents.isSmart(other));
        double px = 0, pz = 0;
        for (Zombie other : touching) {
            double dx = self.getX() - other.getX();
            double dz = self.getZ() - other.getZ();
            double d2 = dx*dx + dz*dz;
            if (d2 < 1.0E-4) { dx = ((self.getId() & 1) == 0 ? .01 : -.01); dz = .01; d2 = dx*dx+dz*dz; }
            if (d2 < 1.25) { double inv = 1.0 / Math.sqrt(d2); px += dx*inv; pz += dz*inv; }
        }
        if (px != 0 || pz != 0) {
            Vec3 m = self.getDeltaMovement();
            double len = Math.max(1.0, Math.sqrt(px*px+pz*pz));
            self.setDeltaMovement(m.x + px/len*.055, m.y, m.z + pz/len*.055);
        }
    }

    private static void moveToGoal(Zombie z, Player target, BlockPos goal) {
        if (!z.getNavigation().isDone()
                && z.tickCount % 10 != Math.floorMod(z.getId(), 10)) return;
        if (goal.equals(target.blockPosition())) z.getNavigation().moveTo(target, SPEED);
        else z.getNavigation().moveTo(goal.getX()+0.5, goal.getY(), goal.getZ()+0.5, SPEED);
    }

    private static boolean isNarrowTunnel(ServerLevel level,BlockPos feet){
        int open=0;
        for(net.minecraft.core.Direction d:net.minecraft.core.Direction.Plane.HORIZONTAL){
            if(isOpen(level,feet.relative(d))&&isOpen(level,feet.above().relative(d)))open++;
        }
        return open<=2&&isOpen(level,feet.above());
    }

    private static boolean tryExitWater(Zombie z,ServerLevel level,BlockPos goal){
        if (z.getFluidHeight(FluidTags.WATER) >= 0.85D) return false;
        BlockPos feet = z.blockPosition();
        net.minecraft.core.Direction bestDir = null;
        BlockPos bestExit = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos exit = waterExitCell(level, feet, d);
            if (exit != null) {
                double dx = goal.getX() + 0.5 - (exit.getX() + 0.5);
                double dz = goal.getZ() + 0.5 - (exit.getZ() + 0.5);
                double score = dx * dx + dz * dz + Math.abs(goal.getY() - exit.getY()) * 1.5;
                if (score < bestScore) {
                    bestScore = score;
                    bestDir = d;
                    bestExit = exit;
                }
            }
        }
        if (bestDir == null) return false;

        if (!z.getNavigation().isDone()) z.getNavigation().stop();
        z.getMoveControl().setWantedPosition(bestExit.getX() + 0.5, bestExit.getY(), bestExit.getZ() + 0.5, 1.25);
        Vec3 current = z.getDeltaMovement();
        double lift = bestExit.getY() > feet.getY() ? 0.30D : 0.18D;
        Vec3 push = new Vec3(bestDir.getStepX() * 0.24D, Math.max(current.y, lift), bestDir.getStepZ() * 0.24D);
        z.setDeltaMovement(push);
        z.move(MoverType.SELF, new Vec3(bestDir.getStepX() * 0.08D, 0.0D, bestDir.getStepZ() * 0.08D));
        z.setJumping(true);
        z.setSprinting(true);
        z.setSwimming(false);
        z.setPose(Pose.STANDING);
        return true;
    }

    private static BlockPos waterExitCell(ServerLevel level,BlockPos feet,net.minecraft.core.Direction d){
        BlockPos same = feet.relative(d);
        if (isStandableCell(level, same)) return same;
        BlockPos up = same.above();
        if (level.getBlockState(same).blocksMotion() && isStandableCell(level, up)) return up;
        return null;
    }

    private static boolean isStandableCell(ServerLevel level,BlockPos pos){
        return isOpen(level, pos) && isOpen(level, pos.above()) && level.getBlockState(pos.below()).blocksMotion();
    }

    private static void swimToward(Zombie z,Player target){
        if (!z.getNavigation().isDone()) z.getNavigation().stop();
        Vec3 to=target.getEyePosition().subtract(z.position().add(0,z.getBbHeight()*.55,0));
        double len=Math.max(.001,to.length());
        Vec3 wanted=to.scale(1.0/len);
        Vec3 desired=wanted.scale(SmartMobsEvents.SWIM_SPEED);
        z.setDeltaMovement(desired);
        faceVector(z, wanted, 75.0F);
        z.getLookControl().setLookAt(target,75,75);
        z.setSprinting(true);
        z.setSwimming(true);
        z.setPose(Pose.SWIMMING);
    }

    private static void faceVector(Zombie z, Vec3 direction, float maxPitch) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = Mth.wrapDegrees((float)(Mth.atan2(direction.z, direction.x) * 180.0F / Math.PI) - 90.0F);
        float pitch = Mth.clamp(Mth.wrapDegrees((float)(-(Mth.atan2(direction.y, horizontal) * 180.0F / Math.PI))), -maxPitch, maxPitch);
        z.setYRot(yaw);
        z.setYHeadRot(yaw);
        z.setYBodyRot(yaw);
        z.setXRot(pitch);
    }

    private static synchronized boolean claimPlannerSlot(long gameTime) {
        if (plannerBudgetTick != gameTime) {
            plannerBudgetTick = gameTime;
            plannersUsedThisTick = 0;
        }
        if ((gameTime & 1L) != 0L || plannersUsedThisTick >= 1) return false;
        plannersUsedThisTick++;
        return true;
    }

    // ==================================================================
    // Plan execution (dumb, per-move)
    // ==================================================================

    private static void executePlan(Zombie z, ServerLevel level, BrainState st, Player target, BlockPos pursuitGoal) {
        if (st.plan == null || st.plan.isEmpty()) {
            moveToGoal(z, target, pursuitGoal);
            return;
        }

        // Advance past moves already completed.
        while (st.planIndex < st.plan.size() && reached(z, st.plan.get(st.planIndex).target)) {
            st.planIndex++;
            st.stuckTicks = 0;
            st.lastNodeDistance = Double.POSITIVE_INFINITY;
        }
        if (st.planIndex >= st.plan.size()) {
            moveToGoal(z, target, pursuitGoal);
            st.plan = null;
            return;
        }

        DiggingPathfinder.Move move = st.plan.get(st.planIndex);

        // If this move requires digging blocks, mine them first (nearest still-solid one).
        if (move.dig != null && !move.dig.isEmpty()) {
            for (BlockPos p : move.dig) {
                if (!isOpen(level, p) && isBreakable(level, p)) {
                    if (distToBlock(z, p) <= DIG_REACH) {
                        startDig(z, level, st, p);
                    } else {
                        driveToward(z, move.target, false); // walk into reach
                    }
                    trackStuck(z, level, st);
                    return;
                }
            }
        }

        // Baritone-style DIRECT control: we steer the mob ourselves (look + forward
        // impulse + jump), NOT via getNavigation() - mixing the two is what made it
        // stutter and freeze. Vanilla navigation is only used for the plain fast-path.
        double cx = move.target.getX() + 0.5;
        double cz = move.target.getZ() + 0.5;
        double hx = cx - z.getX();
        double hz = cz - z.getZ();
        double len = Math.sqrt(hx * hx + hz * hz);

        switch (move.type) {
            case WALK, DIG, DIAGONAL -> driveToward(z, move.target, false);
            case STEP_DOWN, DESCEND -> {
                driveToward(z, move.target, false);
                // MoveControl is reluctant to walk over a ledge and can stop with the
                // mob's nose at the edge forever. A* has already verified clearance
                // and a landing for STEP_DOWN, so give it a small physical nudge into
                // the destination cell once it is close to the edge.
                if (z.onGround() && len < 1.25 && len > 0.05) {
                    Vec3 velocity = z.getDeltaMovement();
                    double push = 0.16;
                    z.setDeltaMovement(hx / len * push, velocity.y, hz / len * push);
                    // Apply the ledge step immediately as well. A queued velocity is
                    // often erased by MoveControl or by another miner crowding the
                    // same one-block tunnel before the physics tick gets to use it.
                    z.move(MoverType.SELF, new Vec3(hx / len * push, 0.0, hz / len * push));
                }
            }
            case DROP -> executeDrop(z, move.target);
            case STEP_UP -> {
                driveToward(z, move.target, false);
                // Jump as soon as we're reasonably near the step (vanilla step-assist
                // handles small rises, but an explicit jump makes it reliable).
                if (z.onGround() && len < 2.0) {
                    z.setJumping(true);
                }
            }
            case JUMP -> {
                // Parkour: sprint straight at the landing and LAUNCH while still on the
                // source block (as soon as we're airborne-ready), with a horizontal
                // impulse scaled to the gap so we clear it. Don't wait to be "close" -
                // over a gap you can never get close on the ground.
                driveToward(z, move.target, true);
                if (z.onGround() && len > 0.05) {
                    double dirx = hx / len;
                    double dirz = hz / len;
                    // Impulse grows with gap length (clamped); enough for up to a 4-block jump.
                    double boost = Math.min(0.34 + len * 0.10, 0.62);
                    z.setDeltaMovement(dirx * boost, JUMP_VELOCITY, dirz * boost);
                    z.setJumping(true);
                }
            }
            case BRIDGE -> {
                if (move.place != null && isOpen(level, move.place) && len < 1.5) {
                    placeTempBlock(level, z, move.place);
                }
                driveToward(z, move.target, false);
            }
            case PILLAR -> startPillar(z, level, st, move);
        }

        trackStuck(z, level, st);
    }

    private static void executeDrop(Zombie z, BlockPos target) {
        double cx = target.getX() + 0.5;
        double cz = target.getZ() + 0.5;
        double dx = cx - z.getX();
        double dz = cz - z.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (!z.getNavigation().isDone()) z.getNavigation().stop();
        if (horizontal > 0.05) {
            double inv = 1.0 / horizontal;
            Vec3 m = z.getDeltaMovement();
            double push = z.onGround() ? 0.34 : 0.18;
            z.setDeltaMovement(dx * inv * push, Math.min(m.y, z.onGround() ? 0.02 : -0.08), dz * inv * push);
            z.move(MoverType.SELF, new Vec3(dx * inv * 0.18, 0.0, dz * inv * 0.18));
            z.setSprinting(true);
            return;
        }
        Vec3 m = z.getDeltaMovement();
        z.setDeltaMovement(dx * 0.28, Math.min(m.y, -0.08), dz * 0.28);
        z.setSprinting(true);
    }

    /**
     * Directly drives the zombie toward a target cell like Baritone: face it and press
     * "forward". Uses the mob's own movement (setZza + setSpeed) so vanilla physics,
     * collision and step-assist all apply - no pathfinder involved.
     */
    private static void driveToward(Zombie z, BlockPos target, boolean sprint) {
        double cx = target.getX() + 0.5;
        double cy = target.getY();
        double cz = target.getZ() + 0.5;

        if(z.level() instanceof ServerLevel level&&isNarrowTunnel(level,z.blockPosition())
                &&hasMinerAhead(level,z,target)){
            z.getNavigation().stop();
            z.setSprinting(false);
            return;
        }

        // Use the mob's OWN move controller. setZza/setSpeed get stomped by the vanilla
        // AI goals every tick (that's why it barely crept and drifted); the move
        // controller cooperates with the goal system and reliably walks the mob to a
        // point using proper physics + step assist.
        // Stop stale navigation BEFORE issuing the direct MoveControl command.
        // PathNavigation.stop() also stops MoveControl; doing this afterwards erased
        // the wanted position and left the zombie standing/replanning forever.
        if (!z.getNavigation().isDone()) {
            z.getNavigation().stop();
        }
        z.getMoveControl().setWantedPosition(cx, cy, cz, sprint ? 1.5 : 1.1);
        z.setSprinting(sprint);
    }

    private static boolean hasMinerAhead(ServerLevel level,Zombie self,BlockPos target){
        double sx=self.distanceToSqr(target.getX()+.5,target.getY(),target.getZ()+.5);
        return !level.getEntitiesOfClass(Zombie.class,self.getBoundingBox().inflate(1.15,.35,1.15),other->
                other!=self&&froz8n.smart.SmartMobsEvents.isSmart(other)
                        &&other.distanceToSqr(target.getX()+.5,target.getY(),target.getZ()+.5)<sx-.12).isEmpty();
    }

    /**
     * A move is "reached" only when the zombie is actually standing IN that cell:
     * same feet Y (exactly) and horizontally over it. Matching the exact Y is
     * essential - the old fuzzy +/-0.7 height check let a whole vertical stack of
     * moves (e.g. climbing a tree/column, same X/Z) collapse into one, so the
     * executor ended up aiming at a node 4 blocks up and froze. Being strict on Y
     * means it completes one vertical step at a time.
     */
    private static boolean reached(Zombie z, BlockPos t) {
        BlockPos feet = z.blockPosition();
        return feet.getX() == t.getX() && feet.getZ() == t.getZ()
                && Mth.floor(z.getY() + 0.05) == t.getY();
    }

    private static void trackStuck(Zombie z, ServerLevel level, BrainState st) {
        double moved = Math.abs(z.getX() - st.lastX) + Math.abs(z.getY() - st.lastY) + Math.abs(z.getZ() - st.lastZ);
        st.lastX = z.getX();
        st.lastY = z.getY();
        st.lastZ = z.getZ();

        // Measure useful progress toward the active node. Collision jitter can move
        // the entity by >0.015 every tick while it remains pinned to the same wall;
        // the old detector treated that as progress forever and never cleared it.
        double nodeDistance = Double.POSITIVE_INFINITY;
        if (st.plan != null && st.planIndex < st.plan.size()) {
            BlockPos target = st.plan.get(st.planIndex).target;
            double dx = target.getX() + 0.5 - z.getX();
            double dy = target.getY() - z.getY();
            double dz = target.getZ() + 0.5 - z.getZ();
            nodeDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        boolean madeProgress = nodeDistance + 0.01 < st.lastNodeDistance;
        st.lastNodeDistance = nodeDistance;
        if (!madeProgress || moved < 0.005) {
            st.stuckTicks++;
        } else {
            st.stuckTicks = Math.max(0, st.stuckTicks - 3);
        }
        // Wedged for a while and not mid-action. Before blindly re-planning (which
        // often yields the same failing plan and loops forever), directly clear
        // whatever solid block is pinning the body - especially a HEAD-level block,
        // which was the classic "digs at feet, not at head, stays stuck" case.
        if (st.stuckTicks > 12 && st.digPos == null && st.pillarPhase == 0) {
            if (smartmobs$forceClearBlocking(z, level, st)) {
                st.stuckTicks = 0;
                return;
            }
            st.plan = null;
            st.replanCooldown = 0;
            st.stuckTicks = 0;
        }
    }

    /**
     * When wedged, mine the first solid block pinning the zombie: its own head cell,
     * then the cells directly ahead at head and feet height (in the facing/move
     * direction). Returns true if it started digging one. This is the safety net that
     * guarantees a head-level obstacle is never ignored.
     */
    private static boolean smartmobs$forceClearBlocking(Zombie z, ServerLevel level, BrainState st) {
        BlockPos feet = z.blockPosition();
        // Direction we're trying to move: toward the current plan target if any,
        // else the way we're facing.
        net.minecraft.core.Direction dir;
        if (st.plan != null && st.planIndex < st.plan.size()) {
            BlockPos t = st.plan.get(st.planIndex).target;
            int dx = Integer.signum(t.getX() - feet.getX());
            int dz = Integer.signum(t.getZ() - feet.getZ());
            dir = Math.abs(t.getX() - feet.getX()) >= Math.abs(t.getZ() - feet.getZ())
                    ? (dx >= 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST)
                    : (dz >= 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH);
        } else {
            dir = z.getDirection();
        }

        // Priority: block right at head, then forward-head, forward-feet, then head above.
        BlockPos[] candidates = {
                feet.above().relative(dir),   // forward at head height  <-- the missing case
                feet.relative(dir),           // forward at feet height
                feet.above(),                 // our own head cell (low ceiling)
                feet.above(2),                // block above head (for stepping up)
        };
        for (BlockPos p : candidates) {
            if (!isOpen(level, p) && isBreakable(level, p) && distToBlock(z, p) <= DIG_REACH) {
                startDig(z, level, st, p);
                return true;
            }
        }
        return false;
    }

    /**
     * Proactive, per-tick head-level obstacle detector. Returns a breakable block that is
     * blocking the zombie's HEAD (either its own head cell, or the head-height cell in the
     * direction it is trying to travel) - but ONLY when its feet-level path forward is
     * otherwise open, i.e. the ONLY thing stopping it is the head block. This is the exact
     * 1-tall-tunnel case: feet cell ahead is clear, head cell ahead is solid, so the mob
     * would walk straight into it forever. Returns null when there's no such obstacle, so
     * normal walking is never interrupted.
     */
    private static BlockPos smartmobs$headBlockToClear(Zombie z, ServerLevel level, BlockPos moveTarget) {
        BlockPos feet = z.blockPosition();

        // Travel direction: toward the current move target, else facing.
        net.minecraft.core.Direction dir;
        int tdx = moveTarget.getX() - feet.getX();
        int tdz = moveTarget.getZ() - feet.getZ();
        if (tdx != 0 || tdz != 0) {
            dir = Math.abs(tdx) >= Math.abs(tdz)
                    ? (tdx >= 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST)
                    : (tdz >= 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH);
        } else {
            dir = z.getDirection();
        }

        BlockPos headAhead = feet.above().relative(dir);  // head-height cell in front
        BlockPos feetAhead = feet.relative(dir);          // feet-height cell in front
        BlockPos ownHead   = feet.above();                // our own head cell (low ceiling)

        // Case A: 1-tall tunnel - feet ahead is walkable but head ahead is a solid,
        // breakable block. This is the reported bug: "digs at feet, not head".
        if (isOpen(level, feetAhead) && !isOpen(level, headAhead)
                && isBreakable(level, headAhead) && distToBlock(z, headAhead) <= DIG_REACH) {
            return headAhead;
        }

        // Case B: a block dropped right on top of our own head (low ceiling), pinning us.
        if (!isOpen(level, ownHead) && isBreakable(level, ownHead)
                && distToBlock(z, ownHead) <= DIG_REACH) {
            return ownHead;
        }

        return null;
    }

    // ==================================================================
    // Pillaring (realistic, block-by-block)
    // ==================================================================

    private static void startPillar(Zombie z, ServerLevel level, BrainState st, DiggingPathfinder.Move move) {
        // If the pillar move also needs a ceiling dug, that was handled by move.dig above.
        BlockPos cell = z.blockPosition();
        double centerX = cell.getX() + 0.5;
        double centerZ = cell.getZ() + 0.5;
        double offX = centerX - z.getX();
        double offZ = centerZ - z.getZ();
        if (offX * offX + offZ * offZ > 0.035) {
            z.getNavigation().stop();
            z.getMoveControl().setWantedPosition(centerX, z.getY(), centerZ, 0.9);
            return;
        }
        st.pillarPhase = 1;
        st.pillarTicks = 0;
        st.pillarBaseY = Mth.floor(z.getY());
        z.getNavigation().stop();
        if (z.onGround()) {
            z.setDeltaMovement(0.0, JUMP_VELOCITY, 0.0);
            z.setJumping(true);
        }
    }

    private static void continuePillar(Zombie z, ServerLevel level, BrainState st) {
        st.pillarTicks++;

        // Phase 2: the support exists; wait until physics has actually landed the
        // zombie one block higher before advancing the A* cursor.
        if (st.pillarPhase == 2) {
            if (z.onGround() && Mth.floor(z.getY() + 0.05) >= st.pillarBaseY + 1) {
                st.pillarPhase = 0;
                st.pillarTicks = 0;
                if (st.plan != null && st.planIndex < st.plan.size()) {
                    st.planIndex++;
                    st.lastNodeDistance = Double.POSITIVE_INFINITY;
                }
                return;
            }
            if (st.pillarTicks > PILLAR_TIMEOUT) {
                st.pillarPhase = 0;
                st.plan = null;
            }
            return;
        }
        int cx = Mth.floor(z.getX());
        int cz = Mth.floor(z.getZ());
        BlockPos support = new BlockPos(cx, st.pillarBaseY, cz);

        // Damp horizontal drift to stay over the column.
        Vec3 m = z.getDeltaMovement();
        z.setDeltaMovement(m.x * 0.2, m.y, m.z * 0.2);

        // At the apex, place the support block, then finish this pillar step.
        if (m.y <= 0.08 && isOpen(level, support)) {
            placeTempBlock(level, z, support);
            if (!isOpen(level, support)) {
                z.setDeltaMovement(m.x * 0.2, Math.min(m.y, 0.0), m.z * 0.2);
                st.pillarPhase = 2;
                st.pillarTicks = 0;
            } else {
                st.pillarPhase = 0;
                st.plan = null;
            }
            return;
        }
        if (st.pillarTicks > PILLAR_TIMEOUT) {
            st.pillarPhase = 0;
            st.plan = null; // give up, replan
        }
    }

    // ==================================================================
    // Progressive digging
    // ==================================================================

    private static void startDig(Zombie z, ServerLevel level, BrainState st, BlockPos pos) {
        st.digPos = pos.immutable();
        st.digProgress = 0.0;
        st.lastStage = -1;
        st.swingTicks = 0;
        equipPickFor(z, level.getBlockState(pos), level, pos, st);
        z.getNavigation().stop();
        z.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static void continueDig(Zombie z, ServerLevel level, BrainState st) {
        BlockPos pos = st.digPos;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level, pos) < 0) {
            clearDig(z, level, st);
            return;
        }
        z.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (!z.getNavigation().isDone()) z.getNavigation().stop();
        st.digProgress += digDelta(z, state, level, pos);

        if (st.swingTicks-- <= 0) {
            z.swing(InteractionHand.MAIN_HAND);
            SoundType s = state.getSoundType();
            level.playSound(null, pos, s.getHitSound(), SoundSource.HOSTILE,
                    (s.getVolume() + 1.0F) / 8.0F, s.getPitch() * 0.5F);
            st.swingTicks = SWING_INTERVAL;
        }
        int stage = Math.min(9, (int) (st.digProgress * 10.0));
        if (stage != st.lastStage) {
            level.destroyBlockProgress(z.getId(), pos, stage);
            st.lastStage = stage;
        }
        if (st.digProgress >= 1.0) {
            SoundType s = state.getSoundType();
            level.playSound(null, pos, s.getBreakSound(), SoundSource.HOSTILE,
                    (s.getVolume() + 1.0F) / 2.0F, s.getPitch() * 0.8F);
            level.destroyBlock(pos, false, z, 512);
            level.destroyBlockProgress(z.getId(), pos, -1);
            st.digPos = null;
            st.lastStage = -1;
            st.digProgress = 0.0;
            st.stuckTicks = 0;
        }
    }

    private static double digDelta(Zombie z, BlockState state, ServerLevel level, BlockPos pos) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness <= 0.0F) {
            return 1.0;
        }
        ItemStack pick = z.getMainHandItem();
        float speed = pick.isEmpty() ? 1.0F : pick.getDestroySpeed(state);
        boolean correct = !pick.isEmpty() && pick.isCorrectToolForDrops(state);
        return speed / hardness / (correct ? 10.0 : 20.0);
    }

    private static void clearDig(Zombie z, ServerLevel level, BrainState st) {
        if (st.digPos != null) {
            level.destroyBlockProgress(z.getId(), st.digPos, -1);
            st.digPos = null;
        }
        st.lastStage = -1;
        st.digProgress = 0.0;
    }

    // ==================================================================
    // Pickaxe
    // ==================================================================

    private static void equipPickFor(Zombie z, BlockState state, ServerLevel level, BlockPos pos, BrainState st) {
        ItemStack held = z.getMainHandItem();
        if (st.holdingPick && held.is(Items.IRON_PICKAXE)) {
            z.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            return;
        }
        // A plain iron pickaxe. The old Efficiency V one chewed through stone almost
        // instantly, which is what made a wall pointless.
        ItemStack tool=new ItemStack(Items.IRON_PICKAXE);
        z.setItemSlot(EquipmentSlot.MAINHAND,tool);
        z.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        st.holdingPick = true;
    }

    private static void putPickAway(Zombie z, BrainState st) {
        if (st.holdingPick) {
            z.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            st.holdingPick = false;
        }
    }

    private static void tickClutch(ServerLevel level, Zombie z, BrainState st) {
        if (st.clutchPos != null) {
            st.clutchTicks++;
            boolean touchedWater = st.clutchWater && (z.isInWater()
                    || (st.clutchTicks >= WATER_PICKUP_TICKS && touchesClutch(z, st.clutchPos)));
            boolean usedPowderSnow = !st.clutchWater && st.clutchTicks >= POWDER_SNOW_PICKUP_TICKS
                    && (z.onGround() || z.getDeltaMovement().y >= -0.08 || touchesClutch(z, st.clutchPos));
            if (touchedWater || usedPowderSnow || st.clutchTicks > CLUTCH_TIMEOUT_TICKS) {
                cleanupClutch(level, z, st);
            }
            return;
        }
        if (z.onGround() || z.isInWater() || z.getDeltaMovement().y >= -0.22 || z.fallDistance < 2.0F) return;
        BlockPos landing = clutchLanding(level, z);
        if (landing == null || !isOpen(level, landing)) return;
        boolean water = canUseWaterClutch(level, landing) && z.getRandom().nextBoolean();
        BlockState cushion = water ? Blocks.WATER.defaultBlockState() : Blocks.POWDER_SNOW.defaultBlockState();
        z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(water ? Items.WATER_BUCKET : Items.POWDER_SNOW_BUCKET));
        st.clutchHadPick = st.holdingPick;
        st.holdingPick = false;
        if (level.setBlock(landing, cushion, Block.UPDATE_ALL)) {
            st.clutchPos = landing.immutable();
            st.clutchWater = water;
            st.clutchTicks = 0;
            z.resetFallDistance();
            level.playSound(null, landing, water ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_EMPTY_POWDER_SNOW,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            z.setItemSlot(EquipmentSlot.MAINHAND, st.clutchHadPick ? new ItemStack(Items.IRON_PICKAXE) : ItemStack.EMPTY);
            st.holdingPick = st.clutchHadPick;
        }
    }

    private static boolean touchesClutch(Zombie z, BlockPos pos) {
        return z.getBoundingBox().intersects(new AABB(pos));
    }

    private static BlockPos clutchLanding(ServerLevel level, Zombie z) {
        int x = Mth.floor(z.getX());
        int zPos = Mth.floor(z.getZ());
        int startY = Mth.floor(z.getY());
        int minY = level.getMinY();
        for (int y = startY; y >= minY; y--) {
            BlockPos feet = new BlockPos(x, y, zPos);
            if (!isOpen(level, feet)) return null;
            BlockPos below = feet.below();
            if (!isOpen(level, below)) return feet;
        }
        return null;
    }

    private static boolean canUseWaterClutch(ServerLevel level, BlockPos pos) {
        return !level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, pos);
    }

    private static void cleanupClutch(ServerLevel level, Zombie z, BrainState st) {
        if (st.clutchPos != null) {
            BlockState state = level.getBlockState(st.clutchPos);
            boolean ownWater = st.clutchWater && state.getFluidState().is(Fluids.WATER);
            boolean ownSnow = !st.clutchWater && state.is(Blocks.POWDER_SNOW);
            if (ownWater || ownSnow) {
                level.setBlock(st.clutchPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, st.clutchPos, st.clutchWater ? SoundEvents.BUCKET_FILL : SoundEvents.BUCKET_FILL_POWDER_SNOW,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            st.clutchPos = null;
            st.clutchTicks = 0;
        }
        z.resetFallDistance();
        z.setItemSlot(EquipmentSlot.MAINHAND, st.clutchHadPick ? new ItemStack(Items.IRON_PICKAXE) : ItemStack.EMPTY);
        st.holdingPick = st.clutchHadPick;
        st.clutchHadPick = false;
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private static Player findTarget(ServerLevel level, Zombie z) {
        Player best = null;
        double bestSq = followRange() * followRange();
        for (Player p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()
                    || froz8n.combat.ZombieSerumSystem.isMasked(p)) {
                continue;
            }
            double dsq = z.distanceToSqr(p);
            if (dsq < bestSq) {
                bestSq = dsq;
                best = p;
            }
        }
        return best;
    }

    /**
     * Places a temporary (no-drop) block, but NEVER inside the zombie's own body
     * cells - placing there was walling the zombie into its own build and trapping it.
     */
    private static void placeTempBlock(ServerLevel level, Zombie z, BlockPos pos) {
        BlockPos feet = z.blockPosition();
        if (pos.equals(feet) || pos.equals(feet.above())) {
            return; // refuse to entomb ourselves
        }
        if (!isOpen(level, pos)) {
            return; // don't overwrite an existing block
        }
        Block block = BUILD_PALETTE[z.getRandom().nextInt(BUILD_PALETTE.length)];
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        TempBlockManager.track(level, pos);
    }

    private static boolean isOpen(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || (!s.blocksMotion() && s.getFluidState().isEmpty());
    }

    private static boolean isBreakable(ServerLevel level, BlockPos pos) {
        if (!froz8n.Config.allowDigging) return false;
        BlockState s = level.getBlockState(pos);
        float hardness = s.getDestroySpeed(level, pos);
        // No hardness ceiling by default: a cap just means "wall yourself in with the one
        // block they cannot chew" and the mod is over. Set maxDigHardness >= 0 to opt in.
        if (froz8n.Config.maxDigHardness >= 0.0 && hardness > froz8n.Config.maxDigHardness) return false;
        return !s.isAir() && s.getFluidState().isEmpty()
                && hardness >= 0.0F && s.blocksMotion();
    }

    private static double horizTo(Zombie z, BlockPos pos) {
        double dx = z.getX() - (pos.getX() + 0.5);
        double dz = z.getZ() - (pos.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double distToBlock(Zombie z, BlockPos pos) {
        double dx = z.getX() - (pos.getX() + 0.5);
        double dy = z.getEyeY() - (pos.getY() + 0.5);
        double dz = z.getZ() - (pos.getZ() + 0.5);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

}
