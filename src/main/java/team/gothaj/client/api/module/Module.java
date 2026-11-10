package team.gothaj.client.api.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.EnumChatFormatting;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.Property;
import team.gothaj.client.api.property.properties.*;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.render.Translate;
import team.gothaj.client.impl.module.client.ToggleSounds;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.minecraft.util.MathHelper.clamp;

@Getter
public abstract class Module implements Utils {

    private final String name, description;

    private boolean toggled = false;

    private final Translate translate = new Translate(0.0f, 0.0f);

    private final ModuleCategory moduleCategory;

    @Setter
    private boolean hidden = false;

    @Setter
    private int keyCode;

    @Setter
    private boolean hold = false;

    @Getter
    private final List<Property<?>> properties = new ArrayList<>();

    private ToggleSounds toggleSounds;

    public Module(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.moduleCategory = category;
        this.keyCode = 0;
    }

    public Module(String name, String description, ModuleCategory category, int keyCode) {
        this.name = name;
        this.description = description;
        this.moduleCategory = category;
        this.keyCode = keyCode;
    }

    public void addProperty(Property<?> property) {
        properties.add(property);
    }

    public void registerProperties(Property<?>... properties) {
        this.properties.addAll(Arrays.asList(properties));
    }

    @SuppressWarnings("unchecked")
    public <P extends Property<?>> P getPropertyByName(String name) {
        return (P) properties.stream().filter(property -> property.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /*
        why is enabling handled like this 💀
        what the fuck
     */

    public void onEnable() {
        if (getCastedModule(ToggleSounds.class).isToggled()) {
            toggleSounds.playSound(true);
        }

        translate.animateSlide(true);

        GothajClient.getInstance().getEventBus().subscribe(this);

        if (mc.theWorld != null) {
            GothajClient.getInstance().getNotificationManager().add("Module", name + " was enabled", Notification.Type.INFO);
        }
    }

    public void onDisable() {
        if (getCastedModule(ToggleSounds.class).isToggled()) {
            toggleSounds.playSound(false);
        }

        translate.animateSlide(false);

        GothajClient.getInstance().getEventBus().unsubscribe(this);

        if (mc.theWorld != null) {
            GothajClient.getInstance().getNotificationManager().add("Module", name + " was disabled", Notification.Type.INFO);
        }
    }

    public void setToggled(boolean toggle) {
        if (toggle == this.toggled) return;
        this.toggled = toggle;

        toggleSounds = getCastedModule(ToggleSounds.class);

        try {
            if (isToggled()) {
                onEnable();
            } else {
                onDisable();
            }

        } catch (Exception e) {
            System.out.println("Error has occured in module " + name + (!toggled ? " (onDisable)" : " (onEnable)"));
            e.printStackTrace(System.err);
        }
    }

    public void toggle() {
        setToggled(!isToggled());
    }

    public String getDisplayName(boolean split) {
        if (getSuffix().isEmpty()) {
            return computeDisplayName(split);
        }
        return computeDisplayName(split) + getPrefix() + getSuffixStyle();
    }

    public final <T extends Module> T getCastedModule(Class<T> clazz) {
        return GothajClient.getInstance().getModuleManager().getAndCast(clazz);
    }

    public String getSuffix() {
        return "";
    }

    public String getSuffixStyle() {
        String suffixString = getSuffix();

        if (!suffixString.isEmpty()) {
            switch (Hud.prefixStyle.getProperty()) {
                case "Bracket" -> {
                    suffixString += ")";
                }
                case "Squared" -> {
                    suffixString += "]";
                }
                case "Curved" -> {
                    suffixString += "}";
                }
                case "Arrow" -> {
                    suffixString += ">";
                }
            }

            if (!Hud.arraylistOpt.getOptionState("Colored Suffix")) {
                return EnumChatFormatting.GRAY + suffixString;
            }
        }
        return suffixString;
    }

    private String getPrefix() {

        String prefixString = "";

        switch (Hud.prefixStyle.getProperty()) {
            case "None" -> {
                prefixString = " ";
            }
            case "Dash" -> {
                prefixString = " - ";
            }
            case "Wave" -> {
                prefixString = " ~ ";
            }
            case "Bracket" -> {
                prefixString = " (";
            }
            case "Squared" -> {
                prefixString = " [";
            }
            case "Curved" -> {
                prefixString = " {";
            }
            case "Arrow" -> {
                prefixString = " <";
            }
            case "At" -> {
                prefixString = " @ ";
            }
        }
        if (getSuffix().isEmpty()) {
            return "";
        }
        if (!Hud.suffixSpace.getProperty()) {
            prefixString = prefixString.replaceAll(" ", "");
        }
        if (!Hud.arraylistOpt.getOptionState("Colored Suffix")) {
            return EnumChatFormatting.GRAY + prefixString;
        }
        return prefixString;
    }

    public JsonObject save() {
        JsonObject object = new JsonObject();

        object.addProperty("toggled", isToggled());
        object.addProperty("key", getKeyCode());
        object.addProperty("hold", isHold());
        object.addProperty("hidden", isHidden());

        if (!getProperties().isEmpty()) {
            JsonObject props = new JsonObject();

            for (Property<?> property : getProperties()) {
                String key = property.getName();

                try {
                    switch (property) {
                        case NumberProperty num -> {
                            if (num.getType() == NumberProperty.Type.INT) {
                                Number v = num.getProperty();
                                props.addProperty(key, v != null ? v.intValue() : 0);
                            } else {
                                Number v = num.getProperty();
                                props.addProperty(key, v != null ? v.doubleValue() : 0.0);
                            }
                        }
                        case RangedNumberProperty num -> {
                            JsonObject rangeObj = new JsonObject();

                            if (num.getType() == RangedNumberProperty.Type.INT) {
                                rangeObj.addProperty("left", (int) num.getLeft());
                                rangeObj.addProperty("right", (int) num.getRight());
                            } else {
                                rangeObj.addProperty("left", num.getLeft());
                                rangeObj.addProperty("right", num.getRight());
                            }

                            props.add(key, rangeObj);
                        }
                        case InputProperty input -> {
                            String v = input.getProperty() != null ? input.getProperty() : "";
                            if (!input.getName().equalsIgnoreCase("Client ID"))
                                props.addProperty(key, v);
                        }
                        case ColorProperty colorProp -> {
                            Color c = colorProp.getProperty();
                            JsonObject cobj = new JsonObject();
                            cobj.addProperty("r", c.getRed());
                            cobj.addProperty("g", c.getGreen());
                            cobj.addProperty("b", c.getBlue());
                            cobj.addProperty("a", c.getAlpha());
                            props.add(key, cobj);
                        }
                        case ModeProperty mode -> {
                            Object v = mode.getProperty();
                            props.add(key, new JsonPrimitive(v != null ? String.valueOf(v) : ""));
                        }
                        case FontProperty mode -> {
                            Object v = mode.getFontName();
                            props.add(key, new JsonPrimitive(v != null ? String.valueOf(v) : ""));
                        }
                        case ListProperty listProp -> {
                            JsonArray arr = new JsonArray();
                            List<?> list = listProp.getProperty();
                            if (list != null) {
                                for (Object v : list) {
                                    arr.add(new JsonPrimitive(v != null ? String.valueOf(v) : ""));
                                }
                            }
                            props.add(key, arr);
                        }
                        case BooleanProperty boolProp -> {
                            Boolean v = (Boolean) boolProp.getProperty();
                            props.addProperty(key, Boolean.TRUE.equals(v));
                        }
                        case PatternProperty patternProp -> {
                            String serialized = patternProp.serializePattern();
                            props.addProperty(key, serialized);
                        }
                        default -> {
                        }
                    }

                } catch (Exception ex) {
                    System.out.println("Failed to serialize property " + key + " in module " + name + ": " + ex.getMessage());
                }
            }

            object.add("Properties", props);
        }

        return object;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public void load(JsonObject object, boolean keys) {
        if (object == null) return;

        if (object.has("toggled")) setToggled(object.get("toggled").getAsBoolean());
        if (keys) {
            if (object.has("key")) setKeyCode(object.get("key").getAsInt());
            if (object.has("hold")) setHold(object.get("hold").getAsBoolean());
        }
        if (object.has("hidden")) setHidden(object.get("hidden").getAsBoolean());


        if (object.has("Properties") && !getProperties().isEmpty()) {
            JsonObject props = object.getAsJsonObject("Properties");

            for (Property<?> property : getProperties()) {
                String key = property.getName();
                if (!props.has(key)) continue;

                JsonElement el = props.get(key);
                try {
                    if (property instanceof NumberProperty num) {
                        if (num.getType() == NumberProperty.Type.INT) {
                            int val;
                            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                                val = el.getAsInt();
                            } else {
                                String s = el.getAsString();
                                int parsed = 0;
                                try {
                                    parsed = Integer.parseInt(s);
                                } catch (Exception ignore) {
                                    try {
                                        parsed = (int) Long.parseLong(s, 16);
                                    } catch (Exception ignore2) {
                                    }
                                }
                                val = parsed;
                            }
                            num.setProperty(val);
                        } else {
                            double d = el.isJsonPrimitive() ? el.getAsDouble() : 0.0;
                            if (num.getType() == NumberProperty.Type.DOUBLE) {
                                num.setProperty((float) d);
                            } else {
                                num.setProperty((int) d);
                            }
                        }
                    } else if (property instanceof RangedNumberProperty num) {
                        if (el.isJsonObject()) {
                            JsonObject rangeObj = el.getAsJsonObject();

                            if (num.getType() == RangedNumberProperty.Type.INT) {
                                int leftVal = 0;
                                int rightVal = 0;

                                if (rangeObj.has("left")) {
                                    JsonElement leftEl = rangeObj.get("left");
                                    if (leftEl.isJsonPrimitive() && leftEl.getAsJsonPrimitive().isNumber()) {
                                        leftVal = leftEl.getAsInt();
                                    } else {
                                        String s = leftEl.getAsString();
                                        try {
                                            leftVal = Integer.parseInt(s);
                                        } catch (Exception ignore) {
                                            try {
                                                leftVal = (int) Long.parseLong(s, 16);
                                            } catch (Exception ignore2) {
                                            }
                                        }
                                    }
                                }

                                if (rangeObj.has("right")) {
                                    JsonElement rightEl = rangeObj.get("right");
                                    if (rightEl.isJsonPrimitive() && rightEl.getAsJsonPrimitive().isNumber()) {
                                        rightVal = rightEl.getAsInt();
                                    } else {
                                        String s = rightEl.getAsString();
                                        try {
                                            rightVal = Integer.parseInt(s);
                                        } catch (Exception ignore) {
                                            try {
                                                rightVal = (int) Long.parseLong(s, 16);
                                            } catch (Exception ignore2) {
                                            }
                                        }
                                    }
                                }

                                num.setLeft(leftVal);
                                num.setRight(rightVal);
                            } else {
                                double leftVal = rangeObj.has("left") && rangeObj.get("left").isJsonPrimitive()
                                        ? rangeObj.get("left").getAsDouble() : 0.0;
                                double rightVal = rangeObj.has("right") && rangeObj.get("right").isJsonPrimitive()
                                        ? rangeObj.get("right").getAsDouble() : 0.0;

                                num.setLeft((float) leftVal);
                                num.setRight((float) rightVal);
                            }
                        }
                    } else if (property instanceof ModeProperty mode) {
                        String s = el.getAsString();
                        Object current = mode.getProperty();
                        if (current instanceof Enum<?> en) {
                            Class<? extends Enum> ec = en.getDeclaringClass();
                            Enum<?> chosen = null;
                            for (Object c : ec.getEnumConstants()) {
                                Enum<?> e = (Enum<?>) c;
                                if (e.name().equalsIgnoreCase(s)) {
                                    chosen = e;
                                    break;
                                }
                            }
                            if (chosen != null) mode.setProperty(String.valueOf(chosen));
                        } else {
                            mode.setProperty(s);
                        }
                    } else if (property instanceof InputProperty input) {
                        String s = el.isJsonNull() ? "" : el.getAsString();
                        input.setProperty(s);
                    } else if (property instanceof ColorProperty colorProp) {
                        if (el.isJsonObject()) {
                            JsonObject cobj = el.getAsJsonObject();
                            int r = cobj.has("r") ? cobj.get("r").getAsInt() : 255;
                            int g = cobj.has("g") ? cobj.get("g").getAsInt() : 255;
                            int b = cobj.has("b") ? cobj.get("b").getAsInt() : 255;
                            int a = cobj.has("a") ? cobj.get("a").getAsInt() : 255;
                            Color c = new Color(
                                    clamp(r, 0, 255),
                                    clamp(g, 0, 255),
                                    clamp(b, 0, 255),
                                    clamp(a, 0, 255)
                            );
                            colorProp.setColor(c);
                            colorProp.setAlpha(c.getAlpha());
                        } else {
                            String s = el.getAsString();
                            try {
                                Color parsed = parseColor(s);
                                colorProp.setColor(parsed);
                                colorProp.setAlpha(parsed.getAlpha());
                            } catch (Exception ignore) {
                            }
                        }
                    } else if (property instanceof FontProperty mode) {
                        String s = el.getAsString();
                        mode.setPropertyString(s);
                    } else if (property instanceof ListProperty listProp) {
                        List current = listProp.getProperty();
                        Class<?> elementType = (current != null && !current.isEmpty()) ? current.get(0).getClass() : String.class;
                        List out = new ArrayList();
                        if (el.isJsonArray()) {
                            for (JsonElement e : el.getAsJsonArray()) {
                                String item = e.isJsonNull() ? "" : e.getAsString();
                                Object val = item;
                                if (Enum.class.isAssignableFrom(elementType)) {
                                    Class<? extends Enum<?>> ec = (Class<? extends Enum<?>>) elementType;
                                    Enum<?> picked = null;
                                    for (Enum<?> c : ec.getEnumConstants()) {
                                        if (c.name().equalsIgnoreCase(item)) {
                                            picked = c;
                                            break;
                                        }
                                    }
                                    if (picked != null) val = picked;
                                    else continue;
                                } else if (elementType == Integer.class) {
                                    try {
                                        val = Integer.parseInt(item);
                                    } catch (Exception ignore) {
                                    }
                                } else if (elementType == Double.class) {
                                    try {
                                        val = Double.parseDouble(item);
                                    } catch (Exception ignore) {
                                    }
                                } else if (elementType == Float.class) {
                                    try {
                                        val = Float.parseFloat(item);
                                    } catch (Exception ignore) {
                                    }
                                } else if (elementType == Boolean.class) {
                                    val = "true".equalsIgnoreCase(item) || "1".equals(item);
                                }
                                out.add(val);
                            }
                        } else {
                            String item = el.isJsonNull() ? "" : el.getAsString();
                            Object val = item;
                            if (Enum.class.isAssignableFrom(elementType)) {
                                Class<? extends Enum<?>> ec = (Class<? extends Enum<?>>) elementType;
                                for (Enum<?> c : ec.getEnumConstants()) {
                                    if (c.name().equalsIgnoreCase(item)) {
                                        val = c;
                                        break;
                                    }
                                }
                            } else if (elementType == Integer.class) {
                                try {
                                    val = Integer.parseInt(item);
                                } catch (Exception ignore) {
                                }
                            } else if (elementType == Double.class) {
                                try {
                                    val = Double.parseDouble(item);
                                } catch (Exception ignore) {
                                }
                            } else if (elementType == Float.class) {
                                try {
                                    val = Float.parseFloat(item);
                                } catch (Exception ignore) {
                                }
                            } else if (elementType == Boolean.class) {
                                val = "true".equalsIgnoreCase(item) || "1".equals(item);
                            }
                            out.add(val);
                        }
                        listProp.setProperty(out);
                    } else if (property instanceof BooleanProperty boolProp) {
                        if (el.isJsonPrimitive()) {
                            boolProp.setProperty(el.getAsBoolean());
                        }
                    } else if (property instanceof PatternProperty patternProp) {
                        String data = el.getAsString();
                        patternProp.deserializePattern(data);
                    }

                } catch (Exception ex) {
                    System.out.println("Failed to load property " + key + " for module " + name + ": " + ex.getMessage());
                }
            }
        }
    }

    private String computeDisplayName(boolean split) {
        String src = getName();
        if (!split) return src == null ? "" : src;

        if (src == null || src.isEmpty()) return "";
        src = src.trim();

        return src.replaceAll("(?<=[\\p{Ll}\\p{Nd}])(?=\\p{Lu})|(?<=\\p{Lu})(?=\\p{Lu}\\p{Ll})", " ");
    }

    private static Color parseColor(String s) {
        if (s == null || s.isEmpty()) return new Color(255, 255, 255, 255);
        s = s.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);

            long v = Long.parseLong(s, 16);
            if (s.length() == 6) {
                int r = (int) ((v >> 16) & 0xFF);
                int g = (int) ((v >> 8) & 0xFF);
                int b = (int) (v & 0xFF);
                return new Color(r, g, b, 255);
            } else if (s.length() == 8) {
                int a = (int) ((v >> 24) & 0xFF);
                int r = (int) ((v >> 16) & 0xFF);
                int g = (int) ((v >> 8) & 0xFF);
                int b = (int) (v & 0xFF);
                return new Color(r, g, b, a);
            }
        }

        String raw = s.replace("rgba(", "").replace(")", "");
        String[] parts = raw.split("[, ]+");
        if (parts.length >= 3) {
            int r = Integer.parseInt(parts[0]);
            int g = Integer.parseInt(parts[1]);
            int b = Integer.parseInt(parts[2]);
            int a = parts.length >= 4 ? Integer.parseInt(parts[3]) : 255;
            return new Color(clamp(r, 0, 255), clamp(g, 0, 255), clamp(b, 0, 255), clamp(a, 0, 255));
        }
        return new Color(255, 255, 255, 255);
    }
}
