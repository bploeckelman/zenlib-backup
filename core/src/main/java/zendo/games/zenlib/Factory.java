package zendo.games.zenlib;

import zendo.games.zenlib.components.Animator;
import zendo.games.zenlib.components.Collider;
import zendo.games.zenlib.components.Mover;
import zendo.games.zenlib.components.Player;
import zendo.games.zenlib.ecs.Entity;
import zendo.games.zenlib.ecs.World;
import zendo.games.zenlib.utils.Point;
import zendo.games.zenlib.utils.RectI;

public class Factory {

    public static Entity player(World world, Point position) {
        var entity = world.addEntity(position);
        entity.add(new Player(), Player.class);

        var anim = entity.add(new Animator("character"), Animator.class);
        anim.play("idle");
        anim.depth = 10;

        var rect = RectI.at(-4, 0, 8, 20);
        var hitbox = entity.add(Collider.makeRect(rect), Collider.class);

        var mover = entity.add(new Mover(), Mover.class);
        mover.collider = hitbox;
//        mover.onHitX = (self) -> Gdx.app.log("Mover", "hit x");
//        mover.onHitY = (self) -> Gdx.app.log("Mover", "hit y");

        return entity;
    }

}
