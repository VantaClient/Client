package today.vanta.client.setting.impl;

import today.vanta.client.setting.Setting;
import today.vanta.client.setting.SettingChangeListener;

import java.util.ArrayList;
import java.util.List;

public class StringSetting extends Setting<String> {
    public String[] allValues;
    public boolean expanded;
    private StringSetting(String name, String value, String[] allValues) {
        super(name, value);
        this.allValues = allValues;
    }

    public void next() {
        int currentIndex = index();
        int nextIndex = (currentIndex + 1) % allValues.length;
        setValue(allValues[nextIndex]);
    }

    public void previous() {
        int currentIndex = index();
        int previousIndex = (currentIndex - 1 + allValues.length) % allValues.length;
        setValue(allValues[previousIndex]);
    }

    private int index() {
        for (int i = 0; i < allValues.length; i++) {
            if (allValues[i].equals(getValue())) {
                return i;
            }
        }
        return -1;
    }

    public static StringSettingBuilder builder() {
        return new StringSettingBuilder();
    }

    public static class StringSettingBuilder {
        private final List<SettingChangeListener<String>> listeners = new ArrayList<>();
        private String name;
        private String value;
        private String[] allValues;

        private StringSettingBuilder() {}

        public StringSettingBuilder name(String name) {
            this.name = name;
            return this;
        }

        public StringSettingBuilder value(String value) {
            this.value = value;
            return this;
        }

        public StringSettingBuilder values(String... allValues) {
            this.allValues = allValues;
            return this;
        }

        public StringSetting.StringSettingBuilder listener(SettingChangeListener<String> listener) {
            this.listeners.add(listener);
            return this;
        }

        public StringSetting build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be null or empty");
            }

            StringSetting setting = new StringSetting(name, value, allValues);
            for (SettingChangeListener<String> listener : listeners) {
                setting.addListener(listener);
            }

            return setting;
        }
    }

}
