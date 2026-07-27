package today.vanta.client.module.impl.client;

import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;

public class FontSettings extends Module {
    public StringSetting font = Setting.of("Font", "SFPT", "SFPT","SFPT", "Tahoma", "IBM Plex Sans", "Roboto", "Geist");
    public FontSettings() {
        super("FontSettings", "Let's you change the global font.", Category.CLIENT);
        frozen = true;
        hideFromArraylist = true;
    }
}
