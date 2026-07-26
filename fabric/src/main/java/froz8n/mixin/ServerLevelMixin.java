package froz8n.mixin;

import froz8n.smart.SmartMobsEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stands in for Forge's cancellable EntityJoinLevelEvent. Only entities that are spawned
 * (not the ones read back from disk) pass through addFreshEntity, which is exactly the
 * subset the old handler acted on.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void smartmobs$addFreshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (SmartMobsEvents.onEntityJoinLevel((ServerLevel) (Object) this, entity)) {
            cir.setReturnValue(false);
        }
    }
}
