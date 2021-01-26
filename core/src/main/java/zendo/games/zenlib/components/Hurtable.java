package zendo.games.zenlib.components;

import lombok.var;
import zendo.games.zenlib.ecs.Component;
import zendo.games.zenlib.utils.Time;

public class Hurtable extends Component {

    public interface OnHurt {
        void hurt(Hurtable hurtable);
    }

    public Collider collider;
    public OnHurt onHurt;
    public int hurtBy;
    public float stunTimer;
    public float flickerTimer;

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
        }
    }

    @Override
    public void update(float dt) {
        if (collider != null && onHurt != null && stunTimer <= 0) {
            if (collider.check(hurtBy)) {
                Time.pause_for(0.1f);
                stunTimer = 0.5f;
                flickerTimer = 0.5f;
                onHurt.hurt(this);
            }
        }

        stunTimer -= dt;

        if (flickerTimer > 0) {
            if (Time.on_interval(0.05f)) {
                entity().visible = !entity().visible;
            }

            flickerTimer -= dt;
            if (flickerTimer <= 0) {
                entity().visible = true;
            }
        }
    }

}
