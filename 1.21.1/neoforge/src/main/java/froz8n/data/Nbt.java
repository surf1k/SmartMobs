package froz8n.data;

import net.minecraft.nbt.CompoundTag;

/**
 * The {@code getXxxOr} accessors arrived in 1.21.5. On 1.21.1 a missing key silently
 * returns zero or false, which is wrong wherever the mod wants a real default, so the
 * gameplay code goes through these instead.
 */
public final class Nbt {

    private Nbt() {
    }

    public static boolean getBooleanOr(CompoundTag tag, String key, boolean fallback) {
        return tag.contains(key) ? tag.getBoolean(key) : fallback;
    }

    public static int getIntOr(CompoundTag tag, String key, int fallback) {
        return tag.contains(key) ? tag.getInt(key) : fallback;
    }

    public static long getLongOr(CompoundTag tag, String key, long fallback) {
        return tag.contains(key) ? tag.getLong(key) : fallback;
    }

    public static double getDoubleOr(CompoundTag tag, String key, double fallback) {
        return tag.contains(key) ? tag.getDouble(key) : fallback;
    }

    public static String getStringOr(CompoundTag tag, String key, String fallback) {
        return tag.contains(key) ? tag.getString(key) : fallback;
    }
}
