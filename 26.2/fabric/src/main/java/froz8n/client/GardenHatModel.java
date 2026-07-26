package froz8n.client;

import froz8n.SmartMobs;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Geometry extracted from Wonderful Hats' Modelluffy and adapted to a humanoid head anchor. */
@Environment(EnvType.CLIENT)
public final class GardenHatModel<T extends HumanoidRenderState> extends HumanoidModel<T>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SmartMobs.MODID,"garden_hat"),"main");
    public GardenHatModel(ModelPart root){super(root);body.visible=false;rightArm.visible=false;leftArm.visible=false;rightLeg.visible=false;leftLeg.visible=false;hat.visible=false;head.visible=true;}
    public static LayerDefinition createLayer(){
        MeshDefinition mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);
        PartDefinition head=mesh.getRoot().addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);
        CubeListBuilder b=CubeListBuilder.create()
                .texOffs(22,17).addBox(7,-6.25F,-9,1,1,10)
                .texOffs(24,15).addBox(-5,-6.25F,-12,10,1,1)
                .texOffs(34,17).addBox(-5,-6.25F,3,10,1,1)
                .texOffs(34,19).addBox(-4,-7.25F,-9,8,1,1)
                .texOffs(34,21).addBox(-4,-7.25F,0,8,1,1)
                .texOffs(0,27).addBox(4,-7.25F,-9,1,1,10)
                .texOffs(12,28).addBox(-5,-7.25F,-9,1,1,10)
                .texOffs(24,29).addBox(-8,-6.25F,-9,1,1,10)
                .texOffs(0,0).addBox(-7,-6.25F,-11,14,1,14)
                .texOffs(0,15).addBox(-4,-10.25F,-8,8,4,8);
        // One model pixel backward: the original 1.20 head anchor sat too far forward
        // on the current 1.21 zombie renderer.
        head.addOrReplaceChild("straw_hat",b,PartPose.offsetAndRotation(0,-0.35F,3,-0.1309F,0,0));
        return LayerDefinition.create(mesh,64,64);
    }
}
