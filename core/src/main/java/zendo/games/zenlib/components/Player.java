package zendo.games.zenlib.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controllers;
import zendo.games.zenlib.utils.Calc;
import zendo.games.zenlib.ecs.Component;

public class Player extends Component {

    public static final float gravity = -450;

    private static final float ground_accel = 500;
    private static final float ground_accel_run = 800;
    private static final float friction = 800;
    private static final float max_ground_speed = 100;
    private static final float max_ground_speed_run = 200;
    private static final float jump_impulse = 130;
    private static final float jump_time = 0.18f;

    private int facing = 1;
    private float jumpTimer = 0;
    private boolean onGround = false;

    private static class InputState {
        int move_dir = 0;
        boolean run_held = false;
        boolean jump_held = false;
        boolean jump = false;
    }
    private final InputState input = new InputState();

    private void updateInputState() {
        var controllers = Controllers.getControllers();
        var controller = controllers.isEmpty() ? null : controllers.get(0);

        var controller_button_a    = (controller == null) ? 0 : controller.getMapping().buttonA;
        var controller_button_r1   = (controller == null) ? 0 : controller.getMapping().buttonR1;
        var controller_axis_left_x = (controller == null) ? 0 : controller.getMapping().axisLeftX;

        var controller_dead_zone = 0.3f;
        var controller_axis_left_x_value = (controller == null) ? 0 : controller.getAxis(controller_axis_left_x);
        var controller_axis_left_x_in_dead_zone = (controller != null && Calc.abs(controller_axis_left_x_value) <= controller_dead_zone);

        // move direction
        input.move_dir = 0;
        if      (Gdx.input.isKeyPressed(Input.Keys.LEFT)  || (controller != null && !controller_axis_left_x_in_dead_zone && controller_axis_left_x_value < 0)) input.move_dir = -1;
        else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || (controller != null && !controller_axis_left_x_in_dead_zone && controller_axis_left_x_value > 0)) input.move_dir =  1;

        // jump input
        input.jump = false;
        input.jump_held = false;
        // TODO: need additional state var to get 'just pressed' for controller button
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || (controller != null && controller.getButton(controller_button_a))) input.jump = true;
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)     || (controller != null && controller.getButton(controller_button_a))) input.jump_held = true;

        // run input
        input.run_held = false;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || (controller != null && controller.getButton(controller_button_r1))) input.run_held = true;
    }

    @Override
    public void update(float dt) {
        // get input
        updateInputState();

        // get components
        var anim = entity().get(Animator.class);
        var mover = entity().get(Mover.class);

        var wasOnGround = onGround;
        onGround = mover.onGround();

        // sprite
        {
            // stopped
            if (onGround) {
                if (input.move_dir != 0) {
                    anim.play("run");
                    anim.speed = (input.run_held) ? 1.4f : 1;
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
            var accel = (input.run_held) ? ground_accel_run : ground_accel;
            mover.speed.x += input.move_dir * accel * dt;

            // max speed
            var max = (input.run_held) ? max_ground_speed_run : max_ground_speed;
            if (Calc.abs(mover.speed.x) > max) {
                mover.speed.x = Calc.approach(mover.speed.x, Calc.sign(mover.speed.x) * max, 2000 * dt);
            }

            // friction
            if (input.move_dir == 0 && onGround) {
                mover.speed.x = Calc.approach(mover.speed.x, 0, friction * dt);
            }

            // facing direction
            if (input.move_dir != 0) {
                facing = input.move_dir;
            }
        }

        // trigger a jump
        {
            if (input.jump && onGround) {
                // squoosh on jomp
                anim.scale.set(facing * 0.65f, 1.4f);

                jumpTimer = jump_time;
            }
        }

        // variable jumping based on how long the button is held down
        if (jumpTimer > 0) {
            jumpTimer -= dt;

            mover.speed.y = jump_impulse;

            if (!input.jump_held) {
                jumpTimer = 0;
            }
        }

        // gravity
        if (!onGround) {
            // make gravity more 'hovery' when in the air
            float grav = gravity;
            if (Calc.abs(mover.speed.y) < 20 && input.jump_held) {
                grav *= 0.4f;
            }

            mover.speed.y += grav * dt;
        }
    }

}
