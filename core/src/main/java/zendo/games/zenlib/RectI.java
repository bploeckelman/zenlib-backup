package zendo.games.zenlib;

public class RectI {
    public int x;
    public int y;
    public int w;
    public int h;

    public RectI() {}

    private RectI(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public static RectI at(int x, int y, int w, int h) {
        return new RectI(x, y, w, h);
    }

    public boolean overlaps(RectI other) {
        return x < other.x + other.w
            && other.x < x + w
            && y < other.y + other.h
            && other.y < y + h;
    }

    public int left()   { return x; }
    public int right()  { return x + w; }
    public int top()    { return y; }
    public int bottom() { return y + h; }

}
