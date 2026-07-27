package froz8n.client;

import froz8n.SmartMobs;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Head geometry for the sapper_cap breed hat. One silhouette per breed, so a glance tells them apart. */
public final class SapperCapModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(SmartMobs.MODID, "sapper_cap"), "main");

    public SapperCapModel(ModelPart root) {
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
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.55F)),
                PartPose.ZERO);
        head.addOrReplaceChild("fuse", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-0.5F, -13.0F, -0.5F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE),
                PartPose.rotation(0.0F, 0.0F, 0.18F));
        head.addOrReplaceChild("spark", CubeListBuilder.create().texOffs(8, 16)
                        .addBox(-1.0F, -14.0F, -1.0F, 2.0F, 1.0F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
