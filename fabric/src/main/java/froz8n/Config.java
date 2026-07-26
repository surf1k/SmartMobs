package froz8n;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An example config class. This is not required, but it's a good idea to have one to keep
 * your config organized.
 *
 * <p>Fabric ships no config framework, so this is the direct equivalent of the old
 * ForgeConfigSpec: a small JSON file in the config directory, read once on startup.
 */
public final class Config {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> DEFAULT_ITEMS = List.of("minecraft:iron_ingot");

    /** Whether to log the dirt block on common setup. */
    public static boolean logDirtBlock = true;
    /** A magic number. */
    public static int magicNumber = 42;
    /** What you want the introduction message to be for the magic number. */
    public static String magicNumberIntroduction = "The magic number is... ";
    /** A list of items to log on common setup. */
    public static Set<Item> items = Set.of();

    private Config() {
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(SmartMobs.MODID + ".json");
        List<String> itemNames = new ArrayList<>(DEFAULT_ITEMS);

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                logDirtBlock = getBoolean(json, "logDirtBlock", logDirtBlock);
                magicNumber = Math.max(0, getInt(json, "magicNumber", magicNumber));
                magicNumberIntroduction = getString(json, "magicNumberIntroduction", magicNumberIntroduction);
                if (json.get("items") instanceof JsonArray array) {
                    itemNames.clear();
                    for (JsonElement element : array) {
                        itemNames.add(element.getAsString());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read {}, falling back to defaults", file, e);
            }
        } else {
            save(file, itemNames);
        }

        // convert the list of strings into a set of items
        Set<Item> parsed = new LinkedHashSet<>();
        for (String itemName : itemNames) {
            Identifier id = Identifier.tryParse(itemName);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                LOGGER.warn("Unknown item in config: {}", itemName);
                continue;
            }
            parsed.add(BuiltInRegistries.ITEM.getValue(id));
        }
        items = Set.copyOf(parsed);
    }

    private static void save(Path file, List<String> itemNames) {
        JsonObject json = new JsonObject();
        json.addProperty("logDirtBlock", logDirtBlock);
        json.addProperty("magicNumber", magicNumber);
        json.addProperty("magicNumberIntroduction", magicNumberIntroduction);
        JsonArray array = new JsonArray();
        itemNames.forEach(array::add);
        json.add("items", array);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write {}", file, e);
        }
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static String getString(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }
}
