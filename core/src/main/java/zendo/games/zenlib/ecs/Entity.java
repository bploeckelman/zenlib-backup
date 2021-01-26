package zendo.games.zenlib.ecs;

import lombok.var;
import zendo.games.zenlib.utils.Point;

import java.util.ArrayList;
import java.util.List;

public class Entity extends ListNode<Entity> {

    public Point position;
    public World world;
    public List<Component> components;
    public boolean active;
    public boolean visible;

    public Entity() {
        this.components = new ArrayList<>();
        reset();
    }

    @Override
    public void reset() {
        this.position = Point.zero();
        this.world = null;
        this.components.clear();
        this.active = true;
        this.visible = true;
    }

    public World world() {
        return world;
    }

    public List<Component> components() {
        return components;
    }

    public void destroy() {
        world.destroyEntity(this);
    }

    public <T extends Component> T add(T component, Class<T> clazz) {
        assert(world != null) : "Entity must be assigned to a World";
        return world.add(this, component, clazz);
    }

    public <T extends Component> T get(Class<T> clazz) {
        assert(world != null) : "Entity must be assigned to a World";
        for (var component : components) {
            if (component.type == Component.Types.id(clazz)) {
                return clazz.cast(component);
            }
        }
        return null;
    }

}
