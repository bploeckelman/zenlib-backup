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

        // TODO: add atlas and sprite packing step

        // todo - dispose Aseprite Pixmap images once done loading them here

        // load player sprite
        var player = new Sprite();
        {
            // load ase file
            var ase = new Aseprite("sprites/player.ase");

            // extract properties from aseprite
            player.name = "player";
            player.origin.set(0, 0);
            if (ase.slices.size() > 0 && ase.slices.get(0).has_pivot) {
                player.origin.set(ase.slices.get(0).pivot.x, ase.slices.get(0).pivot.y);
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
                player.animations.add(anim);
            }
        }
        sprites.add(player);

        // load blob sprite
        var blob = new Sprite();
        {
            // load ase file
            var ase = new Aseprite("sprites/blob.ase");

            // extract properties from aseprite
            blob.name = "blob";
            blob.origin.set(0, 0);
            if (ase.slices.size() > 0 && ase.slices.get(0).has_pivot) {
                blob.origin.set(ase.slices.get(0).pivot.x, ase.slices.get(0).pivot.y);
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
                blob.animations.add(anim);
            }
        }
        sprites.add(blob);
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

}
