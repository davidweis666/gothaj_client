package team.gothaj.cloud.share.packet;

import lombok.Getter;

import java.util.*;

@Getter
public class Packet {
    private final String type;
    private final Map<String, Object> data;

    private Packet(String type, Map<String, Object> data) {
        this.type = type;
        this.data = Collections.unmodifiableMap(data);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    public int getInt(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return 0;
    }

    public long getLong(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    public double getDouble(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    public boolean getBoolean(String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    public byte[] getBytes(String key) {
        Object value = data.get(key);
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        return new byte[0];
    }

    public Object get(String key) {
        return data.get(key);
    }

    public static PacketBuilder builder() {
        return new PacketBuilder();
    }

    public static class PacketBuilder {
        private String type;
        private final Map<String, Object> data = new HashMap<>();

        private PacketBuilder() {
        }

        public PacketBuilder type(String type) {
            this.type = type;
            return this;
        }

        public PacketBuilder putString(String key, String value) {
            data.put(key, value);
            return this;
        }

        public PacketBuilder putInt(String key, int value) {
            data.put(key, value);
            return this;
        }

        public PacketBuilder putLong(String key, long value) {
            data.put(key, value);
            return this;
        }

        public PacketBuilder putDouble(String key, double value) {
            data.put(key, value);
            return this;
        }

        public PacketBuilder putBoolean(String key, boolean value) {
            data.put(key, value);
            return this;
        }

        public PacketBuilder putBytes(String key, byte[] value) {
            data.put(key, value);
            return this;
        }

        public PacketBuilder put(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public Packet build() {
            if (type == null || type.isEmpty()) {
                throw new IllegalStateException("Packet type must be set");
            }
            return new Packet(type, data);
        }
    }

    @Override
    public String toString() {
        return "Packet{" +
                "type='" + type + '\'' +
                ", data=" + data +
                '}';
    }
}
