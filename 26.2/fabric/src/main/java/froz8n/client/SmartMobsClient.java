package froz8n.client;

import froz8n.SmartMobs;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;

import java.util.function.Function;

/**
 * Client entrypoint. Everything Forge used to do from FMLClientSetupEvent and the
 * client mod-bus events is wired up here.
 */
@Environment(EnvType.CLIENT)
public final class SmartMobsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(MiningHelmetModel.LAYER, MiningHelmetModel::createLayer);
        ModelLayerRegistry.registerModelLayer(GardenHatModel.LAYER, GardenHatModel::createLayer);
        ModelLayerRegistry.registerModelLayer(CardboardBoxModel.LAYER, CardboardBoxModel::createLayer);
        ModelLayerRegistry.registerModelLayer(BruteHelmModel.LAYER, BruteHelmModel::createLayer);
        ModelLayerRegistry.registerModelLayer(RunnerCapModel.LAYER, RunnerCapModel::createLayer);
        ModelLayerRegistry.registerModelLayer(ScreamerHornsModel.LAYER, ScreamerHornsModel::createLayer);
        ModelLayerRegistry.registerModelLayer(ThiefHoodModel.LAYER, ThiefHoodModel::createLayer);
        ModelLayerRegistry.registerModelLayer(MedicCapModel.LAYER, MedicCapModel::createLayer);
        ModelLayerRegistry.registerModelLayer(SapperCapModel.LAYER, SapperCapModel::createLayer);
        ModelLayerRegistry.registerModelLayer(GhostVeilModel.LAYER, GhostVeilModel::createLayer);

        // Replaces the vanilla zombie renderer with the swimming-aware one.
        EntityRendererRegistry.register(EntityTypes.ZOMBIE, SwimmingZombieRenderer::new);

        // The Fabric counterpart of Forge's IClientItemExtensions#getHumanoidArmorModel.
        registerHat(SmartMobs.MINING_HELMET, MiningHelmetModel.LAYER, MiningHelmetModel::new, "mining_helmet");
        registerHat(SmartMobs.GARDEN_HAT, GardenHatModel.LAYER, GardenHatModel::new, "garden_hat");
        registerHat(SmartMobs.CARDBOARD_BOX, CardboardBoxModel.LAYER, CardboardBoxModel::new, "cardboard_box");
        registerHat(SmartMobs.BRUTE_HELM, BruteHelmModel.LAYER, BruteHelmModel::new, "brute_helm");
        registerHat(SmartMobs.RUNNER_CAP, RunnerCapModel.LAYER, RunnerCapModel::new, "runner_cap");
        registerHat(SmartMobs.SCREAMER_HORNS, ScreamerHornsModel.LAYER, ScreamerHornsModel::new, "screamer_horns");
        registerHat(SmartMobs.THIEF_HOOD, ThiefHoodModel.LAYER, ThiefHoodModel::new, "thief_hood");
        registerHat(SmartMobs.MEDIC_CAP, MedicCapModel.LAYER, MedicCapModel::new, "medic_cap");
        registerHat(SmartMobs.SAPPER_CAP, SapperCapModel.LAYER, SapperCapModel::new, "sapper_cap");
        registerHat(SmartMobs.GHOST_VEIL, GhostVeilModel.LAYER, GhostVeilModel::new, "ghost_veil");

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(SmartMobs.MODID, "jammer_hud"),
                JammerHud::render);

        JammerKeys.register();
        RootVisualClient.register();
        SoundWaveRenderer.register();
        SoundWaveClientNetwork.register();
    }

    private static void registerHat(Item item, ModelLayerLocation layer,
                                    Function<ModelPart, HumanoidModel<HumanoidRenderState>> modelFactory,
                                    String textureName) {
        Identifier texture = Identifier.fromNamespaceAndPath(SmartMobs.MODID,
                "textures/entity/equipment/humanoid/" + textureName + ".png");
        ArmorRenderer.register(context -> {
            HumanoidModel<HumanoidRenderState> model = modelFactory.apply(context.bakeLayer(layer));
            return (poseStack, collector, stack, state, slot, light, contextModel) -> {
                model.setupAnim(state);
                collector.submitModel(model, state, poseStack, RenderTypes.armorCutoutNoCull(texture),
                        light, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
            };
        }, item);
    }
}
