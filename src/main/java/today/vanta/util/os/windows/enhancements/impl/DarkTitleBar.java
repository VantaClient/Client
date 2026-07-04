package today.vanta.util.os.windows.enhancements.impl;

import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import org.jetbrains.annotations.NotNullByDefault;

import today.vanta.Vanta;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.api.WindowsEnhancement;
import today.vanta.util.os.windows.natives.DwmAPI;

/**
 * Contains JNA stuff for setting the title bar to dark mode
 */
@NotNullByDefault
public final class DarkTitleBar extends WindowsEnhancement {
//    public static final int BACKDROP_BUILD_NUM = 22621;
    private static final int MIN_BUILD_NUMBER = 1809;
    public static final DarkTitleBar INSTANCE = new DarkTitleBar();

    @Override
    public boolean shouldApply(WindowsOS OS) {
        return OS.buildNumber() >= MIN_BUILD_NUMBER;
    }

    @Override
    public void apply(WindowsOS OS) {
        Vanta.instance.logger.info("doing dark mode thing");
        final WinDef.HWND hwnd = OS.hwnd();
        Vanta.instance.logger.info("hwnd {}", hwnd);
        final WinNT.HRESULT hr = DwmAPI.INSTANCE.DwmSetWindowAttribute(
                hwnd,
                DwmAPI.WindowAttribute.immersiveDarkMode(OS.buildNumber()),
                new WinDef.BOOLByReference(new WinDef.BOOL(true)),
                DwmAPI.INT_SIZE
        );
        Vanta.instance.logger.info("dark mode HRESULT: {}", hr);
    }
}
