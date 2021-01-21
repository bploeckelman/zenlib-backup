package zendo.games.zenlib;

import com.badlogic.gdx.math.MathUtils;

public class Calc {

    public static int clampInt(int t, int min, int max) {
        if      (t < min) return min;
        else if (t > max) return max;
        else              return t;
    }

    public static float floor(float value) {
        return MathUtils.floor(value);
    }

    public static float ceiling(float value) {
        return MathUtils.ceil(value);
    }


}
