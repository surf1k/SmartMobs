package froz8n.client;

import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SwimmingZombieModel extends ZombieModel<ZombieRenderState> {
    public SwimmingZombieModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(ZombieRenderState state) {
        super.setupAnim(state);
        float swim = state.swimAmount;
        if (swim <= 0.0F) return;

        float phase = state.ageInTicks * 0.42F;
        float stroke = Mth.sin(phase);
        float recover = Mth.cos(phase);
        float reach = -2.55F;
        float sweep = 0.72F * stroke;
        float roll = 0.34F * recover;

        this.rightArm.xRot = Mth.rotLerpRad(swim, this.rightArm.xRot, reach + sweep);
        this.leftArm.xRot = Mth.rotLerpRad(swim, this.leftArm.xRot, reach - sweep);
        this.rightArm.yRot = Mth.rotLerpRad(swim, this.rightArm.yRot, -0.24F - 0.18F * recover);
        this.leftArm.yRot = Mth.rotLerpRad(swim, this.leftArm.yRot, 0.24F + 0.18F * recover);
        this.rightArm.zRot = Mth.rotLerpRad(swim, this.rightArm.zRot, 0.10F + roll);
        this.leftArm.zRot = Mth.rotLerpRad(swim, this.leftArm.zRot, -0.10F - roll);

        this.rightLeg.xRot = Mth.rotLerpRad(swim, this.rightLeg.xRot, 0.28F * Mth.sin(phase + (float)Math.PI));
        this.leftLeg.xRot = Mth.rotLerpRad(swim, this.leftLeg.xRot, 0.28F * Mth.sin(phase));
    }
}
