package team.gothaj.client.api.util.other;

import team.gothaj.client.api.util.Utils;

public class MouseUtil implements Utils {


    public static boolean isHovered(int mouseX, int mouseY, float x, float y, float x2, float y2){
        return mouseX >= x && mouseY >= y && mouseX <= x2 && mouseY <= y2;
    }
}
