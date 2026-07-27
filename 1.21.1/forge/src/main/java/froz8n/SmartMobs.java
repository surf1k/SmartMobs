package froz8n;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SmartMobs.MODID)
public final class SmartMobs {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "smartmobs";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);
    // 1.21.1 has no equipment assets: an armour material carries its own texture layers,
    // read from assets/smartmobs/textures/models/armor/<name>_layer_1.png.
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);

    private static final RegistryObject<ArmorMaterial> MINING_HELMET_MATERIAL = armorMaterial(
            "mining_helmet", 3, 9, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT));
    private static final RegistryObject<ArmorMaterial> GARDEN_HAT_MATERIAL = armorMaterial(
            "garden_hat", 1, 5, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER));
    private static final RegistryObject<ArmorMaterial> CARDBOARD_BOX_MATERIAL = armorMaterial(
            "cardboard_box", 0, 1, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.PAPER));

    // The custom head geometry these three wear is supplied by the items themselves
    // through Item#initializeClient / IClientItemExtensions.
    public static final RegistryObject<Item> MINING_HELMET = ITEMS.register("mining_helmet",
            () -> new froz8n.client.MiningHelmetItem(MINING_HELMET_MATERIAL.getHolder().orElseThrow(),
                    new Item.Properties()));
    public static final RegistryObject<Item> GARDEN_HAT = ITEMS.register("garden_hat",
            () -> new froz8n.client.GardenHatItem(GARDEN_HAT_MATERIAL.getHolder().orElseThrow(),
                    new Item.Properties()));
    public static final RegistryObject<Item> CARDBOARD_BOX = ITEMS.register("cardboard_box",
            () -> new froz8n.client.CardboardBoxItem(CARDBOARD_BOX_MATERIAL.getHolder().orElseThrow(),
                    new Item.Properties().durability(6)));
    // One hat per breed. They are ordinary helmet items so vanilla armour rendering does
    // the work; the geometry that makes each one recognisable comes from the *Item classes
    // below through Item#initializeClient, and the texture from the material's armour layer.
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
            () -> new froz8n.combat.SoundJammerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ZOMBIE_SERUM = ITEMS.register("zombie_serum",
            () -> new froz8n.combat.ZombieSerumItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Block> GRASPING_ROOTS = BLOCKS.register("grasping_roots",
            () -> new froz8n.block.GraspingRootsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT).noCollission().noOcclusion().replaceable()
                    .instabreak().sound(net.minecraft.world.level.block.SoundType.ROOTS)));
    public static final RegistryObject<MobEffect> ZOMBIE_DISGUISE = MOB_EFFECTS.register("zombie_disguise",
            () -> new froz8n.combat.ZombieDisguiseEffect(MobEffectCategory.BENEFICIAL, 0x71852A));

    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block",
            () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .alwaysEdible().nutrition(1).saturationModifier(2f).build())));

    // Creates a creative tab with the id "smartmobs:equipment" holding every public mod item.
    public static final RegistryObject<CreativeModeTab> EQUIPMENT_TAB =
            CREATIVE_MODE_TABS.register("equipment", () -> CreativeModeTab.builder()
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
        // Forge 52 still has one mod bus per mod and one global game bus.
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(SmartMobs::onClientSetup);
        modEventBus.addListener(SmartMobs::registerLayerDefinitions);
        modEventBus.addListener(SmartMobs::registerEntityRenderers);
        modEventBus.addListener(SmartMobs::registerGuiOverlayLayers);
        modEventBus.addListener(SmartMobs::registerKeyMappings);

        // Register the Deferred Registers to the mod bus so content gets registered.
        ARMOR_MATERIALS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);

        // Register the SmartMobs gameplay handlers (command, AI, temp blocks).
        froz8n.smart.SmartMobsEvents.register();

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us.
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(froz8n.combat.SoundWaveNetwork::register);
        LOGGER.info("SmartMobs ready: miners {}%, garden {}%, breeds {}",
                Math.round(Config.smartChance * 100), Math.round(Config.gardenChance * 100),
                Config.enableBreeds ? Math.round(Config.breedChance * 100) + "%" : "off");
    }

    // Client-only setup. FMLClientSetupEvent is only fired on the physical client,
    // so this listener simply never runs on a dedicated server.
    private static void onClientSetup(FMLClientSetupEvent event) {
        froz8n.client.StunnedZombieRenderHandler.register();
        froz8n.client.SoundWaveRenderer.register();
        froz8n.client.JammerKeys.registerInput();
        froz8n.client.RootedInputControl.register();
        froz8n.client.RootVisualClient.register();
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(froz8n.client.MiningHelmetModel.LAYER,
                froz8n.client.MiningHelmetModel::createLayer);
        event.registerLayerDefinition(froz8n.client.GardenHatModel.LAYER,
                froz8n.client.GardenHatModel::createLayer);
        event.registerLayerDefinition(froz8n.client.CardboardBoxModel.LAYER,
                froz8n.client.CardboardBoxModel::createLayer);
        event.registerLayerDefinition(froz8n.client.BruteHelmModel.LAYER,
                froz8n.client.BruteHelmModel::createLayer);
        event.registerLayerDefinition(froz8n.client.RunnerCapModel.LAYER,
                froz8n.client.RunnerCapModel::createLayer);
        event.registerLayerDefinition(froz8n.client.ScreamerHornsModel.LAYER,
                froz8n.client.ScreamerHornsModel::createLayer);
        event.registerLayerDefinition(froz8n.client.ThiefHoodModel.LAYER,
                froz8n.client.ThiefHoodModel::createLayer);
        event.registerLayerDefinition(froz8n.client.MedicCapModel.LAYER,
                froz8n.client.MedicCapModel::createLayer);
        event.registerLayerDefinition(froz8n.client.SapperCapModel.LAYER,
                froz8n.client.SapperCapModel::createLayer);
        event.registerLayerDefinition(froz8n.client.GhostVeilModel.LAYER,
                froz8n.client.GhostVeilModel::createLayer);
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.ZOMBIE, froz8n.client.SwimmingZombieRenderer::new);
    }

    private static void registerGuiOverlayLayers(AddGuiOverlayLayersEvent event) {
        froz8n.client.JammerHud.addLayer(event);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        froz8n.client.JammerKeys.registerMappings(event);
    }

    private static RegistryObject<ArmorMaterial> armorMaterial(
            String path, int defense, int enchantmentValue, Holder<SoundEvent> equipSound,
            Supplier<Ingredient> repair) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, path);
        return ARMOR_MATERIALS.register(path, () -> new ArmorMaterial(
                Map.of(ArmorItem.Type.HELMET, defense), enchantmentValue, equipSound, repair,
                List.of(new ArmorMaterial.Layer(id)), 0.0F, 0.0F));
    }

    /**
     * A mob-only helmet: its own armour material, its own model, and no repair recipe worth
     * having. The factory picks the froz8n.client.*Item subclass that carries the geometry,
     * which is Forge's stand-in for Fabric's ArmorRenderer registration. On 1.21.1 the
     * material also carries the armour texture layer, read from
     * assets/smartmobs/textures/models/armor/&lt;name&gt;_layer_1.png.
     */
    private static RegistryObject<Item> breedHat(String path, int defense,
                                                 BiFunction<Holder<ArmorMaterial>, Item.Properties, Item> factory) {
        RegistryObject<ArmorMaterial> material = armorMaterial(path, defense, 3,
                SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER));
        // 1.21.1 has no durability multiplier on the material, so the canonical tree's
        // value of 5 (the same one vanilla leather armour uses) lands here instead.
        return ITEMS.register(path, () -> factory.apply(material.getHolder().orElseThrow(),
                new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(5))));
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

    // The gameplay code is shared verbatim with the Fabric tree, where these are plain
    // objects; going through accessors keeps that code identical everywhere.
    public static Item miningHelmet() { return MINING_HELMET.get(); }

    public static Item gardenHat() { return GARDEN_HAT.get(); }

    public static Item cardboardBox() { return CARDBOARD_BOX.get(); }

    public static Item soundJammer() { return SOUND_JAMMER.get(); }

    public static Block graspingRoots() { return GRASPING_ROOTS.get(); }

    public static Holder<MobEffect> zombieDisguise() { return ZOMBIE_DISGUISE.getHolder().orElseThrow(); }
}