package zendo.games.zenlib;

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

public class TestGame implements Game {

    SpriteBatch batch;
    ShapeRenderer shapes;

    FrameBuffer frameBuffer;
    Texture frameBufferTexture;
    TextureRegion frameBufferRegion;

    Matrix4 screenProjection;

    OrthographicCamera worldCamera;
    World world;

    @Override
    public void init() {
        Content.load();

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();

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

        loadMap();

        var player = world.first(Player.class);
        worldCamera.position.set(player.entity().position.x, player.entity().position.y, 0);
        worldCamera.update();
    }

    private void loadMap() {
        // get tiled map parameters
        var collisionLayer = (TiledMapTileLayer) Content.tiledMap.getLayers().get("collision");
        var tileSize = collisionLayer.getTileWidth();
        var columns = collisionLayer.getWidth();
        var rows = collisionLayer.getHeight();

        // create a map entity
        var map = world.addEntity();

        // add a tilemap component for textures
        var tilemap = map.add(new Tilemap(), Tilemap.class);
        tilemap.init(tileSize, columns, rows);

        // add a collider component
        var solids = map.add(Collider.makeGrid(tileSize, columns, rows), Collider.class);
        solids.mask = Mask.solid;

        // parse the tiled map layers
        for (var layer : Content.tiledMap.getLayers()) {
            // parse tile layers
            if (layer instanceof TiledMapTileLayer) {
                var tileLayer = (TiledMapTileLayer) layer;

                for (int x = 0; x < columns; x++) {
                    for (int y = 0; y < rows; y++) {
                        // skip empty cells
                        var cell = tileLayer.getCell(x, y);
                        if (cell == null) continue;

                        // determine what type of layer this is
                        var isCollision = "collision".equals(layer.getName());
                        var isBackground = "background".equals(layer.getName());

                        // only collision layer tiles are used to populate the collider grid
                        if (isCollision) {
                            solids.setCell(x, y, true);
                        }

                        // both collision and background layers are used to set tile textures
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
                    // parse position property from object
                    // scale to specified tileSize in case it's different than the tiled map tile size
                    // this way the scale of the map onscreen can be changed by adjusting the tileSize parameter
                    var position = Point.at(
                            (int) (object.getX() / collisionLayer.getTileWidth())   * tileSize,
                            (int) (object.getY() / collisionLayer.getTileHeight())  * tileSize);

                    // parse the object type
                    var type = (String) object.getProperties().get("type");
                    if ("spawner".equals(type)) {
                        // figure out what to spawn and do so
                        var target = (String) object.getProperties().get("target");
                        switch (target) {
                            case "player": Factory.player(world, position); break;
                            case "blob":   Factory.blob(world, position);   break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        // debug input handling
        {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
                Debug.draw_colliders = !Debug.draw_colliders;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
                Debug.draw_entities = !Debug.draw_entities;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
                Debug.draw_origin = !Debug.draw_origin;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
                Debug.frame_step = !Debug.frame_step;
            }

            if (Debug.frame_step && !Gdx.input.isKeyJustPressed(Debug.frame_step_key)) {
                return;
            }
        }

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
        renderWorldIntoFramebuffer();
        renderFramebufferIntoWindow();
    }

    @Override
    public void shutdown() {
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
            batch.draw(Content.pixel, 0, 0, Config.window_width, Config.window_height);
            batch.setColor(Color.WHITE);

            // composite scene
            batch.draw(frameBufferRegion, 0, 0, Config.window_width, Config.window_height);
        }
        batch.end();
    }

}
