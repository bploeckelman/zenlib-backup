package zendo.games.zenlib.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import zendo.games.zenlib.Calc;
import zendo.games.zenlib.Component;

public class Player extends Component {

    private static final float ground_accel = 500;
    private static final float friction = 800;

    @Override
    public void update(float dt) {
        // get input
        int input = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            input = -1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            input = 1;
        }

        // horizontal movement
        {
            var mover = entity().get(Mover.class);

            // acceleration
            mover.speed.x += input * ground_accel * dt;

            // friction
            if (input == 0) {
                mover.speed.x = Calc.approach(mover.speed.x, 0, friction * dt);
            }
        }
    }

}
