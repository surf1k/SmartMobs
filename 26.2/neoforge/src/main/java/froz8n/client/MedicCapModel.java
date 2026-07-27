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
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Head geometry for the medic_cap breed hat. One silhouette per breed, so a glance tells them apart. */
public final class MedicCapModel<T extends HumanoidRenderState> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SmartMobs.MODID, "medic_cap"), "main");

    public MedicCapModel(ModelPart root) {
        super(root);
        // The texture carries UVs for the hat only; never draw the inherited humanoid body.
        body.visible=false;rightArm.visible=false;leftArm.visible=false;rightLeg.visible=false;leftLeg.visible=false;hat.visible=false;
        head.visible = true;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        // Vanilla's textured head cube becomes an empty anchor; only the children below draw.
        PartDefinition head = mesh.getRoot().addOrReplaceChild(
                "head", CubeListBuilder.create(), PartPose.ZERO);

        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.6F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.ZERO);
        head.addOrReplaceChild("cross", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-1.5F, -8.2F, -4.9F, 3.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
