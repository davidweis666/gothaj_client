package team.gothaj.client.impl.module.visual;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.*;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.event.input.InputEvent;
import team.gothaj.client.impl.event.render.FullScreenEvent;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.event.render.ResizeScreenEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.gui.widget.WidgetScreen;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;
import team.gothaj.eventbus.Priority;

import java.awt.*;

public class Hud extends Module {


    public static Hud module;

    public static final ListProperty widgets = new ListProperty("Widgets", "Watermark", "Scoreboard", "Arraylist", "Active Potions", "Armor Status", "Session Info", "TabGUI", "Inventory Display");

    public static final ModeProperty wmMode = new ModeProperty("Watermark Mode", "Modern", "Modern", "Best", "Text", "Other").setVisible(() -> widgets.getOptionState("Watermark"));

    public static final ListProperty wmOptText = new ListProperty("Watermark Options", "Custom Font", "Sync Color", "Horizontal Color").setVisible(() -> widgets.getOptionState("Watermark") && wmMode.is("Text"));

    public static final ListProperty wmOptOther = new ListProperty("Watermark Options", "Custom Font", "Server", "Ping", "Time", "Date").setVisible(() -> widgets.getOptionState("Watermark") && wmMode.is("Other"));

    public static final FontProperty wmFont = new FontProperty("Watermark Font", Fonts.vk17Medium).setVisible(() -> (wmOptText.getOptionState("Custom Font") || wmOptOther.getOptionState("Custom Font")) && !wmMode.is("Modern") && !wmMode.is("Best"));

    public static final InputProperty wmText = new InputProperty("Watermark Text", GothajClient.getInstance().getFancyName()).setVisible(() -> widgets.getOptionState("Watermark") && wmMode.is("Text"));

    public static final ModeProperty wmColorMode = new ModeProperty("Watermark Color Mode", "Custom", "Custom", "Fade", "Dark Fade", "Rainbow", "Astolfo", "Loading", "Team").setVisible(() -> wmOptText.isAvailable() && !wmOptText.getOptionState("Sync Color"));

    public static final NumberProperty wmRainbowSpeed = new NumberProperty("Watermark Rainbow Speed", 0.2f, 0.1f, 0.5f, 0.1f).setVisible(() -> wmColorMode.isAvailable() && wmColorMode.is("Rainbow"));

    public static final ColorProperty wmPrimaryColor = new ColorProperty("Watermark Primary Color", Color.magenta).setVisible(() -> wmColorMode.isAvailable() && !(wmColorMode.is("Rainbow") || wmColorMode.is("Team") || wmColorMode.is("Astolfo")));

    public static final ColorProperty wmSecondaryColor = new ColorProperty("Watermark Secondary Color", Color.pink).setVisible(() -> wmColorMode.isAvailable() && wmColorMode.is("Fade")  || wmColorMode.is("Loading"));

    public static final ModeProperty wmBestColorMode = new ModeProperty("Best Color", "Rainbow", "Rainbow", "HUD Color").setVisible(() -> widgets.getOptionState("Watermark") && wmMode.is("Best"));

    public static final ListProperty arraylistOpt = new ListProperty("Arraylist Options", "Background", "Outline", "Horizontal Color", "Custom Font", "Shaders", "Split Name", "Colored Suffix", "Rounded").setVisible(() -> widgets.getOptionState("Arraylist"));

    public static final ListProperty tabGuiOpt = new ListProperty("TabGUI Categories", "Combat", "Misc", "Movement", "Player", "Visual", "World", "Client").setVisible(() -> widgets.getOptionState("TabGUI"));

    public static final ListProperty scoreboardOpt = new ListProperty("Scoreboard Options", "Custom Font", "Rounded", "Shaders").setVisible(() -> widgets.getOptionState("Scoreboard"));

    public static final FontProperty cFont = new FontProperty("Arraylist Font", Fonts.vk17Medium).setVisible(() -> arraylistOpt.getOptionState("Custom Font"));

    public static final ModeProperty caseOpt = new ModeProperty("Case Options", "Normal", "Normal", "Upper", "Lower").setVisible(() -> widgets.getOptionState("Arraylist"));

    public static final ModeProperty prefixStyle = new ModeProperty("Prefix Style", "None", "None", "Dash", "Wave", "At", "Bracket", "Squared", "Curved", "Arrow").setVisible(() -> widgets.getOptionState("Arraylist"));

    public static final BooleanProperty suffixSpace = new BooleanProperty("Suffix Space", true).setVisible(() -> widgets.getOptionState("Arraylist"));

