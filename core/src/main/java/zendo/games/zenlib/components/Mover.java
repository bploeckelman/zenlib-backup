package zendo.games.zenlib.components;

import com.badlogic.gdx.math.Vector2;
import zendo.games.zenlib.Calc;
import zendo.games.zenlib.Component;
import zendo.games.zenlib.Mask;
import zendo.games.zenlib.Point;

public class Mover extends Component {

    public interface OnHit {
        void hit(Mover mover);
    }

    public Vector2 speed;
    public Collider collider;
    public OnHit onHitX;
    public OnHit onHitY;
    public float gravity;
    public float friction;

    private Vector2 remainder;

    public Mover() {
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        if (remainder == null) {
            remainder = new Vector2();
        }
        if (speed == null) {
            speed = new Vector2();
        }
        remainder.set(0, 0);
        speed.set(0, 0);
        collider = null;
        onHitX = null;
        onHitY = null;
        gravity = 0;
        friction = 0;
    }

    @Override
    public <T extends Component> void copyFrom(T other) {
        super.copyFrom(other);
        if (other instanceof Mover) {
            var mover = (Mover) other;
            this.remainder.set(mover.remainder);
            this.speed.set(mover.remainder);
            this.collider = mover.collider;
            this.onHitX   = mover.onHitX;
            this.onHitY   = mover.onHitY;
            this.gravity  = mover.gravity;
            this.friction = mover.friction;
        }
    }

    @Override
    public void update(float dt) {
        // apply friction maybe
        if (friction > 0 && onGround()) {
            speed.x = Calc.approach(speed.x, 0, friction * dt);
        }

        // apply gravity
        if (gravity != 0 && (collider == null || !collider.check(Mask.solid, Point.at(0, 1)))) {
            speed.y += gravity * dt;
        }

        // get the amount we should move, including remainder from previous frame
        float totalX = remainder.x + speed.x * dt;
        float totalY = remainder.y + speed.y * dt;

        // round to integer values since we only move in pixels at a time
        int toMoveX = (int) totalX;
        int toMoveY = (int) totalY;

        // store the remainder floating values
        remainder.x = totalX - toMoveX;
        remainder.y = totalY - toMoveY;

        // move by integer values
        moveX(toMoveX);
        moveY(toMoveY);
    }

    public boolean moveX(int amount) {
        if (collider != null) {
            int sign = Calc.sign(amount);

            while (amount != 0) {
                if (collider.check(Mask.solid, Point.at(sign, 0))) {
                    if (onHitX != null) {
                        onHitX.hit(this);
                    } else {
                        stopX();
                    }
                    return true;
                }

                amount -= sign;
                entity().position.x += sign;
            }
        } else {
            entity().position.x += amount;
        }

        return false;
    }

    public boolean moveY(int amount) {
        if (collider != null) {
            int sign = Calc.sign(amount);

            while (amount != 0) {
                if (collider.check(Mask.solid, Point.at(0, sign))) {
                    if (onHitY != null) {
                        onHitY.hit(this);
                    } else {
                        stopY();
                    }
                    return true;
                }

                amount -= sign;
                entity().position.y += sign;
            }
        } else {
            entity().position.y += amount;
        }

        return false;
    }

    public void stopX() {
        speed.x = 0;
        remainder.x = 0;
    }

    public void stopY() {
        speed.y = 0;
        remainder.y = 0;
    }

    public void stop() {
        stopX();
        stopY();
    }

    public boolean onGround() {
        return onGround(-1);
    }

    public boolean onGround(int dist) {
        if (collider == null) {
            return false;
        }

        boolean hit_solid = collider.check(Mask.solid, Point.at(0, dist));

        return hit_solid;
    }

}
