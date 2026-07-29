package froz8n;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every knob that decides how hard the mod is.
 *
 * <p>The defaults are the hardcore tuning: most of what spawns at night is a miner or a
 * garden zombie, they outrun you, they find you through any wall within 256 blocks and a
 * wall only buys seconds. Turn the numbers down here if that is not what you wanted.
 */
@EventBusSubscriber(modid = SmartMobs.MODID)
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue SMART_CHANCE = BUILDER
            .comment("Share of adult zombies that spawn as wall-digging miners")
            .defineInRange("hardcore.minerShare", 0.45, 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue GARDEN_CHANCE = BUILDER
            .comment("Share of adult zombies that spawn as straw-hat garden zombies")
            .defineInRange("hardcore.gardenShare", 0.15, 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue BREED_CHANCE = BUILDER
            .comment("Chance the remaining zombies roll a breed. 1.0 means no plain zombie ever spawns")
            .defineInRange("hardcore.breedShare", 1.00, 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue DAY_MOVE_SPEED = BUILDER
            .comment("Miner movement speed by day. Vanilla zombies use 0.23, a sprinting player ~0.28")
            .defineInRange("hardcore.dayMoveSpeed", 0.29, 0.05, 1.0);
    private static final ModConfigSpec.DoubleValue NIGHT_MOVE_SPEED = BUILDER
            .comment("Miner movement speed at night, and always in the Nether")
            .defineInRange("hardcore.nightMoveSpeed", 0.34, 0.05, 1.0);
    private static final ModConfigSpec.IntValue DETECTION_RANGE = BUILDER
            .comment("How far a miner or garden zombie notices a player, in blocks")
            .defineInRange("hardcore.detectionRange", 256, 8, 512);
    private static final ModConfigSpec.BooleanValue ALLOW_DIGGING = BUILDER
            .comment("Whether miners may tunnel through blocks at all")
            .define("hardcore.allowDigging", true);
    private static final ModConfigSpec.DoubleValue DIG_SPEED = BUILDER
            .comment("How fast a miner chews through a block. 1.0 is a plain iron pickaxe")
            .defineInRange("hardcore.digSpeed", 3.0, 0.1, 20.0);
    private static final ModConfigSpec.DoubleValue MAX_DIG_HARDNESS = BUILDER
            .comment("Hardness ceiling for mining. Negative means no ceiling, which is the default")
            .defineInRange("hardcore.maxDigHardness", -1.0, -1.0, 100.0);
    private static final ModConfigSpec.BooleanValue BREAK_PORTALS = BUILDER
            .comment("Whether miners break nether portal frames they can see")
            .define("hardcore.breakPortals", true);
    private static final ModConfigSpec.BooleanValue SUNLIGHT_IMMUNITY = BUILDER
            .comment("Whether mod zombies ignore daylight. They wear helmets, so on by default")
            .define("hardcore.sunlightImmunity", true);
    private static final ModConfigSpec.BooleanValue ENABLE_BREEDS = BUILDER
            .comment("Whether the seven lesser breeds spawn at all")
            .define("hardcore.enableBreeds", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double smartChance = 0.45;
    public static double gardenChance = 0.15;
    public static double breedChance = 1.00;
    public static double dayMoveSpeed = 0.29;
    public static double nightMoveSpeed = 0.34;
    public static int detectionRange = 256;
    public static boolean allowDigging = true;
    public static double digSpeed = 3.0;
    public static double maxDigHardness = -1.0;
    public static boolean breakPortals = true;
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
        digSpeed = DIG_SPEED.get();
        maxDigHardness = MAX_DIG_HARDNESS.get();
        breakPortals = BREAK_PORTALS.get();
        sunlightImmunity = SUNLIGHT_IMMUNITY.get();
        enableBreeds = ENABLE_BREEDS.get();
    }
}
