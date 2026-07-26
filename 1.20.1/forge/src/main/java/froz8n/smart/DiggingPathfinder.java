package froz8n.smart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A Baritone-style 3D A* planner that produces a list of <b>typed movements</b>.
 *
 * <p>Unlike a plain cell-by-cell path, every edge here is a concrete, self-describing
 * action the zombie knows how to execute exactly: walk, step up, step/descend down,
 * mine-and-walk, pillar up (build under itself), or bridge across a gap. The brain
 * is then a dumb executor - it never has to guess what a step "means", which is what
 * caused the earlier nonsense (building a pillar on flat ground, freezing one block
 * below the player, etc.).
 */
public final class DiggingPathfinder {

    public enum Type {
        WALK,        // step to an adjacent same-level cell
        STEP_UP,     // jump up one block to an adjacent higher cell
        STEP_DOWN,   // step down one block
        DESCEND,     // fall straight down one (no floor)
        DROP,        // commit to a longer fall; miners clutch before landing
        DIG,         // mine blocks, then move into the target cell (any of the 3 heights)
        PILLAR,      // jump straight up and place a block underneath
        BRIDGE,      // place a block into a gap ahead, then walk onto it
        DIAGONAL,    // walk diagonally to a corner-adjacent cell (faster, natural)
        JUMP         // parkour: run-and-jump across a gap of 2-4 blocks
    }

    /** One executable movement toward {@code target} (the destination FEET position). */
    public static final class Move {
        public final Type type;
        public final BlockPos target;      // feet cell we end up standing in
        public final List<BlockPos> dig;   // blocks to mine to perform this move (may be empty)
        public final BlockPos place;       // block to place (PILLAR/BRIDGE), else null

        Move(Type type, BlockPos target, List<BlockPos> dig, BlockPos place) {
            this.type = type;
            this.target = target;
            this.dig = dig;
            this.place = place;
        }
    }

    // Tunables.
    private static final int MAX_EXPANSIONS = 4000;
    private static final int MAX_RADIUS = 48;
    // Costs are in TICKS, matching Baritone's ActionCosts model (player walks
    // 4.317 blocks/s -> 20/4.317 ticks per block). Keeping the same scale as the
    // heuristic is what makes A* pick sane routes instead of thrashing.
    private static final double COST_WALK = 4.633;             // WALK_ONE_BLOCK_COST
    private static final double COST_DIAGONAL = 4.633 * 1.414; // walk * sqrt(2)
    private static final double COST_STEP_UP = 4.633 + 3.0;    // walk + jump-up overhead
    private static final double COST_STEP_DOWN = 4.633 * 0.8;  // WALK_OFF_BLOCK_COST-ish
    private static final double COST_DESCEND = 3.0;            // falling one block
    private static final double COST_PILLAR = 7.0;            // build straight up, ~ladder speed
    private static final double COST_BRIDGE = 8.0;            // place + walk
    private static final double COST_JUMP = 8.0;               // parkour base (sprint jump)
    private static final double COST_DROP = 3.5;               // falling is fast; the brain handles clutching
    private static final int MAX_DROP = 32;
    private static final List<BlockPos> NO_DIG = List.of();
    private static final double DIG_BASE = 10.0;               // per block dug, plus time factor
    private static final double PLAN_DIG_SPEED = 8.0;

    private DiggingPathfinder() {
    }

    // ==================================================================
    // Public API
    // ==================================================================

