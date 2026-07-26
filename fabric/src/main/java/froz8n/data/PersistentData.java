package froz8n.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/** Access to the per-entity persistent tag. The Fabric stand-in for {@code Entity#getPersistentData()}. */
public final class PersistentData {

    private PersistentData() {
    }

    public static CompoundTag of(Entity entity) {
        return ((PersistentDataHolder) entity).smartmobs$getPersistentData();
    }
}
