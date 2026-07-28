package today.vanta.client.processor.impl;

import org.lwjgl.input.Keyboard;
import today.vanta.Vanta;
import today.vanta.client.command.Command;
import today.vanta.client.event.impl.game.network.PrintChatMessage;
import today.vanta.client.event.impl.game.player.SendChatMessageEvent;
import today.vanta.client.event.impl.game.render.ChatDrawScreenEvent;
import today.vanta.client.processor.Processor;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.game.sound.Sounds;

public class ChatProcessor extends Processor {
    public static final int COMMAND_PREFIX_KEY = Keyboard.KEY_PERIOD;
    public static final String COMMAND_PREFIX = ".";

    @EventListen
    private void onSendMessage(SendChatMessageEvent event) {
        String msg = event.message;

        if (!msg.startsWith(COMMAND_PREFIX)) {
            return;
        }

        String withoutPrefix = msg.substring(COMMAND_PREFIX.length());

        String[] parts = withoutPrefix.trim().split("\\s+");

        if (parts.length == 0) {
            return;
        }

        String commandName = parts[0].toLowerCase();

        if (commandName.isEmpty()) {
            ChatUtil.error("Command name can't be empty!");
            event.cancelled = true;
            return;
        }

        String[] args = new String[Math.max(0, parts.length - 1)];
        if (parts.length > 1) {
            System.arraycopy(parts, 1, args, 0, parts.length - 1);
        }

        Command command = Vanta.instance.commandStorage.getCommand(commandName);

        if (command == null) {
            ChatUtil.error("Command &e{}&f not found!", commandName);
        } else {
            command.execute(args);
        }

        event.cancelled = true;
    }

    @EventListen
    private void onPrintMessage(PrintChatMessage event) {
        if (event.message.contains("nigger")) {
            Sounds.NIGGER.play();
        }
    }

    @EventListen
    private void onChatDrawScreen(ChatDrawScreenEvent event) {
        event.inlineSuggestion = Vanta.instance.commandStorage.getInlineSuggestion(event.text);
        event.suggestions.addAll(Vanta.instance.commandStorage.getMatchingCommands(event.text));
    }

    public static ChatProcessor getInstance() {
        return Vanta.instance.processorStorage.getT(ChatProcessor.class);
    }
}