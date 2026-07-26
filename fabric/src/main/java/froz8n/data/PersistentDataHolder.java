package froz8n.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Implemented by every {@link net.minecraft.world.entity.Entity} through
 * {@code froz8n.mixin.EntityPersistentDataMixin}.
 *
 * <p>Fabric has no equivalent of Forge's {@code Entity#getPersistentData()}, so the mod
 * carries its own tag: one {@link CompoundTag} per entity, saved with the entity under
 * the {@code SmartMobsData} key. Call it through {@link PersistentData#of}.
 */
public interface PersistentDataHolder {

    CompoundTag smartmobs$getPersistentData();
}
