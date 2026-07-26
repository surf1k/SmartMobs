package froz8n.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.util.Mth;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SwimmingZombieRenderer extends ZombieRenderer {
    public SwimmingZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SwimmingZombieModel(context.bakeLayer(ModelLayers.ZOMBIE));
    }

    @Override
    protected void setupRotations(ZombieRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        if (state.swimAmount > 0.0F) {
            float targetPitch = -82.0F - state.xRot;
            float pitch = Mth.lerp(state.swimAmount, 0.0F, targetPitch);
            float surfaceFix = (1.0F - state.swimAmount) * 0.42F + (state.isInWater ? 0.22F : 0.0F);
            poseStack.translate(0.0F, -surfaceFix / scale, 0.0F);
            poseStack.rotateAround(
                    Axis.XP.rotationDegrees(pitch),
                    0.0F,
                    state.boundingBoxHeight / 2.0F / scale,
                    0.0F
            );
        }
    }
}
