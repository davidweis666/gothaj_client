package team.gothaj.client.api.widget;


import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.impl.event.input.InputEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.impl.module.client.ShaderProcessing;
import team.gothaj.client.main.GothajClient;


@Getter
@Setter
public abstract class Widget {
    public float x, y, width, height;
    private String label;
    protected Hud hud;
    protected Type type;
    private boolean dragging;

    protected Minecraft mc = Minecraft.getMinecraft();

    protected Resolution sr = Resolution.get();

    public Widget(String label, float x, float y, float width, float height) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        hud = Hud.module;
        this.type = Type.HUD;

    }

    public Widget(String label, float x, float y, float width, float height, Type type) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        hud = Hud.module;
        this.type = type;

    }

    public ShaderProcessing getShaderProcessing() {
        return GothajClient.getInstance().getModuleManager().getAndCast(ShaderProcessing.class);
    }

    public void onShader(ShaderOverlayEvent event) {}

    public void onRender(ScaledResolution sr) {}

    public void onResize(ScaledResolution sr) {}

    public void onFullScreen(float w, float h) {}

    public void onInput(InputEvent event) {}

    public void onDrag() {
        dragging = true;
    }

    public JsonObject save() {
        JsonObject object = new JsonObject();

        object.addProperty("x", getX());
        object.addProperty("y", getY());

        return object;
    }

    public void load(JsonObject object) {
        if (object == null) return;


        if (object.has("x")) {
            setX(object.get("x").getAsFloat());
        }
        if (object.has("y")) {
            setY(object.get("y").getAsFloat());
        }
    }


    public enum Type {
        HUD, MODULE
    }

}
