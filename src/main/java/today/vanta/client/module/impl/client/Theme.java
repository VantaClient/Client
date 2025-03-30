package today.vanta.client.module.impl.client;

import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.impl.StringSetting;

import java.awt.*;

public class Theme extends Module {

    private final StringSetting theme = StringSetting.builder()
            .name("Theme")
            .value("Coral")
            .values("Coral", "Capri", "Twilight", "Margo", "Lust", "Light")
            .listener((setting, oldValue, newValue) -> setColorArray())
            .build();

    public Theme() {
        super("Theme", "Manage the client's colors.", Category.CLIENT);
        hideFromArraylist = true;
        frozen = true;
    }

    public void setColorArray() {
        switch (theme.getValue()) {
            case "Coral":
                colors = new Color[]{new Color(0xE95D3C), new Color(0x010101)};
                break;
            case "Capri":
                colors = new Color[]{new Color(0x28B8D5), new Color(0x020344)};
                break;
            case "Twilight":
                colors = new Color[]{new Color(0xEA98DA), new Color(0x5B6CF9)};
                break;
            case "Margo":
                colors = new Color[]{new Color(0xFFEFBA), new Color(0xFFFFFF)};
                break;
            case "Lust":
                colors = new Color[]{new Color(0xdd1818), new Color(0x333333)};
                break;
            case "Light":
                colors = new Color[]{new Color(255, 255, 255, 185), new Color(0x29A6FF)};
                break;
        }
    }

    //default colors
    private final Color light1 = new Color(0xE95D3C);
    private final Color dark1 = new Color(0x010101);

    public Color[] colors = {light1, dark1};
}
