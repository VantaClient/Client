package today.vanta.client.event.impl.game.network;

import today.vanta.client.event.Event;

public class ChatMessageEvent extends Event {
    public final String message;

    public ChatMessageEvent(String message) {
        this.message = message;
    }
}
