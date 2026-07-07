package today.vanta.client.module.impl.client;

import net.minecraft.util.Util;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.impl.*;

public class OSEnhancements extends Module {
    public final BooleanSetting darkTitleBar = Setting.of("Dark title bar", true);
    public final StringSetting backdrop = Setting.of("Backdrop", "Auto", "Auto", "None", "Mica", "Acrylic", "Tabbed");
    public final BooleanSetting transparent = Setting.of("Transparent", true);
    public final StringSetting windowCorner = Setting.of("Window corner", "Default", "Default", "Don't round", "Round", "Round (Small)");

    public OSEnhancements() {
        super("OSEnhancements", "Configure OS-specific window enhancements.", Category.CLIENT);
        frozen = true;
        hideFromArraylist = true;

        if (Util.getOSType() == Util.EnumOS.WINDOWS) {
            darkTitleBar.addListener((setting, oldValue, newValue) ->
                    DarkTitleBar.INSTANCE.apply(WindowsOS.INSTANCE, newValue));

            backdrop.addListener((setting, oldValue, newValue) ->
                    SystemBackdrop.INSTANCE.apply(WindowsOS.INSTANCE, parseBackdrop(newValue)));

            transparent.addListener((setting, oldValue, newValue) ->
                    Transparent.INSTANCE.apply(WindowsOS.INSTANCE, newValue));

            windowCorner.addListener((setting, oldValue, newValue) ->
                    WindowCorner.INSTANCE.apply(WindowsOS.INSTANCE, parseCorner(newValue)));
        }
    }

    private static SystemBackdrop.Type parseBackdrop(String value) {
        for (SystemBackdrop.Type type : SystemBackdrop.Type.values()) {
            if (type.name.equals(value)) return type;
        }
        return SystemBackdrop.Type.AUTO;
    }

    private static WindowCorner.Preference parseCorner(String value) {
        for (WindowCorner.Preference preference : WindowCorner.Preference.values()) {
            if (preference.name.equals(value)) return preference;
        }
        return WindowCorner.Preference.DEFAULT;
    }
}
