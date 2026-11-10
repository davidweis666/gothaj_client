package team.gothaj.client.impl.gui.account;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import team.gothaj.client.api.account.Account;
import team.gothaj.client.api.account.AccountRepository;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.ui.button.GothajButton;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.other.SkinUtil;
import team.gothaj.client.api.util.other.CookieLogin;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.StencilUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.account.impl.AddAccountMenu;
import team.gothaj.client.impl.gui.account.impl.InfoAccountMenu;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.opengl.GL11.*;
import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class AccountMenu extends GuiScreen {

    private final List<GothajButton> gothajButtons = new ArrayList<>(List.of(
            new GothajButton("Add", 3),
            new GothajButton("Remove", 2),
            new GothajButton("Login", 1),
            new GothajButton("Direct Login", 4),
            new GothajButton("Random", 7),
            new GothajButton("Info", 9),
            new GothajButton("Cancel", 0)
    ));

    private int focusedIndex = 0;

    private int currentScreen = 0;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private static final float SCREEN_WIDTH_MULTIPLIER = 1.0f;
    private static final float SCROLL_SPEED = 0.06f;

    private float scrollIndicatorProgress = 0f;

    private Account selectedAccount = null;
    private int selectedCardIndex = -1;
    private long lastCardClickTime = 0;
    private static final long DOUBLE_CLICK_THRESHOLD = 350;

    private final AccountRepository.Type[] types = AccountRepository.Type.values();

    private final Map<String, Integer> avatarTextureCache = new HashMap<>();

    private int scissorX, scissorY, scissorW, scissorH;

    private Framebuffer cardFrameBuffer;

    private final Map<AccountRepository.Type, List<Account>> accountsByType = new EnumMap<>(AccountRepository.Type.class);
    private final List<List<Rectangle>> cardRectsByScreen = new ArrayList<>();

    private boolean dataDirty = true;
    private boolean layoutDirty = true;
    private int cachedWidth = -1, cachedHeight = -1;

    private final List<Integer> btnWidthsCache = new ArrayList<>();
    private int buttonsTotalWidth = 0;

    private final ExecutorService avatarExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AccountMenu-AvatarLoader");
        t.setDaemon(true);
        return t;
    });
    private final Set<String> avatarRequested = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, ByteBuffer> avatarPending = new ConcurrentHashMap<>();

    private DropTarget dropTarget;
    private volatile boolean isDraggingOver = false;

    @Override
    public void initGui() {
        cachedWidth = width;
        cachedHeight = height;
        snapshotAccounts();
        rebuildLayout();
        recomputeButtons();

        Component dndComponent = getComponent();
        if (dndComponent != null) {
            dropTarget = new DropTarget(dndComponent, new DropTargetAdapter() {
                @Override
                public void dragEnter(DropTargetDragEvent dtde) {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) isDraggingOver = true;
                }

                @Override
                public void dragExit(DropTargetEvent dte) {
                    isDraggingOver = false;
                }

                @Override
                public void drop(DropTargetDropEvent dtde) {
                    isDraggingOver = false;
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        Transferable transferable = dtde.getTransferable();
                        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                            if (files != null && !files.isEmpty()) {
                                for (File file : files) {
                                    if (file.getName().toLowerCase().endsWith(".txt")) handleCookieDrop(file);
                                }
                                dtde.dropComplete(true);
                            } else {
                                dtde.dropComplete(false);
                            }
                        } else {
                            dtde.dropComplete(false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        dtde.dropComplete(false);
                    }
                }
            });
        }

        super.initGui();
    }

    private Component getComponent() {
        if (Display.getParent() != null) return Display.getParent();
        try {
            java.lang.reflect.Field field = net.minecraft.client.Minecraft.class.getDeclaredField("mcCanvas");
            field.setAccessible(true);
            Object canvas = field.get(mc);
            if (canvas instanceof Component) return (Component) canvas;
        } catch (Exception ignored) {}
        for (Frame f : Frame.getFrames()) {
            if (f.isVisible()) return f;
        }
        return null;
    }

    private void snapshotAccounts() {
        accountsByType.clear();
        for (AccountRepository.Type t : types) {
            GothajClient.getInstance().getAccountRepository().setCurrentType(t);
            List<Account> copy = new ArrayList<>(GothajClient.getInstance().getAccountRepository().getItems());
            accountsByType.put(t, copy);
        }
        dataDirty = false;
        selectedAccount = null;
        selectedCardIndex = -1;
    }

    private void rebuildLayout() {
        cardRectsByScreen.clear();
        for (int i = 0; i < types.length; i++) {
            AccountRepository.Type t = types[i];
            List<Account> accounts = accountsByType.getOrDefault(t, Collections.emptyList());
            cardRectsByScreen.add(computeGridRects(accounts.size()));
        }
        layoutDirty = false;
    }

    private List<Rectangle> computeGridRects(int count) {
        List<Rectangle> rects = new ArrayList<>(count);
        int cardWidth = 130, cardHeight = 30, margin = 12;
        int panelWidth = width - 120;
        int cardsPerRow = Math.max(1, (panelWidth + margin) / (cardWidth + margin));
        int startX = 60, startY = 92;
        for (int i = 0; i < count; i++) {
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            int x = startX + col * (cardWidth + margin);
            int y = startY + row * (cardHeight + margin);
            rects.add(new Rectangle(x, y, cardWidth, cardHeight));
        }
        return rects;
    }

    private void recomputeButtons() {
        btnWidthsCache.clear();
        int spacing = 25;
        buttonsTotalWidth = -spacing;
        for (GothajButton button : gothajButtons) {
            int w = (int) Fonts.nunito14.getStringWidth(button.getDisplayText());
            btnWidthsCache.add(w);
            buttonsTotalWidth += w + spacing;
        }
    }

    private void queueAvatar(String key, String url) {
        if (avatarTextureCache.containsKey(key) || avatarRequested.contains(key)) return;
        avatarRequested.add(key);
        avatarExec.submit(() -> {
            try {
                ByteBuffer buf = SkinUtil.bufferedImageToByteBuffer(url);
                avatarPending.put(key, buf);
            } catch (Throwable ignored) {}
        });
    }

    private void tryUploadPendingAvatar(String key, int w, int h) {
        ByteBuffer buf = avatarPending.remove(key);
        if (buf != null) {
            int texId = SkinUtil.uploadHeadTexture(buf, w, h);
            avatarTextureCache.put(key, texId);
        }
    }

    private void handleCookieDrop(File file) {
        GothajClient.getInstance().getNotificationManager().add("Account", "Login process started for " + file.getName(), Notification.Type.INFO);
        new Thread(() -> {
            try {
                Account account = CookieLogin.loginWithCookie(file);
                mc.addScheduledTask(() -> {
                    if (account != null) {
                        GothajClient.getInstance().getAccountRepository().add(account);
                        loginToAccount(account);
                    } else {
                        GothajClient.getInstance().getNotificationManager().add("Account", "Failed to login with " + file.getName(), Notification.Type.ERROR);
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace(System.err);
                mc.addScheduledTask(() -> GothajClient.getInstance().getNotificationManager().add("Account", "Error: " + t.getClass().getSimpleName(), Notification.Type.ERROR));
            }
        }, "CookieLoginThread").start();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float diff = targetScrollOffset - scrollOffset;
        if (Math.abs(diff) > 0.5f) scrollOffset += diff * SCROLL_SPEED;
        else scrollOffset = targetScrollOffset;

        if (width != cachedWidth || height != cachedHeight) {
            cachedWidth = width;
            cachedHeight = height;
            layoutDirty = true;
            recomputeButtons();
        }
        if (dataDirty) snapshotAccounts();
        if (layoutDirty) rebuildLayout();

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        drawBackground();

        float barHeight = 30;
        float barX = 50;
        float barY = 10f;
        float barTextY = barY + barHeight / 2;

        Blur.startBlur();
        RoundedUtil.drawShaderRound(barX, barY, width - 100, barHeight, 15f, Color.BLACK);
        RoundedUtil.drawShaderRound(barX, barY + barHeight + 10, width - 100, height - (barY + barHeight) * 2, 15f, Color.BLACK);
        Blur.endBlur(10, 1);

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(false);
        RoundedUtil.drawShaderRound(barX, barY, width - 100, barHeight, 15f, Color.BLACK);
        RoundedUtil.drawShaderRound(barX, barY + barHeight + 10, width - 100, height - (barY + barHeight) * 2, 15f, Color.BLACK);
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());

        RoundedUtil.drawRound(barX, barY, width - 100, barHeight, 15f, new Color(20, 20, 20, 100));
        RoundedUtil.drawRound(barX, barY + barHeight + 10, width - 100, height - (barY + barHeight) * 2, 15f, new Color(20, 20, 20, 100));
        Fonts.surf20.drawString("ACCOUNT MANAGER", barX + 10, barY + barHeight / 2f, Color.WHITE.getRGB());

        scissorX = (int) barX;
        scissorY = (int) (barY + barHeight + 10);
        scissorW = Math.max(0, (int) (width - 100));
        scissorH = Math.max(0, (int) (height - (barY + barHeight) * 2));

        if (scissorW > 0 && scissorH > 0) {
            cardFrameBuffer = RenderUtil.createFrameBuffer(cardFrameBuffer, true);
            cardFrameBuffer.framebufferClear();
            cardFrameBuffer.bindFramebuffer(true);
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.startScissorBox(scissorX, scissorY, scissorW, scissorH);
            for (int i = 0; i < types.length; i++) {
                renderTypeScreenBloomShapes(types[i], i);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glPopMatrix();
            cardFrameBuffer.unbindFramebuffer();
            Bloom.renderBloom(cardFrameBuffer.framebufferTexture, 4, Color.BLACK.getRGB());

            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.startScissorBox(scissorX, scissorY, scissorW, scissorH);
            for (int i = 0; i < types.length; i++) {
                renderTypeScreen(types[i], mouseX, mouseY, i);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glPopMatrix();
        } else {
            for (int i = 0; i < types.length; i++) {
                renderTypeScreen(types[i], mouseX, mouseY, i);
            }
        }

        float buttonsY = barTextY - Fonts.nunito14.getHeight() - 3;
        renderButtons(mouseX, mouseY, buttonsY);

        Account current = GothajClient.getInstance().getAccountRepository().getCurrentAccount();
        String avatarKey = current.isCracked() ? "cracked" : mc.session.getUsername();
        if (!avatarTextureCache.containsKey(avatarKey)) {
            String url = current.isCracked()
                    ? "https://minotar.net/helm/gothajgayshit/64"
                    : "https://minotar.net/helm/" + mc.session.getUsername() + "/64";
            queueAvatar(avatarKey, url);
            tryUploadPendingAvatar(avatarKey, 64, 64);
        }
        Integer textureId = avatarTextureCache.get(avatarKey);
        if (textureId != null) {
            guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
            guiFrameBuffer.framebufferClear();
            guiFrameBuffer.bindFramebuffer(true);
            RoundedUtil.drawShaderRound((int) (barX + width - 128), (int) (barY + 5.2f), 20, 20, 8f, Color.BLACK);
            guiFrameBuffer.unbindFramebuffer();
            Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 4, Color.BLACK.getRGB());

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            RenderUtil.resetColor();
            RoundedUtil.drawRoundTextured((int) (barX + width - 128), (int) (barY + 5.2f), 20, 20, 8f, 255f);
        } else {
            RoundedUtil.drawRound((int) (barX + width - 128), (int) (barY + 5.2f), 20, 20, 8f, new Color(60, 60, 60, 160));
        }

        Fonts.nunito16.drawString(mc.session.getUsername(),
                barX + width - 133 - Fonts.nunito16.getStringWidth(mc.session.getUsername()),
                barY + barHeight / 2f - 1.5f,
                new Color(156, 156, 156, 255).getRGB());

        float progressTarget = ((float) currentScreen) / (types.length - 1);
        float indicatorAnimSpeed = 0.009f;
        scrollIndicatorProgress += (progressTarget - scrollIndicatorProgress) * indicatorAnimSpeed;

        drawScrollIndicator(scaledResolution);
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (isDraggingOver) {
            RoundedUtil.drawRound(0, 0, width, height, 0, new Color(0, 0, 0, 150));
            Fonts.surf20.drawCenteredString("Drop Cookie File to Login", width / 2f, height / 2f, Color.WHITE.getRGB());
        }
    }

    private void renderTypeScreenBloomShapes(AccountRepository.Type type, int screenIndex) {
        float offsetX = (screenIndex * width * SCREEN_WIDTH_MULTIPLIER) - scrollOffset;
        List<Account> accounts = accountsByType.getOrDefault(type, Collections.emptyList());
        List<Rectangle> currentRects = (screenIndex < cardRectsByScreen.size()) ? cardRectsByScreen.get(screenIndex) : Collections.emptyList();

        int cardHeight = 32;

        for (int i = 0; i < accounts.size() && i < currentRects.size(); i++) {
            Rectangle hitbox = currentRects.get(i);
            float finalX = hitbox.x + offsetX;
            float finalY = hitbox.y;
            float cardWidth = hitbox.width;
            if (finalX + cardWidth < 0 || finalX > width) continue;
            RoundedUtil.drawShaderRound(finalX, finalY, cardWidth, cardHeight, 10f, Color.BLACK);
        }
    }

    private void renderTypeScreen(AccountRepository.Type type, int mouseX, int mouseY, int screenIndex) {
        float titleY = 70;
        float offsetX = (screenIndex * width * SCREEN_WIDTH_MULTIPLIER) - scrollOffset;

        Fonts.surf20.drawCenteredString(type.toString(), (width / 2f) + offsetX, titleY, Color.WHITE.getRGB());

        List<Account> accounts = accountsByType.getOrDefault(type, Collections.emptyList());
        List<Rectangle> currentRects = (screenIndex < cardRectsByScreen.size()) ? cardRectsByScreen.get(screenIndex) : Collections.emptyList();

        int cardHeight = 32;
        int avatarSize = 24;
        int padding = 12;

        for (int i = 0; i < accounts.size() && i < currentRects.size(); i++) {
            Rectangle hitbox = currentRects.get(i);

            float finalX = hitbox.x + offsetX;
            float finalY = hitbox.y;
            float cardWidth = hitbox.width;

            if (finalX + cardWidth < 0 || finalX > width) continue;

            Color pColor = GothajClient.getInstance().getThemeManager().getCurrentTheme().primary();
            Color sColor = GothajClient.getInstance().getThemeManager().getCurrentTheme().secondary();

            if (currentScreen == type.ordinal() && i == selectedCardIndex) {
                RoundedUtil.drawGradientHorizontal(finalX - 1, finalY - 1, cardWidth + 2, cardHeight + 2, 12f,
                        new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 120),
                        new Color(sColor.getRed(), sColor.getGreen(), sColor.getBlue(), 120));
            }

            Blur.startBlur();
            RoundedUtil.drawShaderRound(finalX, finalY, cardWidth, cardHeight, 10f, Color.BLACK);
            Blur.endBlur(10, 1);

            boolean isHovered = mouseX >= finalX && mouseX <= finalX + cardWidth && mouseY >= finalY && mouseY <= finalY + cardHeight;
            RoundedUtil.drawRound(finalX, finalY, cardWidth, cardHeight, 10f, isHovered ? new Color(25, 25, 25, 100) : new Color(25, 25, 25, 40));

            Account acc = accounts.get(i);
            String avatarKey = acc.isCracked() || acc.getUsername() == null ? "cracked" : acc.getUsername();

            if (!avatarTextureCache.containsKey(avatarKey)) {
                String url = acc.isCracked()
                        ? "https://minotar.net/helm/gothajgayshit/64"
                        : "https://minotar.net/helm/" + acc.getUsername() + "/64";
                queueAvatar(avatarKey, url);
                tryUploadPendingAvatar(avatarKey, 64, 64);
            }
            Integer textureId = avatarTextureCache.get(avatarKey);

            if (textureId != null) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
                RenderUtil.resetColor();
                RoundedUtil.drawRoundTextured(
                        finalX + padding,
                        finalY + (float) (cardHeight - avatarSize) / 2,
                        avatarSize, avatarSize, 8f, 255f
                );
            } else {
                RoundedUtil.drawRound(
                        finalX + padding,
                        finalY + (float) (cardHeight - avatarSize) / 2,
                        avatarSize, avatarSize, 8f,
                        new Color(60, 60, 60, 160)
                );
            }

            Fonts.nunito18.drawString(
                    acc.getUsername(),
                    finalX + padding + avatarSize + 8,
                    finalY + (cardHeight / 2f) - (Fonts.nunito16.getHeight() / 2f) - 6,
                    Color.WHITE.getRGB()
            );

            Fonts.nunito18.drawString(
                    acc.isCracked() ? "§cCracked" : acc.isExpired() ? "§cExpired" : "§aPremium",
                    finalX + padding + avatarSize + 8,
                    finalY + (cardHeight / 2f) - (Fonts.nunito16.getHeight() / 2f) + 4,
                    Color.WHITE.getRGB()
            );
        }
    }

    private void renderButtons(int mouseX, int mouseY, float textY) {
        int btnHeight = 20;
        int spacing = 25;
        if (btnWidthsCache.isEmpty() || btnWidthsCache.size() != gothajButtons.size()) recomputeButtons();
        int currentX = (width - buttonsTotalWidth) / 2;
        for (int i = 0; i < gothajButtons.size(); i++) {
            GothajButton button = gothajButtons.get(i);
            int w = btnWidthsCache.get(i);
            button.setXPos(currentX);
            button.setYPos(textY);
            button.setWidth(w);
            button.setHeight(btnHeight);
            currentX += w + spacing;
            if (button.getOnClick() == null) button.setOnClick(() -> handleButtonAction(button.getId()));
            button.drawScreen(mouseX, mouseY);
        }
    }

    private void drawScrollIndicator(ScaledResolution scaledResolution) {
        float indicatorWidth = 100;
        float indicatorHeight = 4;
        float indicatorX = (float) scaledResolution.getScaledWidth() / 2 - indicatorWidth / 2;
        float indicatorY = scaledResolution.getScaledHeight() - 20;

        RoundedUtil.drawShaderRound(indicatorX, indicatorY, indicatorWidth, indicatorHeight, 2f, new Color(255, 255, 255, 30));
        float progress = (scrollOffset / (width * SCREEN_WIDTH_MULTIPLIER)) / (types.length - 1);
        float progressWidth = indicatorWidth * Math.max(0, Math.min(1, progress));
        RoundedUtil.drawShaderRound(indicatorX, indicatorY, progressWidth, indicatorHeight, 2f, new Color(255, 255, 255, 80));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == 203 && currentScreen > 0) {
            currentScreen--;
            targetScrollOffset = currentScreen * width * SCREEN_WIDTH_MULTIPLIER;
        } else if (keyCode == 205 && currentScreen < types.length - 1) {
            currentScreen++;
            targetScrollOffset = currentScreen * width * SCREEN_WIDTH_MULTIPLIER;
        } else if (keyCode == Keyboard.KEY_A || keyCode == Keyboard.KEY_LEFT) {
            if (focusedIndex > 0) focusedIndex--;
        } else if (keyCode == Keyboard.KEY_D || keyCode == Keyboard.KEY_RIGHT) {
            if (focusedIndex < gothajButtons.size() - 1) focusedIndex++;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        gothajButtons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));
        if (mouseButton == 0) {
            List<Account> accounts = accountsByType.getOrDefault(types[currentScreen], Collections.emptyList());
            List<Rectangle> currentHitboxes = (currentScreen < cardRectsByScreen.size()) ? cardRectsByScreen.get(currentScreen) : null;
            if (currentHitboxes != null) {
                int clickedIndex = -1;
                for (int i = 0; i < Math.min(currentHitboxes.size(), accounts.size()); i++) {
                    if (currentHitboxes.get(i).contains(mouseX, mouseY)) {
                        clickedIndex = i;
                        break;
                    }
                }
                if (clickedIndex != -1) {
                    Account clickedAccount = accounts.get(clickedIndex);
                    long now = System.currentTimeMillis();
                    if (clickedIndex == selectedCardIndex && clickedAccount.equals(selectedAccount)
                            && now - lastCardClickTime < DOUBLE_CLICK_THRESHOLD) {
                        loginToAccount(clickedAccount);
                    } else {
                        selectedCardIndex = clickedIndex;
                        selectedAccount = clickedAccount;
                        lastCardClickTime = now;
                    }
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel > 0 && currentScreen > 0) currentScreen--;
        else if (dWheel < 0 && currentScreen < types.length - 1) currentScreen++;
        targetScrollOffset = currentScreen * width * SCREEN_WIDTH_MULTIPLIER;
    }

    private void handleButtonAction(int id) {
        switch (id) {
            case 0 -> mc.displayGuiScreen(null);
            case 1 -> { if (selectedAccount != null) loginToAccount(selectedAccount); }
            case 2 -> {
                if (selectedAccount != null) {
                    GothajClient.getInstance().getAccountRepository().remove(selectedAccount);
                    dataDirty = true;
                    layoutDirty = true;
                }
            }
            case 3 -> mc.displayGuiScreen(new AddAccountMenu());
            case 4 -> mc.displayGuiScreen(new AddAccountMenu(true));
            case 7 -> {
                List<Account> accounts = accountsByType.getOrDefault(types[currentScreen], Collections.emptyList());
                if (!accounts.isEmpty()) loginToAccount(accounts.get(new Random().nextInt(accounts.size())));
            }
            case 8 -> {}
            case 9 -> { if (selectedAccount != null) mc.displayGuiScreen(new InfoAccountMenu(selectedAccount)); }
            default -> {}
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (dropTarget != null) {
            dropTarget.removeNotify();
            dropTarget = null;
        }
        for (int textureId : avatarTextureCache.values()) GL11.glDeleteTextures(textureId);
        avatarTextureCache.clear();
        avatarExec.shutdownNow();
        avatarPending.clear();
        avatarRequested.clear();
        GothajClient.getInstance().getAccountRepository().save();
    }

    private void loginToAccount(Account account) {
        GothajClient.getInstance().getAccountRepository().setCurrentAccount(account);
        System.out.println("[Login] token: " + account.getAccessToken());
        System.out.println("[Login] uuid: " + account.getUuid());
        System.out.println("[Login] cracked: " + account.isCracked());
        mc.session = new Session(
                account.getUsername(),
                account.getUuid().toString().replace("-", ""),
                account.getAccessToken() != null ? account.getAccessToken() : "",
                account.isCracked() ? "legacy" : "msa"
        );
        GothajClient.getInstance().getIrcManager().getClient().setIngameName(account.getUsername());
        GothajClient.getInstance().getNotificationManager().add("Account", "Logged in as " + account.getUsername(), Notification.Type.SUCCESS);
        dataDirty = true;
        layoutDirty = true;
    }
}