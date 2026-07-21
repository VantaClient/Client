package today.vanta.client.processor.impl;

import net.minecraft.client.gui.GuiMainMenu;
import org.lwjgl.input.Mouse;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.event.impl.game.render.DisplayGuiScreenEvent;
import today.vanta.client.event.impl.game.render.RenderEvent;
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
    private void onRender(RenderEvent event) {
        final int width = event.scaledResolution.getScaledWidth();
        final int height = event.scaledResolution.getScaledHeight();
        final int mouseX = Mouse.getX() * width / mc.displayWidth;
        final int mouseY = height - Mouse.getY() * height / mc.displayHeight - 1;

        if (!event.screen) {
            RenderOverlayEvent overlayEvent = new RenderOverlayEvent(event.partialTicks, event.scaledResolution);
            overlayEvent.call();
        } else {
            RenderScreenEvent screenEvent = new RenderScreenEvent(mouseX, mouseY, event.partialTicks);
            screenEvent.call();
        }
    }

    @EventListen
    private void onDisplayGui(DisplayGuiScreenEvent event) {
        if (event.screen == null && mc.theWorld == null && mc.thePlayer == null) {
            event.screen = screenStorage.getT(MainMenuScreen.class);
        }

        if (event.screen instanceof GuiMainMenu) {
            event.screen = screenStorage.getT(MainMenuScreen.class);

            mc.gameSettings.showDebugInfo = false;
            mc.ingameGUI.getChatGUI().clearChatMessages();
        }
    }

    public static ScreenProcessor getInstance() {
        return Vanta.instance.processorStorage.getT(ScreenProcessor.class);
    }
}