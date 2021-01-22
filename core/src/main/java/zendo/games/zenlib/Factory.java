package zendo.games.zenlib;

import zendo.games.zenlib.components.*;
import zendo.games.zenlib.ecs.Entity;
import zendo.games.zenlib.ecs.Mask;
import zendo.games.zenlib.ecs.World;
import zendo.games.zenlib.utils.Calc;
import zendo.games.zenlib.utils.Point;
import zendo.games.zenlib.utils.RectI;

public class Factory {

    public static Entity player(World world, Point position) {
        var entity = world.addEntity(position);
        entity.add(new Player(), Player.class);

        var anim = entity.add(new Animator("player"), Animator.class);
        anim.play("idle");
        anim.depth = 10;

        var rect = RectI.at(-4, -32, 8, 20);
        var hitbox = entity.add(Collider.makeRect(rect), Collider.class);

        var mover = entity.add(new Mover(), Mover.class);
        mover.collider = hitbox;
//        mover.onHitX = (self) -> Gdx.app.log("Mover", "hit x");
//        mover.onHitY = (self) -> Gdx.app.log("Mover", "hit y");

        return entity;
    }

    public static Entity blob(World world, Point position) {
        var en = world.addEntity(position);

        var anim = en.add(new Animator("blob"), Animator.class);
        anim.play("idle");
        anim.depth = 11;

        var hitbox = en.add(Collider.makeRect(RectI.at(-4, -16, 8, 8)), Collider.class);
        hitbox.mask = Mask.enemy;

        var mover = en.add(new Mover(), Mover.class);
        mover.collider = hitbox;
        mover.gravity = -300;
        mover.friction = 400;
        mover.onHitY = (self) -> {
            anim.play("idle");
            self.stopY();
        };

        // jump timer
        en.add(new Timer(2, (self) -> {
            if (!mover.onGround()) {
                self.start(0.05f);
            } else {
                self.start(2);

                anim.play("jump");
                mover.speed.y = 110;

                var player = self.world().first(Player.class);
                if (player != null) {
                    var dir = Calc.sign(player.entity().position.x - self.entity().position.x);
                    if (dir == 0) {
                        dir = 1;
                    }
                    anim.scale.set(dir, 1);
                    mover.speed.x = dir * 80;
                }
            }
        }), Timer.class);


        return en;
    }
}
