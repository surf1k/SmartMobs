package froz8n;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
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
import org.slf4j.Logger;

import java.util.Map;

/**
 * Mod entrypoint. {@link #MODID} must match the "id" field in fabric.mod.json.
 *
 * <p>Fabric has no deferred registers: registry objects are created and registered right
 * here, in static initialisers that run when Loader constructs this entrypoint - which is
 * inside the registration window, before the game freezes its registries.
 */
public final class SmartMobs implements ModInitializer {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "smartmobs";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

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
    // froz8n.client.SmartMobsClient through Fabric's ArmorRenderer, which replaces
    // Forge's IClientItemExtensions#getHumanoidArmorModel.
    public static final Item MINING_HELMET = registerItem("mining_helmet",
            new Item(new Item.Properties().setId(itemKey("mining_helmet"))
                    .humanoidArmor(MINING_HELMET_MATERIAL, ArmorType.HELMET)));
    public static final Item GARDEN_HAT = registerItem("garden_hat",
            new Item(new Item.Properties().setId(itemKey("garden_hat"))
                    .humanoidArmor(new ArmorMaterial(5, Map.of(ArmorType.HELMET, 1), 5,
                            SoundEvents.ARMOR_EQUIP_LEATHER, 0, 0, ItemTags.REPAIRS_LEATHER_ARMOR,
                            GARDEN_HAT_ASSET), ArmorType.HELMET)));
    public static final Item CARDBOARD_BOX = registerItem("cardboard_box",
            new Item(new Item.Properties().setId(itemKey("cardboard_box")).durability(6)
                    .humanoidArmor(new ArmorMaterial(3, Map.of(ArmorType.HELMET, 0), 1, SoundEvents.ARMOR_EQUIP_LEATHER,
                            0, 0, ItemTags.REPAIRS_LEATHER_ARMOR, CARDBOARD_BOX_ASSET), ArmorType.HELMET)));
    public static final Item SOUND_JAMMER = registerItem("sound_jammer",
            new froz8n.combat.SoundJammerItem(new Item.Properties().setId(itemKey("sound_jammer")).stacksTo(1)));
    public static final Item ZOMBIE_SERUM = registerItem("zombie_serum",
            new froz8n.combat.ZombieSerumItem(new Item.Properties().setId(itemKey("zombie_serum")).stacksTo(16)));
    public static final Block GRASPING_ROOTS = registerBlock("grasping_roots",
            new froz8n.block.GraspingRootsBlock(BlockBehaviour.Properties.of().setId(blockKey("grasping_roots"))
                    .mapColor(MapColor.PLANT).noCollision().noOcclusion().replaceable()
                    .instabreak().sound(net.minecraft.world.level.block.SoundType.ROOTS)));
    public static final Holder<MobEffect> ZOMBIE_DISGUISE = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(MODID, "zombie_disguise"),
            new froz8n.combat.ZombieDisguiseEffect(MobEffectCategory.BENEFICIAL, 0x71852A));

    // Creates a new Block with the id "smartmobs:example_block", combining the namespace and path.
    // Since MC 1.21.x every Block/Item requires its registry id to be set on the Properties
    // (BlockBehaviour.Properties#setId / Item.Properties#setId), otherwise construction throws
    // "id not set".
    public static final Block EXAMPLE_BLOCK = registerBlock("example_block",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).setId(blockKey("example_block"))));
    // Creates a new BlockItem with the id "smartmobs:example_block", combining the namespace and path
    public static final Item EXAMPLE_BLOCK_ITEM = registerItem("example_block",
            new BlockItem(EXAMPLE_BLOCK, new Item.Properties().setId(itemKey("example_block"))));

    // Creates a new food item with the id "smartmobs:example_item", nutrition 1 and saturation 2
    public static final Item EXAMPLE_ITEM = registerItem("example_item",
            new Item(new Item.Properties().setId(itemKey("example_item")).food(new FoodProperties.Builder()
                    .alwaysEdible().nutrition(1).saturationModifier(2f).build())));

    // Creates a creative tab with the id "smartmobs:equipment" holding every public mod item.
    public static final CreativeModeTab EQUIPMENT_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MODID, "equipment")),
            FabricItemGroup.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup." + MODID + ".equipment"))
                    .icon(() -> ZOMBIE_SERUM.getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(SOUND_JAMMER);
                        output.accept(ZOMBIE_SERUM);
                        output.accept(MINING_HELMET);
                        output.accept(GARDEN_HAT);
                        output.accept(CARDBOARD_BOX);
                    }).build());

    @Override
    public void onInitialize() {
        // Read (and create, on first run) the mod config file.
        Config.load();

        // Payload types have to be known to both sides before play starts.
        froz8n.combat.SoundWaveNetwork.register();
        froz8n.smart.viz.PathVizNetwork.register();

        // Register the SmartMobs gameplay handlers (command, AI, temp blocks).
        froz8n.smart.SmartMobsEvents.register();

        // A modest nudge, not the horde the old weight of 80 produced.
        BiomeModifications.addSpawn(BiomeSelectors.foundInTheNether(), MobCategory.MONSTER,
                EntityType.ZOMBIE, 25, 2, 3);

        LOGGER.info("SmartMobs ready: miners {}%, garden {}%, breeds {}",
                Math.round(Config.smartChance * 100), Math.round(Config.gardenChance * 100),
                Config.enableBreeds ? Math.round(Config.breedChance * 100) + "%" : "off");
    }

    // The gameplay code is shared verbatim with the NeoForge and Forge trees, where these
    // are DeferredHolders; going through accessors keeps that code identical everywhere.
    public static Item miningHelmet() { return MINING_HELMET; }

    public static Item gardenHat() { return GARDEN_HAT; }

    public static Item cardboardBox() { return CARDBOARD_BOX; }

    public static Item soundJammer() { return SOUND_JAMMER; }

    public static Block graspingRoots() { return GRASPING_ROOTS; }

    public static Holder<MobEffect> zombieDisguise() { return ZOMBIE_DISGUISE; }

    private static <T extends Item> T registerItem(String path, T item) {
        return Registry.register(BuiltInRegistries.ITEM, itemKey(path), item);
    }

    private static <T extends Block> T registerBlock(String path, T block) {
        return Registry.register(BuiltInRegistries.BLOCK, blockKey(path), block);
    }

    // Builds the registry key required by BlockBehaviour.Properties#setId in MC 1.21.x.
    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, path));
    }

    // Builds the registry key required by Item.Properties#setId in MC 1.21.x.
    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, path));
    }
}
