package today.vanta.util.game.events.bus;

import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.game.events.exception.EventCallException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
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
                List<EventHandler> handlers = eventHandlers
                        .computeIfAbsent(eventType, k -> Collections.synchronizedList(new ArrayList<>()));
                synchronized (handlers) {
                    handlers.add(new EventHandler(listener, method, priority));
                }
                sortHandlers(eventType);
            }
        }
    }

    public void unregister(Object listener) {
        for (List<EventHandler> handlers : eventHandlers.values()) {
            synchronized (handlers) {
                handlers.removeIf(handler -> handler.listener == listener);
            }
        }
    }

    public void call(Object event) {
        List<EventHandler> handlers = eventHandlers.get(event.getClass());
        if (handlers != null) {
            List<EventHandler> handlersCopy;
            synchronized (handlers) {
                handlersCopy = new ArrayList<>(handlers);
            }
            for (EventHandler handler : handlersCopy) {
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
            synchronized (handlers) {
                handlers.sort(Comparator.comparingInt(handler -> handler.priority.ordinal()));
            }
        }
    }
}