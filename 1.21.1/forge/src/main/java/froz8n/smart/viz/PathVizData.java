package froz8n.smart.viz;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * One smart zombie's pathfinding plan for visualization: an ordered list of cells,
 * each flagged as a walk step or a dig step. Encoded compactly for the network.
 */
public final class PathVizData {

    /** A single cell in the plan. */
    public record Cell(BlockPos pos, boolean dig) {
    }

    public final int entityId;
    public final List<Cell> cells;

    public PathVizData(int entityId, List<Cell> cells) {
        this.entityId = entityId;
        this.cells = cells;
    }

    public static PathVizData empty(int entityId) {
        return new PathVizData(entityId, new ArrayList<>());
    }
}
