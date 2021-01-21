package zendo.games.zenlib.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import zendo.games.zenlib.Calc;
import zendo.games.zenlib.Component;

public class Player extends Component {

    public static final float gravity = -450;

    private static final float ground_accel = 500;
    private static final float friction = 800;
    private static final float max_ground_speed = 100;
    private static final float jump_impulse = 130;
    private static final float jump_time = 0.18f;

    private int facing = 1;
    boolean onGround = false;
    private float jumpTimer = 0;

    @Override
    public void update(float dt) {
        // get input
        int input = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            input = -1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            input = 1;
        }
        boolean inputJump = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            inputJump = true;
        }
        boolean inputJumpHeld = false;
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            inputJumpHeld = true;
        }

        // get components
        var anim = entity().get(Animator.class);
        var mover = entity().get(Mover.class);

        var wasOnGround = onGround;
        onGround = mover.onGround();

        // sprite
        {
            // stopped
            if (onGround) {
                if (input != 0) {
                    anim.play("run");
                } else {
                    anim.play("idle");
                }
            } else {
                if (mover.speed.y > 0) {
                    anim.play("jump");
                } else {
                    anim.play("fall");
                }
            }

            // landing squish
            if (!wasOnGround && onGround) {
                anim.scale.set(facing * 1.5f, 0.7f);
            }

            // lerp scale back to one
            anim.scale.set(
                    Calc.approach(anim.scale.x, facing, 4 * dt),
                    Calc.approach(anim.scale.y, 1, 4 * dt)
            );

            // facing
            anim.scale.x = Calc.abs(anim.scale.x) * facing;
        }

        // horizontal movement
        {
            // acceleration
            mover.speed.x += input * ground_accel * dt;

            // max speed
            if (Calc.abs(mover.speed.x) > max_ground_speed) {
                mover.speed.x = Calc.approach(mover.speed.x, Calc.sign(mover.speed.x) * max_ground_speed, 2000 * dt);
            }

            // friction
            if (input == 0 && onGround) {
                mover.speed.x = Calc.approach(mover.speed.x, 0, friction * dt);
            }

            // facing direction
            if (input != 0) {
                facing = input;
            }
        }

        // trigger a jump
        {
            if (inputJump && onGround) {
                // squoosh on jomp
                anim.scale.set(facing * 0.65f, 1.4f);

                jumpTimer = jump_time;
            }
        }

        // variable jumping based on how long the button is held down
        if (jumpTimer > 0) {
            jumpTimer -= dt;

            mover.speed.y = jump_impulse;

            if (!inputJumpHeld) {
                jumpTimer = 0;
            }
        }
    }

}
