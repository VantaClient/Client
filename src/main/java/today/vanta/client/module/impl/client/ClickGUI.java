package today.vanta.client.module.impl.client;

import org.lwjgl.input.Keyboard;
import today.vanta.Vanta;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.screen.BoxyClickGUIScreen;
import today.vanta.client.screen.ClickGUIScreen;
import today.vanta.client.screen.ImGuiClickGUIScreen;
import today.vanta.client.screen.VantaScreen;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.system.lwjgl.imgui.ImGuiImpl;

public class ClickGUI extends Module {
    public final BooleanSetting
            pauseGame = Setting.of("Pause singleplayer", false),
            darkenBackground = Setting.of("Dark background", true),
            gradientBackground = Setting.of("Gradient background", true),
            image = Setting.of("Image", false);

    private final StringSetting design = Setting.of("Design", "Dropdown", "Dropdown", "ImGui", "Boxy");
    public final StringSetting mascot = Setting.of("Mascot", "longboy", "ermwhat", "silly", "cousin", "longboy").hide(() -> !image.getValue());

    public ClickGUI() {
        super("ClickGUI", "Opens up the ClickGUI.", Category.CLIENT, Keyboard.KEY_RSHIFT);
        hideFromArraylist = true;
    }

    private ClickGUIScreen clickGUIScreen;
    private ImGuiClickGUIScreen imGuiClickGuiScreen;
    private BoxyClickGUIScreen boxyClickGUIScreen;

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

        switch (design.getValue()) {
            case "ImGui":
                if (!ImGuiImpl.canUseImGui) {
                    send("Can't use ImGUI when on ARM CPUs and not on macOS, defaulting to dropdown ClickGUI.");
                    return clickGUIScreen;
                }
                return imGuiClickGuiScreen;

            case "Boxy":
                return boxyClickGUIScreen;

            case "Dropdown":
            default:
                return clickGUIScreen;
        }
    }
}