package froz8n.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Zombie;

@Environment(EnvType.CLIENT)
public final class SwimmingZombieModel extends ZombieModel<Zombie> {
    public SwimmingZombieModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(Zombie entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float swim = entity.getSwimAmount(1.0F);
        if (swim <= 0.0F) return;

        float phase = ageInTicks * 0.42F;
        float stroke = Mth.sin(phase);
        float recover = Mth.cos(phase);
        float reach = -2.55F;
        float sweep = 0.72F * stroke;
        float roll = 0.34F * recover;

        this.rightArm.xRot = Mth.lerp(swim, this.rightArm.xRot, reach + sweep);
        this.leftArm.xRot = Mth.lerp(swim, this.leftArm.xRot, reach - sweep);
        this.rightArm.yRot = Mth.lerp(swim, this.rightArm.yRot, -0.24F - 0.18F * recover);
        this.leftArm.yRot = Mth.lerp(swim, this.leftArm.yRot, 0.24F + 0.18F * recover);
        this.rightArm.zRot = Mth.lerp(swim, this.rightArm.zRot, 0.10F + roll);
        this.leftArm.zRot = Mth.lerp(swim, this.leftArm.zRot, -0.10F - roll);

        this.rightLeg.xRot = Mth.lerp(swim, this.rightLeg.xRot, 0.28F * Mth.sin(phase + (float)Math.PI));
        this.leftLeg.xRot = Mth.lerp(swim, this.leftLeg.xRot, 0.28F * Mth.sin(phase));
    }
}