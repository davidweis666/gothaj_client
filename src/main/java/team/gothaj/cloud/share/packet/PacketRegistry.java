package team.gothaj.cloud.share.packet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PacketRegistry {
    private final ConcurrentHashMap<String, List<PacketHandler>> handlers = new ConcurrentHashMap<>();

    public void register(String type, PacketHandler handler) {
        handlers.computeIfAbsent(type, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(handler);
    }

    public void handle(String type, Packet packet, PacketResponder responder) {
        List<PacketHandler> typeHandlers = handlers.get(type);
        if (typeHandlers == null || typeHandlers.isEmpty()) {
            return;
        }

        synchronized (typeHandlers) {
            for (PacketHandler handler : typeHandlers) {
                try {
                    handler.handle(packet, responder);
                } catch (Exception e) {
                    System.err.println("Error in packet handler for type '" + type + "': " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    public boolean hasHandlers(String type) {
        List<PacketHandler> typeHandlers = handlers.get(type);
        return typeHandlers != null && !typeHandlers.isEmpty();
    }
}
