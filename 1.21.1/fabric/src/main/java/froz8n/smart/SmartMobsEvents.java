package froz8n.smart;

import com.mojang.brigadier.CommandDispatcher;
import froz8n.SmartMobs;
import froz8n.data.Nbt;
import froz8n.data.PersistentData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * All gameplay event handlers for SmartMobs: the {@code /spawnsmart} command, the
 * per-tick AI driver, temporary-block ageing and the no-drop rule for mob-placed blocks.
 *
 * <p>On Fabric the handlers come from three places: Fabric API callbacks (commands, server
 * tick, block break, damage), and two mixins ({@code LivingEntityTickMixin},
 * {@code ServerLevelMixin}) for the two hooks Fabric API does not expose - the per-entity
 * tick and a cancellable entity spawn.
 */
public final class SmartMobsEvents {

    /** Persistent-data flag marking an entity as "smart". */
    public static final String SMART_KEY = "smartmobs_smart";
    public static final String GARDEN_KEY = "smartmobs_garden_zombie";
    private static final String LEGACY_BOX_KEY = "smartmobs_box_zombie";
    private static final String LEGACY_BOX_SHIELD_KEY = "smartmobs_box_shield";
    /** Direct water pursuit velocity. Used by smart and regular zombies. */
    public static final double SWIM_SPEED = 0.15D;

    private SmartMobsEvents() {
    }

    /** Wire all gameplay handlers onto their respective Fabric events. */
    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> onRegisterCommands(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(SmartMobsEvents::onServerTick);
        // Returning false cancels the break, so nothing drops.
        PlayerBlockBreakEvents.BEFORE.register(SmartMobsEvents::onBlockBreak);
        // Forge fired three separate LivingHurtEvent listeners; ALLOW_DAMAGE is the single
        // Fabric hook, so the three suppressors are asked in the same order here.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(SmartMobsEvents::allowDamage);
    }

