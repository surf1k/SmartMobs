package froz8n.mixin;

import froz8n.data.PersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Gives every entity the mod-owned NBT tag that Forge provides as getPersistentData().
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

    @Inject(method = "saveWithoutId(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void smartmobs$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.smartmobs$persistentData != null && !this.smartmobs$persistentData.isEmpty()) {
            cir.getReturnValue().put(SMARTMOBS$TAG, this.smartmobs$persistentData);
        }
    }

    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void smartmobs$load(CompoundTag tag, CallbackInfo ci) {
        this.smartmobs$persistentData = tag.contains(SMARTMOBS$TAG) ? tag.getCompound(SMARTMOBS$TAG) : null;
    }
}