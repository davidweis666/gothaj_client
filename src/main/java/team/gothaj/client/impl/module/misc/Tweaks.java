package team.gothaj.client.impl.module.misc;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.eventbus.Listen;
import static org.lwjgl.glfw.GLFW.*;

/**
 * @author vajzii
 * @since 2025
 */
public class Tweaks extends Module {


    private boolean prevBorderlessToggle = false;

    private final long window = glfwGetCurrentContext();

    public static final ListProperty tweaks = new ListProperty("Tweaks", "Block Break Delay", "No Jump Delay" , "No Click Delay", "Borderless", "Raw Input");

    public Tweaks() {
        super("Tweaks", "Enhances your game in many ways", ModuleCategory.MISC);
        registerProperties(tweaks);
    }

    @Listen
    public void onTick(TickEvent event) {

        if (tweaks.getOptionState("No Click Delay")) {
            mc.leftClickCounter = 0;
        }

        if (tweaks.getOptionState("No Jump Delay")) {
            mc.thePlayer.jumpTicks = 0;
        }

        if (tweaks.getOptionState("Block Break Delay")) {
            mc.playerController.blockHitDelay = 0;
        }

        if (tweaks.getOptionState("Borderless")) {
            long window = glfwGetCurrentContext();

            if (window == 0L) {
                Logger.clientChat("Borderless requires GLFW to be initialized");
                return;
            }

            boolean pressed = mc.gameSettings.keyBindFullscreen.isPressed();

            if (pressed && !prevBorderlessToggle) {
                try {
                    boolean isFullscreen = glfwGetWindowMonitor(window) != 0L;
                    boolean isUndecorated = glfwGetWindowAttrib(window, GLFW_DECORATED) == GLFW_FALSE;

                    if (!isFullscreen && isUndecorated) {
                        glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_TRUE);
                    } else {
                        long monitor = GLFW.glfwGetPrimaryMonitor();

                        if (monitor == 0L) {
                            return;
                        }

                        GLFWVidMode videoMode = glfwGetVideoMode(monitor);

                        if (videoMode == null) {
                            return;
                        }

                        int x = 0;
                        int y = 0;
                        int width = videoMode.width();
                        int height = videoMode.height();

                        glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_FALSE);
                        GLFW.glfwSetWindowMonitor(window, 0, x, y, width, height, GLFW.GLFW_DONT_CARE);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
            prevBorderlessToggle = pressed;
        }

    }

    @Override
    public void onEnable() {
        Logger.clientChat("Enabling Raw Input & Borderless requires module re-toggle");
        if (tweaks.getOptionState("Raw Input")) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            if (glfwRawMouseMotionSupported()) {
                glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
            }
        }

        if (tweaks.getOptionState("Borderless")) {

            long monitor = GLFW.glfwGetPrimaryMonitor();

            GLFWVidMode videoMode = glfwGetVideoMode(monitor);
            int x = 0;
            int y = 0;
            int width = videoMode.width();
            int height = videoMode.height();

            if (monitor == 0L) return;

            glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(window, 0, x, y, width, height, GLFW.GLFW_DONT_CARE);


        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (window == 0) return;
        glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_FALSE);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        glfwSetWindowAttrib(window, GLFW_DECORATED, GLFW_TRUE);
        super.onDisable();
    }
}
