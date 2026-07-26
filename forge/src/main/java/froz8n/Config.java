package froz8n;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Every knob that decides how hard the mod is.
 *
 * <p>The defaults are the playable tuning: special zombies are a minority, none of them
 * outruns a sprinting player, they notice you at a normal render distance rather than
 * across the map, and they cannot chew through obsidian.
 */
@Mod.EventBusSubscriber(modid = SmartMobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue SMART_CHANCE = BUILDER
            .comment("Share of adult zombies that spawn as helmet-wearing miners")
            .defineInRange("smartChance", 0.12, 0.0, 1.0);
    private static final ForgeConfigSpec.DoubleValue GARDEN_CHANCE = BUILDER
            .comment("Share of adult zombies that spawn as straw-hat garden zombies")
            .defineInRange("gardenChance", 0.06, 0.0, 1.0);
    private static final ForgeConfigSpec.DoubleValue BREED_CHANCE = BUILDER
            .comment("Chance for an ordinary zombie to roll one of the six lesser breeds")
            .defineInRange("breedChance", 0.30, 0.0, 1.0);
    private static final ForgeConfigSpec.DoubleValue DAY_MOVE_SPEED = BUILDER
            .comment("Miner movement speed by day. Vanilla zombies use 0.23, a sprinting player ~0.28")
            .defineInRange("dayMoveSpeed", 0.25, 0.05, 1.0);
    private static final ForgeConfigSpec.DoubleValue NIGHT_MOVE_SPEED = BUILDER
            .comment("Miner movement speed at night, and always in the Nether")
            .defineInRange("nightMoveSpeed", 0.30, 0.05, 1.0);
    private static final ForgeConfigSpec.IntValue DETECTION_RANGE = BUILDER
            .comment("How far a miner or garden zombie notices a player, in blocks")
            .defineInRange("detectionRange", 32, 8, 128);
    private static final ForgeConfigSpec.BooleanValue ALLOW_DIGGING = BUILDER
            .comment("Whether miners may tunnel through blocks at all")
            .define("allowDigging", true);
    private static final ForgeConfigSpec.DoubleValue MAX_DIG_HARDNESS = BUILDER
            .comment("Blocks with a higher destroy speed than this are never mined (obsidian is 50)")
            .defineInRange("maxDigHardness", 5.0, 0.0, 100.0);
    private static final ForgeConfigSpec.BooleanValue BREAK_PORTALS = BUILDER
            .comment("Whether miners break nether portal frames they can see")
            .define("breakPortals", false);
    private static final ForgeConfigSpec.BooleanValue SUNLIGHT_IMMUNITY = BUILDER
            .comment("Whether mod zombies ignore daylight. Off means vanilla burning rules apply")
            .define("sunlightImmunity", false);
    private static final ForgeConfigSpec.BooleanValue ENABLE_BREEDS = BUILDER
            .comment("Whether the six lesser breeds spawn at all")
            .define("enableBreeds", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static double smartChance = 0.12;
    public static double gardenChance = 0.06;
    public static double breedChance = 0.30;
    public static double dayMoveSpeed = 0.25;
    public static double nightMoveSpeed = 0.30;
    public static int detectionRange = 32;
    public static boolean allowDigging = true;
    public static double maxDigHardness = 5.0;
    public static boolean breakPortals = false;
    public static boolean sunlightImmunity = false;
    public static boolean enableBreeds = true;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        smartChance = SMART_CHANCE.get();
        gardenChance = GARDEN_CHANCE.get();
        breedChance = BREED_CHANCE.get();
        dayMoveSpeed = DAY_MOVE_SPEED.get();
        nightMoveSpeed = NIGHT_MOVE_SPEED.get();
        detectionRange = DETECTION_RANGE.get();
        allowDigging = ALLOW_DIGGING.get();
        maxDigHardness = MAX_DIG_HARDNESS.get();
        breakPortals = BREAK_PORTALS.get();
        sunlightImmunity = SUNLIGHT_IMMUNITY.get();
        enableBreeds = ENABLE_BREEDS.get();
    }
}
