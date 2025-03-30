package today.vanta.util.game.events.bus;

import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.game.events.exception.EventCallException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventBus {
    private final Map<Class<?>, List<EventHandler>> eventHandlers = new ConcurrentHashMap<>();

    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventListen.class) && method.getParameterCount() == 1) {
                Class<?> eventType = method.getParameterTypes()[0];
                method.setAccessible(true);
                EventListen listenAnnotation = method.getAnnotation(EventListen.class);
                EventPriority priority = listenAnnotation.priority();
                eventHandlers
                        .computeIfAbsent(eventType, k -> new ArrayList<>())
                        .add(new EventHandler(listener, method, priority));
                sortHandlers(eventType);
            }
        }
    }

    public void unregister(Object listener) {
        for (List<EventHandler> handlers : eventHandlers.values()) {
            handlers.removeIf(handler -> handler.listener == listener);
        }
    }

    public void call(Object event) {
        List<EventHandler> handlers = eventHandlers.get(event.getClass());
        if (handlers != null) {
            for (EventHandler handler : handlers) {
                try {
                    handler.method.invoke(handler.listener, event);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new EventCallException(String.format(
                            "Failed to call event handler method '%s' in listener '%s' for event class '%s'",
                            handler.method.getName(),
                            handler.listener.getClass().getName(),
                            event.getClass().getName()
                    ), e);
                }
            }
        }
    }

    private void sortHandlers(Class<?> eventType) {
        List<EventHandler> handlers = eventHandlers.get(eventType);
        if (handlers != null) {
            handlers.sort(Comparator.comparingInt(handler -> handler.priority.ordinal()));
        }
    }
}
