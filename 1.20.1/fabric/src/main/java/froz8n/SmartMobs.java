package froz8n;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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
import org.slf4j.Logger;

import java.util.function.Supplier;

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

    // On 1.20.1 an armour material is a plain interface, and vanilla derives its texture
    // from getName() in the minecraft namespace. That path is never taken here: the three
    // hats draw through Fabric's ArmorRenderer, which supplies its own texture.
    private record Hat(String name, int defense, int enchantmentValue, SoundEvent equipSound,
                       Supplier<Ingredient> repair) implements ArmorMaterial {
        @Override public int getDurabilityForType(ArmorItem.Type type) { return 165; }
        @Override public int getDefenseForType(ArmorItem.Type type) {
            return type == ArmorItem.Type.HELMET ? defense : 0;
        }
        @Override public int getEnchantmentValue() { return enchantmentValue; }
        @Override public SoundEvent getEquipSound() { return equipSound; }
        @Override public Ingredient getRepairIngredient() { return repair.get(); }
        @Override public String getName() { return MODID + "_" + name; }
        @Override public float getToughness() { return 0.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    }

    private static final ArmorMaterial MINING_HELMET_MATERIAL =
            new Hat("mining_helmet", 3, 9, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT));
    private static final ArmorMaterial GARDEN_HAT_MATERIAL =
            new Hat("garden_hat", 1, 5, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER));
    private static final ArmorMaterial CARDBOARD_BOX_MATERIAL =
            new Hat("cardboard_box", 0, 1, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.PAPER));

    // The custom head geometry these three wear is supplied client-side by
    // froz8n.client.SmartMobsClient through Fabric's ArmorRenderer.
    public static final Item MINING_HELMET = registerItem("mining_helmet",
            new ArmorItem(MINING_HELMET_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final Item GARDEN_HAT = registerItem("garden_hat",
            new ArmorItem(GARDEN_HAT_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final Item CARDBOARD_BOX = registerItem("cardboard_box",
            new ArmorItem(CARDBOARD_BOX_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(6)));
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
            new froz8n.combat.SoundJammerItem(new Item.Properties().stacksTo(1)));
    public static final Item ZOMBIE_SERUM = registerItem("zombie_serum",
            new froz8n.combat.ZombieSerumItem(new Item.Properties().stacksTo(16)));
    public static final Block GRASPING_ROOTS = registerBlock("grasping_roots",
            new froz8n.block.GraspingRootsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT).noCollission().noOcclusion().replaceable()
                    .instabreak().sound(net.minecraft.world.level.block.SoundType.ROOTS)));
    public static final MobEffect ZOMBIE_DISGUISE = Registry.register(
            BuiltInRegistries.MOB_EFFECT, new ResourceLocation(MODID, "zombie_disguise"),
            new froz8n.combat.ZombieDisguiseEffect(MobEffectCategory.BENEFICIAL, 0x71852A));

    public static final Block EXAMPLE_BLOCK = registerBlock("example_block",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    public static final Item EXAMPLE_BLOCK_ITEM = registerItem("example_block",
            new BlockItem(EXAMPLE_BLOCK, new Item.Properties()));

    public static final Item EXAMPLE_ITEM = registerItem("example_item",
            new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .alwaysEat().nutrition(1).saturationMod(2f).build())));

    // Creates a creative tab with the id "smartmobs:equipment" holding every public mod item.
    public static final CreativeModeTab EQUIPMENT_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            new ResourceLocation(MODID, "equipment"),
            FabricItemGroup.builder()
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

        // Channels have to be known to both sides before play starts.
        froz8n.combat.SoundWaveNetwork.register();
        froz8n.smart.viz.PathVizNetwork.register();

        // Register the SmartMobs gameplay handlers (command, AI, temp blocks).
        froz8n.smart.SmartMobsEvents.register();

        // Zombies are the whole mod, so they get a bigger share of the monster budget:
        // vanilla weights them 95 against roughly 410 on land, this takes them to about a
        // third of everything that spawns. The mob cap is untouched, only the mix.
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER,
                EntityType.ZOMBIE, 60, 2, 4);
        // A smaller nudge in the Nether, not the horde the old weight of 80 produced.
        BiomeModifications.addSpawn(BiomeSelectors.foundInTheNether(), MobCategory.MONSTER,
                EntityType.ZOMBIE, 25, 2, 3);

        LOGGER.info("SmartMobs ready: miners {}%, garden {}%, breeds {}",
                Math.round(Config.smartChance * 100), Math.round(Config.gardenChance * 100),
                Config.enableBreeds ? Math.round(Config.breedChance * 100) + "%" : "off");
    }

    // The gameplay code is shared verbatim with the Forge tree, where these are
    // RegistryObjects; going through accessors keeps that code identical everywhere.
    public static Item miningHelmet() { return MINING_HELMET; }

    public static Item gardenHat() { return GARDEN_HAT; }

    public static Item cardboardBox() { return CARDBOARD_BOX; }

    public static Item soundJammer() { return SOUND_JAMMER; }

    public static Block graspingRoots() { return GRASPING_ROOTS; }

    public static MobEffect zombieDisguise() { return ZOMBIE_DISGUISE; }

    /**
     * A mob-only helmet: its own armour material, its own model, and no repair recipe worth
     * having. On 1.20.1 the material's own texture path is never read - the hat draws through
     * Fabric's ArmorRenderer, which points at
     * assets/smartmobs/textures/models/armor/&lt;path&gt;_layer_1.png itself.
     */
    private static Item breedHat(String path, int defense) {
        ArmorMaterial material = new Hat(path, defense, 3,
                SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER));
        return registerItem(path, new ArmorItem(material, ArmorItem.Type.HELMET, new Item.Properties()));
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
        return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MODID, path), item);
    }

    private static <T extends Block> T registerBlock(String path, T block) {
        return Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(MODID, path), block);
    }
}