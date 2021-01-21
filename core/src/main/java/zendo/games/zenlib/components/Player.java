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

    private int facing = 1;

    @Override
    public void update(float dt) {
        // get input
        int input = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            input = -1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            input = 1;
        }

        // get components
        var anim = entity().get(Animator.class);
        var mover = entity().get(Mover.class);

        // sprite
        {
            // stopped
            if (input != 0) {
                anim.play("run");
            } else {
                anim.play("idle");
            }

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
            if (input == 0) {
                mover.speed.x = Calc.approach(mover.speed.x, 0, friction * dt);
            }

            // facing direction
            if (input != 0) {
                facing = input;
            }
        }

        // vertical movement
//        {
//            // acceleration
//            mover.speed.y += gravity * dt;
//
//            // max speed
//            if (Calc.abs(mover.speed.y) > max_air_speed) {
//                mover.speed.y = Calc.approach(mover.speed.y, Calc.sign(mover.speed.y) * max_air_speed, 2000 * dt);
//            }
//        }
    }

}
