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
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Matrix4;
import zendo.games.zenlib.assets.Content;
import zendo.games.zenlib.components.Collider;
import zendo.games.zenlib.components.Mover;
import zendo.games.zenlib.components.Player;
import zendo.games.zenlib.components.Tilemap;
import zendo.games.zenlib.config.Config;
import zendo.games.zenlib.config.Debug;
import zendo.games.zenlib.ecs.Mask;
import zendo.games.zenlib.ecs.World;
import zendo.games.zenlib.utils.Calc;
import zendo.games.zenlib.utils.Point;
import zendo.games.zenlib.utils.RectI;

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

        // load tile map entity and parse Tiled map
        var room = world.addEntity();

        var collisionLayer = (TiledMapTileLayer) Content.tiledMap.getLayers().get("collision");
        var tileSize = collisionLayer.getTileWidth();
        var columns = collisionLayer.getWidth();
        var rows = collisionLayer.getHeight();

        var tilemap = room.add(new Tilemap(), Tilemap.class);
        tilemap.init(tileSize, columns, rows);

        var solids = room.add(Collider.makeGrid(tileSize, columns, rows), Collider.class);
        solids.mask = Mask.solid;

        var spawn = Point.at(0, 0);
        for (var layer : Content.tiledMap.getLayers()) {
            // parse tile layers
            if (layer instanceof TiledMapTileLayer) {
                var tileLayer = (TiledMapTileLayer) layer;
                for (int x = 0; x < columns; x++) {
                    for (int y = 0; y < rows; y++) {
                        var cell = tileLayer.getCell(x, y);
                        if (cell == null) continue;

                        var isCollision = "collision".equals(layer.getName());
                        var isBackground = "background".equals(layer.getName());

                        if (isCollision) {
                            solids.setCell(x, y, true);
                        }

                        if (isCollision || isBackground) {
                            tilemap.setCell(x, y, cell.getTile().getTextureRegion());
                        }
                    }
                }
            }
            // parse objects layer
            else if ("objects".equals(layer.getName())) {
                var objects = layer.getObjects().getByType(TiledMapTileMapObject.class);
                for (var object : objects) {
                    var type = object.getProperties().get("type");

                    // parse player spawn position
                    if ("spawn".equals(type)) {
                        spawn.x = (int) (object.getX() / collisionLayer.getTileWidth())  * tileSize;
                        spawn.y = (int) (object.getY() / collisionLayer.getTileHeight()) * tileSize;
                    }
                }
            }
        }

        var player = Factory.player(world, spawn);

        // spawn a blob to test aseprite loading
        Factory.blob(world, Point.at(spawn.x + 64, spawn.y));

        worldCamera.position.set(player.position.x, player.position.y, 0);
        worldCamera.update();
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

        // keep player in bounds
        var solids = world.first(Tilemap.class).entity().get(Collider.class).getGrid();
        var bounds = RectI.at(0, 0, solids.columns * solids.tileSize, solids.rows * solids.tileSize);

        var player = world.first(Player.class);
        player.entity().position.x = Calc.clampInt(player.entity().position.x, bounds.x, bounds.x + bounds.w);
        player.entity().position.y = Calc.clampInt(player.entity().position.y, bounds.y, bounds.y + bounds.h);

        // find camera targets to follow player
        // NOTE: this is a little silly because depending which way the player is moving ceiling/floor tracks quickly while the other doesn't
        var targetX = (player.get(Mover.class).speed.x > 0)
                ? Calc.ceiling(Calc.approach(worldCamera.position.x, player.entity().position.x, 400 * dt))
                : Calc.floor  (Calc.approach(worldCamera.position.x, player.entity().position.x, 400 * dt));
        var targetY = Calc.ceiling(Calc.approach(worldCamera.position.y, player.entity().position.y, 100 * dt));

        // keep camera in bounds
        var halfViewW = (int) worldCamera.viewportWidth / 2;
        var halfViewH = (int) worldCamera.viewportHeight / 2;
        targetX = Calc.clampInt((int) targetX, bounds.x + halfViewW, bounds.x + bounds.w - halfViewW);
        targetY = Calc.clampInt((int) targetY, bounds.y + halfViewH, bounds.y + bounds.h - halfViewH);

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