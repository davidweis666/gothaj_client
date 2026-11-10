package team.gothaj.client.impl.gui.click.oldgothaj;

public class PositionUtils {
    private double x, y, width, height;
    private float scale;

    public PositionUtils(double x, double y, double width, double height, float scale) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scale = scale;
    }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }

    public double getX2() { return x + width; }
    public double getY2() { return y + height; }

    public boolean isInside(int mx, int my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }
}
