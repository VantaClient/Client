package today.vanta.client.screen;

import net.minecraft.client.gui.GuiScreen;
import today.vanta.Vanta;

public abstract class VantaScreen extends GuiScreen {
    @Override
    public final void initGui() {
        Vanta.instance.eventBus.unregister(this);
        if (mc.currentScreen != this) {
            return;
        }

        initScreen();

        Vanta.instance.eventBus.register(this);
    }

    protected void initScreen() {
    }

    @Override
    public void onGuiClosed() {
        Vanta.instance.eventBus.unregister(this);
    }

    protected void scheduleInitGui() {
        if (mc == null) {
            return;
        }

        mc.addScheduledTask(() -> {
            if (mc.currentScreen == this) {
                initGui();
            }
        });
    }
}