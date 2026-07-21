package today.vanta.util.os.windows.enhancements.impl;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.api.WindowsConfigurableEnhancement;

@NotNullByDefault
public final class Transparent extends WindowsConfigurableEnhancement<Boolean> {
    public static final Transparent INSTANCE = new Transparent();

    @Override
    public boolean canApply(WindowsOS os) {
        return true;
    }

    @Override
    public void apply(WindowsOS os, @Nullable Boolean enabled) {
        final boolean transparent = enabled == null || enabled;

        int exStyle = User32.INSTANCE.GetWindowLong(
                os.hwnd(),
                WinUser.GWL_EXSTYLE
        );

        exStyle |= WinUser.WS_EX_LAYERED;

        User32.INSTANCE.SetWindowLong(
                os.hwnd(),
                WinUser.GWL_EXSTYLE,
                exStyle
        );

        if (transparent) {
            exStyle &= ~WinUser.WS_EX_LAYERED;
            User32.INSTANCE.SetWindowLong(os.hwnd(), WinUser.GWL_EXSTYLE, exStyle);
            exStyle |= WinUser.WS_EX_LAYERED;
            User32.INSTANCE.SetWindowLong(os.hwnd(), WinUser.GWL_EXSTYLE, exStyle);
        } else {
            User32.INSTANCE.SetLayeredWindowAttributes(
                    os.hwnd(),
                    0,
                    (byte) 255,
                    WinUser.LWA_ALPHA
            );
        }
    }
}