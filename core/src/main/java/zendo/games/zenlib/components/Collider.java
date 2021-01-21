package zendo.games.zenlib.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import zendo.games.zenlib.*;

import java.util.ArrayList;
import java.util.List;

public class Collider extends Component {

    public enum Shape { none, rect, grid }

    public static class Grid {
        public int columns;
        public int rows;
        public int tileSize;
        public List<Boolean> cells;
    }

    public int mask = 0;

    private Shape shape = Shape.none;
    private RectI rect;
    private Grid grid;

    public Collider() {
        visible = false;
        active = false;
    }

    @Override
    public <T extends Component> void copyFrom(T other) {
        super.copyFrom(other);
        if (other instanceof Collider) {
            var collider = (Collider) other;
            this.mask = collider.mask;
            this.shape = collider.shape;
            this.rect = collider.rect;
            this.grid = collider.grid;
        }
    }

    public static Collider makeRect(RectI rect) {
        Collider collider = new Collider();
        collider.shape = Shape.rect;
        collider.rect = rect;
        return collider;
    }

    public static Collider makeGrid(int tileSize, int columns, int rows) {
        Collider collider = new Collider();
        collider.shape = Shape.grid;
        collider.grid.tileSize = tileSize;
        collider.grid.columns = columns;
        collider.grid.rows = rows;
        collider.grid.cells = new ArrayList<>(columns * rows);
        return collider;
    }

    public Shape shape() {
        return shape;
    }

    public RectI getRect() {
        assert (shape == Shape.rect) : "Collider is not a Rectangle";
        return rect;
    }

    public void setRect(RectI rect) {
        assert (shape == Shape.rect) : "Collider is not a Rectangle";
        this.rect = rect;
    }

    public boolean getCell(int x, int y) {
        assert (shape == Shape.grid) : "Collider is not a Grid";
        assert (x >= 0 && y >= 0 && x < grid.columns && y < grid.rows) : "Cell is out of bounds";
        return grid.cells.get(x + y * grid.columns);
    }

    public void setCell(int x, int y, boolean value) {
        assert (shape == Shape.grid) : "Collider is not a Grid";
        assert (x >= 0 && y >= 0 && x < grid.columns && y < grid.rows) : "Cell is out of bounds";
        grid.cells.set(x + y * grid.columns, value);
    }

    public void setCells(int x, int y, int w, int h, boolean value) {
        assert (shape == Shape.grid) : "Collider is not a Grid";
        assert (x >= 0 && y >= 0 && x < grid.columns && x + w < grid.columns && y < grid.rows && y + h < grid.rows) : "Cell is out of bounds";
        for (int ix = x; ix < x + w; ix++) {
            for (int iy = y; iy < y + h; iy++) {
                setCell(ix, iy, value);
            }
        }
    }

    public boolean check(int mask) {
        return check(mask, Point.zero());
    }

    public boolean check(int mask, Point offset) {
        var other = world().first(Collider.class);
        while (other != null) {
            if (other != this
             && (other.mask & mask) == mask
             && overlaps(other, offset)) {
                return true;
            }

            other = (Collider) other.next();
        }

        return false;
    }

    public boolean overlaps(Collider other) {
        return overlaps(other, Point.zero());
    }

    public boolean overlaps(Collider other, Point offset) {
        if (shape == Shape.rect) {
            if (other.shape == Shape.rect) {
                return rectToRect(this, other, offset);
            }
            else if (other.shape == Shape.grid) {
                return rectToGrid(this, other, offset);
            }
        }
        else if (shape == Shape.grid) {
            if (other.shape == Shape.rect) {
                return rectToGrid(other, this, offset);
            }
            else if (other.shape == Shape.grid) {
                assert(false) : "Grid->Grid overlap checks not supported";
            }
        }

        return false;
    }

    @Override
    public void render(ShapeRenderer shapes) {
        final Color color = Color.RED.cpy();
        color.a = 0.75f;

        {
            shapes.setColor(color);
            if (shape == Shape.rect) {
                float x1 = rect.x + entity().position.x;
                float y1 = rect.y + entity().position.y;
                shapes.rect(x1, y1, rect.w, rect.h);
            }
            else if (shape == Shape.grid) {
                for (int x = 0; x < grid.columns; x++) {
                    for (int y = 0; y < grid.rows; y++) {
                        if (!grid.cells.get(x + y * grid.columns)) continue;

                        RectI rect = RectI.at(
                                x * grid.tileSize + entity().position.x,
                                y * grid.tileSize + entity().position.y,
                                grid.tileSize, grid.tileSize);
                        float x1 = rect.x + entity().position.x;
                        float y1 = rect.y + entity().position.y;
                        shapes.rect(x1, y1, rect.w, rect.h);
                    }
                }

            }
            shapes.setColor(Color.WHITE);
        }
    }

    private static boolean rectToRect(Collider a, Collider b, Point offset) {
        RectI ar = new RectI();
        ar.x = a.rect.x + a.entity().position.x + offset.x;
        ar.y = a.rect.y + a.entity().position.y + offset.y;

        RectI br = new RectI();
        br.x = b.rect.x + b.entity().position.x + offset.x;
        br.y = b.rect.y + b.entity().position.y + offset.y;

        return ar.overlaps(br);
    }

    private static boolean rectToGrid(Collider a, Collider b, Point offset) {
        // get a relative rectangle to the grid
        RectI rect = new RectI();
        rect.x = a.rect.x + a.entity().position.x + offset.x - b.entity().position.x;
        rect.y = a.rect.y + a.entity().position.y + offset.y - b.entity().position.y;

        // get the cells the rectangle overlaps
        int left   = Calc.clampInt((int) Calc.floor  (rect.x        / (float) b.grid.tileSize), 0, b.grid.columns);
        int right  = Calc.clampInt((int) Calc.ceiling(rect.right()  / (float) b.grid.tileSize), 0, b.grid.columns);
        int top    = Calc.clampInt((int) Calc.floor  (rect.y        / (float) b.grid.tileSize), 0, b.grid.rows);
        int bottom = Calc.clampInt((int) Calc.ceiling(rect.bottom() / (float) b.grid.tileSize), 0, b.grid.rows);

        // check each cell
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                if (b.grid.cells.get(x + y * b.grid.columns)) {
                    return true;
                }
            }
        }

        // all cells were empty
        return false;
    }

}
