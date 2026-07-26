package froz8n.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * Access to the per-entity persistent tag. NeoForge and Forge provide it natively; the
 * Fabric tree adds the same thing with a mixin. Going through this helper is what lets
 * the gameplay classes be byte-identical in every loader tree.
 */
public final class PersistentData {

    private PersistentData() {
    }

    public static CompoundTag of(Entity entity) {
        return entity.getPersistentData();
    }
}