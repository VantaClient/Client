package today.vanta.client.file.impl;

import com.google.gson.JsonObject;
import today.vanta.Vanta;
import today.vanta.client.file.File;
import today.vanta.client.module.Category;
import today.vanta.client.screen.BoxyClickGUIScreen;
import today.vanta.client.screen.ClickGUIScreen;

public class ScreenFile extends File {
    public ScreenFile() {
        super("screens");
    }

    @Override
    protected JsonObject writeJson() {
        JsonObject screensObject = new JsonObject();

        ClickGUIScreen dropdownCGUI = Vanta.instance.screenStorage.getT(ClickGUIScreen.class);
        if (dropdownCGUI != null) {
            JsonObject dropdownObject = new JsonObject();
            for (Category cat : Category.values()) {
                JsonObject catObject = new JsonObject();
                catObject.addProperty("X", cat.position.x);
                catObject.addProperty("Y", cat.position.y);

                dropdownObject.add(cat.name, catObject);
            }

            screensObject.add("Dropdown", dropdownObject);
        }

        BoxyClickGUIScreen boxyCGUI = Vanta.instance.screenStorage.getT(BoxyClickGUIScreen.class);
        if (boxyCGUI != null) {
            JsonObject boxyObject = new JsonObject();

            boxyObject.addProperty("X", boxyCGUI.x);
            boxyObject.addProperty("Y", boxyCGUI.y);
            boxyObject.addProperty("Width", boxyCGUI.sWidth);
            boxyObject.addProperty("Height", boxyCGUI.sHeight);

            screensObject.add("Boxy", boxyObject);
        }

        return screensObject;
    }

    @Override
    protected void readJson(JsonObject json) {
        if (json == null) return;

        if (json.has("Dropdown")) {
            JsonObject cguiObject = json.getAsJsonObject("Dropdown");

            for (Category cat : Category.values()) {
                if (!cguiObject.has(cat.name)) {
                    continue;
                }

                JsonObject catObject = cguiObject.getAsJsonObject(cat.name);

                if (catObject.has("X")) {
                    cat.position.x = catObject.get("X").getAsFloat();
                }

                if (catObject.has("Y")) {
                    cat.position.y = catObject.get("Y").getAsFloat();
                }
            }
        }

        if (json.has("Boxy")) {
            BoxyClickGUIScreen boxyCGUI = Vanta.instance.screenStorage.getT(BoxyClickGUIScreen.class);
            if (boxyCGUI != null) {
                JsonObject boxyObject = json.getAsJsonObject("Boxy");

                if (boxyObject.has("X")) {
                    boxyCGUI.x = boxyObject.get("X").getAsFloat();
                }
                if (boxyObject.has("Y")) {
                    boxyCGUI.y = boxyObject.get("Y").getAsFloat();
                }
                if (boxyObject.has("Width")) {
                    boxyCGUI.sWidth = boxyObject.get("Width").getAsFloat();
                }
                if (boxyObject.has("Height")) {
                    boxyCGUI.sHeight = boxyObject.get("Height").getAsFloat();
                }
            }
        }
    }
}