package froz8n;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SmartMobs.MODID)
public final class SmartMobs {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "smartmobs";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    // 1.21.1 has no equipment assets: an armour material carries its own texture layers,
    // read from assets/smartmobs/textures/models/armor/<name>_layer_1.png.
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);

    private static final DeferredHolder<ArmorMaterial, ArmorMaterial> MINING_HELMET_MATERIAL = armorMaterial(
            "mining_helmet", 3, 9, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT));
    private static final DeferredHolder<ArmorMaterial, ArmorMaterial> GARDEN_HAT_MATERIAL = armorMaterial(
            "garden_hat", 1, 5, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER));
    private static final DeferredHolder<ArmorMaterial, ArmorMaterial> CARDBOARD_BOX_MATERIAL = armorMaterial(
            "cardboard_box", 0, 1, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.PAPER));

    // The custom head geometry these three wear is supplied client-side by
    // froz8n.client.SmartMobsClient through RegisterClientExtensionsEvent.
    public static final DeferredHolder<Item, Item> MINING_HELMET = ITEMS.register("mining_helmet",
            () -> new ArmorItem(MINING_HELMET_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final DeferredHolder<Item, Item> GARDEN_HAT = ITEMS.register("garden_hat",
            () -> new ArmorItem(GARDEN_HAT_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final DeferredHolder<Item, Item> CARDBOARD_BOX = ITEMS.register("cardboard_box",
            () -> new ArmorItem(CARDBOARD_BOX_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(6)));
    // One hat per breed. They are ordinary helmet items so vanilla armour rendering does
    // the work; the geometry that makes each one recognisable is registered client-side in
    // froz8n.client.SmartMobsClient, and the texture comes from each hat's own armour material.
    public static final DeferredHolder<Item, Item> BRUTE_HELM = breedHat("brute_helm", 3);
    public static final DeferredHolder<Item, Item> RUNNER_CAP = breedHat("runner_cap", 1);
    public static final DeferredHolder<Item, Item> SCREAMER_HORNS = breedHat("screamer_horns", 1);
    public static final DeferredHolder<Item, Item> THIEF_HOOD = breedHat("thief_hood", 1);
    public static final DeferredHolder<Item, Item> MEDIC_CAP = breedHat("medic_cap", 1);
    public static final DeferredHolder<Item, Item> SAPPER_CAP = breedHat("sapper_cap", 1);
    public static final DeferredHolder<Item, Item> GHOST_VEIL = breedHat("ghost_veil", 0);
    public static final DeferredHolder<Item, Item> SOUND_JAMMER = ITEMS.register("sound_jammer",
            () -> new froz8n.combat.SoundJammerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> ZOMBIE_SERUM = ITEMS.register("zombie_serum",
            () -> new froz8n.combat.ZombieSerumItem(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Block, Block> GRASPING_ROOTS = BLOCKS.register("grasping_roots",
            () -> new froz8n.block.GraspingRootsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT).noCollission().noOcclusion().replaceable()
                    .instabreak().sound(net.minecraft.world.level.block.SoundType.ROOTS)));
    public static final DeferredHolder<MobEffect, MobEffect> ZOMBIE_DISGUISE = MOB_EFFECTS.register("zombie_disguise",
            () -> new froz8n.combat.ZombieDisguiseEffect(MobEffectCategory.BENEFICIAL, 0x71852A));

    // Creates a creative tab with the id "smartmobs:equipment" holding every public mod item.
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EQUIPMENT_TAB =
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

    public SmartMobs(IEventBus modEventBus, ModContainer modContainer) {
        // Register the Deferred Registers to the mod bus so content gets registered.
        ARMOR_MATERIALS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);

        // Mod-bus lifecycle.
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(froz8n.combat.SoundWaveNetwork::register);

        // Register the SmartMobs gameplay handlers (command, AI, temp blocks).
        froz8n.smart.SmartMobsEvents.register();

        // Register our mod's ModConfigSpec so that NeoForge can create and load the config file for us.
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("SmartMobs ready: miners {}%, garden {}%, breeds {}",
                Math.round(Config.smartChance * 100), Math.round(Config.gardenChance * 100),
                Config.enableBreeds ? Math.round(Config.breedChance * 100) + "%" : "off");
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> armorMaterial(
            String path, int defense, int enchantmentValue, Holder<SoundEvent> equipSound,
            Supplier<Ingredient> repair) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, path);
        return ARMOR_MATERIALS.register(path, () -> new ArmorMaterial(
                Map.of(ArmorItem.Type.HELMET, defense), enchantmentValue, equipSound, repair,
                List.of(new ArmorMaterial.Layer(id)), 0.0F, 0.0F));
    }

    /** A mob-only helmet: its own armour material, its own model, and no repair recipe worth having. */
    private static DeferredHolder<Item, Item> breedHat(String path, int defense) {
        DeferredHolder<ArmorMaterial, ArmorMaterial> material = armorMaterial(
                path, defense, 3, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER));
        return ITEMS.register(path, () -> new ArmorItem(material, ArmorItem.Type.HELMET, new Item.Properties()));
    }

    /** The hat a given breed wears, or null for a breed that goes bare-headed. */
    public static Item breedHatFor(String breed) {
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

    public static Holder<MobEffect> zombieDisguise() { return ZOMBIE_DISGUISE; }
}