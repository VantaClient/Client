package today.vanta.storage.impl;

import net.minecraft.client.gui.GuiScreen;
import today.vanta.client.screen.*;
import today.vanta.storage.Storage;
import today.vanta.util.game.IMinecraft;

public class ScreenStorage extends Storage<GuiScreen> implements IMinecraft {
    @Override
    public void subscribe() {
        super.subscribe();

        list.add(new MainMenuScreen());
        list.add(new ClickGUIScreen());
        list.add(new AltLoginScreen());
        list.add(new ChangelogScreen());
    }
}
