package today.vanta.client.setting.impl;

import today.vanta.client.setting.Setting;
import today.vanta.client.setting.SettingChangeListener;

import java.util.ArrayList;
import java.util.List;

public class BooleanSetting extends Setting<Boolean> {
    private BooleanSetting(String name, Boolean value) {
        super(name, value);
    }

    public static BooleanSettingBuilder builder() {
        return new BooleanSettingBuilder();
    }

    public static class BooleanSettingBuilder {
        private final List<SettingChangeListener<Boolean>> listeners = new ArrayList<>();
        private String name;
        private boolean value;

        private BooleanSettingBuilder() {}

        public BooleanSettingBuilder name(String name) {
            this.name = name;
            return this;
        }

        public BooleanSettingBuilder value(boolean value) {
            this.value = value;
            return this;
        }

        public BooleanSettingBuilder listener(SettingChangeListener<Boolean> listener) {
            this.listeners.add(listener);
            return this;
        }

        public BooleanSetting build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }

            BooleanSetting setting = new BooleanSetting(name, value);
            for (SettingChangeListener<Boolean> listener : listeners) {
                setting.addListener(listener);
            }

            return setting;
        }
    }

}
