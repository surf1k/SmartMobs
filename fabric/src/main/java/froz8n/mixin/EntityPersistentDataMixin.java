package froz8n.mixin;

import froz8n.data.PersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives every entity the mod-owned NBT tag that Forge provided as {@code getPersistentData()}. */
@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements PersistentDataHolder {

    @Unique
    private static final String SMARTMOBS$TAG = "SmartMobsData";

    @Unique
    private CompoundTag smartmobs$persistentData;

    @Override
    public CompoundTag smartmobs$getPersistentData() {
        if (this.smartmobs$persistentData == null) {
            this.smartmobs$persistentData = new CompoundTag();
        }
        return this.smartmobs$persistentData;
    }

    @Inject(method = "saveWithoutId(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At("TAIL"))
    private void smartmobs$save(ValueOutput output, CallbackInfo ci) {
        if (this.smartmobs$persistentData != null && !this.smartmobs$persistentData.isEmpty()) {
            output.store(SMARTMOBS$TAG, CompoundTag.CODEC, this.smartmobs$persistentData);
        }
    }

    @Inject(method = "load(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("TAIL"))
    private void smartmobs$load(ValueInput input, CallbackInfo ci) {
        this.smartmobs$persistentData = input.read(SMARTMOBS$TAG, CompoundTag.CODEC).orElse(null);
    }
}
