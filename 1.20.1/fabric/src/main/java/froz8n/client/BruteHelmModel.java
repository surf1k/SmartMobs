package froz8n.client;

import froz8n.SmartMobs;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;

/** Head geometry for the brute_helm breed hat. One silhouette per breed, so a glance tells them apart. */
@Environment(EnvType.CLIENT)
public final class BruteHelmModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(SmartMobs.MODID, "brute_helm"), "main");

    public BruteHelmModel(ModelPart root) {
        super(root);
        // The texture carries UVs for the hat only; never draw the inherited humanoid body.
        setAllVisible(false);
        head.visible = true;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        // Vanilla's textured head cube becomes an empty anchor; only the children below draw.
        PartDefinition head = mesh.getRoot().addOrReplaceChild(
                "head", CubeListBuilder.create(), PartPose.ZERO);

        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.6F)),
                PartPose.ZERO);
        head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-4.5F, -6.4F, -5.1F, 9.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        head.addOrReplaceChild("nose_guard", CubeListBuilder.create().texOffs(28, 16)
                        .addBox(-1.0F, -5.4F, -5.2F, 2.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}