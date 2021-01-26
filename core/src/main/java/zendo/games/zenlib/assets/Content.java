package zendo.games.zenlib.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.Array;
import lombok.var;
import zendo.games.zenlib.config.Debug;
import zendo.games.zenlib.utils.Aseprite;

public class Content {

    public static BitmapFont font;
    public static TiledMap tiledMap;
    public static Texture pixel;
    private static TextureAtlas atlas;

    private static Array<Sprite> sprites;

    public static void load() {
        font = new BitmapFont();
        tiledMap = new TmxMapLoader().load("maps/room_0x0.tmx");
        pixel = new Texture("pixel.png");
        pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        sprites = new Array<>();

        // create a pixmap packer to generate a texture atlas from aseprite frames
        var pageWidth = 1024;
        var pageHeight = 1024;
        var pageFormat = Pixmap.Format.RGBA8888;
        var padding = 0;
        var duplicateBorder = false;
        var stripWhitespaceX = false;
        var stripWhitespaceY = false;
        var packStrategy = new PixmapPacker.GuillotineStrategy();
        var packer = new PixmapPacker(
                pageWidth, pageHeight, pageFormat, padding,
                duplicateBorder, stripWhitespaceX, stripWhitespaceY,
                packStrategy);

        // load aseprite files and pack animation frame pixmaps
        var playerSpriteInfo = Aseprite.loadAndPack(packer, "sprites/player.ase");
        var blobSpriteInfo = Aseprite.loadAndPack(packer, "sprites/blob.ase");
        var popSpriteInfo = Aseprite.loadAndPack(packer, "sprites/pop.ase");

        // create texture atlas from packer
        var filter = Texture.TextureFilter.Nearest;
        var useMipMaps = false;
        atlas = packer.generateTextureAtlas(filter, filter, useMipMaps);

        // cleanup packer
        packer.dispose();

        // create sprites (and dispose pixmaps in SpriteInfo.Aseprite)
        var player = Aseprite.createSprite(playerSpriteInfo, atlas);
        var blob = Aseprite.createSprite(blobSpriteInfo, atlas);
        var pop = Aseprite.createSprite(popSpriteInfo, atlas);

        // save the loaded sprites
        sprites.addAll(
                  player
                , blob
                , pop
        );

        // write out the aseprite texture atlas for debugging purposes if so desired
        if (Debug.output_aseprite_atlas_as_png) {
            var atlas_texture = atlas.getTextures().first();
            var atlas_pixmap = atlas_texture.getTextureData().consumePixmap();
            var file = new FileHandle("aseprite_atlas.png");
            PixmapIO.writePNG(file, atlas_pixmap);
            atlas_pixmap.dispose();
        }
    }

    public static void unload() {
        sprites.clear();

        tiledMap.dispose();
        atlas.dispose();
        pixel.dispose();
        font.dispose();
    }

    public static Sprite findSprite(String name) {
        for (var sprite : sprites) {
            if (sprite.name.equals(name)) {
                return sprite;
            }
        }
        return null;
    }

}
