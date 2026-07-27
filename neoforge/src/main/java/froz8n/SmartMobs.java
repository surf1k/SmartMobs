package froz8n;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.Map;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SmartMobs.MODID)
public final class SmartMobs {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "smartmobs";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "smartmobs" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "smartmobs" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "smartmobs" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);

    private static final ResourceKey<EquipmentAsset> MINING_HELMET_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "mining_helmet"));
    private static final ResourceKey<EquipmentAsset> GARDEN_HAT_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "garden_hat"));
    private static final ResourceKey<EquipmentAsset> CARDBOARD_BOX_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "cardboard_box"));
    private static final ArmorMaterial MINING_HELMET_MATERIAL = new ArmorMaterial(
            15, Map.of(ArmorType.HELMET, 3), 9, SoundEvents.ARMOR_EQUIP_IRON,
            0.0F, 0.0F, ItemTags.REPAIRS_IRON_ARMOR, MINING_HELMET_ASSET);

    // The custom head geometry these three wear is supplied client-side by
    // froz8n.client.SmartMobsClient through RegisterClientExtensionsEvent.
    public static final DeferredHolder<Item, Item> MINING_HELMET = ITEMS.register("mining_helmet",
            () -> new Item(new Item.Properties().setId(itemKey("mining_helmet"))
                    .humanoidArmor(MINING_HELMET_MATERIAL, ArmorType.HELMET)));
    public static final DeferredHolder<Item, Item> GARDEN_HAT = ITEMS.register("garden_hat",
            () -> new Item(new Item.Properties().setId(itemKey("garden_hat"))
                    .humanoidArmor(new ArmorMaterial(5, Map.of(ArmorType.HELMET, 1), 5,
                            SoundEvents.ARMOR_EQUIP_LEATHER, 0, 0, ItemTags.REPAIRS_LEATHER_ARMOR,
                            GARDEN_HAT_ASSET), ArmorType.HELMET)));
    public static final DeferredHolder<Item, Item> CARDBOARD_BOX = ITEMS.register("cardboard_box",
            () -> new Item(new Item.Properties().setId(itemKey("cardboard_box")).durability(6)
                    .humanoidArmor(new ArmorMaterial(3, Map.of(ArmorType.HELMET, 0), 1, SoundEvents.ARMOR_EQUIP_LEATHER,
                            0, 0, ItemTags.REPAIRS_LEATHER_ARMOR, CARDBOARD_BOX_ASSET), ArmorType.HELMET)));
    // One hat per breed. They are ordinary helmet items so vanilla armour rendering does
    // the work; the geometry and texture that make each one recognisable are registered
    // client-side in froz8n.client.SmartMobsClient.
    public static final DeferredHolder<Item, Item> BRUTE_HELM = breedHat("brute_helm", 3);
    public static final DeferredHolder<Item, Item> RUNNER_CAP = breedHat("runner_cap", 1);
    public static final DeferredHolder<Item, Item> SCREAMER_HORNS = breedHat("screamer_horns", 1);
    public static final DeferredHolder<Item, Item> THIEF_HOOD = breedHat("thief_hood", 1);
    public static final DeferredHolder<Item, Item> MEDIC_CAP = breedHat("medic_cap", 1);
    public static final DeferredHolder<Item, Item> SAPPER_CAP = breedHat("sapper_cap", 1);
    public static final DeferredHolder<Item, Item> GHOST_VEIL = breedHat("ghost_veil", 0);
    public static final DeferredHolder<Item, Item> SOUND_JAMMER = ITEMS.register("sound_jammer",
            () -> new froz8n.combat.SoundJammerItem(new Item.Properties().setId(itemKey("sound_jammer")).stacksTo(1)));
    public static final DeferredHolder<Item, Item> ZOMBIE_SERUM = ITEMS.register("zombie_serum",
            () -> new froz8n.combat.ZombieSerumItem(new Item.Properties().setId(itemKey("zombie_serum")).stacksTo(16)));
    public static final DeferredHolder<Block, Block> GRASPING_ROOTS = BLOCKS.register("grasping_roots",
            () -> new froz8n.block.GraspingRootsBlock(BlockBehaviour.Properties.of().setId(blockKey("grasping_roots"))
                    .mapColor(MapColor.PLANT).noCollision().noOcclusion().replaceable()
                    .instabreak().sound(net.minecraft.world.level.block.SoundType.ROOTS)));
    public static final DeferredHolder<MobEffect, MobEffect> ZOMBIE_DISGUISE = MOB_EFFECTS.register("zombie_disguise",
            () -> new froz8n.combat.ZombieDisguiseEffect(MobEffectCategory.BENEFICIAL, 0x71852A));

    // Creates a creative tab with the id "smartmobs:equipment" holding every public mod item.
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EQUIPMENT_TAB =
            CREATIVE_MODE_TABS.register("equipment", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
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

    public SmartMobs(IEventBus modEventBus, ModContainer modContainer) {
        // Register the Deferred Registers to the mod bus so content gets registered.
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);

        // Mod-bus lifecycle.
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(froz8n.combat.SoundWaveNetwork::register);

        // Game-bus handlers.
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        // Register the SmartMobs gameplay handlers (command, AI, temp blocks).
        froz8n.smart.SmartMobsEvents.register();

        // Register our mod's ModConfigSpec so that NeoForge can create and load the config file for us.
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("SmartMobs ready: miners {}%, garden {}%, breeds {}",
                Math.round(Config.smartChance * 100), Math.round(Config.gardenChance * 100),
                Config.enableBreeds ? Math.round(Config.breedChance * 100) + "%" : "off");
    }

    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    /** A mob-only helmet: its own asset, its own model, and no repair recipe worth having. */
    private static DeferredHolder<Item, Item> breedHat(String path, int defense) {
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(
                EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, path));
        return ITEMS.register(path, () -> new Item(new Item.Properties().setId(itemKey(path))
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

    public static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> zombieDisguise() { return ZOMBIE_DISGUISE; }
}