package zendo.games.zenlib.ecs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import lombok.var;
import zendo.games.zenlib.utils.Point;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class World {

    static final int max_component_types = 256;

    static class Pool<T extends ListNode<T>> {
        public T first = null;
        public T last = null;

        public void insert(T instance) {
            if (last != null) {
                last.setNext(instance);
                instance.setPrev(last);
                instance.setNext(null);
                last = instance;
            } else {
                first = last = instance;
                instance.setNext(null);
                instance.setPrev(null);
            }

        }
        public void remove(T instance) {
            if (instance.prev() != null) instance.prev().setNext(instance.next());
            if (instance.next() != null) instance.next().setPrev(instance.prev());

            if (first == instance) first = instance.next();
            if (last  == instance) last  = instance.prev();

            instance.setNext(null);
            instance.setPrev(null);
        }
    }

    private Pool<Entity> entitiesCache;
    private Pool<Entity> entitiesAlive;
    private Pool<Component>[] componentsCache;
    private Pool<Component>[] componentsAlive;
    private List<Component> componentsVisible;

    public World() {
        entitiesCache = new Pool<>();
        entitiesAlive = new Pool<>();
        componentsCache = new Pool[max_component_types];
        componentsAlive = new Pool[max_component_types];
        componentsVisible = new ArrayList<>();
    }

    public Entity firstEntity() {
        return entitiesAlive.first;
    }

    public Entity lastEntity() {
        return entitiesAlive.last;
    }

    public <T extends Component> T first(Class<T> clazz) {
        int type = Component.Types.id(clazz);
        return clazz.cast(componentsAlive[type].first);
    }

    public <T extends Component> T last(Class<T> clazz) {
        int type = Component.Types.id(clazz);
        return clazz.cast(componentsAlive[type].last);
    }

    public <T extends Component> T add(Entity entity, T component, Class<T> clazz) {
        assert(entity != null) : "Entity cannot be null";
        assert(entity.world == this) : "Entity must be part of this world";

        // get the component type
        int type = Component.Types.id(clazz);
        if (componentsCache[type] == null) {
            componentsCache[type] = new Pool<>();
        }
        if (componentsAlive[type] == null) {
            componentsAlive[type] = new Pool<>();
        }
        var cache = componentsCache[type];
        var alive = componentsAlive[type];

        // instantiate a new instance
        T instance = null;
        if (cache.first != null) {
            instance = clazz.cast(cache.first);
            cache.remove(instance);
        } else {
            try {
                instance = ClassReflection.newInstance(clazz);
            } catch (ReflectionException e) {
                e.printStackTrace();
            }
        }
        assert(instance != null) : "Component instance was could not be instantiated";

        // construct the new instance
        instance.copyFrom(component);
        instance.type = type;
        instance.entity = entity;

        // add it to the live components;
        alive.insert(instance);

        // add it to the entity
        entity.components.add(instance);

        return instance;
    }

    public Entity addEntity() {
        return addEntity(Point.zero());
    }

    public Entity addEntity(Point position) {
        // create entity instance
        Entity instance;
        if (entitiesCache.first != null) {
            instance = entitiesCache.first;
            entitiesCache.remove(instance);
            instance.reset();
        } else {
            instance = new Entity();
        }

        // add to list
        entitiesAlive.insert(instance);

        // assign
        instance.position = position;
        instance.world    = this;

        return instance;
    }

    public void destroyEntity(Entity entity) {
        if (entity != null && entity.world == this) {
            // destroy components
            for (int i = entity.components.size() - 1; i >= 0; i--) {
                destroy(entity.components.get(i));
            }

            // remove ourselves from the list
            entitiesAlive.remove(entity);
            entitiesCache.insert(entity);

            entity.world = null;
        }
    }

    public void destroy(Component component) {
        if (component != null && component.entity != null && component.entity.world == this) {
            var type = component.type;

            // mark destroyed
            component.destroyed();

            // remove from entity
            var list = component.entity.components;
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i) == component) {
                    list.remove(i);
                    break;
                }
            }

            // remove from list
            componentsAlive[type].remove(component);
            componentsCache[type].insert(component);
        }
    }

    public void clear() {
        Entity entity = firstEntity();
        while (entity != null) {
            var next = entity.next();
            destroyEntity(entity);
            entity = next;
        }
    }

    public void update(float dt) {
        for (int i = 0; i < Component.Types.count(); i++) {
            var component = componentsAlive[i].first;
            while (component != null) {
                var next = component.next();
                if (component.active && component.entity.active) {
                    component.update(dt);
                }
                component = next;
            }
        }
    }

    public void render(SpriteBatch batch) {
        // Notes:
        // In general this isn't a great way to render objects
        // Every frame it has to rebuild the list and sort it
        // A more ideal way would be to cache the visible list
        // and insert / remove objects as they update or change
        // their depth

        // assemble list
        for (int i = 0; i < Component.Types.count(); i++) {
            var component = componentsAlive[i].first;
            while (component != null) {
                if (component.visible && component.entity.visible) {
                    componentsVisible.add(component);
                }
                component = component.next();
            }
        }

        // sort by depth
        componentsVisible.sort(Comparator.comparingInt(Component::depth));

        // render them
        for (var component : componentsVisible) {
            component.render(batch);
        }

        // clear the list for next time around
        componentsVisible.clear();
    }

}
