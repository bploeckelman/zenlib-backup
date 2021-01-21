package zendo.games.zenlib;

public class Point {
    public int x;
    public int y;
    private Point() {
        this(0, 0);
    }
    private Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Point zero() {
        return new Point();
    }

    public static Point at(int x, int y) {
        return new Point(x, y);
    }
}
