package today.vanta.util.os.windows.enhancements.api;

import org.jetbrains.annotations.NotNullByDefault;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.impl.DarkTitleBar;
import today.vanta.util.os.windows.enhancements.impl.SystemBackdrop;
import today.vanta.util.os.windows.enhancements.impl.Transparent;
import today.vanta.util.os.windows.enhancements.impl.WindowCorner;
import today.vanta.util.os.windows.natives.NtDll;

import java.util.Arrays;

@NotNullByDefault
public final class WindowsEnhancements {
    public static int majorVersion = Integer.MIN_VALUE;
    public static int buildNumber = Integer.MIN_VALUE;
    private static final WindowsEnhancement<?>[] PRESETS
            = new WindowsEnhancement<?>[] {
            DarkTitleBar.INSTANCE,
            SystemBackdrop.INSTANCE,
            WindowCorner.INSTANCE,
//            Transparent.INSTANCE
    };

    public static void apply() {
        NtDll.setBuildInfo();
        Arrays.stream(PRESETS)
                .filter(f -> f.canApply(WindowsOS.INSTANCE))
                .forEach(f -> f.apply(
                        WindowsOS.INSTANCE, null
                ));
    }
}
