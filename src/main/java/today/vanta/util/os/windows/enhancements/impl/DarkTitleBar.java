package today.vanta.util.os.windows.enhancements.impl;

import com.sun.jna.platform.win32.WinDef;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.api.WindowsConfigurableEnhancement;
import today.vanta.util.os.windows.natives.DwmAPI;

@NotNullByDefault
public final class DarkTitleBar extends WindowsConfigurableEnhancement<Boolean> {
    private static final int MIN_BUILD_NUMBER = 1809;
    public static final DarkTitleBar INSTANCE = new DarkTitleBar();

    @Override
    public boolean canApply(final WindowsOS OS) {
        return OS.build() >= MIN_BUILD_NUMBER;
    }

    @Override
    public void apply(final WindowsOS OS, @Nullable final Boolean d) {
        final boolean dark = d == null || d;
        final WinDef.HWND hwnd = OS.hwnd();
        DwmAPI.INSTANCE.DwmSetWindowAttribute(
                hwnd,
                DwmAPI.WindowAttribute.immersiveDarkMode(OS.build()),
                new WinDef.BOOLByReference(new WinDef.BOOL(dark)),
                DwmAPI.INT_SIZE
        );
    }
}
