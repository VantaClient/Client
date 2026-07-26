package today.vanta.client.module.impl.client;

import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.StringSetting;

import java.awt.*;

public class Theme extends Module {
    public final StringSetting theme = Setting.of("Theme", "Coral", "Coral", "Capri", "Twilight", "Margo", "Lust", "Light", "ShadowNotro", "Moral", "Forest","Pale","Evening Night", "Vanusa","Ocean View", "Tenacity", "Monochrome");

    public Theme() {
        super("Theme", "Manage the client's colors.", Category.CLIENT);
        frozen = true;

        theme.addListener((setting, oldValue, newValue) -> setColorArray());
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
            case "ShadowNotro":
                colors = new Color[]{new Color(255, 0, 181), new Color(28, 0, 100)};
                break;
            case "Moral":
                colors = new Color[]{new Color(147, 251, 157), new Color(9, 199, 251)};
                break;
            case "Forest":
                colors = new Color[]{new Color(2, 219, 128), new Color(21, 77, 52)};
                break;
            case "Pale":
                colors = new Color[]{new Color(158, 255, 255), new Color(21, 77, 52)};
                break;
            case "Evening Night":
                colors = new Color[]{new Color(255, 253, 228), new Color(0, 90, 167)};
                break;
            case "Vanusa":
                colors = new Color[]{new Color(218, 68, 83), new Color(137, 33, 107)};
                break;
            case "Ocean View":
                colors = new Color[]{new Color(168, 192, 255), new Color(63, 43, 150)};
                break;
            case "Tenacity":
                colors = new Color[]{new Color(236, 133, 209), new Color(28, 167, 222)};
                break;
            case "Monochrome":
                colors = new Color[]{new Color(255,255,255), new Color(0,0,0)};
                break;
        }
    }

    //default colors
    private final Color light1 = new Color(0xE95D3C);
    private final Color dark1 = new Color(0x010101);

    public Color[] colors = {light1, dark1};
}