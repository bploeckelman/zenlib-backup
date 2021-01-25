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

        var rect = RectI.at(-4, 0, 8, 20);
        var hitbox = entity.add(Collider.makeRect(rect), Collider.class);

        var mover = entity.add(new Mover(), Mover.class);
        mover.collider = hitbox;

        return entity;
    }

    public static Entity blob(World world, Point position) {
        var en = world.addEntity(position);

        var anim = en.add(new Animator("blob"), Animator.class);
        anim.play("idle");
        anim.depth = 11;

        // hitbox is updated based on current Animator frame in Animator.play
        var rect = new RectI();
        if (anim.frame().hitbox != null) {
            rect.set(anim.frame().hitbox);
        }
        var hitbox = en.add(Collider.makeRect(rect), Collider.class);
        hitbox.mask = Mask.enemy;

        var mover = en.add(new Mover(), Mover.class);
        mover.collider = hitbox;
        mover.gravity = -300;
        mover.friction = 400;
        mover.onHitX = (self) -> {
            var move_sign = Calc.sign(mover.speed.x);

            self.stopX();

            // note - this sucks
            // if the hitbox changed as a part of switching to a new animation,
            // move the blob out of collision in case the x extents are different
            var prev_hitbox = anim.frame().hitbox;
            anim.play("idle");
            var new_hitbox = anim.frame().hitbox;
            var dx = new_hitbox.x - prev_hitbox.x;
            self.entity().position.x += move_sign * dx;
        };
        mover.onHitY = (self) -> {
            var move_sign = Calc.sign(mover.speed.y);

            self.stopY();

            // note - this sucks
            // if the hitbox changed as a part of switching to a new animation,
            // move the blob out of collision in case the y extents are different
            var prev_hitbox = anim.frame().hitbox;
            anim.play("idle");
            var new_hitbox = anim.frame().hitbox;
            var dy = new_hitbox.y - prev_hitbox.y;
            self.entity().position.y += move_sign * dy;
        };

        var hurtable = en.add(new Hurtable(), Hurtable.class);
        hurtable.hurtBy = Mask.player_attack;
        hurtable.collider = hitbox;
        hurtable.onHurt = new Hurtable.OnHurt() {
            // note: if we want to include some mutable state with the callback
            //       it is necessary to use an anonymous class instead of a lambda
            private int health = 3;

            @Override
            public void hurt(Hurtable self) {
                var player = self.world().first(Player.class);
                if (player != null) {
                    var sign = Calc.sign(self.entity().position.x - player.entity().position.x);
                    mover.speed.x = sign * 120;
                }

                health--;
                if (health <= 0) {
                    Factory.pop(world, Point.at(self.entity().position.x, self.entity().position.y - 16));
                    self.entity().destroy();
                }
            }
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

    public static Entity pop(World world, Point position) {
        var en = world.addEntity(position);

        var anim = en.add(new Animator("pop"), Animator.class);
        anim.play("pop");
        anim.depth = 20;

        // self terminate when complete
        en.add(new Timer(anim.animation().duration(), (self) -> self.entity().destroy()), Timer.class);

        return en;
    }

}
