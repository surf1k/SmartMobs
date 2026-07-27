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
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Head geometry for the screamer_horns breed hat. One silhouette per breed, so a glance tells them apart. */
public final class ScreamerHornsModel<T extends HumanoidRenderState> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SmartMobs.MODID, "screamer_horns"), "main");

    public ScreamerHornsModel(ModelPart root) {
        super(root);
        // The texture carries UVs for the hat only; never draw the inherited humanoid body.
        body.visible=false;rightArm.visible=false;leftArm.visible=false;rightLeg.visible=false;leftLeg.visible=false;hat.visible=false;
        head.visible = true;
    }

    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, ScreamerHornsModel::createLayer);
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        // Vanilla's textured head cube becomes an empty anchor; only the children below draw.
        PartDefinition head = mesh.getRoot().addOrReplaceChild(
                "head", CubeListBuilder.create(), PartPose.ZERO);

        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 4.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.ZERO);
        head.addOrReplaceChild("horn_left", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(3.2F, -12.5F, -1.0F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE),
                PartPose.rotation(0.0F, 0.0F, -0.38F));
        head.addOrReplaceChild("horn_right", CubeListBuilder.create().texOffs(12, 16)
                        .addBox(-5.2F, -12.5F, -1.0F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE),
                PartPose.rotation(0.0F, 0.0F, 0.38F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
