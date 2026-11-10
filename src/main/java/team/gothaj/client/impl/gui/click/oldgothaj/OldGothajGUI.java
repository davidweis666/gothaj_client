package team.gothaj.client.impl.gui.click.oldgothaj;

import java.io.IOException;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.Property;
import team.gothaj.client.api.property.properties.*;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.main.GothajClient;

public class OldGothajGUI extends GuiScreen {

    private float px, py;
    private static final float PW = 430f, PH = 295f;
    private static final float SBW = 88f;
    private static final float HEADER_H = 34f;

    private final List<Module> allModules = new ArrayList<>();
    private final List<Module> modules = new ArrayList<>();
    private ModuleCategory cat = ModuleCategory.COMBAT;
    private Module expanded;

    private String searchText = "";
    private boolean searchFocused;

    private float scroll, targetScroll, maxScroll;

    private final float[] catAnim = new float[ModuleCategory.values().length];
    private float open;
    private boolean closing;
    private long lastClick;

    public OldGothajGUI() {}

    @Override
    public void initGui() {
        ScaledResolution sr = new ScaledResolution(mc);
        px = (sr.getScaledWidth() - PW) / 2f;
        py = (sr.getScaledHeight() - PH) / 2f;
        open = 0f;
        closing = false;
        reload();
    }

    private void reload() {
        allModules.clear();
        allModules.addAll(GothajClient.getInstance().getModuleManager().getModulesInCategory(cat));
        filterModules();
        scroll = targetScroll = maxScroll = 0;
    }

    private void filterModules() {
        modules.clear();
        if (searchText.isEmpty()) {
            modules.addAll(allModules);
        } else {
            for (Module m : allModules) {
                if (m.getName().toLowerCase().contains(searchText.toLowerCase()))
                    modules.add(m);
            }
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        if (closing) { mc.displayGuiScreen(null); return; }
        open += (1f - open) * 0.1f;
        scroll += (targetScroll - scroll) * 0.45f;

        Blur.startBlur();
        Gui.drawRect(0, 0, width, height, new Color(0, 0, 0, (int)(70 * open)).getRGB());
        Blur.endBlur(15, 1);

        int accent = new Color(90, 55, 155).getRGB();

        RoundedUtil.drawRound(px, py, PW, PH, 5, new Color(18, 18, 21, 245));
        RoundedUtil.drawRound(px + 1, py + 1, SBW, PH - 2, 4, new Color(14, 14, 17, 250));

        // Logo centered in sidebar header
        float headerCY = py + HEADER_H / 2f;
        Fonts.rubik18Bold.drawString("Gothaj", px + SBW / 2f - Fonts.rubik18Bold.getStringWidth("Gothaj") / 2f,
                headerCY - Fonts.rubik18Bold.getHeight() / 2f, -1);

        Gui.drawRect((int)(px + 6), (int)(py + HEADER_H + 2), (int)(px + SBW - 6), (int)(py + HEADER_H + 3), new Color(30, 30, 36).getRGB());

        ModuleCategory[] cats = ModuleCategory.values();
        float cY = py + HEADER_H + 10;
        for (int i = 0; i < cats.length; i++) {
            ModuleCategory c = cats[i];
            float t = (cat == c) ? 1f : 0f;
            catAnim[i] += (t - catAnim[i]) * 0.16f;
            int col = ColorUtil.mix(new Color(150, 150, 160).getRGB(), accent, catAnim[i], 1.0);
            Color cb = new Color(accent);
            cb = new Color(cb.getRed(), cb.getGreen(), cb.getBlue(), (int)(35 * catAnim[i]));

            float bx = px + 4, bw = SBW - 8, bh = 22, by = cY + i * (bh + 2);
            boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
            if (hov && cat != c) Gui.drawRect((int)bx, (int)by, (int)(bx+bw), (int)(by+bh), new Color(28, 28, 33).getRGB());
            RoundedUtil.drawRound(bx, by, bw, bh, 3, cb);

            Fonts.catFont18.drawString(getIcon(c), bx + 7, by + bh / 2 - Fonts.catFont18.getHeight() / 2f, col);
            Fonts.nunito12.drawString(c.name().charAt(0) + c.name().substring(1).toLowerCase(), bx + 25, by + bh / 2 - Fonts.nunito12.getHeight() / 2f, col);
        }

        float cX = px + SBW + 3;
        float cYr = py + 3;
        float cW = PW - SBW - 6;
        float cH = PH - 6;

        String cn = cat.name().charAt(0) + cat.name().substring(1).toLowerCase();
        Fonts.nunito18.drawString(cn, cX + 8, cYr + 4, -1);
        Fonts.nunito12.drawString(modules.size() + " modules", cX + 8, cYr + 18, new Color(120, 120, 130).getRGB());
        Gui.drawRect((int)(cX + 8), (int)(cYr + 28), (int)(cX + cW - 8), (int)(cYr + 29), new Color(30, 30, 36).getRGB());

        // Search bar
        float searchY = cYr + 34;
        float searchH = 16;
        Gui.drawRect((int)(cX + 8), (int)searchY, (int)(cX + cW - 8), (int)(searchY + searchH), new Color(22, 22, 28).getRGB());
        Gui.drawRect((int)(cX + 8), (int)searchY, (int)(cX + cW - 8), (int)(searchY + 1), new Color(accent).getRGB());

        String displayText = searchText.isEmpty() && !searchFocused ? "Search modules..." : searchText;
        int searchColor = searchText.isEmpty() && !searchFocused ? new Color(80, 80, 90).getRGB() : new Color(180, 180, 190).getRGB();
        Fonts.nunito12.drawString(displayText, cX + 12, searchY + searchH / 2f - Fonts.nunito12.getHeight() / 2f, searchColor);

        // Cursor
        if (searchFocused && System.currentTimeMillis() % 1000 > 500) {
            float cursorX = cX + 12 + (searchText.isEmpty() ? 0 : Fonts.nunito12.getStringWidth(searchText));
            Gui.drawRect((int)cursorX, (int)(searchY + 3), (int)(cursorX + 1), (int)(searchY + searchH - 3), new Color(180, 180, 190).getRGB());
        }

        float lY = searchY + searchH + 6;
        float lH = cH - (lY - cYr) - 4;
        float lX = cX + 2;

        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int)(lX * sf), sr.getScaledHeight() * sf - (int)((lY + lH) * sf), (int)((cW - 4) * sf), (int)(lH * sf));

