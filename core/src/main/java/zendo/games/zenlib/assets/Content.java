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
import zendo.games.zenlib.config.Debug;
import zendo.games.zenlib.utils.Aseprite;

import java.util.HashMap;
import java.util.Map;

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
        var playerSpriteInfo = loadAseprite(packer, "sprites/player.ase");
        var blobSpriteInfo = loadAseprite(packer, "sprites/blob.ase");
        var popSpriteInfo = loadAseprite(packer, "sprites/pop.ase");

        // create texture atlas from packer
        var filter = Texture.TextureFilter.Nearest;
        var useMipMaps = false;
        atlas = packer.generateTextureAtlas(filter, filter, useMipMaps);

        // cleanup packer
        packer.dispose();

        // create sprites (and dispose pixmaps in SpriteInfo.Aseprite)
        var player = createSprite(playerSpriteInfo, atlas);
        var blob = createSprite(blobSpriteInfo, atlas);
        var pop = createSprite(popSpriteInfo, atlas);

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

    // ------------------------------------------------------------------------
    // Implementation details
    // ------------------------------------------------------------------------

    /**
     * Data required to create a Sprite from an Aseprite file and packed textures
     */
    private static class SpriteInfo {
        public String path;
        public String name;
        public Aseprite aseprite;
        public Map<String, AnimFrameInfo[]> anim_frame_infos;

        static class AnimFrameInfo {
            public String region_name;
            public int region_index;
            public float duration;
        }
    }

    /**
     * Load the Aseprite file specified by 'path', packing animation frames
     * with the specified 'packer' and disposing of loaded Pixmap data from
     * the Aseprite files after it is packed
     *
     * @param packer a configured PixmapPacker used to pack animation frame data
     * @param path the path of the Aseprite file to load
     *
     * @return a SpriteInfo object populated with details of the loaded Aseprite file
     *         and references for how to find the TextureRegions packed by the PixmapPacker
     */
    private static SpriteInfo loadAseprite(PixmapPacker packer, String path) {
        var info = new SpriteInfo();
        {
            info.path = path;
            info.name = path.subSequence(path.lastIndexOf('/') + 1, path.indexOf(".ase")).toString();
            info.aseprite = new Aseprite(path);
            info.anim_frame_infos = new HashMap<>();

            // build animation info for each tag
            for (var anim_tag : info.aseprite.tags) {
                var num_frames = anim_tag.to - anim_tag.from + 1;

                // build frame infos for each frame of this animation
                info.anim_frame_infos.putIfAbsent(anim_tag.name, new SpriteInfo.AnimFrameInfo[num_frames]);
                for (int i = 0; i < num_frames; i++) {
                    int frame_index = anim_tag.from + i;

                    // collect frame information from the aseprite file
                    // note:
                    //  the string used for atlas.findRegion must _not_ include the frame index
                    //  while the string used to pack a region into the atlas _must_ include the frame index
                    var frame = info.aseprite.frames.get(frame_index);
                    var frame_region_name = info.name + "-" + anim_tag.name;
                    var frame_region_name_w_index = frame_region_name + "_" + i;
                    var frame_duration = frame.duration;

                    // pack the frame image into the texture atlas
                    packer.pack(frame_region_name_w_index, frame.image);

                    // save the info needed to build the sprite's animation for this tag/frame
                    var anim_frame_infos = info.anim_frame_infos.get(anim_tag.name);
                    anim_frame_infos[i] = new SpriteInfo.AnimFrameInfo();
                    anim_frame_infos[i].region_name = frame_region_name;
                    anim_frame_infos[i].region_index = i;
                    anim_frame_infos[i].duration = frame_duration;
                }
            }

            // dispose Aseprite Pixmap images since they are now packed into the texture atlas
            for (var frame : info.aseprite.frames) {
                for (var cel : frame.cels) {
                    cel.image.dispose();
                }
                frame.image.dispose();
            }
        }
        return info;
    }

    /**
     * Create a Sprite object based on the specified SpriteInfo with animation frames
     * pulled from the specified TextureAtlas
     *
     * @param info the data required to create a Sprite
     * @param atlas the TextureAtlas that holds animation frame TextureRegions referred
     *              to by the specified SpriteInfo
     *
     * @return a Sprite object populated based on data specified in SpriteInfo
     */
    private static Sprite createSprite(SpriteInfo info, TextureAtlas atlas) {
        var sprite = new Sprite();
        {
            // extract properties from aseprite
            sprite.name = info.name;
            sprite.origin.set(0, 0);
            if (info.aseprite.slices.size() > 0 && info.aseprite.slices.get(0).has_pivot) {
                var slice = info.aseprite.slices.get(0);
                // flip slice pivot point to be y-up to match in-game reference with aseprite pivot point
                sprite.origin.set(slice.pivot.x, slice.pivot.y - slice.height);
            }

            // build sprite animations
            for (var anim_name : info.anim_frame_infos.keySet()) {
                var anim_frame_info = info.anim_frame_infos.get(anim_name);

                // build frames for animation
                var anim_frames = new Sprite.Frame[anim_frame_info.length];
                for (int i = 0; i < anim_frame_info.length; i++) {
                    var frame_info = anim_frame_info[i];
                    var frame_region = atlas.findRegion(frame_info.region_name, frame_info.region_index);
                    var frame_duration = anim_frame_info[i].duration;
                    anim_frames[i] = new Sprite.Frame(frame_region, frame_duration / 1000f);
                }

                // build animation from frames
                var anim = new Sprite.Anim(anim_name, anim_frames);

                // add to sprite
                sprite.animations.add(anim);
            }
        }
        return sprite;
    }

}
