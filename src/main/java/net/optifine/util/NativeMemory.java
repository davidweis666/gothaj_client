package net.optifine.util;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.function.LongSupplier;

import net.minecraft.src.Config;

public class NativeMemory {
    private static final LongSupplier bufferAllocatedSupplier = createDirectBufferSupplier();
    private static final LongSupplier bufferMaximumSupplier = createMaxDirectMemorySupplier();

    public static long getBufferAllocated() {
        return bufferAllocatedSupplier != null ? bufferAllocatedSupplier.getAsLong() : -1L;
    }

    public static long getBufferMaximum() {
        return bufferMaximumSupplier != null ? bufferMaximumSupplier.getAsLong() : -1L;
    }

    private static LongSupplier createDirectBufferSupplier() {
        try {
            List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);

            for (BufferPoolMXBean pool : pools) {
                if ("direct".equals(pool.getName())) {
                    return new LongSupplier() {
                        @Override
                        public long getAsLong() {
                            try {
                                return pool.getMemoryUsed();
                            } catch (Exception e) {
                                Config.warn("Failed to get direct buffer memory usage: " + e.getMessage());
                                return -1L;
                            }
                        }
                    };
                }
            }

            Config.warn("Direct buffer pool not found");
            return null;
        } catch (Exception e) {
            Config.warn("Failed to initialize direct buffer supplier: " + e.getMessage());
            return null;
        }
    }

    private static LongSupplier createMaxDirectMemorySupplier() {
        try {
            long maxMemory = Runtime.getRuntime().maxMemory();

            String maxDirectProp = System.getProperty("sun.nio.MaxDirectMemorySize");
            if (maxDirectProp != null) {
                try {
                    long explicitMax = Long.parseLong(maxDirectProp);
                    return () -> explicitMax;
                } catch (NumberFormatException e) {
                    Config.warn("Invalid MaxDirectMemorySize property: " + maxDirectProp);
                }
            }

            return () -> maxMemory;
        } catch (Exception e) {
            Config.warn("Failed to determine max direct memory: " + e.getMessage());
            return null;
        }
    }
}
