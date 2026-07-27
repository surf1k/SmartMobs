package froz8n;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.Map;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SmartMobs.MODID)
public final class SmartMobs {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "smartmobs";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "smartmobs" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "smartmobs" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "smartmobs" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);

    private static final ResourceKey<EquipmentAsset> MINING_HELMET_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "mining_helmet"));
    private static final ResourceKey<EquipmentAsset> GARDEN_HAT_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "garden_hat"));
    private static final ResourceKey<EquipmentAsset> CARDBOARD_BOX_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "cardboard_box"));
    private static final ArmorMaterial MINING_HELMET_MATERIAL = new ArmorMaterial(
            15, Map.of(ArmorType.HELMET, 3), 9, SoundEvents.ARMOR_EQUIP_IRON,
            0.0F, 0.0F, ItemTags.REPAIRS_IRON_ARMOR, MINING_HELMET_ASSET);
    public static final RegistryObject<Item> MINING_HELMET = ITEMS.register("mining_helmet",
            () -> new froz8n.client.MiningHelmetItem(new Item.Properties().setId(itemKey("mining_helmet"))
                    .humanoidArmor(MINING_HELMET_MATERIAL, ArmorType.HELMET)));
    public static final RegistryObject<Item> GARDEN_HAT = ITEMS.register("garden_hat",
            () -> new froz8n.client.GardenHatItem(new Item.Properties().setId(itemKey("garden_hat"))
                    .humanoidArmor(new ArmorMaterial(5, Map.of(ArmorType.HELMET, 1), 5,
                            SoundEvents.ARMOR_EQUIP_LEATHER, 0, 0, ItemTags.REPAIRS_LEATHER_ARMOR,
                            GARDEN_HAT_ASSET), ArmorType.HELMET)));
    public static final RegistryObject<Item> CARDBOARD_BOX = ITEMS.register("cardboard_box",
            () -> new froz8n.client.CardboardBoxItem(new Item.Properties().setId(itemKey("cardboard_box")).durability(6)
                    .humanoidArmor(new ArmorMaterial(3,Map.of(ArmorType.HELMET,0),1,SoundEvents.ARMOR_EQUIP_LEATHER,
                            0,0,ItemTags.REPAIRS_LEATHER_ARMOR,CARDBOARD_BOX_ASSET),ArmorType.HELMET)));
    // One hat per breed. They are ordinary helmet items so vanilla armour rendering does
    // the work; the geometry and texture that make each one recognisable come from the
    // matching froz8n.client.*Item / *Model pair.
    public static final RegistryObject<Item> BRUTE_HELM =
            breedHat("brute_helm", 3, froz8n.client.BruteHelmItem::new);
    public static final RegistryObject<Item> RUNNER_CAP =
            breedHat("runner_cap", 1, froz8n.client.RunnerCapItem::new);
    public static final RegistryObject<Item> SCREAMER_HORNS =
            breedHat("screamer_horns", 1, froz8n.client.ScreamerHornsItem::new);
    public static final RegistryObject<Item> THIEF_HOOD =
            breedHat("thief_hood", 1, froz8n.client.ThiefHoodItem::new);
    public static final RegistryObject<Item> MEDIC_CAP =
            breedHat("medic_cap", 1, froz8n.client.MedicCapItem::new);
    public static final RegistryObject<Item> SAPPER_CAP =
            breedHat("sapper_cap", 1, froz8n.client.SapperCapItem::new);
    public static final RegistryObject<Item> GHOST_VEIL =
            breedHat("ghost_veil", 0, froz8n.client.GhostVeilItem::new);
    public static final RegistryObject<Item> SOUND_JAMMER = ITEMS.register("sound_jammer",
            () -> new froz8n.combat.SoundJammerItem(new Item.Properties().setId(itemKey("sound_jammer")).stacksTo(1)));
    public static final RegistryObject<Item> ZOMBIE_SERUM = ITEMS.register("zombie_serum",
            () -> new froz8n.combat.ZombieSerumItem(new Item.Properties().setId(itemKey("zombie_serum")).stacksTo(16)));
    public static final RegistryObject<Block> GRASPING_ROOTS = BLOCKS.register("grasping_roots",
            () -> new froz8n.block.GraspingRootsBlock(BlockBehaviour.Properties.of().setId(blockKey("grasping_roots"))
                    .mapColor(MapColor.PLANT).noCollision().noOcclusion().replaceable()
                    .instabreak().sound(net.minecraft.world.level.block.SoundType.ROOTS)));
    public static final RegistryObject<MobEffect> ZOMBIE_DISGUISE = MOB_EFFECTS.register("zombie_disguise",
            () -> new froz8n.combat.ZombieDisguiseEffect(MobEffectCategory.BENEFICIAL, 0x71852A));

    // Creates a creative tab with the id "smartmobs:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EQUIPMENT_TAB = CREATIVE_MODE_TABS.register("equipment", () -> CreativeModeTab.builder()
            // 1.21.11 removed Builder.withTabsBefore(...); tab ordering is no longer set here.
            .title(net.minecraft.network.chat.Component.translatable("itemGroup." + MODID + ".equipment"))
            .icon(() -> ZOMBIE_SERUM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(SOUND_JAMMER.get());
                output.accept(ZOMBIE_SERUM.get());
                output.accept(MINING_HELMET.get());
                output.accept(GARDEN_HAT.get());
                output.accept(CARDBOARD_BOX.get());
                output.accept(BRUTE_HELM.get());
                output.accept(RUNNER_CAP.get());
                output.accept(SCREAMER_HORNS.get());
                output.accept(THIEF_HOOD.get());
                output.accept(MEDIC_CAP.get());
                output.accept(SAPPER_CAP.get());
                output.accept(GHOST_VEIL.get());
            }).build());

    public SmartMobs(FMLJavaModLoadingContext context) {
        // In EventBus 7 the mod bus is addressed through a BusGroup rather than an IEventBus.
        BusGroup modBusGroup = context.getModBusGroup();

        // Register the mod-lifecycle listeners (mod-bus events -> obtained via getBus(group)).
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
        FMLClientSetupEvent.getBus(modBusGroup).addListener(SmartMobs::onClientSetup);
        EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusGroup)
                .addListener(froz8n.client.MiningHelmetModel::registerLayer);
        EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusGroup)
                .addListener(froz8n.client.GardenHatModel::registerLayer);
        EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusGroup)
                .addListener(froz8n.client.CardboardBoxModel::registerLayer);
        EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusGroup)
                .addListener(froz8n.client.BruteHelmModel::registerLayer);
        EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusGroup)
                .addListener(froz8n.client.RunnerCapModel::registerLayer);
        EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusGroup)
                .addListener(froz8n.client.ScreamerHornsModel::registerLayer);
        EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusGroup)
                .addListener(froz8n.client.ThiefHoodModel::registerLayer);
        EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusGroup)
                .addListener(froz8n.client.MedicCapModel::registerLayer);
        EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusGroup)
                .addListener(froz8n.client.SapperCapModel::registerLayer);
        EntityRenderersEvent.RegisterLayerDefinitions.getBus(modBusGroup)
                .addListener(froz8n.client.GhostVeilModel::registerLayer);
        EntityRenderersEvent.RegisterRenderers.getBus(modBusGroup)
                .addListener(SmartMobs::registerEntityRenderers);
        net.minecraftforge.client.event.AddGuiOverlayLayersEvent.getBus(modBusGroup)
                .addListener(froz8n.client.JammerHud::addLayer);
        net.minecraftforge.client.event.RegisterKeyMappingsEvent.getBus(modBusGroup)
                .addListener(froz8n.client.JammerKeys::registerMappings);

        // Register the Deferred Registers to the mod bus so content gets registered.
        BLOCKS.register(modBusGroup);
        ITEMS.register(modBusGroup);
        CREATIVE_MODE_TABS.register(modBusGroup);
        MOB_EFFECTS.register(modBusGroup);

        // Register the item to a creative tab. This is a game-bus event, so it is
        // addressed through its own static BUS rather than the mod bus group.

        // Register our game-bus handlers. In EventBus 7 every event carries its own
        // bus, so we register listeners directly against each event's bus.
        ServerStartingEvent.BUS.addListener(this::onServerStarting);
        // Register the SmartMobs gameplay handlers (command, AI, temp blocks).
        froz8n.smart.SmartMobsEvents.register();

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us.
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(froz8n.combat.SoundWaveNetwork::register);
        // Some common setup code
        LOGGER.info("SmartMobs ready: miners {}%, garden {}%, breeds {}",
                Math.round(Config.smartChance * 100), Math.round(Config.gardenChance * 100),
                Config.enableBreeds ? Math.round(Config.breedChance * 100) + "%" : "off");
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Intentionally empty: all public mod items live in our own clean tab.
    }

    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // Client-only setup. FMLClientSetupEvent is only fired on the physical client,
    // so this listener simply never runs on a dedicated server.
    private static void onClientSetup(FMLClientSetupEvent event) {
        froz8n.client.StunnedZombieRenderHandler.register();
        froz8n.client.SoundWaveRenderer.register();
        froz8n.client.JammerKeys.registerInput();
        froz8n.client.RootedInputControl.register();
        froz8n.client.RootVisualClient.register();
        LOGGER.info("HELLO FROM CLIENT SETUP");
        LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.ZOMBIE, froz8n.client.SwimmingZombieRenderer::new);
    }

    /**
     * A mob-only helmet: its own equipment asset, its own model, and no repair recipe worth
     * having. The factory picks the froz8n.client.*Item subclass that carries the geometry,
     * which is Forge's stand-in for Fabric's ArmorRenderer registration.
     */
    private static RegistryObject<Item> breedHat(String path, int defense,
                                                 java.util.function.Function<Item.Properties, Item> factory) {
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(
                EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, path));
        return ITEMS.register(path, () -> factory.apply(new Item.Properties().setId(itemKey(path))
                .humanoidArmor(new ArmorMaterial(5, Map.of(ArmorType.HELMET, defense), 3,
                        SoundEvents.ARMOR_EQUIP_LEATHER, 0, 0, ItemTags.REPAIRS_LEATHER_ARMOR, asset),
                        ArmorType.HELMET)));
    }

    /** The hat a given breed wears, or null for a breed that goes bare-headed. */
    public static net.minecraft.world.item.Item breedHatFor(String breed) {
        return switch (breed) {
            case "brute" -> BRUTE_HELM.get();
            case "runner" -> RUNNER_CAP.get();
            case "screamer" -> SCREAMER_HORNS.get();
            case "thief" -> THIEF_HOOD.get();
            case "medic" -> MEDIC_CAP.get();
            case "sapper" -> SAPPER_CAP.get();
            case "ghost" -> GHOST_VEIL.get();
            default -> null;
        };
    }

    // Builds the registry key required by BlockBehaviour.Properties#setId in MC 1.21.x.
    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, path));
    }

    // Builds the registry key required by Item.Properties#setId in MC 1.21.x.
    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, path));
    }

    // The gameplay code is shared verbatim with the Fabric tree, where these are plain
    // objects; going through accessors keeps that code identical everywhere.
    public static net.minecraft.world.item.Item miningHelmet() { return MINING_HELMET.get(); }

    public static net.minecraft.world.item.Item gardenHat() { return GARDEN_HAT.get(); }

    public static net.minecraft.world.item.Item cardboardBox() { return CARDBOARD_BOX.get(); }

    public static net.minecraft.world.item.Item soundJammer() { return SOUND_JAMMER.get(); }

    public static net.minecraft.world.level.block.Block graspingRoots() { return GRASPING_ROOTS.get(); }

    public static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> zombieDisguise() { return ZOMBIE_DISGUISE.getHolder().orElseThrow(); }
}