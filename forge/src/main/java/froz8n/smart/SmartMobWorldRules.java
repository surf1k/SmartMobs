package froz8n.smart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SmartMobWorldRules {
    private static final int PORTAL_SCAN_HORIZONTAL = 8;
    private static final int PORTAL_SCAN_VERTICAL = 5;
    private static final int PORTAL_SCAN_INTERVAL = 40;
    private static final double PORTAL_BREAK_REACH_SQR = 4.5D * 4.5D;

    private SmartMobWorldRules() {}

    public static boolean isNether(Level level) {
        return level.dimension() == Level.NETHER;
    }

    public static boolean isNightLike(Level level) {
        return isNether(level) || !level.isBrightOutside();
    }

    public static boolean canUseOutdoorNightBehavior(ServerLevel level, BlockPos pos) {
        return isNether(level) || level.canSeeSky(pos);
    }

    public static boolean tryBreakVisiblePortal(Zombie zombie) {
        if (!froz8n.Config.breakPortals) return false;
        if (!(zombie.level() instanceof ServerLevel level)
                || !SmartMobsEvents.isSmartMobZombie(zombie)
                || (zombie.tickCount + zombie.getId()) % PORTAL_SCAN_INTERVAL != 0) {
            return false;
        }

        BlockPos frame = nearestVisiblePortalFrame(level, zombie);
        if (frame == null) return false;
        level.destroyBlock(frame, false, zombie, 512);
        return true;
    }

    private static BlockPos nearestVisiblePortalFrame(ServerLevel level, Zombie zombie) {
        BlockPos origin = zombie.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-PORTAL_SCAN_HORIZONTAL, -PORTAL_SCAN_VERTICAL, -PORTAL_SCAN_HORIZONTAL),
                origin.offset(PORTAL_SCAN_HORIZONTAL, PORTAL_SCAN_VERTICAL, PORTAL_SCAN_HORIZONTAL))) {
            if (!level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) continue;
            for (Direction direction : Direction.values()) {
                BlockPos frame = pos.relative(direction);
                if (!level.getBlockState(frame).is(Blocks.OBSIDIAN)) continue;
                double distance = zombie.distanceToSqr(Vec3.atCenterOf(frame));
                if (distance > PORTAL_BREAK_REACH_SQR || distance >= bestDistance || !canSeeFrame(level, zombie, frame)) continue;
                bestDistance = distance;
                best = frame.immutable();
            }
        }
        return best;
    }

    private static boolean canSeeFrame(ServerLevel level, Zombie zombie, BlockPos frame) {
        HitResult hit = level.clip(new ClipContext(zombie.getEyePosition(), Vec3.atCenterOf(frame),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, zombie));
        if (hit.getType() == HitResult.Type.MISS) return true;
        return hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(frame);
    }
}
