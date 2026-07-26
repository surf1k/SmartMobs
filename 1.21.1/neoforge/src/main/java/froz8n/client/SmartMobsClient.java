package froz8n.client;

import froz8n.SmartMobs;
import froz8n.combat.SoundWaveNetwork;
import froz8n.smart.viz.ClientPathStore;
import froz8n.smart.viz.PathVizNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
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
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

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
        event.registerAboveAll(Identifier.fromNamespaceAndPath(SmartMobs.MODID, "jammer_hud"), JammerHud::render);
    }

    /** The custom head geometry for the three hats (Forge's IClientItemExtensions hook). */
    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        registerHat(event, SmartMobs.MINING_HELMET.get(), MiningHelmetModel.LAYER, MiningHelmetModel::new);
        registerHat(event, SmartMobs.GARDEN_HAT.get(), GardenHatModel.LAYER, GardenHatModel::new);
        registerHat(event, SmartMobs.CARDBOARD_BOX.get(), CardboardBoxModel.LAYER, CardboardBoxModel::new);
    }

    @SubscribeEvent
    static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(SoundWaveNetwork.Start.TYPE, (payload, context) ->
                context.enqueueWork(() -> SoundWaveRenderer.activate(payload.playerId())));
        event.register(SoundWaveNetwork.Status.TYPE, (payload, context) ->
                context.enqueueWork(() -> JammerHud.update(payload.mode(), payload.downTicks(), payload.upTicks())));
        event.register(SoundWaveNetwork.Rooted.TYPE, (payload, context) ->
                context.enqueueWork(() -> RootedInputControl.rootFor(payload.durationTicks())));
        event.register(SoundWaveNetwork.RootBurst.TYPE, (payload, context) ->
                context.enqueueWork(() -> RootVisualClient.activate(payload.targetId(), payload.x(), payload.y(),
                        payload.z(), payload.seed(), payload.durationTicks())));
        event.register(PathVizNetwork.Payload.TYPE, (payload, context) ->
                context.enqueueWork(() -> ClientPathStore.put(payload.data())));
    }

    private static void registerHat(RegisterClientExtensionsEvent event, Item item, ModelLayerLocation layer,
                                    Function<ModelPart, HumanoidModel<HumanoidRenderState>> modelFactory) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original) {
                return modelFactory.apply(Minecraft.getInstance().getEntityModels().bakeLayer(layer));
            }
        }, item);
    }
}
