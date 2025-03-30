package today.vanta.client.setting;

import today.vanta.Vanta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class Setting<T> {

    private final List<SettingChangeListener<T>> listeners = new ArrayList<>();
    private BooleanSupplier hidden = () -> false;
    public String name;
    private T value;

    public Setting(String name, T value) {
        this.name = name;
        this.value = value;

        Vanta.instance.moduleStorage.context.settings.add(this);
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        T oldValue = this.value;
        this.value = value;

        for (SettingChangeListener<T> listener : listeners) {
            listener.onSettingChanged(this, oldValue, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <I extends Setting<?>> I hide(BooleanSupplier hidden) {
        this.hidden = hidden;
        return (I) this;
    }

    public boolean isHidden() {
        return hidden.getAsBoolean();
    }

    public void addListener(SettingChangeListener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(SettingChangeListener<T> listener) {
        listeners.remove(listener);
    }
}
