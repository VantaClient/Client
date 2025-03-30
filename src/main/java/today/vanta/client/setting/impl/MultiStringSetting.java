package today.vanta.client.setting.impl;

import today.vanta.client.setting.Setting;
import today.vanta.client.setting.SettingChangeListener;

import java.util.ArrayList;
import java.util.List;

public class MultiStringSetting extends Setting<String[]> {
    public String[] allValues;
    public boolean expanded;

    public boolean isEnabled(String value) {
        for (String s : getValue()) {
            if (s.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private MultiStringSetting(String name, String[] value, String[] allValues) {
        super(name, value);
        this.allValues = allValues;
    }

    public static MultiStringSettingBuilder builder() {
        return new MultiStringSettingBuilder();
    }

    public static class MultiStringSettingBuilder {
        private final List<SettingChangeListener<String[]>> listeners = new ArrayList<>();
        private String name;
        private String[] value;
        private String[] allValues;

        private MultiStringSettingBuilder() {}

        public MultiStringSettingBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MultiStringSettingBuilder value(String... value) {
            this.value = value;
            return this;
        }

        public MultiStringSettingBuilder values(String... allValues) {
            this.allValues = allValues;
            return this;
        }

        public MultiStringSettingBuilder listener(SettingChangeListener<String[]> listener) {
            this.listeners.add(listener);
            return this;
        }

        public MultiStringSetting build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }

            MultiStringSetting setting = new MultiStringSetting(name, value, allValues);
            for (SettingChangeListener<String[]> listener : listeners) {
                setting.addListener(listener);
            }

            return setting;
        }
    }

}
