package froz8n.client;

import froz8n.SmartMobs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.function.Function;

/** Everything the mod registers on the client side. */
@EventBusSubscriber(modid = SmartMobs.MODID, value = Dist.CLIENT)
public final class SmartMobsClient {

    private SmartMobsClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        StunnedZombieRenderHandler.register();
        SoundWaveRenderer.register();
        JammerKeys.registerInput();
        RootedInputControl.register();
        RootVisualClient.register();
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MiningHelmetModel.LAYER, MiningHelmetModel::createLayer);
        event.registerLayerDefinition(GardenHatModel.LAYER, GardenHatModel::createLayer);
        event.registerLayerDefinition(CardboardBoxModel.LAYER, CardboardBoxModel::createLayer);
        event.registerLayerDefinition(BruteHelmModel.LAYER, BruteHelmModel::createLayer);
        event.registerLayerDefinition(RunnerCapModel.LAYER, RunnerCapModel::createLayer);
        event.registerLayerDefinition(ScreamerHornsModel.LAYER, ScreamerHornsModel::createLayer);
        event.registerLayerDefinition(ThiefHoodModel.LAYER, ThiefHoodModel::createLayer);
        event.registerLayerDefinition(MedicCapModel.LAYER, MedicCapModel::createLayer);
        event.registerLayerDefinition(SapperCapModel.LAYER, SapperCapModel::createLayer);
        event.registerLayerDefinition(GhostVeilModel.LAYER, GhostVeilModel::createLayer);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.ZOMBIE, SwimmingZombieRenderer::new);
    }

    @SubscribeEvent
    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        JammerKeys.registerMappings(event);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(SmartMobs.MODID, "jammer_hud"), JammerHud::render);
    }

    /** The custom head geometry for every mod hat (Forge's IClientItemExtensions hook). */
    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        registerHat(event, SmartMobs.MINING_HELMET.get(), MiningHelmetModel.LAYER, MiningHelmetModel::new);
        registerHat(event, SmartMobs.GARDEN_HAT.get(), GardenHatModel.LAYER, GardenHatModel::new);
        registerHat(event, SmartMobs.CARDBOARD_BOX.get(), CardboardBoxModel.LAYER, CardboardBoxModel::new);
        registerHat(event, SmartMobs.BRUTE_HELM.get(), BruteHelmModel.LAYER, BruteHelmModel::new);
        registerHat(event, SmartMobs.RUNNER_CAP.get(), RunnerCapModel.LAYER, RunnerCapModel::new);
        registerHat(event, SmartMobs.SCREAMER_HORNS.get(), ScreamerHornsModel.LAYER, ScreamerHornsModel::new);
        registerHat(event, SmartMobs.THIEF_HOOD.get(), ThiefHoodModel.LAYER, ThiefHoodModel::new);
        registerHat(event, SmartMobs.MEDIC_CAP.get(), MedicCapModel.LAYER, MedicCapModel::new);
        registerHat(event, SmartMobs.SAPPER_CAP.get(), SapperCapModel.LAYER, SapperCapModel::new);
        registerHat(event, SmartMobs.GHOST_VEIL.get(), GhostVeilModel.LAYER, GhostVeilModel::new);
    }

    private static void registerHat(RegisterClientExtensionsEvent event, Item item, ModelLayerLocation layer,
                                    Function<ModelPart, HumanoidModel<LivingEntity>> modelFactory) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                          EquipmentSlot slot, HumanoidModel<?> original) {
                HumanoidModel<LivingEntity> model =
                        modelFactory.apply(Minecraft.getInstance().getEntityModels().bakeLayer(layer));
                // Both are HumanoidModel; the wildcard on the vanilla side makes the copy raw.
                ((HumanoidModel) original).copyPropertiesTo(model);
                return model;
            }
        }, item);
    }
}