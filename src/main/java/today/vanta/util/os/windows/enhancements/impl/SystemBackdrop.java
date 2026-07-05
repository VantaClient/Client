package today.vanta.util.os.windows.enhancements.impl;

import com.sun.jna.ptr.IntByReference;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.api.WindowsEnhancement;
import today.vanta.util.os.windows.natives.DwmAPI;

@NotNullByDefault
public final class SystemBackdrop extends WindowsEnhancement<SystemBackdrop.Type> {
    public static final SystemBackdrop INSTANCE = new SystemBackdrop();
    public enum Type {
        AUTO("Auto"), // 0 Auto
        NONE("None"), // 1 None
        MICA("Mica"), // 2 Mica
        ACRYLIC("Acrylic"), // 3 Acrylic
        TABBED("Tabbed"); // 4 Tabbed
        public final String name;

        Type(final String name) {
            this.name = name;
        }
        public IntByReference value() {
            return new IntByReference(this.ordinal());
        }
    }

    @Override
    public boolean canApply(WindowsOS OS) {
        return OS.buildNumber() >= 22621;
    }

    @Override
    public void apply(WindowsOS OS, @Nullable SystemBackdrop.Type type) {
        DwmAPI.INSTANCE.DwmSetWindowAttribute(
                OS.hwnd(),
                DwmAPI.WindowAttribute.SYSTEMBACKDROP_TYPE,
                (type != null ? type : Type.AUTO).value(),
                DwmAPI.INT_SIZE
        );
    }
}
