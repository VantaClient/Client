package today.vanta.storage.impl;

import net.minecraft.client.gui.GuiScreen;
import today.vanta.client.screen.*;
import today.vanta.storage.Storage;
import today.vanta.util.game.Commons;
import today.vanta.util.game.render.ImageUtil;

public class ScreenStorage extends Storage<GuiScreen> implements Commons {
    @Override
    public void subscribe() {
        super.subscribe();

        list.add(new MainMenuScreen());
        list.add(new AltLoginScreen());
        list.add(new ClickGUIScreen());
        list.add(new ImGuiClickGUIScreen());
        list.add(new BoxyClickGUIScreen());
    }

    public void stop() {
        ImageUtil.clearCache();
    }
}