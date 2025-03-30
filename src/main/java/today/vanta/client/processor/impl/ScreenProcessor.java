package today.vanta.client.processor.impl;

import net.minecraft.client.gui.GuiMainMenu;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.render.DisplayGuiScreenEvent;
import today.vanta.client.processor.Processor;
import today.vanta.client.screen.MainMenuScreen;
import today.vanta.storage.impl.ScreenStorage;
import today.vanta.util.game.events.EventListen;

public class ScreenProcessor extends Processor {

    private ScreenStorage screenStorage;

    @Override
    public void onInitialize() {
        super.onInitialize();

        screenStorage = Vanta.instance.screenStorage;
    }

    @EventListen
    private void onDisplayGui(DisplayGuiScreenEvent event) {
        if (event.screen == null && mc.theWorld == null) {
            event.screen = screenStorage.getT(MainMenuScreen.class);
        }

        if (event.screen instanceof GuiMainMenu) {
            event.screen = screenStorage.getT(MainMenuScreen.class);

            mc.gameSettings.showDebugInfo = false;
            mc.ingameGUI.getChatGUI().clearChatMessages();
        }
    }
}
