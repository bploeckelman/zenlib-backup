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
import zendo.games.zenlib.components.Mover;
import zendo.games.zenlib.components.Player;
import zendo.games.zenlib.components.Tilemap;

public class Main extends ApplicationAdapter {

    SpriteBatch batch;
    ShapeRenderer shapes;

    FrameBuffer frameBuffer;
    Texture frameBufferTexture;
    TextureRegion frameBufferRegion;

    Matrix4 screenProjection;

    OrthographicCamera worldCamera;
    World world;

    Texture pixel;

    @Override
    public void create () {
        Content.load();

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();

        pixel = new Texture("pixel.png");
        pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Config.framebuffer_width, Config.framebuffer_height, false);
        frameBufferTexture = frameBuffer.getColorBufferTexture();
        frameBufferTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        frameBufferRegion = new TextureRegion(frameBufferTexture);
        frameBufferRegion.flip(false, true);

        screenProjection = new Matrix4().setToOrtho2D(0, 0, Config.window_width, Config.window_height);

        worldCamera = new OrthographicCamera();
        worldCamera.setToOrtho(false, Config.framebuffer_width, Config.framebuffer_height);
        worldCamera.update();

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

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            Debug.draw_entities = !Debug.draw_entities;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            Debug.draw_origin = !Debug.draw_origin;
        }

        float dt = Gdx.graphics.getDeltaTime();

        world.update(dt);

        var player = world.first(Player.class);
        // NOTE: this is a little silly because depending which way the player is moving ceiling/floor tracks quickly while the other doesn't
        var targetX = (player.get(Mover.class).speed.x > 0)
                ? Calc.ceiling(Calc.approach(worldCamera.position.x, player.entity().position.x, 100 * dt))
                : Calc.floor  (Calc.approach(worldCamera.position.x, player.entity().position.x, 100 * dt));
        var targetY = Calc.ceiling(Calc.approach(worldCamera.position.y, player.entity().position.y, 100 * dt));
        worldCamera.position.set(targetX, targetY, 0);
        worldCamera.update();
    }

    @Override
    public void render() {
        update();

        renderWorldIntoFramebuffer();
        renderFramebufferIntoWindow();
    }

    @Override
    public void dispose() {
        pixel.dispose();
        frameBufferTexture.dispose();
        frameBuffer.dispose();
        batch.dispose();
        Content.unload();
    }

    // ------------------------------------------------------------------------

    private void renderWorldIntoFramebuffer() {
        frameBuffer.begin();
        {
            Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 0f);
            Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

            batch.setProjectionMatrix(worldCamera.combined);
            batch.begin();
            {
                world.render(batch);
            }
            batch.end();

            shapes.setProjectionMatrix(worldCamera.combined);
            shapes.setAutoShapeType(true);
            shapes.begin();
            {
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
                if (Debug.draw_entities) {
                    shapes.setColor(Color.YELLOW);
                    var entity = world.firstEntity();
                    while (entity != null) {
                        shapes.point(entity.position.x, entity.position.y, 0);
                        entity = entity.next();
                    }
                    shapes.setColor(Color.WHITE);
                }

                // origin coord axis
                if (Debug.draw_origin) {
                    shapes.setColor(Color.BLUE);
                    shapes.rectLine(0, 0, 10, 0, 1);
                    shapes.setColor(Color.GREEN);
                    shapes.rectLine(0, 0, 0, 10, 1);
                    shapes.setColor(Color.RED);
                    shapes.circle(0, 0, 1);
                    shapes.setColor(Color.WHITE);
                }
            }
            shapes.end();
        }
        frameBuffer.end();
    }

    private void renderFramebufferIntoWindow() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(screenProjection);
        batch.begin();
        {
            // background
            batch.setColor(Color.SKY);
            batch.draw(pixel, 0, 0, Config.window_width, Config.window_height);
            batch.setColor(Color.WHITE);

            // composite scene
            batch.draw(frameBufferRegion, 0, 0, Config.window_width, Config.window_height);
        }
        batch.end();
    }

}