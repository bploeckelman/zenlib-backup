package zendo.games.zenlib.components;

import zendo.games.zenlib.ecs.Component;

public class Hurtable extends Component {

    public interface OnHurt {
        void hurt(Hurtable hurtable);
    }

    public Collider collider;
    public OnHurt onHurt;
    public int hurtBy;
    public float stunTimer;
    public float flickerTimer;
    public float lastFlickerTime;

    public Hurtable() {
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        collider = null;
        onHurt = null;
        hurtBy = 0;
        stunTimer = 0;
        flickerTimer = 0;
        lastFlickerTime = 0;
    }

    @Override
    public <T extends Component> void copyFrom(T other) {
        super.copyFrom(other);
        if (other instanceof Hurtable) {
            var hurtable = (Hurtable) other;
            this.collider        = hurtable.collider;
            this.onHurt          = hurtable.onHurt;
            this.hurtBy          = hurtable.hurtBy;
            this.stunTimer       = hurtable.stunTimer;
            this.flickerTimer    = hurtable.flickerTimer;
            this.lastFlickerTime = hurtable.lastFlickerTime;
        }
    }

    @Override
    public void update(float dt) {
        if (collider != null && onHurt != null && stunTimer <= 0) {
            if (collider.check(hurtBy)) {
                // TODO: void Time.pause_for(sec);
                stunTimer = 0.5f;
                flickerTimer = 0.5f;
                lastFlickerTime = flickerTimer;
                onHurt.hurt(this);
            }
        }

        stunTimer -= dt;

        if (flickerTimer > 0) {
            // TODO: boolean Time.on_interval(sec)
            if (lastFlickerTime > flickerTimer + 0.08f) {
                lastFlickerTime = flickerTimer;
                entity().visible = !entity().visible;
            }

            flickerTimer -= dt;
            if (flickerTimer <= 0) {
                entity().visible = true;
            }
        }
    }

}
