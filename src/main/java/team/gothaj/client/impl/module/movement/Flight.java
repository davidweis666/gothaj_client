package team.gothaj.client.impl.module.movement;

import javafx.geometry.Side;
import net.minecraft.init.Items;
import net.minecraft.network.Packet;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Action;
import team.gothaj.client.api.util.ActionManager;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.client.impl.event.player.WorldChangeEvent;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.client.impl.module.misc.Disabler;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;
import team.gothaj.eventbus.Priority;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Flight extends Module {


    private final ModeProperty mode = new ModeProperty("Mode", "PolarWater", "PolarWater", "Vanilla", "MMC", "Dev", "PolarDeath");

    private final NumberProperty speed = new NumberProperty("Speed", 2.0f, 0.1f, 10.0f, 0.1f).setVisible(() -> mode.is("Vanilla") || mode.is("PolarDeath"));

    private final BooleanProperty kickBypass = new BooleanProperty("Kick Bypass", false).setVisible(() -> mode.is("Dev"));

    private final ConcurrentLinkedQueue<Packet<?>> packetsList = new ConcurrentLinkedQueue<>();

    private boolean originalIsFlying;
    private long lastKickBypass;

    public final ConcurrentLinkedQueue<Packet<?>> c0fPackets =
            new ConcurrentLinkedQueue<>();

    public final ConcurrentLinkedQueue<Packet<?>> keepAlivePackets =
            new ConcurrentLinkedQueue<>();

    private long lastReleaseTime;
    private boolean cancelingPackets;
    private int ticksUntilStart;
    private long lastChunkRefreshTime;


    public Flight() {
        super("Flight", "", ModuleCategory.MOVEMENT);
        registerProperties(mode, speed, kickBypass);
    }

    private boolean listenForTheFUCKIGNdamage = false;

    @Override
    public String getSuffix() {
        return mode.getProperty();
    }

    @Override
    public void onEnable() {

        if (mode.is("PolarWater")) sendPackets();

        if (mode.is("Dev")) {
            originalIsFlying = mc.thePlayer.capabilities.isFlying;
            lastKickBypass = 0L;
        }

        listenForTheFUCKIGNdamage = false;
        super.onEnable();

    }

    @Override
    public void onDisable() {
        flagCounter = 0;
        queueRejoin = false;
        if (mode.is("PolarWater")) sendPackets();

        if (mode.is("Dev")) {
            mc.thePlayer.capabilities.isFlying = originalIsFlying;
        }
        cancelingPackets = false;
        clearPackets();
        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
        mc.timer.timerSpeed = 1.0f;
        listenForTheFUCKIGNdamage = false;
        charging = false;
        chargeTicks = 0;
        doFloat = false;
        floatTicks = 0;
        nigga = false;
        super.onDisable();
    }

    @Listen
    public void onMotion(MotionEvent event) {
        if (mode.is("PolarWater")) {
            event.setOnGround(true);
        }
    }

    public int findDaNigga() {
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.mainInventory[i] == null) continue;
            if (mc.thePlayer.inventory.mainInventory[i].getItem() == null) continue;
            if (mc.thePlayer.inventory.mainInventory[i].getItem() != Items.bow) continue;
            return i;
        }
        return -1;
    }

    private boolean charging = false;
    private boolean nigga = false;
    private int chargeTicks = 0;
    private int flagCounter = 0;

    @Listen(priority = Priority.CRITICAL)
    public void onMoveInput(MoveInputEvent event) {
        if (charging) {
            event.setForward(0f);
            event.setStrafe(0f);
            event.setJump(false);
        }
    }

    @Listen
    public void onUpdate(LivingUpdateEvent event) {
        if (mode.is("PolarDeath") && iKnowThisCodeIsShitButItsFineAndIfYoureReadingThisUloveCP()) {
            ticksUntilStart++;
            if (cancelingPackets) {
                if ((System.currentTimeMillis() - lastReleaseTime) >= 10000) {
                    cancelingPackets = false;
                    clearPackets();
                } else {
                    if (System.currentTimeMillis() - lastReleaseTime >= 4000L) {
                        lastReleaseTime = System.currentTimeMillis();

                        Packet<?> packet = c0fPackets.poll();

                        if (packet != null) {
                            PacketUtil.send(packet);
                        }
                    }

                    if (System.currentTimeMillis() - lastChunkRefreshTime >= 300L) {
                        lastChunkRefreshTime = System.currentTimeMillis();
                        mc.renderGlobal.loadRenderers();
                    }
                }
            }
            if (ticksUntilStart % 2 == 0) {
                mc.thePlayer.motionY = 0.0;

                if (mc.gameSettings.keyBindJump.isKeyDown()) mc.thePlayer.motionY = 1;
                if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.thePlayer.motionY = -1;

                if (MovementUtil.isMoving()) {
                    MovementUtil.strafe(speed.getProperty());
                } else {
                    MovementUtil.strafe(0);
                }
            }
        } else if (mode.is("PolarDeath") && !iKnowThisCodeIsShitButItsFineAndIfYoureReadingThisUloveCP() && mc.thePlayer.ticksExisted % 10 == 0) {
            Logger.clientChat("!! Please turn on disabler !!!");
            Logger.clientChat("Disabler -> 'Polar TimeOut'");
        }
        if (mode.is("MMC")) {
            if (doFloat) {
                floatTicks++;
                mc.thePlayer.motionY *= 1.155;
                MovementUtil.boost(0.3f);
                if (floatTicks >= 20) {
                    doFloat = false;
                    onDisable();
                }
            }
            int bowSlot = findDaNigga();
            if (bowSlot != -1) {
                RotationComponent.setRotations(mc.thePlayer.rotationYaw, -90f, 180, true);
                mc.thePlayer.inventory.currentItem = bowSlot;
                mc.playerController.syncCurrentPlayItem();

                if (!nigga) {
                    if (!charging) {
                        charging = true;
                        chargeTicks = 0;
                    } else {
                        mc.gameSettings.keyBindUseItem.pressed = true;
                        chargeTicks++;
                        //mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                        if (chargeTicks >= 4) {
                            nigga = true;
                            mc.gameSettings.keyBindUseItem.pressed = false;
                            charging = false;
                            listenForTheFUCKIGNdamage = true;
                            chargeTicks = 0;
                        }
                    }
                }

            }
        }
    }

    @Listen
    public void onTick(TickEvent event) {
        if (mode.is("PolarWater")) {
            mc.thePlayer.motionY = 0.0f;
            mc.thePlayer.onGround = true;
        }

        if (mode.is("Vanilla")) {
            if (MovementUtil.isMoving()) MovementUtil.strafe(speed.getProperty());
            else MovementUtil.strafe(0.0);
            mc.thePlayer.onGround = false;
            mc.thePlayer.isInWeb = false;
            mc.thePlayer.capabilities.isFlying = false;

            double ySpeed = 0.0;
            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                ySpeed += speed.getProperty();
            }
            if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                ySpeed -= speed.getProperty();
            }
            mc.thePlayer.motionY = ySpeed;
        }

        if (mode.is("Dev")) {
            mc.thePlayer.capabilities.isFlying = true;
            if (kickBypass.getProperty()) {
                handleCreativeKickBypass();
            }
        }
    }

    private boolean isDesyncActive() {
        Disabler disabler = GothajClient.getInstance().getModuleManager().getAndCast(Disabler.class);
        return disabler != null && disabler.isToggled() && disabler.mode.is("Netanyahu");
    }

    private void handleCreativeKickBypass() {
        if (System.currentTimeMillis() - lastKickBypass < 1000L) return;

        double ground = calculateGround() + 0.5;

        double posY = mc.thePlayer.posY;
        while (posY > ground) {
            PacketUtil.send(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ, true));
            if (posY - 8.0 < ground) break;
            posY -= 8.0;
        }

        PacketUtil.send(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, ground, mc.thePlayer.posZ, true));
        posY = ground;
        while (posY < mc.thePlayer.posY) {
            PacketUtil.send(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ, true));
            if (posY + 8.0 > mc.thePlayer.posY) break;
            posY += 8.0;
        }

        PacketUtil.send(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
        lastKickBypass = System.currentTimeMillis();
    }

    private double calculateGround() {
        AxisAlignedBB playerBoundingBox = mc.thePlayer.getEntityBoundingBox();
        double blockHeight = 0.05;
        double ground = mc.thePlayer.posY;

        while (ground > 0.0) {
            AxisAlignedBB customBox = AxisAlignedBB.fromBounds(
                    playerBoundingBox.maxX, ground + blockHeight, playerBoundingBox.maxZ,
                    playerBoundingBox.minX, ground, playerBoundingBox.minZ);

            if (mc.theWorld.checkBlockCollision(customBox)) {
                if (blockHeight <= 0.05) return ground + blockHeight;
                ground += blockHeight;
                blockHeight = 0.05;
            }

            ground -= blockHeight;
        }

        return 0.0;
    }

    public boolean iKnowThisCodeIsShitButItsFineAndIfYoureReadingThisUloveCP() {
        var mod = GothajClient.getInstance().getModuleManager().getAndCast(Disabler.class);
        return mod.isToggled() && mod.mode.is("Polar TimeOut");
    }

    @Listen
    public void onWorldChange(WorldChangeEvent event) {
        if (queueRejoin && mode.is("PolarDeath") && iKnowThisCodeIsShitButItsFineAndIfYoureReadingThisUloveCP()) {
            PacketUtil.send(new C01PacketChatMessage("/rejoin"));
            queueRejoin = false;
            flagCounter = 0;
            if (cancelingPackets) {
                clearPackets();
                cancelingPackets = false;
            }
        }
    }

    private boolean doFloat = false;
    private int floatTicks = 0;
    private boolean queueRejoin = false;

    @Listen
    public void onPacket(PacketEvent event) {
        if (mode.is("PolarDeath") && iKnowThisCodeIsShitButItsFineAndIfYoureReadingThisUloveCP()) {
            if (event.getPacket() instanceof S45PacketTitle titlePacket) {
                if (titlePacket.getMessage() != null) {
                    String text = titlePacket.getMessage().getUnformattedText();
                    if (text.contains("До респавна 1 сек.")) {
                        new Thread(() -> {
                            try {
                                Thread.sleep(920);
                            } catch (InterruptedException ignored) {}
                            cancelingPackets = true;
                            lastReleaseTime = System.currentTimeMillis();
                        }).start();
                    }
                }
            }
            if (cancelingPackets) {
                if ((System.currentTimeMillis() - lastReleaseTime) >= 10000) {
                    cancelingPackets = false;
                    clearPackets();
                } else {
                    if (event.getPacket() instanceof C0FPacketConfirmTransaction) {
                        c0fPackets.add(event.getPacket());
                        event.setCancelled(true);
                    } else if (event.getPacket() instanceof C00PacketKeepAlive) {
                        keepAlivePackets.add(event.getPacket());
                        event.setCancelled(true);
                    }
                }
            }

            if (event.getPacket() instanceof S08PacketPlayerPosLook) {
                if (++flagCounter >= 5) {
                    PacketUtil.send(new C01PacketChatMessage("/leave"));
                    queueRejoin = true;
                    flagCounter = 0;
                }
            }
        }
        if (event.getDirection() == PacketEvent.Direction.RECEIVE && event.getPacket() instanceof S12PacketEntityVelocity s12) {
            if (s12.getEntityID() == mc.thePlayer.getEntityId()) {
                GothajClient.getInstance().getActionManager().queue(
                        new Action(
                                () -> {
                                    doFloat = true;
                                },
                                4
                        )
                );
                listenForTheFUCKIGNdamage = false;
            }
        }
        if (mode.is("PolarWater")) {
            if (event.getDirection() == PacketEvent.Direction.SEND && event.getPacket() instanceof C0FPacketConfirmTransaction) {
                if (!isDesyncActive()) {
                    synchronized (packetsList) {
                        event.setCancelled(true);
                        packetsList.add(event.getPacket());
                    }
                }
            }
        }
    }

    private void sendPackets() {
        if (mc.thePlayer != null) synchronized (packetsList) {
            packetsList.forEach(packet -> mc.getNetHandler().getNetworkManager().sendPacket(packet));
        }
        packetsList.clear();
    }

    private void clearPackets() {
        if (!c0fPackets.isEmpty()) {
            c0fPackets.forEach(PacketUtil::sendSilent);
            c0fPackets.clear();
        }
        if (!keepAlivePackets.isEmpty()) {
            keepAlivePackets.forEach(PacketUtil::sendSilent);
            keepAlivePackets.clear();
        }
    }
}
