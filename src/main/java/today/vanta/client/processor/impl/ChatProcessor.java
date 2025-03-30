package today.vanta.client.processor.impl;

import today.vanta.client.event.impl.game.network.ChatMessageEvent;
import today.vanta.client.processor.Processor;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.sound.Sounds;

public class ChatProcessor extends Processor {
    @EventListen
    private void onChat(ChatMessageEvent event) {
        if (event.message.contains("nigger")) {
            Sounds.NIGGER.play();
        }
    }
}
