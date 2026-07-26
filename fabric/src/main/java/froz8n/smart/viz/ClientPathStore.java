package froz8n.smart.viz;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side store of the most recent pathfinding plans received from the server,
 * keyed by entity id. Entries expire if not refreshed, so paths vanish when a
 * zombie stops sending (dies / loses target).
 */
public final class ClientPathStore {

    private record Entry(PathVizData data, long expireAtMs) {
    }

    private static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static final long TTL_MS = 2000;

    private ClientPathStore() {
    }

    public static void put(PathVizData data) {
        if (data.cells.isEmpty()) {
            ENTRIES.remove(data.entityId);
        } else {
            ENTRIES.put(data.entityId, new Entry(data, System.currentTimeMillis() + TTL_MS));
        }
    }

    /** Returns all live paths, pruning expired ones. */
    public static Iterable<PathVizData> live() {
        long now = System.currentTimeMillis();
        ENTRIES.values().removeIf(e -> e.expireAtMs < now);
        return ENTRIES.values().stream().map(Entry::data).toList();
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
