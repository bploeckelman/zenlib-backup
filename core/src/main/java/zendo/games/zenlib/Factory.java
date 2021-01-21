package zendo.games.zenlib;

import com.badlogic.gdx.Gdx;
import zendo.games.zenlib.components.Collider;
import zendo.games.zenlib.components.Mover;
import zendo.games.zenlib.components.Player;

public class Factory {

    public static Entity player(World world, Point position) {
        var entity = world.addEntity(position);
        entity.add(new Player(), Player.class);

        // TODO: add animation

        var rect = RectI.at(-5, -5, 10, 10);
        var hitbox = entity.add(Collider.makeRect(rect), Collider.class);

        var mover = entity.add(new Mover(), Mover.class);
        mover.collider = hitbox;
        mover.onHitX = (self) -> Gdx.app.log("Mover", "hit x");
        mover.onHitY = (self) -> Gdx.app.log("Mover", "hit y");

        return entity;
    }

}
