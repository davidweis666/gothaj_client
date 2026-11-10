package team.gothaj.cloud.share.packet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PacketCodec {
    private static final byte TYPE_STRING = 1;
    private static final byte TYPE_INT = 2;
    private static final byte TYPE_LONG = 3;
    private static final byte TYPE_DOUBLE = 4;
    private static final byte TYPE_BOOLEAN = 5;
    private static final byte TYPE_BYTES = 6;

    public static byte[] serialize(Packet packet) throws IOException {
        // First pass: Calculate size
        int size = 0;
        byte[] typeBytes = packet.getType().getBytes(StandardCharsets.UTF_8);
        size += 4 + typeBytes.length; // Type string
        size += 4; // Field count

        for (Map.Entry<String, Object> entry : packet.getData().entrySet()) {
            size += 4 + entry.getKey().getBytes(StandardCharsets.UTF_8).length;
            size += 1; // Type tag

            Object value = entry.getValue();
            switch (value) {
                case String s -> size += 4 + s.getBytes(StandardCharsets.UTF_8).length;
                case Integer ignored -> size += 4;
                case Long ignored -> size += 8;
                case Double ignored -> size += 8;
                case Boolean ignored -> size += 1;
                case byte[] bytes -> size += 4 + bytes.length;
                default -> throw new IOException("Unsupported value type: " + value.getClass().getName());
            }
        }

        // SecureBuffer buffer = SecureBuffer.allocate(size);
        byte[] buffer = new byte[size];
        int position = 0;

        // Write Type
        // buffer.putInt(typeBytes.length);
        int v_typeLen = typeBytes.length;
        if (buffer.length - position < 4) {
            throw new IndexOutOfBoundsException("Buffer overflow");
        }
        buffer[position++] = (byte) (v_typeLen >> 24);
        buffer[position++] = (byte) (v_typeLen >> 16);
        buffer[position++] = (byte) (v_typeLen >> 8);
        buffer[position++] = (byte) (v_typeLen);

        // buffer.put(typeBytes);
        int len_type = typeBytes.length;
        if (len_type > buffer.length - position) {
            throw new IndexOutOfBoundsException("Buffer overflow");
        }
        System.arraycopy(typeBytes, 0, buffer, position, len_type);
        position += len_type;


        // Write Field Count
        // buffer.putInt(packet.getData().size());
        int v_size = packet.getData().size();
        if (buffer.length - position < 4) {
            throw new IndexOutOfBoundsException("Buffer overflow");
        }
        buffer[position++] = (byte) (v_size >> 24);
        buffer[position++] = (byte) (v_size >> 16);
        buffer[position++] = (byte) (v_size >> 8);
        buffer[position++] = (byte) (v_size);

        // Write Fields
        for (Map.Entry<String, Object> entry : packet.getData().entrySet()) {
            byte[] keyBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
            // buffer.putInt(keyBytes.length);
            int v_keyLen = keyBytes.length;
            if (buffer.length - position < 4) {
                throw new IndexOutOfBoundsException("Buffer overflow");
            }
            buffer[position++] = (byte) (v_keyLen >> 24);
            buffer[position++] = (byte) (v_keyLen >> 16);
            buffer[position++] = (byte) (v_keyLen >> 8);
            buffer[position++] = (byte) (v_keyLen);

            // buffer.put(keyBytes);
            int len_key = keyBytes.length;
            if (len_key > buffer.length - position) {
                throw new IndexOutOfBoundsException("Buffer overflow");
            }
            System.arraycopy(keyBytes, 0, buffer, position, len_key);
            position += len_key;

            Object value = entry.getValue();
            if (value instanceof String s) {
                // buffer.put(TYPE_STRING);
                if (position >= buffer.length) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = TYPE_STRING;

                byte[] strBytes = s.getBytes(StandardCharsets.UTF_8);
                // buffer.putInt(strBytes.length);
                int v_strLen = strBytes.length;
                if (buffer.length - position < 4) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = (byte) (v_strLen >> 24);
                buffer[position++] = (byte) (v_strLen >> 16);
                buffer[position++] = (byte) (v_strLen >> 8);
                buffer[position++] = (byte) (v_strLen);

                // buffer.put(strBytes);
                int len_str = strBytes.length;
                if (len_str > buffer.length - position) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                System.arraycopy(strBytes, 0, buffer, position, len_str);
                position += len_str;

            } else if (value instanceof Integer i) {
                // buffer.put(TYPE_INT);
                if (position >= buffer.length) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = TYPE_INT;

                // buffer.putInt(i);
                int v_i = i;
                if (buffer.length - position < 4) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = (byte) (v_i >> 24);
                buffer[position++] = (byte) (v_i >> 16);
                buffer[position++] = (byte) (v_i >> 8);
                buffer[position++] = (byte) (v_i);

            } else if (value instanceof Long l) {
                // buffer.put(TYPE_LONG);
                if (position >= buffer.length) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = TYPE_LONG;

                // buffer.putLong(l);
                long v_l = l;
                if (buffer.length - position < 8) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = (byte) (v_l >> 56);
                buffer[position++] = (byte) (v_l >> 48);
                buffer[position++] = (byte) (v_l >> 40);
                buffer[position++] = (byte) (v_l >> 32);
                buffer[position++] = (byte) (v_l >> 24);
                buffer[position++] = (byte) (v_l >> 16);
                buffer[position++] = (byte) (v_l >> 8);
                buffer[position++] = (byte) (v_l);

            } else if (value instanceof Double d) {
                // buffer.put(TYPE_DOUBLE);
                if (position >= buffer.length) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = TYPE_DOUBLE;

                // buffer.putLong(Double.doubleToRawLongBits(d));
                long v_d = Double.doubleToRawLongBits(d);
                if (buffer.length - position < 8) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = (byte) (v_d >> 56);
                buffer[position++] = (byte) (v_d >> 48);
                buffer[position++] = (byte) (v_d >> 40);
                buffer[position++] = (byte) (v_d >> 32);
                buffer[position++] = (byte) (v_d >> 24);
                buffer[position++] = (byte) (v_d >> 16);
                buffer[position++] = (byte) (v_d >> 8);
                buffer[position++] = (byte) (v_d);

            } else if (value instanceof Boolean b) {
                // buffer.put(TYPE_BOOLEAN);
                if (position >= buffer.length) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = TYPE_BOOLEAN;

                // buffer.put((byte) (b ? 1 : 0));
                if (position >= buffer.length) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = (byte) (b ? 1 : 0);

            } else if (value instanceof byte[] b) {
                // buffer.put(TYPE_BYTES);
                if (position >= buffer.length) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = TYPE_BYTES;

                // buffer.putInt(b.length);
                int v_blen = b.length;
                if (buffer.length - position < 4) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                buffer[position++] = (byte) (v_blen >> 24);
                buffer[position++] = (byte) (v_blen >> 16);
                buffer[position++] = (byte) (v_blen >> 8);
                buffer[position++] = (byte) (v_blen);

                // buffer.put(b);
                int len_b = b.length;
                if (len_b > buffer.length - position) {
                    throw new IndexOutOfBoundsException("Buffer overflow");
                }
                System.arraycopy(b, 0, buffer, position, len_b);
                position += len_b;
            }
        }

        return buffer;
    }

    public static Packet deserialize(byte[] buffer) throws IOException {
        int position = 0;
        // Read Type
        // int typeLen = buffer.getInt();
        if (buffer.length - position < 4) {
            throw new IndexOutOfBoundsException("Buffer underflow");
        }
        int typeLen = (((buffer[position++]) & 0xFF) << 24) |
                (((buffer[position++]) & 0xFF) << 16) |
                (((buffer[position++]) & 0xFF) << 8) |
                (((buffer[position++]) & 0xFF));

        byte[] typeBytes = new byte[typeLen];
        // buffer.get(typeBytes);
        int len_type = typeBytes.length;
        /* checkBounds(0, len, dst.length) -> (0 | len | (0+len) | (size - (0+len))) < 0 */
        if (len_type > buffer.length - position) {
            throw new IndexOutOfBoundsException("Buffer underflow");
        }
        System.arraycopy(buffer, position, typeBytes, 0, len_type);
        position += len_type;

        String type = new String(typeBytes, StandardCharsets.UTF_8);

        Packet.PacketBuilder builder = Packet.builder().type(type);

        // Read Field Count
        // int fieldCount = buffer.getInt();
        // Read Field Count
        // int fieldCount = buffer.getInt();
        if (buffer.length - position < 4) {
            throw new IndexOutOfBoundsException("Buffer underflow");
        }
        int fieldCount = (((buffer[position++]) & 0xFF) << 24) |
                (((buffer[position++]) & 0xFF) << 16) |
                (((buffer[position++]) & 0xFF) << 8) |
                (((buffer[position++]) & 0xFF));

        for (int i = 0; i < fieldCount; i++) {
            // Read Key
            // int keyLen = buffer.getInt();
            // Read Key
            // int keyLen = buffer.getInt();
            if (buffer.length - position < 4) {
                throw new IndexOutOfBoundsException("Buffer underflow");
            }
            int keyLen = (((buffer[position++]) & 0xFF) << 24) |
                    (((buffer[position++]) & 0xFF) << 16) |
                    (((buffer[position++]) & 0xFF) << 8) |
                    (((buffer[position++]) & 0xFF));

            byte[] keyBytes = new byte[keyLen];
            // buffer.get(keyBytes);
            int len_key = keyBytes.length;
            if (len_key > buffer.length - position) {
                throw new IndexOutOfBoundsException("Buffer underflow");
            }
            System.arraycopy(buffer, position, keyBytes, 0, len_key);
            position += len_key;

            String key = new String(keyBytes, StandardCharsets.UTF_8);

            // Read Type Tag
            // byte typeTag = buffer.get();
            // Read Type Tag
            // byte typeTag = buffer.get();
            if (position >= buffer.length) {
                throw new IndexOutOfBoundsException("Buffer underflow");
            }
            byte typeTag = buffer[position++];

            switch (typeTag) {
                case TYPE_STRING -> {
                    // int len = buffer.getInt();
                    if (buffer.length - position < 4) {
                        throw new IndexOutOfBoundsException("Buffer underflow");
                    }
                    int len = (((buffer[position++]) & 0xFF) << 24) |
                            (((buffer[position++]) & 0xFF) << 16) |
                            (((buffer[position++]) & 0xFF) << 8) |
                            (((buffer[position++]) & 0xFF));

                    byte[] strBytes = new byte[len];
                    // buffer.get(strBytes);
                    if (len > buffer.length - position) {
                        throw new IndexOutOfBoundsException("Buffer underflow");
                    }
                    System.arraycopy(buffer, position, strBytes, 0, len);
                    position += len;

                    builder.putString(key, new String(strBytes, StandardCharsets.UTF_8));
                }
                case TYPE_INT -> {
                    // builder.putInt(key, buffer.getInt());
                    if (buffer.length - position < 4) {
                        throw new IndexOutOfBoundsException("Buffer underflow");
                    }
                    int val = (((buffer[position++]) & 0xFF) << 24) |
                            (((buffer[position++]) & 0xFF) << 16) |
                            (((buffer[position++]) & 0xFF) << 8) |
                            (((buffer[position++]) & 0xFF));
                    builder.putInt(key, val);
                }
                case TYPE_LONG -> {
                    // builder.putLong(key, buffer.getLong());
                    if (buffer.length - position < 8) {
                        throw new IndexOutOfBoundsException("Buffer underflow");
                    }
                    long val = ((((long) buffer[position++]) & 0xFF) << 56) |
                            ((((long) buffer[position++]) & 0xFF) << 48) |
                            ((((long) buffer[position++]) & 0xFF) << 40) |
                            ((((long) buffer[position++]) & 0xFF) << 32) |
                            ((((long) buffer[position++]) & 0xFF) << 24) |
                            ((((long) buffer[position++]) & 0xFF) << 16) |
                            ((((long) buffer[position++]) & 0xFF) << 8) |
                            ((((long) buffer[position++]) & 0xFF));
                    builder.putLong(key, val);
                }
                case TYPE_DOUBLE -> {
                    // builder.putDouble(key, Double.longBitsToDouble(buffer.getLong()));
                    if (buffer.length - position < 8) {
                        throw new IndexOutOfBoundsException("Buffer underflow");
                    }
                    long val = ((((long) buffer[position++]) & 0xFF) << 56) |
                            ((((long) buffer[position++]) & 0xFF) << 48) |
                            ((((long) buffer[position++]) & 0xFF) << 40) |
                            ((((long) buffer[position++]) & 0xFF) << 32) |
                            ((((long) buffer[position++]) & 0xFF) << 24) |
                            ((((long) buffer[position++]) & 0xFF) << 16) |
                            ((((long) buffer[position++]) & 0xFF) << 8) |
                            ((((long) buffer[position++]) & 0xFF));
                    builder.putDouble(key, Double.longBitsToDouble(val));
                }
                case TYPE_BOOLEAN -> {
                    // builder.putBoolean(key, buffer.get() != 0);
                    if (position >= buffer.length) {
                        throw new IndexOutOfBoundsException("Buffer underflow");
                    }
                    builder.putBoolean(key, buffer[position++] != 0);
                }
                case TYPE_BYTES -> {
                    // int len = buffer.getInt();
                    if (buffer.length - position < 4) {
                        throw new IndexOutOfBoundsException("Buffer underflow");
                    }
                    int len = (((buffer[position++]) & 0xFF) << 24) |
                            (((buffer[position++]) & 0xFF) << 16) |
                            (((buffer[position++]) & 0xFF) << 8) |
                            (((buffer[position++]) & 0xFF));

                    byte[] bytes = new byte[len];
                    // buffer.get(bytes);
                    if (len > buffer.length - position) {
                        throw new IndexOutOfBoundsException("Buffer underflow");
                    }
                    System.arraycopy(buffer, position, bytes, 0, len);
                    position += len;

                    builder.putBytes(key, bytes);
                }
                default -> throw new IOException("Unknown type tag: " + typeTag);
            }
        }

        return builder.build();
    }
}
