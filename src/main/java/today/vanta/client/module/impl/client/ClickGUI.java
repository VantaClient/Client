package today.vanta.client.module.impl.client;

import org.lwjgl.input.Keyboard;
import today.vanta.Vanta;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.screen.*;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.system.lwjgl.imgui.ImGuiImpl;

public class ClickGUI extends Module {
    public final BooleanSetting
            pauseGame = Setting.of("Pause singleplayer", false),
            darkenBackground = Setting.of("Dark background", true),
            gradientBackground = Setting.of("Gradient background", true),
            image = Setting.of("Image", false);

    private final StringSetting design = Setting.of("Design", "Dropdown", "Dropdown", "ImGui", "Boxy", "Experimental");
    public final StringSetting mascot = Setting.of("Mascot", "longboy", "ermwhat", "silly", "cousin", "longboy", "mj", "mj2", "mj3").hide(() -> !image.getValue());

    public ClickGUI() {
        super("ClickGUI", "Opens up the ClickGUI.", Category.CLIENT, Keyboard.KEY_RSHIFT);
        hideFromArraylist = true;

        design.addListener((setting, oldValue, newValue) -> {
            if (newValue.equals("ImGui") && ImGuiImpl.DISABLED) {
                design.setValue(!oldValue.equals("ImGui") ? oldValue : "Dropdown");
            }
        });
    }

    private ClickGUIScreen clickGUIScreen;
    private ImGuiClickGUIScreen imGuiClickGuiScreen;
    private BoxyClickGUIScreen boxyClickGUIScreen;
    private CickGIUScreen cickGIUScreen;

    @Override
    public void onEnable() {
        mc.displayGuiScreen(getClickGui());

        setEnabled(false);
    }

    public VantaScreen getClickGui() {
        if (clickGUIScreen == null) {
            clickGUIScreen = Vanta.instance.screenStorage.getT(ClickGUIScreen.class);
        }

        if (imGuiClickGuiScreen == null) {
            imGuiClickGuiScreen = Vanta.instance.screenStorage.getT(ImGuiClickGUIScreen.class);
        }

        if (boxyClickGUIScreen == null) {
            boxyClickGUIScreen = Vanta.instance.screenStorage.getT(BoxyClickGUIScreen.class);
        }

        if (cickGIUScreen == null) {
            cickGIUScreen = Vanta.instance.screenStorage.getT(CickGIUScreen.class);
        }

        switch (design.getValue()) {
            case "ImGui":
                return imGuiClickGuiScreen;

            case "Boxy":
                return boxyClickGUIScreen;

            case "Experimental":
                return cickGIUScreen;

            case "Dropdown":
            default:
                return clickGUIScreen;
        }
    }
}