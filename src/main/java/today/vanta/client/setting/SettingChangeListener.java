package today.vanta.client.setting;

@FunctionalInterface
public interface SettingChangeListener<T> {
    void onSettingChanged(Setting<T> setting, T oldValue, T newValue);
}
