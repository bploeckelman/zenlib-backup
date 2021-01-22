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

    private static final String[] textureFiles = new String[] {
              "char-idle_0.png"
            , "char-idle_1.png"
            , "char-idle_2.png"
            , "char-idle_3.png"
            , "char-idle_4.png"
            , "char-idle_5.png"
            , "char-idle_6.png"
            , "char-idle_7.png"
            , "char-idle_8.png"
            , "char-idle_9.png"
            , "char-run-right_0.png"
            , "char-run-right_1.png"
            , "char-run-right_2.png"
            , "char-run-right_3.png"
            , "char-run-right_4.png"
            , "char-run-right_5.png"
            , "char-jump-up_0.png"
            , "char-jump-down_0.png"
            , "pixel.png"
    };

    public static void load() {
        font = new BitmapFont();
        tiledMap = new TmxMapLoader().load("maps/room_0x0.tmx");
        pixel = new Texture("pixel.png");
        pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        textures = new ArrayList<>();
        sprites = new ArrayList<>();

        // TODO: add atlas and sprite packing step

        for (var filename : textureFiles) {
            var texture = new Texture(filename);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textures.add(texture);
        }

        // test aseprite loading by creating a blob sprite
        // todo - dispose Aseprite Pixmap images once done loading them here
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
        // testing -------------------

        // load player sprite for testing....
        var sprite = new Sprite();
        sprite.name = "character";
        sprite.origin.set(16f, 0f);
        sprite.animations.add(new Sprite.Anim("idle",
                  new Sprite.Frame(new TextureRegion(textures.get(0)))
                , new Sprite.Frame(new TextureRegion(textures.get(1)))
                , new Sprite.Frame(new TextureRegion(textures.get(2)))
                , new Sprite.Frame(new TextureRegion(textures.get(3)))
                , new Sprite.Frame(new TextureRegion(textures.get(4)))
                , new Sprite.Frame(new TextureRegion(textures.get(5)))
                , new Sprite.Frame(new TextureRegion(textures.get(6)))
                , new Sprite.Frame(new TextureRegion(textures.get(7)))
                , new Sprite.Frame(new TextureRegion(textures.get(8)))
                , new Sprite.Frame(new TextureRegion(textures.get(9)))
        ));
        sprite.animations.add(new Sprite.Anim("run",
                  new Sprite.Frame(new TextureRegion(textures.get(10)))
                , new Sprite.Frame(new TextureRegion(textures.get(11)))
                , new Sprite.Frame(new TextureRegion(textures.get(12)))
                , new Sprite.Frame(new TextureRegion(textures.get(13)))
                , new Sprite.Frame(new TextureRegion(textures.get(14)))
                , new Sprite.Frame(new TextureRegion(textures.get(15)))
        ));
        sprite.animations.add(new Sprite.Anim("jump",
                  new Sprite.Frame(new TextureRegion(textures.get(16)))
        ));
        sprite.animations.add(new Sprite.Anim("fall",
                new Sprite.Frame(new TextureRegion(textures.get(17)))
        ));

        sprites.add(sprite);
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
