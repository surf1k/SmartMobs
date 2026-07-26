package froz8n.client;

import froz8n.SmartMobs;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Native 1.21 humanoid adaptation of Somies' OptiFine cardboard-box model. */
public final class CardboardBoxModel<T extends HumanoidRenderState> extends HumanoidModel<T>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(SmartMobs.MODID,"cardboard_box"),"main");
    public CardboardBoxModel(ModelPart root){super(root);setAllVisible(false);head.visible=true;}
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(LAYER,CardboardBoxModel::layer);}
    private static LayerDefinition layer(){
        MeshDefinition mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);
        PartDefinition head=mesh.getRoot().addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);
        CubeListBuilder box=CubeListBuilder.create().texOffs(0,0).addBox(-6,-10,-6,12,10,12,new CubeDeformation(.12F));
        head.addOrReplaceChild("box",box,PartPose.offset(0,-.25F,0));
        return LayerDefinition.create(mesh,64,32);
    }
}
