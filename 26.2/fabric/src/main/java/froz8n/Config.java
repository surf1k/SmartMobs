package froz8n;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Every knob that decides how hard the mod is. Fabric ships no config framework, so this
 * is a small JSON file in the config directory, read once on startup.
 *
 * <p>The defaults are the hardcore tuning: most of what spawns at night is a miner or a
 * garden zombie, they outrun you, they find you through any wall within 256 blocks and a
 * wall only buys seconds. Turn the numbers down here if that is not what you wanted.
 */
public final class Config {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Bumped whenever the defaults change enough that an old config file would quietly
     * cancel the change. A file from an older schema is replaced, not merged.
     */
    private static final int SCHEMA = 3;

    /** Share of adult zombies that spawn as helmet-wearing miners. */
    public static double smartChance = 0.45;
    /** Share of adult zombies that spawn as straw-hat garden zombies. */
    public static double gardenChance = 0.15;
    /** Chance for an ordinary zombie to roll one of the six lesser breeds. */
    public static double breedChance = 1.00;
    /** Miner movement speed by day. Vanilla zombies use 0.23, a sprinting player ~0.28. */
    public static double dayMoveSpeed = 0.29;
    /** Miner movement speed at night (and always in the Nether). */
    public static double nightMoveSpeed = 0.34;
    /** How far a miner or garden zombie notices a player, in blocks. Walls do not stop it. */
    public static int detectionRange = 256;
    /** Whether miners may tunnel through blocks at all. */
    public static boolean allowDigging = true;
    /** How fast a miner chews through a block. 1.0 is a plain iron pickaxe. */
    public static double digSpeed = 3.0;
    /** Hardness ceiling for mining. Negative means no ceiling, which is the default. */
    public static double maxDigHardness = -1.0;
    /** Whether miners break nether portal frames they can see. */
    public static boolean breakPortals = true;
    /** Whether mod zombies ignore daylight. They wear helmets, so on by default. */
    public static boolean sunlightImmunity = true;
    /** Whether the six lesser breeds spawn at all. */
    public static boolean enableBreeds = true;

    private Config() {
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(SmartMobs.MODID + ".json");
        if (!Files.exists(file)) {
            save(file);
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            // A file written by an older version carries the old, tamer numbers under the
            // same keys. Keeping it would silently cancel the retune, so it is replaced.
            if (getInt(json, "configVersion", 0) < SCHEMA) {
                LOGGER.info("Replacing {} - it predates the {} tuning", file, "2.7");
                reader.close();
                save(file);
                return;
            }
            smartChance = clamp(getDouble(json, "minerShare", smartChance), 0.0, 1.0);
            gardenChance = clamp(getDouble(json, "gardenShare", gardenChance), 0.0, 1.0);
            breedChance = clamp(getDouble(json, "breedShare", breedChance), 0.0, 1.0);
            dayMoveSpeed = clamp(getDouble(json, "dayMoveSpeed", dayMoveSpeed), 0.05, 1.0);
            nightMoveSpeed = clamp(getDouble(json, "nightMoveSpeed", nightMoveSpeed), 0.05, 1.0);
            detectionRange = (int) clamp(getInt(json, "detectionRange", detectionRange), 8, 512);
            allowDigging = getBoolean(json, "allowDigging", allowDigging);
            digSpeed = clamp(getDouble(json, "digSpeed", digSpeed), 0.1, 20.0);
            maxDigHardness = clamp(getDouble(json, "maxDigHardness", maxDigHardness), -1.0, 100.0);
            breakPortals = getBoolean(json, "breakPortals", breakPortals);
            sunlightImmunity = getBoolean(json, "sunlightImmunity", sunlightImmunity);
            enableBreeds = getBoolean(json, "enableBreeds", enableBreeds);
        } catch (Exception e) {
            LOGGER.error("Failed to read {}, falling back to defaults", file, e);
        }
    }

    private static void save(Path file) {
        JsonObject json = new JsonObject();
        json.addProperty("configVersion", SCHEMA);
        json.addProperty("minerShare", smartChance);
        json.addProperty("gardenShare", gardenChance);
        json.addProperty("breedShare", breedChance);
        json.addProperty("dayMoveSpeed", dayMoveSpeed);
        json.addProperty("nightMoveSpeed", nightMoveSpeed);
        json.addProperty("detectionRange", detectionRange);
        json.addProperty("allowDigging", allowDigging);
        json.addProperty("digSpeed", digSpeed);
        json.addProperty("maxDigHardness", maxDigHardness);
        json.addProperty("breakPortals", breakPortals);
        json.addProperty("sunlightImmunity", sunlightImmunity);
        json.addProperty("enableBreeds", enableBreeds);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write {}", file, e);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static double getDouble(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }
}
