package zendo.games.zenlib;

import com.badlogic.gdx.ApplicationAdapter;
import zendo.games.zenlib.utils.Time;

public class App extends ApplicationAdapter {

    static final int target_framerate = 60;
    static final int max_updates = 5;

    public final Game game;

    long time_last;
    long time_accum;

    public App(Game game) {
        this.game = game;
    }

    @Override
    public void create() {
        Time.init();
        game.init();
    }

    @Override
    public void render() {
        // update at a fixed rate
        long time_target = (long) ((1.f / target_framerate) * 1000);
        long time_curr = Time.elapsed_millis();
        long time_diff = time_curr - time_last;
        time_last = time_curr;
        time_accum += time_diff;

        // don't run too fast
        while (time_accum < time_target) {
            try {
                //noinspection BusyWait
                Thread.sleep(time_target - time_accum);
            } catch (InterruptedException ignored) {}

            time_curr = Time.elapsed_millis();
            time_diff = time_curr - time_last;
            time_last = time_curr;
            time_accum += time_diff;
        }

        // don't fall behind too many updates
        long time_max = max_updates * time_target;
        if (time_accum > time_max) {
            time_accum = time_max;
        }

        // do as many updates as possible
        while (time_accum >= time_target) {
            time_accum -= time_target;

            Time.delta = (1.f / target_framerate);

            if (Time.pause_timer > 0) {
                Time.pause_timer -= Time.delta;
                if (Time.pause_timer <= -0.0001f) {
                    Time.delta = -Time.pause_timer;
                } else {
                    continue;
                }
            }

            Time.millis += time_target;
            Time.previous_elapsed = Time.elapsed_millis();
//            Time.elapsed += Time.delta;  // do we need this if elapsed_millis() is using the system time since start?

            game.update(Time.delta);
        }

        // draw the things
        game.render();
    }

}
