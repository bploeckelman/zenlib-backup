package zendo.games.zenlib.components;

import zendo.games.zenlib.ecs.Component;

public class Timer extends Component {

    public interface OnEnd {
        void run(Timer timer);
    }

    public OnEnd onEnd;

    private float duration;

    public Timer() {
        reset();
    }

    public Timer(float duration, OnEnd onEnd) {
        this.duration = duration;
        this.onEnd = onEnd;
    }

    @Override
    public void reset() {
        super.reset();
        duration = 0;
        onEnd = null;
    }

    @Override
    public <T extends Component> void copyFrom(T other) {
        super.copyFrom(other);
        if (other instanceof Timer) {
            var timer = (Timer) other;
            this.duration = timer.duration;
            this.onEnd    = timer.onEnd;
        }
    }

    public void start(float duration) {
        this.duration = duration;
    }

    @Override
    public void update(float dt) {
        if (duration > 0) {
            duration -= dt;
            if (duration <= 0 && onEnd != null) {
                onEnd.run(this);
            }
        }
    }

}
