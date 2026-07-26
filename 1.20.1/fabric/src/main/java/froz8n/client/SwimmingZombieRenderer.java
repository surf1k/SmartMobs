package froz8n.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Zombie;

@Environment(EnvType.CLIENT)
public final class SwimmingZombieRenderer extends ZombieRenderer {
    public SwimmingZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SwimmingZombieModel(context.bakeLayer(ModelLayers.ZOMBIE));
    }

    @Override
    protected void setupRotations(Zombie entity, PoseStack poseStack, float ageInTicks, float yBodyRot,
                                  float partialTick) {
        super.setupRotations(entity, poseStack, ageInTicks, yBodyRot, partialTick);
        float scale = 1.0F;
        float swimAmount = entity.getSwimAmount(partialTick);
        if (swimAmount > 0.0F) {
            float targetPitch = -82.0F - entity.getXRot();
            float pitch = Mth.lerp(swimAmount, 0.0F, targetPitch);
            float surfaceFix = (1.0F - swimAmount) * 0.42F + (entity.isInWater() ? 0.22F : 0.0F);
            poseStack.translate(0.0F, -surfaceFix / scale, 0.0F);
            poseStack.rotateAround(Axis.XP.rotationDegrees(pitch), 0.0F,
                    entity.getBbHeight() / 2.0F / scale, 0.0F);
        }
    }
}