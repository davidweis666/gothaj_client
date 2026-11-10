package team.gothaj.client.api.util.rotation.library;

import dev.algorithm.net.client.AlgorithmClient;
import dev.algorithm.net.client.LocalGuiClient;
import dev.algorithm.net.client.RemoteRotation;
import dev.algorithm.net.protocol.dto.AabbDto;
import dev.algorithm.net.protocol.dto.Vec3Dto;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import team.gothaj.client.api.property.properties.ModeProperty;

import java.util.concurrent.CompletableFuture;

import static team.gothaj.client.api.util.Utils.mc;

public class LibraryClient implements AutoCloseable {
    public AlgorithmClient client;
    public LocalGuiClient  gui;
    private volatile float[] cachedRotations = new float[]{0f, 0f};
    private volatile boolean requesting;
    private long lastRequest;
    public void resetRotationData() {
        CompletableFuture.runAsync(() -> {
            ensureConnected();
            if (client != null) try { client.reset(); } catch (Exception ignored) {}
        });
    }

    @Override
    public void close() {
        CompletableFuture.runAsync(() -> {
            if (gui != null) try {
                gui.close();
            } catch (Exception ignored) {
            }
            if (client != null) try {
                client.close();
            } catch (Exception ignored) {
            }
            client = null;
            gui = null;
        });
    }

    public void getGui(ModeProperty mode) {
        if (gui == null) return;
        boolean want = mode.is("Library");
        if (want && !gui.isSettingsOpen()) gui.openSettings();
        if (!want &&  gui.isSettingsOpen()) gui.closeSettings();
    }
    public boolean isConnected() {
        return client != null;
    }

    public float[] getRotation(Entity target) {
        if (target == null || mc.thePlayer == null) {
            lastRequest = System.currentTimeMillis();
            requesting = false;
            return cachedRotations;
        }

        if (!isConnected()) {
            ensureConnected();
            lastRequest = System.currentTimeMillis();
            requesting = false;
            return cachedRotations;
        }

        long now = System.currentTimeMillis();

        if (!requesting && now - lastRequest > 0) {
            lastRequest = now;
            requesting = true;

            AxisAlignedBB bb = target.getEntityBoundingBox();

            Vec3Dto playerPos = new Vec3Dto(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                    mc.thePlayer.posZ
            );

            Vec3Dto lookVec = new Vec3Dto(
                    mc.thePlayer.getLookVec().xCoord,
                    mc.thePlayer.getLookVec().yCoord,
                    mc.thePlayer.getLookVec().zCoord
            );

            Vec3Dto targetPos = new Vec3Dto(
                    target.posX,
                    target.posY,
                    target.posZ
            );

            AabbDto aabb = new AabbDto(
                    bb.minX,
                    bb.minY,
                    bb.minZ,
                    bb.maxX,
                    bb.maxY,
                    bb.maxZ
            );

            int hurtTime = target instanceof EntityLivingBase
                    ? ((EntityLivingBase) target).hurtTime
                    : 0;

            boolean sprinting = mc.thePlayer.isSprinting();

            CompletableFuture.runAsync(() -> {
                try {
                    RemoteRotation r = client.getRotationFromServer(
                            playerPos,
                            lookVec,
                            targetPos,
                            aabb,
                            hurtTime,
                            sprinting
                    );

                    cachedRotations = new float[]{
                            r.yaw,
                            r.pitch
                    };

                } catch (Exception e) {
                    client = null;
                    cachedRotations = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
                } finally {
                    requesting = false;
                }
            });
        }

        return cachedRotations;
    }

    public void ensureConnected() {
        client = null;
        // because my server is over; I'll add new rotations soon, most of currently randomizes, got patched, i will recode rotations, and add new rotation
    }
}
