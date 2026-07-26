package froz8n.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import froz8n.client.StunnedZombieRenderHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Brackets the living-entity render call so a stunned zombie can be tipped over.
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void smartmobs$beforeRender(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                        MultiBufferSource buffers, int light, CallbackInfo ci) {
        StunnedZombieRenderHandler.before(entity, poseStack);
    }

    // RETURN, not TAIL: every exit path has to pop what HEAD pushed.
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void smartmobs$afterRender(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                       MultiBufferSource buffers, int light, CallbackInfo ci) {
        StunnedZombieRenderHandler.after(poseStack);
    }
}