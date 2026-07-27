package froz8n.client;

import froz8n.SmartMobs;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Client entrypoint. 1.21.1 predates render states and the submit pipeline, so the armour
 * hook draws the baked model itself and the HUD hangs off HudRenderCallback.
 */
@Environment(EnvType.CLIENT)
public final class SmartMobsClient implements ClientModInitializer {

    private static final Map<ModelLayerLocation, HumanoidModel<LivingEntity>> HAT_MODELS = new HashMap<>();

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(MiningHelmetModel.LAYER, MiningHelmetModel::createLayer);
        EntityModelLayerRegistry.registerModelLayer(GardenHatModel.LAYER, GardenHatModel::createLayer);
        EntityModelLayerRegistry.registerModelLayer(CardboardBoxModel.LAYER, CardboardBoxModel::createLayer);
        EntityModelLayerRegistry.registerModelLayer(BruteHelmModel.LAYER, BruteHelmModel::createLayer);
        EntityModelLayerRegistry.registerModelLayer(RunnerCapModel.LAYER, RunnerCapModel::createLayer);
        EntityModelLayerRegistry.registerModelLayer(ScreamerHornsModel.LAYER, ScreamerHornsModel::createLayer);
        EntityModelLayerRegistry.registerModelLayer(ThiefHoodModel.LAYER, ThiefHoodModel::createLayer);
        EntityModelLayerRegistry.registerModelLayer(MedicCapModel.LAYER, MedicCapModel::createLayer);
        EntityModelLayerRegistry.registerModelLayer(SapperCapModel.LAYER, SapperCapModel::createLayer);
        EntityModelLayerRegistry.registerModelLayer(GhostVeilModel.LAYER, GhostVeilModel::createLayer);

        EntityRendererRegistry.register(EntityType.ZOMBIE, SwimmingZombieRenderer::new);

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

        HudRenderCallback.EVENT.register(JammerHud::render);

        JammerKeys.register();
        RootVisualClient.register();
        SoundWaveRenderer.register();
        SoundWaveClientNetwork.register();
    }

    private static void registerHat(Item item, ModelLayerLocation layer,
                                    Function<ModelPart, HumanoidModel<LivingEntity>> modelFactory,
                                    String textureName) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(SmartMobs.MODID,
                "textures/models/armor/" + textureName + "_layer_1.png");
        ArmorRenderer.register((poseStack, buffers, stack, entity, slot, light, contextModel) -> {
            HumanoidModel<LivingEntity> model = HAT_MODELS.computeIfAbsent(layer,
                    l -> modelFactory.apply(Minecraft.getInstance().getEntityModels().bakeLayer(l)));
            contextModel.copyPropertiesTo(model);
            ArmorRenderer.renderPart(poseStack, buffers, light, stack, model, texture);
        }, item);
    }
}