        double yOff = lY - scroll;
        double totalH = 0;
        int gray = new Color(130, 130, 140).getRGB();

        for (Module m : modules) {
            double mH = (expanded == m) ? expandedH(m) : 22;
            double mY = yOff;
            yOff += mH + 2;
            totalH += mH + 2;

            boolean hov = mx >= lX && mx <= lX + cW - 6 && my >= mY && my <= mY + 22;

            int bg = m.isToggled() ? new Color(26, 26, 30).getRGB() : new Color(21, 21, 25).getRGB();
            if (hov) bg = new Color(32, 32, 37).getRGB();
            RoundedUtil.drawRound(lX, (float)mY, cW - 6, (float)mH, 3, new Color(bg, true));

            Fonts.nunito14.drawString(m.getName(), lX + 7, (float)(mY + 3), m.isToggled() ? -1 : gray);

            float tX = lX + cW - 30;
            float tY = (float)(mY + 5);
            RoundedUtil.drawRound(tX, tY, 16, 10, 5, m.isToggled() ? new Color(accent) : new Color(42, 42, 48));
            float kX = m.isToggled() ? tX + 7 : tX + 1;
            RoundedUtil.drawRound(kX, tY + 1, 8, 8, 4, new Color(210, 210, 215));

            if (expanded == m) {
                float pY = (float)(mY + 22);
                for (Property<?> p : m.getProperties()) {
                    if (!p.isAvailable()) continue;
                    drawProp(p, lX + 10, pY, cW - 24, mx, my, accent);
                    pY += 13;
                }
            }
        }
        totalH = Math.max(0, totalH - lH);
        maxScroll = (float) totalH;
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (maxScroll > 0) {
            float sbH = lH * (lH / (lH + maxScroll));
            float sbY = lY + (scroll / maxScroll) * (lH - sbH);
            RoundedUtil.drawRound(lX + cW - 12, sbY, 2, sbH, 1, new Color(55, 55, 60));
        }

