package froz8n.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import froz8n.client.StunnedZombieRenderHandler;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets the living-entity submit call the way Forge's RenderLivingEvent.Pre/Post did,
 * so a stunned zombie can be tipped over on the pose stack.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"))
    private void smartmobs$beforeSubmit(LivingEntityRenderState state, PoseStack poseStack,
                                        SubmitNodeCollector collector, CameraRenderState cameraState,
                                        CallbackInfo ci) {
        StunnedZombieRenderHandler.before(state, poseStack);
    }

    // RETURN, not TAIL: every exit path has to pop what HEAD pushed.
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("RETURN"))
    private void smartmobs$afterSubmit(LivingEntityRenderState state, PoseStack poseStack,
                                       SubmitNodeCollector collector, CameraRenderState cameraState,
                                       CallbackInfo ci) {
        StunnedZombieRenderHandler.after(poseStack);
    }
}
