package froz8n.smart;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Keeps track of blocks that smart mobs place while building. Such blocks are
 * temporary: they are automatically removed after {@link #LIFETIME} ticks and,
 * when broken by anyone before that, they drop nothing.
 *
 * All access happens on the server thread (from the tick and block-break handlers),
 * so a plain list is enough.
 */
public final class TempBlockManager {

    /** 30 seconds: bridges/pillars must survive long enough for the whole horde. */
    public static final int LIFETIME = 600;

    private static final List<TempBlock> BLOCKS = new ArrayList<>();

    private TempBlockManager() {
    }

    private static final class TempBlock {
        final ServerLevel level;
        final BlockPos pos;
        int ticksLeft;

        TempBlock(ServerLevel level, BlockPos pos, int ticksLeft) {
            this.level = level;
            this.pos = pos;
            this.ticksLeft = ticksLeft;
        }
    }

    /** Register a freshly placed temporary block. */
    public static void track(ServerLevel level, BlockPos pos) {
        BlockPos immutable = pos.immutable();
        // Refresh the timer if the same spot is tracked again.
        for (TempBlock tb : BLOCKS) {
            if (tb.level == level && tb.pos.equals(immutable)) {
                tb.ticksLeft = LIFETIME;
                return;
            }
        }
        BLOCKS.add(new TempBlock(level, immutable, LIFETIME));
    }

    public static boolean isTemp(ServerLevel level, BlockPos pos) {
        for (TempBlock tb : BLOCKS) {
            if (tb.level == level && tb.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    /** Stop tracking a position (it has already been removed elsewhere). */
    public static void untrack(ServerLevel level, BlockPos pos) {
        BLOCKS.removeIf(tb -> tb.level == level && tb.pos.equals(pos));
    }

    /** Called once per server tick: ages out expired blocks. */
    public static void tick() {
        if (BLOCKS.isEmpty()) {
            return;
        }
        Iterator<TempBlock> it = BLOCKS.iterator();
        while (it.hasNext()) {
            TempBlock tb = it.next();
            if (--tb.ticksLeft <= 0) {
                if (!tb.level.getBlockState(tb.pos).isAir()) {
                    // false -> no drops
                    tb.level.removeBlock(tb.pos, false);
                }
                it.remove();
            }
        }
    }
}
