package today.vanta.storage.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import today.vanta.Vanta;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.storage.Storage;
import today.vanta.util.system.FileUtil;
import today.vanta.util.system.VantaFile;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigStorage extends Storage<File> {
    public final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void subscribe() {
        super.subscribe();

        if (Files.exists(Paths.get(VantaFile.getString("configs/default.json")))) {
            list.add(VantaFile.getFile("configs/default.json"));
            loadConfig(list.get(0));
        } else {
            Vanta.instance.logger.warn("No default config found");
        }
    }

    public void loadConfig(File configFile) {
        if (!configFile.exists()) return;

        try (Reader reader = new FileReader(configFile)) {
            Type type = new TypeToken<Map<String, JsonObject>>() {}.getType();
            Map<String, JsonObject> moduleDataMap = GSON.fromJson(reader, type);
            Vanta.instance.logger.info("Loading config {}", configFile.getName());

            for (Map.Entry<String, JsonObject> entry : moduleDataMap.entrySet()) {
                String name = entry.getKey();
                JsonObject jsonObject = entry.getValue();
                Module module = Vanta.instance.moduleStorage.getModule(name);
                if (module == null) continue;

                module.displayName = jsonObject.get("Display name").getAsString();
                module.addSuffix = jsonObject.get("Show suffix").getAsBoolean();
                module.hideFromArraylist = jsonObject.get("Hide on arraylist").getAsBoolean();
                module.key = jsonObject.get("Keybind").getAsInt();
                module.setEnabled(jsonObject.get("Enabled").getAsBoolean());
                module.setExpanded(jsonObject.get("Expanded").getAsBoolean());

                if (jsonObject.has("Settings")) {
                    JsonObject settingsObject = jsonObject.getAsJsonObject("Settings");
                    for (Map.Entry<String, JsonElement> settingEntry : settingsObject.entrySet()) {
                        Setting<?> setting = module.getSettingByName(settingEntry.getKey());
                        if (setting == null) continue;

                        if (setting instanceof BooleanSetting) {
                            ((BooleanSetting) setting).setValue(settingEntry.getValue().getAsBoolean());
                        } else if (setting instanceof NumberSetting) {
                            ((NumberSetting) setting).setValue(settingEntry.getValue().getAsFloat());
                        } else if (setting instanceof StringSetting) {
                            ((StringSetting) setting).setValue(settingEntry.getValue().getAsString());
                        } else if (setting instanceof MultiStringSetting) {
                            List<String> values = GSON.fromJson(settingEntry.getValue(), new TypeToken<List<String>>() {}.getType());
                            String[] valuesArray = values.toArray(new String[0]);
                            ((MultiStringSetting) setting).setValue(valuesArray);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Vanta.instance.logger.warn("Error occurred when loading {} {}", configFile.getName(), e.getMessage());
        }
    }

    public void saveConfig(File configFile) {
        try (Writer writer = new FileWriter(configFile)) {
            Map<String, JsonObject> moduleDataMap = new HashMap<>();

            for (Module module : Vanta.instance.moduleStorage.list) {
                if (module.addToConfig) {
                    JsonObject moduleObject = new JsonObject();
                    moduleObject.addProperty("Display name", module.displayName);
                    moduleObject.addProperty("Show suffix", module.addSuffix);
                    moduleObject.addProperty("Hide on arraylist", module.hideFromArraylist);
                    moduleObject.addProperty("Keybind", module.key);
                    moduleObject.addProperty("Enabled", module.isEnabled());
                    moduleObject.addProperty("Expanded", module.isExpanded());

                    if (!module.settings.isEmpty() && module.addToConfig) {
                        JsonObject settingsObject = new JsonObject();
                        for (Setting<?> setting : module.settings) {
                            if (setting instanceof BooleanSetting) {
                                settingsObject.addProperty(setting.name, ((BooleanSetting) setting).getValue());
                            } else if (setting instanceof NumberSetting) {
                                settingsObject.addProperty(setting.name, ((NumberSetting) setting).getValue().floatValue());
                            } else if (setting instanceof StringSetting) {
                                settingsObject.addProperty(setting.name, ((StringSetting) setting).getValue());
                            } else if (setting instanceof MultiStringSetting) {
                                List<String> values = Arrays.asList(((MultiStringSetting) setting).getValue());
                                settingsObject.add(setting.name, GSON.toJsonTree(values));
                            }
                        }
                        moduleObject.add("Settings", settingsObject);
                    }

                    moduleDataMap.put(module.name, moduleObject);
                }
            }

            Vanta.instance.logger.info("Saving config {}", configFile.getName());
            GSON.toJson(moduleDataMap, writer);
            writer.flush();
            writer.close();
            list.add(configFile);
        } catch (IOException e) {
            Vanta.instance.logger.warn("Error occurred when saving {} {}", configFile.getName(), e.getMessage());
        }
    }

}