    private static void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("spawnsmart")
                        // Plain op level 2; the PermissionCheck API only arrives in 1.21.11.
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("zombie")
                                .executes(ctx -> spawnSmartZombie(ctx.getSource())))
        );
    }

    private static int spawnSmartZombie(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 p = source.getPosition();
        BlockPos pos = BlockPos.containing(p);

        Entity entity = EntityType.ZOMBIE.spawn(level, pos, MobSpawnType.COMMAND);
        if (!(entity instanceof Zombie zombie)) {
            source.sendSuccess(() -> Component.literal("Failed to spawn smart zombie."), false);
            return 0;
        }
        if (zombie.isBaby()) zombie.setBaby(false);

        makeSmart(zombie);
        source.sendSuccess(() -> Component.literal("Spawned a smart miner zombie."), true);
        return 1;
    }

    private static void makeSmart(Zombie zombie) {
        PersistentData.of(zombie).putBoolean(SMART_KEY, true);
        zombie.setPersistenceRequired();
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SmartMobs.MINING_HELMET));
        damageHat(zombie.getItemBySlot(EquipmentSlot.HEAD),zombie);
        zombie.setDropChance(EquipmentSlot.HEAD, 0.05F);
    }

    private static void makeGarden(Zombie zombie){
        PersistentData.of(zombie).putBoolean(GARDEN_KEY,true);
        zombie.setPersistenceRequired();
        zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(SmartMobs.GARDEN_HAT));
        damageHat(zombie.getItemBySlot(EquipmentSlot.HEAD),zombie);
        zombie.setDropChance(EquipmentSlot.HEAD,.05F);
    }

    private static void damageHat(ItemStack stack,Zombie zombie){
        if(!stack.isDamageableItem())return;
        int remaining=Math.max(1,Math.round(stack.getMaxDamage()*(.05F+zombie.getRandom().nextFloat()*.25F)));
        stack.setDamageValue(Math.max(0,stack.getMaxDamage()-remaining));
    }

    /**
     * Cancels the spawn of any baby zombie and applies random gear to adult zombies.
     * Called from {@code ServerLevelMixin} before the entity is added.
     *
     * @return {@code true} to cancel the spawn (the entity is never added to the level).
     */
    public static boolean onEntityJoinLevel(ServerLevel level, Entity joining) {
        if (!(joining instanceof Zombie zombie)) return false;

        // Block ALL baby zombies from spawning, regardless of variant or equipment.
        if (zombie.isBaby()) {
            return true; // cancel the spawn -> baby zombie never appears
        }

        if (zombie.getType() != EntityType.ZOMBIE) return false;
        if (isSmartMobZombie(zombie) || ZombieBreeds.isBreed(zombie)) return false;
        double roll = zombie.getRandom().nextDouble();
        if (roll < froz8n.Config.gardenChance) makeGarden(zombie);
        else if (roll < froz8n.Config.gardenChance + froz8n.Config.smartChance) makeSmart(zombie);
        else ZombieBreeds.assign(zombie);
        return false;
    }

    /** Called from {@code LivingEntityTickMixin} for every living entity, on both sides. */
    public static void onLivingTick(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            // The humanoid renderer builds swimAmount locally from this pose. Keep it
            // in step with the server so zombies use the swimming model animation.
            if (entity instanceof Zombie zombie && zombie.getType() == EntityType.ZOMBIE) syncSwimmingPose(zombie);
            return;
        }
        if (entity instanceof Zombie zombie) {
            // Safety net: if anything turned this zombie into a baby after it joined,
            // remove it so no baby zombie (any variant, hat or helmet included) survives.
            if (zombie.isBaby()) {
                zombie.discard();
                return;
            }
            if (zombie.getType() != EntityType.ZOMBIE) return;
            clearLegacyBoxZombie(zombie);
            boolean smartMob = isSmartMobZombie(zombie);
            if (smartMob) {
                // SmartMobs use the tuned day/night pace and may stay outdoors.
                // Updating once per second avoids needless attribute work.
                if ((zombie.tickCount + zombie.getId()) % 20 == 0) {
                    var speed=zombie.getAttribute(Attributes.MOVEMENT_SPEED);
                    if(speed!=null) speed.setBaseValue(SmartMobWorldRules.isNightLike(zombie.level())
                            ?SmartZombieBrain.nightMoveSpeed():SmartZombieBrain.dayMoveSpeed());
                    // Vanilla drops a target once it leaves follow range, which would undo
                    // the detection radius the moment a wall got between you.
                    var follow=zombie.getAttribute(Attributes.FOLLOW_RANGE);
                    if(follow!=null) follow.setBaseValue(froz8n.Config.detectionRange);
                }
            }
            syncSwimmingPose(zombie);
            // Daylight is the player's ally again: only the hat wearers survive it, and
            // that is plain vanilla behaviour for a mob with a head item.
            if(froz8n.Config.sunlightImmunity&&!zombie.isInLava()&&zombie.getRemainingFireTicks()>0)zombie.clearFire();
            if(Nbt.getBooleanOr(PersistentData.of(zombie), GARDEN_KEY,false))
                froz8n.combat.GardenZombieSystem.tickGarden(zombie);
            froz8n.combat.ZombieSerumSystem.tickZombie(zombie);
            froz8n.combat.SoundJammerSystem.tickZombie(zombie);
            if (froz8n.combat.SoundJammerSystem.isControlled(zombie)) return;
            ZombieBreeds.tick(zombie);
            if(SmartMobWorldRules.tryBreakVisiblePortal(zombie)) return;
            boolean smart = isSmart(zombie);
            if (smart) SmartZombieBrain.tickFallClutch(zombie);
            if(!smart&&zombie.isInWater()&&zombie.getTarget() instanceof net.minecraft.world.entity.player.Player target)
                swimLikePlayer(zombie,target);
            if (!smart) {
                return;
            }
            // Persistence and the helmet are the visible, durable identity of a miner.
            // Durable identity maintenance does not need to allocate/check equipment
            // 20 times per second for every mob. Stagger it across a 2-second window.
            if ((zombie.tickCount + zombie.getId()) % 40 == 0) {
                zombie.setPersistenceRequired();
                if (!zombie.getItemBySlot(EquipmentSlot.HEAD).is(SmartMobs.MINING_HELMET)) {
                    zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SmartMobs.MINING_HELMET));
                }
                zombie.setDropChance(EquipmentSlot.HEAD, 0.05F);
            }
            // When the smart zombie dies or is being removed, clean up its brain
            // state, remove the crack overlay, drop the pickaxe from its hand and
            // clear the glow effect.
            if (!zombie.isAlive() || zombie.isRemoved()) {
                SmartZombieBrain.cleanup(zombie);
                return;
            }
            SmartZombieBrain.tick(zombie);
        }
        if(entity instanceof net.minecraft.world.entity.player.Player player)
            froz8n.combat.GardenZombieSystem.tickRooted(player);
    }

    private static void onServerTick(MinecraftServer server) {
        froz8n.combat.SoundJammerSystem.tickFields(server);
        froz8n.combat.GardenZombieSystem.tickRootVisuals();
        froz8n.combat.GardenZombieSystem.tickCharges();
        TempBlockManager.tick();
    }

    /**
     * Cancels the break of blocks placed by smart mobs so they never drop items.
     *
     * @return {@code false} to cancel the vanilla break (Fabric's cancellation contract).
     */
    private static boolean onBlockBreak(Level world, net.minecraft.world.entity.player.Player player,
                                        BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(world instanceof ServerLevel level)) {
            return true;
        }
        if (TempBlockManager.isTemp(level, pos)) {
            // Block was placed by a smart mob: remove it without dropping anything.
            level.removeBlock(pos, false);
            TempBlockManager.untrack(level, pos);
            return false; // cancel the vanilla break so nothing drops
        }
        return true;
    }

    /**
     * The three damage suppressors Forge registered on LivingHurtEvent. Forge's feared-zombie
     * handler zeroed the damage amount instead of cancelling; on Fabric both outcomes are
     * "the victim takes nothing", so all three cancel.
     *
     * @return {@code false} to deny the damage.
     */
    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (froz8n.combat.SoundJammerSystem.suppressFearedAttack(source)) return false;
        if (froz8n.combat.ZombieSerumSystem.preventAttack(entity, source)) return false;
        if (froz8n.combat.GardenZombieSystem.suppressChargeAttack(source)) return false;
        // Breed reactions: a thief robs the player it just hit, a sapper goes off on the
        // blow that would kill it.
        if (entity instanceof net.minecraft.world.entity.player.Player victim
                && source.getEntity() instanceof Zombie attacker) {
            ZombieBreeds.onZombieHitPlayer(attacker, victim);
            if (ZombieBreeds.isFleeing(attacker)) return false; // it took your stuff, not your health
        }
        if (entity instanceof Zombie hurt) {
            ZombieBreeds.onZombieDamaged(hurt, source, amount);
        }
        return true;
    }

    public static boolean isSmart(Entity entity) {
        return Nbt.getBooleanOr(PersistentData.of(entity), SMART_KEY, false);
    }

    public static boolean isSmartMobZombie(Zombie zombie) {
        return Nbt.getBooleanOr(PersistentData.of(zombie), SMART_KEY, false)
                || Nbt.getBooleanOr(PersistentData.of(zombie), GARDEN_KEY, false);
    }

    private static void clearLegacyBoxZombie(Zombie zombie) {
        if (Nbt.getBooleanOr(PersistentData.of(zombie), LEGACY_BOX_KEY, false)) {
            PersistentData.of(zombie).remove(LEGACY_BOX_KEY);
            PersistentData.of(zombie).remove(LEGACY_BOX_SHIELD_KEY);
        }
        if (zombie.getItemBySlot(EquipmentSlot.HEAD).is(SmartMobs.CARDBOARD_BOX)) {
            zombie.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            zombie.setDropChance(EquipmentSlot.HEAD, 0.0F);
        }
    }

    private static void swimLikePlayer(Zombie zombie,net.minecraft.world.entity.player.Player target){
        Vec3 toward=target.getEyePosition().subtract(zombie.getEyePosition());
        if(toward.lengthSqr()<.0001)return;
        Vec3 direction=toward.normalize();
        Vec3 desired=direction.scale(SWIM_SPEED);
        if (zombie.getFluidHeight(FluidTags.WATER) < 0.85D) {
            desired = new Vec3(desired.x, Math.min(desired.y, -0.16D), desired.z);
        }
        zombie.setDeltaMovement(desired);
        faceVector(zombie, direction, 75.0F);
        zombie.setSwimming(true);
        zombie.setPose(Pose.SWIMMING);
        zombie.setSprinting(true);
        zombie.getLookControl().setLookAt(target,75,75);
    }

    private static void faceVector(Zombie zombie, Vec3 direction, float maxPitch) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = net.minecraft.util.Mth.wrapDegrees((float)(net.minecraft.util.Mth.atan2(direction.z, direction.x) * 180.0F / Math.PI) - 90.0F);
        float pitch = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees((float)(-(net.minecraft.util.Mth.atan2(direction.y, horizontal) * 180.0F / Math.PI))), -maxPitch, maxPitch);
        zombie.setYRot(yaw);
        zombie.setYHeadRot(yaw);
        zombie.setYBodyRot(yaw);
        zombie.setXRot(pitch);
    }

    private static void syncSwimmingPose(Zombie zombie) {
        if (zombie.isInWater()) {
            if (!zombie.isSwimming()) zombie.setSwimming(true);
            if (zombie.getPose() != Pose.SWIMMING) zombie.setPose(Pose.SWIMMING);
        } else {
            if (zombie.isSwimming()) zombie.setSwimming(false);
            if (zombie.getPose() == Pose.SWIMMING) zombie.setPose(Pose.STANDING);
        }
    }
}
