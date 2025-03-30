package today.vanta.client.module.impl.client;

import org.lwjgl.input.Keyboard;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.screen.ClickGUIScreen;
import today.vanta.client.setting.impl.BooleanSetting;

public class ClickGUI extends Module {

    public BooleanSetting
    pauseGame = BooleanSetting.builder()
            .name("Pause singleplayer")
            .value(false)
            .build(),

    darkenBackground = BooleanSetting.builder()
            .name("Dark background")
            .value(true)
            .build();

    public ClickGUI() {
        super("ClickGUI", "Opens up the ClickGUI.", Category.CLIENT, Keyboard.KEY_RSHIFT);
        hideFromArraylist = true;
    }

    private ClickGUIScreen clickGUIScreen;

    @Override
    public void onEnable() {
        super.onEnable();

        if (clickGUIScreen == null) {
            clickGUIScreen = new ClickGUIScreen();
        }

        mc.displayGuiScreen(clickGUIScreen);

        setEnabled(false);
    }
}
