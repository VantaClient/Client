package today.vanta.util.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import today.vanta.Vanta;
import today.vanta.client.module.Category;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ScreenSavingUtil {

    public static boolean loadConfig(File configFile) {
        if (!configFile.exists()) return false;

        try (Reader reader = new FileReader(configFile)) {
            Type type = new TypeToken<Map<String, JsonObject>>() {}.getType();
            Map<String, JsonObject> categoryMap = Vanta.instance.configStorage.GSON.fromJson(reader, type);
            Vanta.instance.logger.info("Loading category positions {}", configFile.getName());

            for (Map.Entry<String, JsonObject> entry : categoryMap.entrySet()) {
                String name = entry.getKey();
                JsonObject jsonObject = entry.getValue();
                Category category = Category.valueOf(name.toUpperCase());

                category.position.set(jsonObject.get("X").getAsFloat(), jsonObject.get("Y").getAsFloat());
            }
            return true;
        } catch (IOException e) {
            Vanta.instance.logger.warn("Error occurred when loading {} {}", configFile.getName(), e.getMessage());
            return false;
        }
    }

    public static void saveConfig(File configFile) {
        try (Writer writer = new FileWriter(configFile)) {
            Map<String, JsonObject> categoryMap = new HashMap<>();

            for (Category category : Category.values()) {
                JsonObject categoryObject = new JsonObject();
                categoryObject.addProperty("X", category.position.x);
                categoryObject.addProperty("Y", category.position.y);

                categoryMap.put(category.name, categoryObject);
            }

            Vanta.instance.logger.info("Saving category positions {}", configFile.getName());
            Vanta.instance.configStorage.GSON.toJson(categoryMap, writer);
            writer.flush();
        } catch (IOException e) {
            Vanta.instance.logger.warn("Error occurred when saving {} {}", configFile.getName(), e.getMessage());
        }
    }

}
