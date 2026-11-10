package team.gothaj.client.api.widget;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.apache.commons.math3.analysis.function.Min;

@Getter
public class Resolution {

    private static Resolution instance;

    private float width;
    private float height;

    private float scaleFactor = 1.0f;

    private Resolution() {
        update();
    }

    public static Resolution get() {
        if (instance == null) {
            instance = new Resolution();
        }
        instance.update();
        return instance;
    }

    public void update() {
        this.width = Minecraft.getMinecraft().displayWidth;
        this.height = Minecraft.getMinecraft().displayHeight;
        this.scaleFactor = 1;
        boolean flag = Minecraft.getMinecraft().isUnicode();
        int i = Minecraft.getMinecraft().gameSettings.guiScale;

        if (i == 0)
        {
            i = 1000;
        }

        while (this.scaleFactor < i && this.width / (this.scaleFactor + 1) >= 320 && this.height / (this.scaleFactor + 1) >= 240)
        {
            ++this.scaleFactor;
        }

        if (flag && this.scaleFactor % 2 != 0 && this.scaleFactor != 1)
        {
            --this.scaleFactor;
        }
    }
}