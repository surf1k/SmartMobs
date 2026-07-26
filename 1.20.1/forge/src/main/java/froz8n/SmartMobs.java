package froz8n;

import com.mojang.logging.LogUtils;
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
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

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
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);

    // On 1.20.1 an armour material is a plain interface. The texture comes from
    // Item#getArmorTexture on the three hat items, so getName() only has to be unique.
    private record Hat(String name, int defense, int enchantmentValue, SoundEvent equipSound,
                       Supplier<Ingredient> repair) implements ArmorMaterial {
        @Override public int getDurabilityForType(ArmorItem.Type type) { return 165; }
        @Override public int getDefenseForType(ArmorItem.Type type) {
            return type == ArmorItem.Type.HELMET ? defense : 0;
        }
        @Override public int getEnchantmentValue() { return enchantmentValue; }
        @Override public SoundEvent getEquipSound() { return equipSound; }
        @Override public Ingredient getRepairIngredient() { return repair.get(); }
        @Override public String getName() { return MODID + ":" + name; }
        @Override public float getToughness() { return 0.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    }

    private static final ArmorMaterial MINING_HELMET_MATERIAL =
            new Hat("mining_helmet", 3, 9, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT));
    private static final ArmorMaterial GARDEN_HAT_MATERIAL =
            new Hat("garden_hat", 1, 5, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER));
    private static final ArmorMaterial CARDBOARD_BOX_MATERIAL =
            new Hat("cardboard_box", 0, 1, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.PAPER));

    // The custom head geometry these three wear is supplied by the items themselves
    // through Item#initializeClient / IClientItemExtensions.
    public static final RegistryObject<Item> MINING_HELMET = ITEMS.register("mining_helmet",
            () -> new froz8n.client.MiningHelmetItem(MINING_HELMET_MATERIAL, new Item.Properties()));
    public static final RegistryObject<Item> GARDEN_HAT = ITEMS.register("garden_hat",
            () -> new froz8n.client.GardenHatItem(GARDEN_HAT_MATERIAL, new Item.Properties()));
    public static final RegistryObject<Item> CARDBOARD_BOX = ITEMS.register("cardboard_box",
            () -> new froz8n.client.CardboardBoxItem(CARDBOARD_BOX_MATERIAL, new Item.Properties().durability(6)));
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
                    .alwaysEat().nutrition(1).saturationMod(2f).build())));

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
                    }).build());

    public SmartMobs() {
        // Forge 47 has no constructor injection: the contexts are thread-locals.
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(SmartMobs::onClientSetup);
        modEventBus.addListener(SmartMobs::registerLayerDefinitions);
        modEventBus.addListener(SmartMobs::registerEntityRenderers);
        modEventBus.addListener(SmartMobs::registerGuiOverlays);
        modEventBus.addListener(SmartMobs::registerKeyMappings);

        // Register the Deferred Registers to the mod bus so content gets registered.
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);

        // Register the SmartMobs gameplay handlers (command, AI, temp blocks).
        froz8n.smart.SmartMobsEvents.register();

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
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
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.ZOMBIE, froz8n.client.SwimmingZombieRenderer::new);
    }

    private static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        froz8n.client.JammerHud.addLayer(event);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        froz8n.client.JammerKeys.registerMappings(event);
    }

    // The gameplay code is shared verbatim with the Fabric tree, where these are plain
    // objects; going through accessors keeps that code identical everywhere.
    public static Item miningHelmet() { return MINING_HELMET.get(); }

    public static Item gardenHat() { return GARDEN_HAT.get(); }

    public static Item cardboardBox() { return CARDBOARD_BOX.get(); }

    public static Item soundJammer() { return SOUND_JAMMER.get(); }

    public static Block graspingRoots() { return GRASPING_ROOTS.get(); }

    public static MobEffect zombieDisguise() { return ZOMBIE_DISGUISE.get(); }
}