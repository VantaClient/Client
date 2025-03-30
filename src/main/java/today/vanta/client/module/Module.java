package today.vanta.client.module;

import today.vanta.Vanta;
import today.vanta.client.event.impl.client.ModuleDisableEvent;
import today.vanta.client.event.impl.client.ModuleEnableEvent;
import today.vanta.client.setting.Setting;
import today.vanta.util.game.IMinecraft;

import java.util.ArrayList;
import java.util.List;

public class Module implements IMinecraft {
    public String name, description;
    public Category category;

    public int key;
    private boolean enabled, expanded;

    private String suffix = null;

    public String displayName;
    public String[] displayNames;

    public boolean frozen; //When true, module is not toggleable.

    public Module(String name, String description, Category category, int key) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.key = key;

        this.displayName = name;
        this.displayNames = new String[] {displayName};

        Vanta.instance.moduleStorage.context = this;
    }

    public boolean addSuffix = true;
    public boolean hideFromClickGui;
    public boolean hideFromArraylist;
    public boolean addToConfig = true;

    public Module(String name, String description, Category category) {
        this(name, description, category, 0);
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void setEnabled(boolean enabled) {
        if (frozen) {
            return;
        }

        this.enabled = enabled;

        if (enabled) {
            new ModuleEnableEvent(this).call();
            onEnable();
            Vanta.instance.eventBus.register(this);
        } else {
            new ModuleDisableEvent(this).call();
            Vanta.instance.eventBus.unregister(this);
            onDisable();
        }
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Setting<?>> settings = new ArrayList<>();

    public void next() {
        int currentIndex = index();
        int nextIndex = (currentIndex + 1) % displayNames.length;
        displayName = displayNames[nextIndex];
    }

    public void previous() {
        int currentIndex = index();
        int previousIndex = (currentIndex - 1 + displayNames.length) % displayNames.length;
        displayName = displayNames[previousIndex];
    }

    private int index() {
        for (int i = 0; i < displayNames.length; i++) {
            if (displayNames[i].equals(displayName)) {
                return i;
            }
        }
        return -1;
    }

    public String getSuffix() {
        return suffix;
    }

    public Setting<?> getSettingByName(String input) {
        return settings.stream().filter(s -> s.name.equalsIgnoreCase(input)).findFirst().orElse(null);
    }

}
