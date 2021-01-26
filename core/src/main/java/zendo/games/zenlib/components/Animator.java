package zendo.games.zenlib.components;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import lombok.var;
import zendo.games.zenlib.ecs.Component;
import zendo.games.zenlib.assets.Content;
import zendo.games.zenlib.assets.Sprite;

public class Animator extends Component {

    public Vector2 scale;
    public float rotation;
    public float speed;

    private Sprite sprite;
    private int animationIndex;
    private int frameIndex;
    private float frameCounter;

    public Animator() {
        reset();
    }

    public Animator(String spriteName) {
        reset();
        sprite = Content.findSprite(spriteName);
    }

    @Override
    public void reset() {
        super.reset();
        if (scale == null) {
            scale = new Vector2();
        }
        scale.set(1, 1);
        rotation = 0;
        speed = 1;
        sprite = null;
        animationIndex = 0;
        frameIndex = 0;
        frameCounter = 0;
    }

    @Override
    public <T extends Component> void copyFrom(T other) {
        super.copyFrom(other);
        if (other instanceof Animator) {
            var animator = (Animator) other;
            this.scale.set(animator.scale);
            this.rotation       = animator.rotation;
            this.speed          = animator.speed;
            this.sprite         = animator.sprite;
            this.animationIndex = animator.animationIndex;
            this.frameIndex     = animator.frameIndex;
            this.frameCounter   = animator.frameCounter;
        }
    }

    public Sprite sprite() {
        return sprite;
    }

    public Sprite.Anim animation() {
        if (sprite != null && animationIndex >= 0 && animationIndex < sprite.animations.size()) {
            return sprite.animations.get(animationIndex);
        }
        return null;
    }

    public Sprite.Frame frame() {
        var anim = animation();
        return anim.frames.get(frameIndex);
    }

    public void play(String animation) {
        play(animation, false);
    }

    public void play(String animation, boolean restart) {
        assert(sprite != null) : "No Sprite assigned!";

        for (int i = 0; i < sprite.animations.size(); i++) {
            if (sprite.animations.get(i).name.equals(animation)) {
                if (animationIndex != i || restart) {
                    animationIndex = i;
                    frameIndex = 0;
                    frameCounter = 0;

                    // update collider if appropriate
                    var collider = get(Collider.class);
                    if (collider != null && collider.shape() == Collider.Shape.rect) {
                        var hitbox = frame().hitbox;
                        if (hitbox != null) {
                            collider.setRect(hitbox);
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    public void update(float dt) {
        if (!inValidState()) return;

        var anim = sprite.animations.get(animationIndex);
        var frame = anim.frames.get(frameIndex);

        // increment frame counter
        frameCounter += speed * dt;

        // move to next frame after duration
        while (frameCounter >= frame.duration) {
            // reset frame counter
            frameCounter -= frame.duration;

            // TODO: add play modes, pingpong, reversed, etc...
            // increment frame, move back if we're at the end
            frameIndex++;
            if (frameIndex >= anim.frames.size()) {
                frameIndex = 0;
            }
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        if (!inValidState()) return;

        var anim = sprite.animations.get(animationIndex);
        var frame = anim.frames.get(frameIndex);

        batch.draw(frame.image,
                entity().position.x - sprite.origin.x,
                entity().position.y - sprite.origin.y,
                sprite.origin.x,
                sprite.origin.y,
                frame.image.getRegionWidth(),
                frame.image.getRegionHeight(),
                scale.x, scale.y,
                rotation
        );
    }

    private boolean inValidState() {
        return (sprite != null
             && animationIndex >= 0
             && animationIndex < sprite.animations.size()
             && frameIndex >= 0
             && frameIndex < sprite.animations.get(animationIndex).frames.size()
        );
    }

}
