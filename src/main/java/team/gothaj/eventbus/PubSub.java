package team.gothaj.eventbus;

import team.gothaj.client.api.util.Utils;
import team.gothaj.eventbus.impl.CancellableEvent;
import team.gothaj.eventbus.impl.Event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PubSub {

    private final Map<Class<?>, List<Subscribe>> eventSubscriptionMap = new ConcurrentHashMap<>();

    private final Map<Class<?>, Subscribe[]> dispatchCache = new ConcurrentHashMap<>();

    public void subscribe(final Object listener) {
        final Class<?> clazz = listener.getClass();

        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
            boolean anyAdded = false;

            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Listen.class)
                        || method.getParameterTypes().length != 1) continue;

                Class<?> eventClass = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(eventClass)) continue;

                method.setAccessible(true);
                MethodHandle handle = lookup.unreflect(method);
                int priority = method.getAnnotation(Listen.class).priority().value;

                List<Subscribe> list = eventSubscriptionMap
                        .computeIfAbsent(eventClass, k -> new ArrayList<>());


                boolean alreadyRegistered = list.stream()
                        .anyMatch(s -> s.parent() == listener && s.methodHandle().equals(handle));
                if (alreadyRegistered) continue;

                list.add(new Subscribe(listener, handle, priority));
                anyAdded = true;

                dispatchCache.remove(eventClass);
            }

            if (anyAdded) {
                eventSubscriptionMap.forEach((eventClass, list) -> {
                    if (list.stream().anyMatch(s -> s.parent() == listener)) {
                        list.sort(Comparator.comparingInt(a -> -a.priority()));
                        dispatchCache.remove(eventClass);
                    }
                });
            }

        } catch (IllegalAccessException e) {
            System.err.println("Failed to subscribe " + clazz.getName());
            e.printStackTrace();
        }
    }

    public void unsubscribe(final Object listener) {
        eventSubscriptionMap.forEach((eventClass, list) -> {
            boolean removed = list.removeIf(sub -> sub.parent() == listener);
            if (removed) dispatchCache.remove(eventClass);
        });
    }

    public void fire(final Event event) {
        Subscribe[] subscribers = dispatchCache.computeIfAbsent(
                event.getClass(),
                k -> {
                    List<Subscribe> list = eventSubscriptionMap.get(k);
                    return list == null ? new Subscribe[0] : list.toArray(new Subscribe[0]);
                });

        if (Utils.mc.thePlayer == null || Utils.mc.theWorld == null) return;

        for (final Subscribe sub : subscribers) {
            try {
                sub.methodHandle().invoke(sub.parent(), event);
            } catch (Throwable t) {
                System.err.println("Failed to invoke subscriber " + sub.parent()
                        + " for event " + event.getClass().getSimpleName());
                t.printStackTrace();
            }

            if (event instanceof CancellableEvent cancellableEvent)
                if (cancellableEvent.isCancelled()) break;
        }
    }
}