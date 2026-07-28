package today.vanta.client.processor.impl;

import net.minecraft.client.gui.GuiChat;
import today.vanta.Vanta;
import today.vanta.client.event.impl.system.KeyboardEvent;
import today.vanta.client.module.impl.client.ClickGUI;
import today.vanta.client.processor.Processor;
import today.vanta.client.screen.MainMenuScreen;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.system.lwjgl.imgui.ImGuiImpl;

public class KeyProcessor extends Processor {
    @EventListen(priority = EventPriority.HIGHEST)
    private void onKey(KeyboardEvent event) {
        ImGuiImpl.key(event.key);

        if (mc.currentScreen == null) {
            if (event.key == ChatProcessor.COMMAND_PREFIX_KEY) {
                mc.displayGuiScreen(new GuiChat(ChatProcessor.COMMAND_PREFIX));
            }

            Vanta.instance.moduleStorage.list.forEach(mod -> {
                if (event.key == mod.key) {
                    mod.setEnabled(!mod.isEnabled());
                }
            });
        } else if (mc.currentScreen instanceof MainMenuScreen && event.key == Vanta.instance.moduleStorage.getT(ClickGUI.class).key) {
            mc.displayGuiScreen(Vanta.instance.moduleStorage.getT(ClickGUI.class).getClickGui());
        }
    }

    public static KeyProcessor getInstance() {
        return Vanta.instance.processorStorage.getT(KeyProcessor.class);
    }
}