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

/** Exact wearable geometry used by mining_helmet 2.1.2, adapted for SmartMobs. */
public final class MiningHelmetModel<T extends HumanoidRenderState> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SmartMobs.MODID, "mining_helmet"), "main");

    public MiningHelmetModel(ModelPart root) {
        super(root);
        // The equipment texture contains UVs only for the helmet. Never render the
        // inherited humanoid body/arms/legs with it.
        setAllVisible(false);
        head.visible = true;
    }

    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, MiningHelmetModel::createLayer);
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        // Replace vanilla's textured head cube with an empty anchor. Its children
        // below are the only geometry this armor model is allowed to render.
        PartDefinition head = mesh.getRoot().addOrReplaceChild(
                "head", CubeListBuilder.create(), PartPose.ZERO);

        head.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-6.0F, -6.0F, -6.0F, 12.0F, 2.0F, 12.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        head.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -10.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F)),
                PartPose.ZERO);
        head.addOrReplaceChild("light", CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-3.0F, -12.0F, -7.0F, 6.0F, 6.0F, 3.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
