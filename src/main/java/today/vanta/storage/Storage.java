package today.vanta.storage;

import today.vanta.Vanta;
import today.vanta.util.game.events.ISubscriber;

import java.util.ArrayList;
import java.util.List;

public abstract class Storage<T> implements ISubscriber {

    public final List<T> list = new ArrayList<>();

    @Override
    public void subscribe() {
        Vanta.instance.eventBus.register(this);
    }

    public <V extends T> V getT(final Class<V> clazz) {
        final T obj = list.stream().filter(ob -> ob.getClass().equals(clazz)).findFirst().orElse(null);

        if (obj == null)
            return null;

        return clazz.cast(obj);
    }

}
