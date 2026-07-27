package froz8n;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
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
    // One hat per breed. They are ordinary helmet items so vanilla armour rendering does
    // the work; the geometry and texture that make each one recognisable are registered
    // client-side in froz8n.client.SmartMobsClient.
    public static final Item BRUTE_HELM = breedHat("brute_helm", 3);
    public static final Item RUNNER_CAP = breedHat("runner_cap", 1);
    public static final Item SCREAMER_HORNS = breedHat("screamer_horns", 1);
    public static final Item THIEF_HOOD = breedHat("thief_hood", 1);
    public static final Item MEDIC_CAP = breedHat("medic_cap", 1);
    public static final Item SAPPER_CAP = breedHat("sapper_cap", 1);
    public static final Item GHOST_VEIL = breedHat("ghost_veil", 0);
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

    // Creates a creative tab with the id "smartmobs:equipment" holding every public mod item.
    public static final CreativeModeTab EQUIPMENT_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MODID, "equipment")),
            FabricCreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup." + MODID + ".equipment"))
                    .icon(() -> ZOMBIE_SERUM.getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(SOUND_JAMMER);
                        output.accept(ZOMBIE_SERUM);
                        output.accept(MINING_HELMET);
                        output.accept(GARDEN_HAT);
                        output.accept(CARDBOARD_BOX);
                        output.accept(BRUTE_HELM);
                        output.accept(RUNNER_CAP);
                        output.accept(SCREAMER_HORNS);
                        output.accept(THIEF_HOOD);
                        output.accept(MEDIC_CAP);
                        output.accept(SAPPER_CAP);
                        output.accept(GHOST_VEIL);
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

        // Zombies are the whole mod, so they get a bigger share of the monster budget:
        // vanilla weights them 95 against roughly 410 on land, this takes them to about a
        // third of everything that spawns. The mob cap is untouched, only the mix.
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER,
                EntityTypes.ZOMBIE, 60, 2, 4);
        // A smaller nudge in the Nether, not the horde the old weight of 80 produced.
        BiomeModifications.addSpawn(BiomeSelectors.foundInTheNether(), MobCategory.MONSTER,
                EntityTypes.ZOMBIE, 25, 2, 3);

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

    /** A mob-only helmet: its own asset, its own model, and no repair recipe worth having. */
    private static Item breedHat(String path, int defense) {
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(
                EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, path));
        return registerItem(path, new Item(new Item.Properties().setId(itemKey(path))
                .humanoidArmor(new ArmorMaterial(5, Map.of(ArmorType.HELMET, defense), 3,
                        SoundEvents.ARMOR_EQUIP_LEATHER, 0, 0, ItemTags.REPAIRS_LEATHER_ARMOR, asset),
                        ArmorType.HELMET)));
    }

    /** The hat a given breed wears, or null for a breed that goes bare-headed. */
    public static Item breedHatFor(String breed) {
        return switch (breed) {
            case "brute" -> BRUTE_HELM;
            case "runner" -> RUNNER_CAP;
            case "screamer" -> SCREAMER_HORNS;
            case "thief" -> THIEF_HOOD;
            case "medic" -> MEDIC_CAP;
            case "sapper" -> SAPPER_CAP;
            case "ghost" -> GHOST_VEIL;
            default -> null;
        };
    }

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
