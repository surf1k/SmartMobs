package froz8n;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every knob that decides how hard the mod is.
 *
 * <p>The defaults are the playable tuning: special zombies are a minority, none of them
 * outruns a sprinting player, they notice you at a normal render distance rather than
 * across the map, and they cannot chew through obsidian.
 */
@EventBusSubscriber(modid = SmartMobs.MODID)
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue SMART_CHANCE = BUILDER
            .comment("Share of adult zombies that spawn as wall-digging miners")
            .defineInRange("minerShare", 0.20, 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue GARDEN_CHANCE = BUILDER
            .comment("Share of adult zombies that spawn as straw-hat garden zombies")
            .defineInRange("gardenShare", 0.10, 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue BREED_CHANCE = BUILDER
            .comment("Chance the remaining zombies roll a breed. 1.0 means no plain zombie ever spawns")
            .defineInRange("breedShare", 1.00, 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue DAY_MOVE_SPEED = BUILDER
            .comment("Miner movement speed by day. Vanilla zombies use 0.23, a sprinting player ~0.28")
            .defineInRange("dayMoveSpeed", 0.25, 0.05, 1.0);
    private static final ModConfigSpec.DoubleValue NIGHT_MOVE_SPEED = BUILDER
            .comment("Miner movement speed at night, and always in the Nether")
            .defineInRange("nightMoveSpeed", 0.30, 0.05, 1.0);
    private static final ModConfigSpec.IntValue DETECTION_RANGE = BUILDER
            .comment("How far a miner or garden zombie notices a player, in blocks")
            .defineInRange("detectionRange", 32, 8, 128);
    private static final ModConfigSpec.BooleanValue ALLOW_DIGGING = BUILDER
            .comment("Whether miners may tunnel through blocks at all")
            .define("allowDigging", true);
    private static final ModConfigSpec.DoubleValue MAX_DIG_HARDNESS = BUILDER
            .comment("Hardness ceiling for mining. Negative means no ceiling, which is the default")
            .defineInRange("maxDigHardness", -1.0, -1.0, 100.0);
    private static final ModConfigSpec.BooleanValue BREAK_PORTALS = BUILDER
            .comment("Whether miners break nether portal frames they can see")
            .define("breakPortals", false);
    private static final ModConfigSpec.BooleanValue SUNLIGHT_IMMUNITY = BUILDER
            .comment("Whether mod zombies ignore daylight. They wear helmets, so on by default")
            .define("sunlightImmunity", true);
    private static final ModConfigSpec.BooleanValue ENABLE_BREEDS = BUILDER
            .comment("Whether the seven lesser breeds spawn at all")
            .define("enableBreeds", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double smartChance = 0.20;
    public static double gardenChance = 0.10;
    public static double breedChance = 1.00;
    public static double dayMoveSpeed = 0.25;
    public static double nightMoveSpeed = 0.30;
    public static int detectionRange = 32;
    public static boolean allowDigging = true;
    public static double maxDigHardness = -1.0;
    public static boolean breakPortals = false;
    public static boolean sunlightImmunity = true;
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