    /** Plans a typed movement route from {@code start} (feet) to beside {@code goal} (feet). */
    public static List<Move> plan(ServerLevel level, BlockPos start, BlockPos goal) {
        Map<Long, Node> nodes = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));

        Node startNode = new Node(start.getX(), start.getY(), start.getZ());
        startNode.g = 0;
        startNode.f = heuristic(startNode.x, startNode.y, startNode.z, goal);
        nodes.put(key(startNode.x, startNode.y, startNode.z), startNode);
        open.add(startNode);

        Node best = startNode;
        double bestH = startNode.f;
        int expansions = 0;

        while (!open.isEmpty() && expansions < MAX_EXPANSIONS) {
            Node cur = open.poll();
            if (cur.closed) {
                continue;
            }
            cur.closed = true;
            expansions++;

            double h = heuristic(cur.x, cur.y, cur.z, goal);
            if (h < bestH) {
                bestH = h;
                best = cur;
            }
            // Reached a cell adjacent to (or at) the goal.
            if (Math.abs(cur.x - goal.getX()) + Math.abs(cur.z - goal.getZ()) <= 1
                    && Math.abs(cur.y - goal.getY()) <= 1) {
                best = cur;
                break;
            }
            if (Math.abs(cur.x - start.getX()) > MAX_RADIUS
                    || Math.abs(cur.z - start.getZ()) > MAX_RADIUS
                    || Math.abs(cur.y - start.getY()) > MAX_RADIUS) {
                continue;
            }
            expand(level, cur, goal, nodes, open);
        }

        return reconstruct(best);
    }

    // ==================================================================
    // A* internals
    // ==================================================================

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    /** The four diagonal (dx, dz) offsets. */
    private static final int[][] DIAGONALS = {
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private static void expand(ServerLevel level, Node cur, BlockPos goal,
                               Map<Long, Node> nodes, PriorityQueue<Node> open) {
        int x = cur.x, y = cur.y, z = cur.z;

        for (Direction d : HORIZONTAL) {
            int nx = x + d.getStepX();
            int nz = z + d.getStepZ();

            // 1) WALK: same level, both body cells clear, floor solid.
            if (bodyClear(level, nx, y, nz) && solidFloor(level, nx, y - 1, nz)) {
                add(level, cur, goal, nodes, open,
                        new Move(Type.WALK, new BlockPos(nx, y, nz), NO_DIG, null), COST_WALK);
            }
            // 2) STEP_UP: rise one; need clearance above current head + body clear up there + floor.
            else if (bodyClear(level, nx, y + 1, nz) && solidFloor(level, nx, y, nz)
                    && passable(level, x, y + 2, z)) {
                add(level, cur, goal, nodes, open,
                        new Move(Type.STEP_UP, new BlockPos(nx, y + 1, nz), NO_DIG, null), COST_STEP_UP);
            }
            // 3) STEP_DOWN: drop one onto a solid floor.
            else if (bodyClear(level, nx, y - 1, nz) && solidFloor(level, nx, y - 2, nz)
                    && passable(level, nx, y, nz)) {
                add(level, cur, goal, nodes, open,
                        new Move(Type.STEP_DOWN, new BlockPos(nx, y - 1, nz), NO_DIG, null), COST_STEP_DOWN);
            }

            // 4) DIG: forward is blocked but breakable -> mine head+feet ahead and step in.
            if (!bodyClear(level, nx, y, nz) && solidFloor(level, nx, y - 1, nz)) {
                List<BlockPos> dig = new ArrayList<>(2);
                double digCost = 0;
                double c1 = digIfNeeded(level, nx, y, nz, dig);
                double c2 = digIfNeeded(level, nx, y + 1, nz, dig);
                if (c1 >= 0 && c2 >= 0) {
                    digCost = c1 + c2;
                    add(level, cur, goal, nodes, open,
                            new Move(Type.DIG, new BlockPos(nx, y, nz), dig, null), COST_WALK + digCost);
                }
            }

            // 5) BRIDGE: gap ahead (body clear, no floor) -> place a block, walk on.
            if (bodyClear(level, nx, y, nz) && !solidFloor(level, nx, y - 1, nz)
                    && replaceable(level, nx, y - 1, nz)) {
                add(level, cur, goal, nodes, open,
                        new Move(Type.BRIDGE, new BlockPos(nx, y, nz), NO_DIG,
                                new BlockPos(nx, y - 1, nz)), COST_BRIDGE);
            }

            // 5a) DROP over an edge. This is what removes the vanilla cliff fear for miners.
            if (bodyClear(level, nx, y, nz) && !solidFloor(level, nx, y - 1, nz)) {
                addDrop(level, cur, goal, nodes, open, nx, y, nz, true);
            }
        }

        // 5b) DIAGONAL: move to a corner-adjacent same-level cell (natural, faster).
        //     Only allowed when BOTH orthogonal cells between are passable (no corner clipping).
        for (int[] diag : DIAGONALS) {
            int nx = x + diag[0];
            int nz = z + diag[1];
            if (bodyClear(level, nx, y, nz) && solidFloor(level, nx, y - 1, nz)
                    && bodyClear(level, x + diag[0], y, z)     // side A clear
                    && solidFloor(level, x + diag[0], y - 1, z)
                    && bodyClear(level, x, y, z + diag[1])     // side B clear
                    && solidFloor(level, x, y - 1, z + diag[1])) {
                add(level, cur, goal, nodes, open,
                        new Move(Type.DIAGONAL, new BlockPos(nx, y, nz), NO_DIG, null), COST_DIAGONAL);
            }
        }

        // 5c) JUMP (parkour): run-and-jump across a gap of 2..4 blocks on the same level.
        //     Requires clear body cells along the arc and a solid landing, with the gap
        //     cells below actually empty (so it's a real jump, not just walking).
        for (Direction d : HORIZONTAL) {
            for (int dist = 2; dist <= 4; dist++) {
                int lx = x + d.getStepX() * dist;
                int lz = z + d.getStepZ() * dist;
                // Landing must be a standable cell.
                if (!(bodyClear(level, lx, y, lz) && solidFloor(level, lx, y - 1, lz))) {
                    continue;
                }
                // Every intermediate cell must be air at body height AND have NO floor
                // (a genuine gap), and headroom clear for the jump arc.
                boolean validGap = true;
                for (int i = 1; i < dist; i++) {
                    int gx = x + d.getStepX() * i;
                    int gz = z + d.getStepZ() * i;
                    if (!bodyClear(level, gx, y, gz) || !passable(level, gx, y + 1, gz)
                            || solidFloor(level, gx, y - 1, gz)) {
                        validGap = false;
                        break;
                    }
                }
                if (validGap && passable(level, x, y + 2, z)) {
                    double cost = COST_JUMP + (dist - 2) * 0.8;
                    add(level, cur, goal, nodes, open,
                            new Move(Type.JUMP, new BlockPos(lx, y, lz), NO_DIG, null), cost);
                    break; // take the shortest valid jump in this direction
                }
            }
        }

        // 6) DESCEND: straight down one (no floor here, cell below body-clear with floor).
        if (!solidFloor(level, x, y - 1, z) && bodyClear(level, x, y - 1, z)
                && solidFloor(level, x, y - 2, z)) {
            add(level, cur, goal, nodes, open,
                    new Move(Type.DESCEND, new BlockPos(x, y - 1, z), NO_DIG, null), COST_DESCEND);
        }

        // 7) PILLAR: build straight up (head clearance needed; place under feet).
        if (passable(level, x, y + 2, z)) {
            add(level, cur, goal, nodes, open,
                    new Move(Type.PILLAR, new BlockPos(x, y + 1, z), NO_DIG, new BlockPos(x, y, z)),
                    COST_PILLAR);
        }

        // 8) DIG straight up (ceiling blocked but breakable) then pillar-ish rise.
        if (!passable(level, x, y + 2, z) && breakable(level, x, y + 2, z)) {
            List<BlockPos> dig = new ArrayList<>(1);
            double c = digIfNeeded(level, x, y + 2, z, dig);
            if (c >= 0) {
                add(level, cur, goal, nodes, open,
                        new Move(Type.PILLAR, new BlockPos(x, y + 1, z), dig, new BlockPos(x, y, z)),
                        COST_PILLAR + c);
            }
        }

        // 9) DIG straight down (floor blocked but breakable) to descend.
        if (solidFloor(level, x, y - 1, z) && breakable(level, x, y - 1, z)) {
            List<BlockPos> dig = new ArrayList<>(1);
            double c = digIfNeeded(level, x, y - 1, z, dig);
            if (c >= 0) {
                add(level, cur, goal, nodes, open,
                        new Move(Type.DIG, new BlockPos(x, y - 1, z), dig, null), COST_STEP_DOWN + c);
            }
        }

        // 10) DROP: miners can intentionally take longer falls and clutch before landing.
        if (!solidFloor(level, x, y - 1, z)) {
            addDrop(level, cur, goal, nodes, open, x, y, z, false);
        }
    }

    private static void addDrop(ServerLevel level, Node cur, BlockPos goal,
                                Map<Long, Node> nodes, PriorityQueue<Node> open,
                                int x, int y, int z, boolean offEdge) {
        for (int drop = 2; drop <= MAX_DROP; drop++) {
            int ny = y - drop;
            if (!bodyClear(level, x, ny, z)) break;
            if (solidFloor(level, x, ny - 1, z)) {
                add(level, cur, goal, nodes, open,
                        new Move(Type.DROP, new BlockPos(x, ny, z), NO_DIG, null),
                        COST_DROP + drop * 0.25 + (offEdge ? COST_WALK * 0.35 : 0.0));
                break;
            }
        }
    }

    private static void add(ServerLevel level, Node from, BlockPos goal,
                            Map<Long, Node> nodes, PriorityQueue<Node> open, Move move, double cost) {
        BlockPos t = move.target;
        long k = key(t.getX(), t.getY(), t.getZ());
        Node existing = nodes.get(k);
        double g = from.g + cost;
        if (existing != null && (existing.closed || g >= existing.g)) {
            return;
        }
        Node n = existing != null ? existing : new Node(t.getX(), t.getY(), t.getZ());
        n.g = g;
        n.f = g + heuristic(t.getX(), t.getY(), t.getZ(), goal);
        n.parent = from;
        n.moveFromParent = move;
        nodes.put(k, n);
        open.add(n);
    }

    private static List<Move> reconstruct(Node end) {
        Deque<Move> out = new ArrayDeque<>();
        Node cur = end;
        while (cur != null && cur.moveFromParent != null) {
            out.addFirst(cur.moveFromParent);
            cur = cur.parent;
        }
        return new ArrayList<>(out);
    }

    // ==================================================================
    // Block predicates
    // ==================================================================

    /** Both feet+head cells at (x,y,z) are passable (air / non-blocking, no fluid). */
    private static boolean bodyClear(ServerLevel level, int x, int y, int z) {
        return passable(level, x, y, z) && passable(level, x, y + 1, z);
    }

    private static boolean passable(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState s = level.getBlockState(pos);
        // 1.20.1 still passes the level and position into isPathfindable.
        return (s.isAir() || (!s.blocksMotion() && s.isPathfindable(level, pos, PathComputationType.LAND)))
                && s.getFluidState().isEmpty();
    }

    private static boolean solidFloor(ServerLevel level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z)).blocksMotion();
    }

    private static boolean replaceable(ServerLevel level, int x, int y, int z) {
        BlockState s = level.getBlockState(new BlockPos(x, y, z));
        return (s.isAir() || s.canBeReplaced()) && s.getFluidState().isEmpty();
    }

    private static boolean breakable(ServerLevel level, int x, int y, int z) {
        BlockState s = level.getBlockState(new BlockPos(x, y, z));
        return !s.isAir() && s.getFluidState().isEmpty()
                && s.getDestroySpeed(level, new BlockPos(x, y, z)) >= 0.0F
                && s.blocksMotion();
    }

    /**
     * If the cell needs digging, adds it to {@code dig} and returns its cost;
     * if already passable returns 0; if it must not be mined returns -1.
     */
    private static double digIfNeeded(ServerLevel level, int x, int y, int z, List<BlockPos> dig) {
        BlockPos p = new BlockPos(x, y, z);
        BlockState s = level.getBlockState(p);
        if (s.isAir() || (!s.blocksMotion() && s.getFluidState().isEmpty())) {
            return 0.0;
        }
        if (!s.getFluidState().isEmpty()) {
            return -1;
        }
        float hardness = s.getDestroySpeed(level, p);
        if (hardness < 0) {
            return -1;
        }
        dig.add(p);
        return DIG_BASE + (hardness * 30.0) / PLAN_DIG_SPEED;
    }

    private static double heuristic(int x, int y, int z, BlockPos goal) {
        // Distance-to-go expressed in TICKS (same scale as the movement costs), using
        // the cheapest per-block cost (walk) so the heuristic stays admissible.
        double manhattan = Math.abs(x - goal.getX()) + Math.abs(z - goal.getZ()) + Math.abs(y - goal.getY());
        return manhattan * COST_WALK;
    }

    private static long key(int x, int y, int z) {
        long bx = (long) (x + (1 << 25)) & 0x3FFFFFF;
        long by = (long) (y + 2048) & 0xFFF;
        long bz = (long) (z + (1 << 25)) & 0x3FFFFFF;
        return (bx << 38) | (by << 26) | bz;
    }

    private static final class Node {
        final int x, y, z;
        double g;
        double f;
        boolean closed;
        Node parent;
        Move moveFromParent;

        Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