    public static final ListProperty lineOpt = new ListProperty("Line Options", "Top", "Bottom", "Left", "Right").setVisible(() -> widgets.getOptionState("Arraylist") && arraylistOpt.getOptionState("Outline"));

    public static final NumberProperty xOffset = new NumberProperty("X Offset", 0.0f, -1000.0f, 1000.0f, 1.0f).setVisible(() -> widgets.getOptionState("Arraylist"));

    public static final NumberProperty yOffset = new NumberProperty("Y Offset", 0.0f, -500.0f, 500.0f, 1.0f).setVisible(() -> widgets.getOptionState("Arraylist"));

    public static final NumberProperty backgroundAlpha = new NumberProperty("Background Opacity", 100, 1, 255, 1).setVisible(() -> arraylistOpt.getOptionState("Background"));

    public static final NumberProperty moduleSpacing = new NumberProperty("Module Spacing", 3.0f, 1.0f, 5.0f, 0.1f).setVisible(() -> widgets.getOptionState("Arraylist"));

     public static final ModeProperty colorMode = new ModeProperty("Arraylist Color Mode", "Custom", "Custom", "Fade", "Dark Fade", "Rainbow", "Astolfo", "Loading", "Team");

     public static final BooleanProperty syncWave = new BooleanProperty("Sync Horizontal Wave", false).setVisible(() -> widgets.getOptionState("Arraylist") && arraylistOpt.getOptionState("Horizontal Color"));

     public static final ModeProperty colorDirection = new ModeProperty("Arraylist Color Direction", "Up", "Up", "Down").setVisible(() -> !colorMode.is("Custom"));

     public static final NumberProperty rainbowSpeed = new NumberProperty("Arraylist Rainbow Speed", 0.2f, 0.1f, 0.5f, 0.1f).setVisible(() -> colorMode.is("Rainbow"));

     public static final ColorProperty primaryColor = new ColorProperty("Arraylist Primary Color", Color.magenta).setVisible(() -> !(colorMode.is("Rainbow") || colorMode.is("Team") || colorMode.is("Astolfo")));

     public static final ColorProperty secondaryColor = new ColorProperty("Arraylist Secondary Color", Color.pink).setVisible(() -> colorMode.is("Fade")  || colorMode.is("Loading"));


    public Hud() {
        super("Hud", "Watermark, Arraylist etc...", ModuleCategory.VISUAL);
        registerProperties(widgets, wmMode, wmOptText, wmOptOther, wmFont, wmText, wmColorMode, wmPrimaryColor, wmSecondaryColor, wmBestColorMode, arraylistOpt, tabGuiOpt, scoreboardOpt, cFont, caseOpt, prefixStyle, suffixSpace, lineOpt, xOffset, yOffset, backgroundAlpha, moduleSpacing, colorMode, syncWave, colorDirection, rainbowSpeed, primaryColor, secondaryColor);
    }


