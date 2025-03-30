package today.vanta.client.setting.impl;

import today.vanta.client.setting.Setting;
import today.vanta.client.setting.SettingChangeListener;

import java.util.ArrayList;
import java.util.List;

public class NumberSetting extends Setting<Number> {
    public Number min, max;
    public int places;

    private NumberSetting(String name, Number value, Number min, Number max, int places) {
        super(name, value);
        this.min = min;
        this.max = max;
        this.places = places;
    }

    public static NumberSettingBuilder builder() {
        return new NumberSettingBuilder();
    }

    public static class NumberSettingBuilder {
        private final List<SettingChangeListener<Number>> listeners = new ArrayList<>();
        private String name;
        private Number value, min, max;
        private int places;

        private NumberSettingBuilder() {}

        public NumberSettingBuilder name(String name) {
            this.name = name;
            return this;
        }

        public NumberSettingBuilder value(Number value) {
            this.value = value;
            return this;
        }

        public NumberSettingBuilder min(Number min) {
            this.min = min;
            return this;
        }

        public NumberSettingBuilder max(Number max) {
            this.max = max;
            return this;
        }

        public NumberSettingBuilder places(int places) {
            this.places = places;
            return this;
        }

        public NumberSetting.NumberSettingBuilder listener(SettingChangeListener<Number> listener) {
            this.listeners.add(listener);
            return this;
        }

        public NumberSetting build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }

            NumberSetting setting = new NumberSetting(name, value, min, max, places);
            for (SettingChangeListener<Number> listener : listeners) {
                setting.addListener(listener);
            }

            return setting;
        }
    }

}
