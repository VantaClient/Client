package today.vanta.client.module.impl.client;

import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.StringSetting;

public class WindowSettings extends Module {
    public final StringSetting
            font = Setting.of("Font", "SFPT", "SFPT","SFPT", "Tahoma", "IBM Plex Sans", "Roboto", "Geist", "Minecraft", "Exhibition"),
            outline = Setting.of("Outline mode", "None", "Horizontal gradient", "Vertical gradient", "Primary", "Secondary", "None"),
            textAlignment = Setting.of("Title alignment", "Left", "Center", "Left");

    public WindowSettings() {
        super("WindowSettings", "Let's you customise the window rects.", Category.CLIENT);
        frozen = true;
    }
}