    @Listen(priority = Priority.LOW)
    public void onRender(RenderOverlayEvent event) {
        if (mc.currentScreen instanceof WidgetScreen) return;
        GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
            if (widgets.getOptionState(w.getLabel()) && w.getType() == Widget.Type.HUD) {
                w.onRender(event.getScaledResolution());
            }
        });
    }

    @Listen(priority = Priority.LOW)
    public void onShader(ShaderOverlayEvent event) {
  
        GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
            if (widgets.getOptionState(w.getLabel()) && w.getType() == Widget.Type.HUD) {
                w.onShader(event);
            }
        });
    }

    @Listen
    public void onResize(ResizeScreenEvent event) {
        GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
            if (widgets.getOptionState(w.getLabel()) && w.getType() == Widget.Type.HUD) {
                w.onResize(event.getSr());
            }
        });
    }


    @Listen
    public void onFullScreen(FullScreenEvent event) {
        GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
            if (widgets.getOptionState(w.getLabel()) && w.getType() == Widget.Type.HUD) {
                w.onFullScreen(event.getWidth(), event.getHeight());
            }
        });
    }


    @Listen
    public void onInput(InputEvent event) {
        GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
            if (widgets.getOptionState("TabGUI")) {
                w.onInput(event);
            }
        });
    }
    
    

    @Override
    public void onEnable() {
        module = this;
        for (String cat : tabGuiOpt.getOptions()) {
            tabGuiOpt.setOptionState(cat, true);
        }
        super.onEnable();
    }


    @Override
    public void onDisable() {
        super.onDisable();
    }


    public static int getColor(int i) {

        final long ms = (long) (4.0 * 1000.0f);

        long currentMillis = System.currentTimeMillis() * 2;

        final double yBase = (i * 4) + 5;

        final double y = colorDirection.is("Up") ? yBase : -yBase;

        float cOffset = -1;

        switch (colorMode.getProperty()) {
            case "Rainbow": {
                return ColorUtil.rainbow((float) y, rainbowSpeed.getProperty()).getRGB();

            }
            case "Team": {
                cOffset = (float) ((currentMillis + (y * 100)) % ms / (ms / 2.0F));

                Color c = new Color(ColorUtil.getTeamColor(mc.thePlayer), true);

                Color darkerPrimary = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);

                for (int i1 = 0; i1 < 3; i1++) {
                    darkerPrimary = darkerPrimary.darker();
                }

                return ColorUtil.fadeBetween(ColorUtil.getTeamColor(mc.thePlayer), darkerPrimary.getRGB(), cOffset);
            }
            case "Fade": {
                cOffset = (float) ((currentMillis + (y * 100)) % ms / (ms / 2.0F));
                return ColorUtil.fadeBetween(primaryColor.getProperty().getRGB(), secondaryColor.getProperty().getRGB(), cOffset);
            }
            case "Dark Fade": {
                cOffset = (float) ((currentMillis + (y * 100)) % ms / (ms / 2.0F));

                Color darkerPrimary = primaryColor.getProperty();

                for (int i1 = 0; i1 < 3; i1++) {
                    darkerPrimary = darkerPrimary.darker();
                }

                return ColorUtil.fadeBetween(primaryColor.getProperty().getRGB(), darkerPrimary.getRGB(), cOffset);
            }

            case "Astolfo": {
                return ColorUtil.astolfoColors((int) -y, 420 + GothajClient.getInstance().getModuleManager().size() * 2);

            }

            case "Loading": {
                cOffset = (float) ((currentMillis + (i * 100)) % ms / (ms / 1.4F));
                return ColorUtil.fadeBetween(primaryColor.getProperty().getRGB(), secondaryColor.getProperty().getRGB(), cOffset);
            }

            case "Custom": {
                return primaryColor.getProperty().getRGB();
            }

        }
        return Color.white.getRGB();
    }

    public static int getColorWatermark(int i) {

        final long ms = (long) (4.0 * 1000.0f);

        long currentMillis = System.currentTimeMillis() * 2;

        final double y = (i * 4) + 5;

        float cOffset = -1;

        switch (Hud.wmColorMode.getProperty()) {
            case "Rainbow": {
                return ColorUtil.rainbow((float) y, Hud.wmRainbowSpeed.getProperty()).getRGB();

            }
            case "Team": {
                cOffset = (float) ((currentMillis + (y * 100)) % ms / (ms / 2.0F));

                Color c = new Color(ColorUtil.getTeamColor(mc.thePlayer), true);

                Color darkerPrimary = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);

                for (int i1 = 0; i1 < 3; i1++) {
                    darkerPrimary = darkerPrimary.darker();
                }

                return ColorUtil.fadeBetween(ColorUtil.getTeamColor(mc.thePlayer), darkerPrimary.getRGB(), cOffset);
            }
            case "Fade": {
                cOffset = (float) ((currentMillis + (y * 100)) % ms / (ms / 2.0F));
                return ColorUtil.fadeBetween(Hud.wmPrimaryColor.getProperty().getRGB(), Hud.wmSecondaryColor.getProperty().getRGB(), cOffset);
            }
            case "Dark Fade": {
                cOffset = (float) ((currentMillis + (y * 100)) % ms / (ms / 2.0F));

                Color darkerPrimary = Hud.wmPrimaryColor.getProperty();

                for (int i1 = 0; i1 < 3; i1++) {
                    darkerPrimary = darkerPrimary.darker();
                }

                return ColorUtil.fadeBetween(Hud.wmPrimaryColor.getProperty().getRGB(), darkerPrimary.getRGB(), cOffset);
            }

            case "Astolfo": {
                return ColorUtil.astolfoColors((int) -y, 420 + GothajClient.getInstance().getModuleManager().size() * 2);

            }

            case "Loading": {
                cOffset = (float) ((currentMillis + (i * 100)) % ms / (ms / 1.4F));
                return ColorUtil.fadeBetween(Hud.wmPrimaryColor.getProperty().getRGB(), Hud.secondaryColor.getProperty().getRGB(), cOffset);
            }

            case "Custom": {
                return Hud.wmPrimaryColor.getProperty().getRGB();
            }

        }
        return Color.white.getRGB();
    }
}
