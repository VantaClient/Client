package today.vanta.util.game.events.bus;

import today.vanta.util.game.events.EventPriority;

import java.lang.reflect.Method;

public class EventHandler {
    public Object listener;
    public Method method;
    public EventPriority priority;

    public EventHandler(Object listener, Method method, EventPriority priority) {
        this.listener = listener;
        this.method = method;
        this.priority = priority;
    }
}
