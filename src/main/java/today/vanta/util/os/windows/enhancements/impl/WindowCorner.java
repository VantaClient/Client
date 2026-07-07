package today.vanta.util.os.windows.enhancements.impl;

import com.sun.jna.ptr.IntByReference;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.api.WindowsEnhancement;
import today.vanta.util.os.windows.natives.DwmAPI;

@NotNullByDefault
public class WindowCorner extends WindowsEnhancement<WindowCorner.Preference> {
    public static final WindowCorner INSTANCE = new WindowCorner();
    public enum Preference {
        DEFAULT("Default"), // 0
        DO_NOT_ROUND("Don't round"), // 1
        ROUND("Round"), // 2
        ROUND_SMALL("Round (Small)"); // 3

        public final String name;

        Preference(final String name) {
            this.name = name;
        }
    }

    @Override
    public boolean canApply(WindowsOS os) {
        return os.build() >= 22621;
    }

    @Override
    public void apply(final WindowsOS OS, @Nullable final WindowCorner.Preference p) {
        final Preference preference = p == null ? Preference.DEFAULT : p;
        DwmAPI.INSTANCE.DwmSetWindowAttribute(
                OS.hwnd(),
                DwmAPI.WindowAttribute.WINDOW_CORNER_PREFERENCE,
                new IntByReference(preference.ordinal()),
                DwmAPI.INT_SIZE
        );
    }
}
