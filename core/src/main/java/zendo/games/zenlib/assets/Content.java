package zendo.games.zenlib.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import zendo.games.zenlib.utils.Aseprite;

import java.util.ArrayList;
import java.util.List;

public class Content {

    public static BitmapFont font;
    public static TiledMap tiledMap;
    public static Texture pixel;

    private static List<Texture> textures;
    private static List<Sprite> sprites;

    public static void load() {
        font = new BitmapFont();
        tiledMap = new TmxMapLoader().load("maps/room_0x0.tmx");
        pixel = new Texture("pixel.png");
        pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        textures = new ArrayList<>();
        sprites = new ArrayList<>();

        // TODO: add a sprite packing step that generates SpriteInfo objects that contain data needed to create actual sprites
        //       change loadSprite to create Sprite objects based on data in a SpriteInfo param
        //       that references TextureRegions in a packed atlas and other aseprite metadata used to create sprites

        loadSprite("player", "sprites/player.ase");
        loadSprite("blob", "sprites/blob.ase");
        loadSprite("pop", "sprites/pop.ase");
    }

    public static void unload() {
        for (var texture : textures) {
            texture.dispose();
        }
        textures.clear();
        sprites.clear();
        tiledMap.dispose();
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

    private static Sprite loadSprite(String name, String path) {
        var sprite = new Sprite();
        {
            // load ase file
            var ase = new Aseprite(path);

            // extract properties from aseprite
            sprite.name = name;
            sprite.origin.set(0, 0);
            if (ase.slices.size() > 0 && ase.slices.get(0).has_pivot) {
                var slice = ase.slices.get(0);
                // flip slice pivot point to be y-up to match in-game reference with in-aseprite pivot point
                sprite.origin.set(slice.pivot.x, slice.pivot.y - slice.height);
            }

            // build animation for each tag
            for (var tag : ase.tags) {
                var num_frames = tag.to - tag.from + 1;
                var anim_frames = new Sprite.Frame[num_frames];
                for (int i = 0; i < num_frames; i++) {
                    int frame_index = tag.from + i;
                    var frame = ase.frames.get(frame_index);
                    var frame_texture = new Texture(frame.image);
                    var sprite_frame = new Sprite.Frame(new TextureRegion(frame_texture), frame.duration / 1000f);
                    anim_frames[i] = sprite_frame;
                    textures.add(frame_texture);
                }

                var anim = new Sprite.Anim(tag.name, anim_frames);
                sprite.animations.add(anim);
            }

            // dispose Aseprite Pixmap images now that they're loaded as Textures
            for (var frame : ase.frames) {
                for (var cel : frame.cels) {
                    cel.image.dispose();
                }
                frame.image.dispose();
            }
        }
        sprites.add(sprite);


        return sprite;
    }

}
