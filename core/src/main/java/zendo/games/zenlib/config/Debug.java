package zendo.games.zenlib.config;

import com.badlogic.gdx.Input;

public class Debug {
    public static boolean frame_step = false;
    public static final int frame_step_key = Input.Keys.F12;

    public static boolean draw_colliders = false;
    public static boolean draw_entities = false;
    public static boolean draw_origin = false;

    public static boolean output_aseprite_atlas_as_png = false;
}
