package zendo.games.zenlib;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Matrix4;
import zendo.games.zenlib.components.Collider;
import zendo.games.zenlib.components.Tilemap;

public class Main extends ApplicationAdapter {

    SpriteBatch batch;
    ShapeRenderer shapes;

    FrameBuffer frameBuffer;
    Texture frameBufferTexture;
    TextureRegion frameBufferRegion;
    Matrix4 frameBufferMatrix;

    OrthographicCamera camera;
    World world;

    Texture texture;

    @Override
    public void create () {
        Content.load();

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();

        texture = new Texture("pixel.png");
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Config.framebuffer_width, Config.framebuffer_height, false);
        frameBufferTexture = frameBuffer.getColorBufferTexture();
        frameBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        frameBufferRegion = new TextureRegion(frameBufferTexture);
        frameBufferRegion.flip(false, true);
        frameBufferMatrix = new Matrix4();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Config.window_width, Config.window_height);
        camera.update();

        world = new World();

        var layer = (TiledMapTileLayer) Content.tiledMap.getLayers().get("collision");
        int tileSize = 18;//layer.getTileWidth();
        int columns = layer.getWidth();
        int rows = layer.getHeight();
        var room = world.addEntity();
        var tilemap = room.add(new Tilemap(), Tilemap.class);
        tilemap.init(tileSize, columns, rows);
        var solids = room.add(Collider.makeGrid(tileSize, columns, rows), Collider.class);
        solids.mask = Mask.solid;
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                if (layer.getCell(x, y) != null) {
                    tilemap.setCell(x, y, layer.getCell(x, y).getTile().getTextureRegion());
                    solids.setCell(x, y, true);
                }
            }
        }

        Factory.player(world, Point.at(Config.framebuffer_width / 2, Config.framebuffer_height / 2));
    }

    public void update() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            Debug.draw_colliders = !Debug.draw_colliders;
        }

        float dt = Gdx.graphics.getDeltaTime();

        world.update(dt);
    }

    @Override
    public void render() {
        update();

        renderWorldIntoFramebuffer();
        renderFramebufferIntoWindow();
    }

    @Override
    public void dispose() {
        texture.dispose();
        frameBufferTexture.dispose();
        frameBuffer.dispose();
        batch.dispose();
        Content.unload();
    }

    // ------------------------------------------------------------------------

    private void renderWorldIntoFramebuffer() {
        frameBufferMatrix.setToOrtho2D(0, 0, Config.framebuffer_width, Config.framebuffer_height);

        frameBuffer.begin();
        {
            Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 0f);
            Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

            batch.setProjectionMatrix(frameBufferMatrix);
            batch.begin();
            {
                batch.setColor(Color.SLATE);
                batch.draw(texture, 0, 0, Config.framebuffer_width, Config.framebuffer_height);
                batch.setColor(Color.WHITE);

                world.render(batch);
            }
            batch.end();

            shapes.setProjectionMatrix(frameBufferMatrix);
            shapes.setAutoShapeType(true);
            shapes.begin();
            {
                // origin coord axis
                shapes.setColor(Color.BLUE);
                shapes.rectLine(0, 0, 10, 0, 1);
                shapes.setColor(Color.GREEN);
                shapes.rectLine(0, 0, 0, 10, 1);
                shapes.setColor(Color.RED);
                shapes.circle(0, 0, 1);
                shapes.setColor(Color.WHITE);

                // colliders
                if (Debug.draw_colliders) {
                    shapes.setColor(Color.RED);
                    var collider = world.first(Collider.class);
                    while (collider != null) {
                        collider.render(shapes);
                        collider = (Collider) collider.next();
                    }
                    shapes.setColor(Color.WHITE);
                }

                // entities
                shapes.setColor(Color.YELLOW);
                var entity = world.firstEntity();
                while (entity != null) {
                    shapes.point(entity.position.x, entity.position.y, 0);
                    entity = entity.next();
                }
                shapes.setColor(Color.WHITE);
            }
            shapes.end();
        }
        frameBuffer.end();
    }

    private void renderFramebufferIntoWindow() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        {
            batch.draw(frameBufferRegion, 0, 0, Config.window_width, Config.window_height);
        }
        batch.end();
    }

}