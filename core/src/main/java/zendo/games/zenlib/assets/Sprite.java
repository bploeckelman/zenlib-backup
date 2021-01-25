package zendo.games.zenlib.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import zendo.games.zenlib.utils.RectI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sprite {

    public static class Frame {
        public TextureRegion image;
        public RectI hitbox = null;
        public float duration; // in seconds
        public Frame(TextureRegion image) {
            this(image, 0.1f);
        }
        public Frame(TextureRegion image, float duration) {
            this.image = image;
            this.duration = duration;
        }
    }

    public static class Anim {
        public String name;
        public List<Frame> frames;

        public Anim(String name, Frame... frames) {
            this.name = name;
            this.frames = new ArrayList<>();
            Collections.addAll(this.frames, frames);
        }

        public float duration() {
            float d = 0;
            for (var frame : frames) {
                d += frame.duration;
            }
            return d;
        }
    }

    public String name;
    public Vector2 origin;
    public List<Anim> animations;

    public Sprite() {
        name = "";
        origin = new Vector2();
        animations = new ArrayList<>();
    }

    public Anim getAnimation(String name) {
        for (var anim : animations) {
            if (anim.name.equals(name)) {
                return anim;
            }
        }
        return null;
    }

}