        super.drawScreen(mx, my, pt);
    }

    private float expandedH(Module m) {
        float h = 24;
        for (Property<?> p : m.getProperties()) if (p.isAvailable()) h += 13;
        return h;
    }

    private void drawProp(Property<?> p, float x, float y, float w, int mx, int my, int acc) {
        int g = new Color(140, 140, 150).getRGB();
        if (p instanceof BooleanProperty bp) {
            Fonts.nunito12.drawString(p.getName(), x, y + 1, g);
            float cbX = x + w - 12;
            RoundedUtil.drawRound(cbX, y, 10, 10, 2, bp.getProperty() ? new Color(acc) : new Color(38, 38, 43));
            if (bp.getProperty()) Fonts.rubik18Bold.drawString("v", cbX + 1, y - 1, -1);
        } else if (p instanceof ModeProperty mp) {
            Fonts.nunito12.drawString(p.getName(), x, y + 1, g);
            String mode = mp.getProperty();
            Fonts.nunito12.drawString(mode, x + w - Fonts.nunito12.getStringWidth(mode) - 2, y + 1, acc);
        } else if (p instanceof NumberProperty np) {
            Fonts.nunito12.drawString(p.getName() + ": " + String.format("%.1f", np.getProperty()), x, y + 1, g);
            float sX = x + w - 52;
            float sW = 48;
            Gui.drawRect((int)sX, (int)(y+7), (int)(sX+sW), (int)(y+9), new Color(38, 38, 43).getRGB());
            float fill = (np.getProperty() - np.getMin()) / (np.getMax() - np.getMin()) * sW;
            Gui.drawRect((int)sX, (int)(y+7), (int)(sX+fill), (int)(y+9), acc);
        } else if (p instanceof ColorProperty cp) {
            Fonts.nunito12.drawString(p.getName(), x, y + 1, g);
            RoundedUtil.drawRound(x + w - 14, y, 12, 12, 2, cp.getProperty());
        }
    }

    private String getIcon(ModuleCategory c) {
        switch (c) {
            case COMBAT: return "b"; case MOVEMENT: return "d"; case PLAYER: return "e";
            case VISUAL: return "f"; case WORLD: return "g"; case MISC: return "c";
            case GHOST: return "h"; default: return "a";
        }
    }

    @Override
    public void handleMouseInput() {
        try { super.handleMouseInput(); } catch (IOException ignored) {}
        int s = Mouse.getEventDWheel();
        if (s != 0) {
            targetScroll -= s / 3f;
            targetScroll = Math.max(0, Math.min(targetScroll, maxScroll));
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int mb) throws IOException {
        if (System.currentTimeMillis() - lastClick < 150) return;
        lastClick = System.currentTimeMillis();

        float cX = px + SBW + 3;
        float cYr = py + 3;
        float cW = PW - SBW - 6;
        float searchY = cYr + 34;
        float searchH = 16;

        // Search bar click
        if (mx >= cX + 8 && mx <= cX + cW - 8 && my >= searchY && my <= searchY + searchH) {
            searchFocused = true;
            return;
        }
        searchFocused = false;

        // Category clicks
        ModuleCategory[] cats = ModuleCategory.values();
        float cY = py + HEADER_H + 10;
        for (int i = 0; i < cats.length; i++) {
            float bx = px + 4, bw = SBW - 8, bh = 22, by = cY + i * (bh + 2);
            if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                cat = cats[i]; expanded = null; reload(); return;
            }
        }

        // Module clicks
        float lY = searchY + searchH + 6;
        float lX = cX + 2;
        double yOff = lY - scroll;

        for (Module m : modules) {
            double mH = (expanded == m) ? expandedH(m) : 22;
            double mY = yOff;
            yOff += mH + 2;
            if (mx < lX || mx > lX + cW - 6 || my < mY || my > mY + mH) continue;

            if (my <= mY + 22) {
                float tX = lX + cW - 30;
                if (mx >= tX - 4) { m.toggle(); return; }
                if (mb == 1) { expanded = (expanded == m) ? null : m; return; }
            } else if (expanded == m) {
                float pY = (float)(mY + 22);
                for (Property<?> p : m.getProperties()) {
                    if (!p.isAvailable()) continue;
                    if (my >= pY && my <= pY + 13) {
                        if (p instanceof BooleanProperty bp) bp.setProperty(!bp.getProperty());
                        else if (p instanceof ModeProperty mp) {
                            List<String> modes = mp.getModes();
                            int idx = modes.indexOf(mp.getProperty());
                            if (idx >= 0) mp.setProperty(modes.get((idx + 1) % modes.size()));
                        }
                        return;
                    }
                    pY += 13;
                }
            }
        }
        super.mouseClicked(mx, my, mb);
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        if (k == 1) {
            if (searchFocused) { searchFocused = false; return; }
            closing = true; return;
        }

        if (searchFocused) {
            if (k == 28) { // Enter
                searchFocused = false;
                return;
            }
            if (k == 14 && !searchText.isEmpty()) { // Backspace
                searchText = searchText.substring(0, searchText.length() - 1);
                filterModules(); scroll = targetScroll = 0;
                return;
            }
            if (ChatAllowedCharacters.isAllowedCharacter(c) && searchText.length() < 30) {
                searchText += c;
                filterModules(); scroll = targetScroll = 0;
                return;
            }
        }

        super.keyTyped(c, k);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
