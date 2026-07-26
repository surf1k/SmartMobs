package froz8n.mixin;

import froz8n.smart.SmartMobsEvents;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric has no per-entity tick event, so this stands in for Forge's LivingEvent.LivingTickEvent. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTickMixin {

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void smartmobs$tick(CallbackInfo ci) {
        SmartMobsEvents.onLivingTick((LivingEntity) (Object) this);
    }
}
