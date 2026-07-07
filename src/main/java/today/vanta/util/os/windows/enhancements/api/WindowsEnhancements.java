package today.vanta.util.os.windows.enhancements.api;

import org.jetbrains.annotations.NotNullByDefault;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.impl.*;
import today.vanta.util.os.windows.natives.NtDll;

import java.util.Arrays;

@NotNullByDefault
public final class WindowsEnhancements {
    private static final WindowsEnhancement<?>[] PRESETS
            = new WindowsEnhancement<?>[] {
            DarkTitleBar.INSTANCE,
            SystemBackdrop.INSTANCE,
            WindowCorner.INSTANCE,
            Transparent.INSTANCE,
            BackgroundBlur.INSTANCE
    };

    public static void apply() {
        Arrays.stream(PRESETS)
                .filter(f -> f.canApply(WindowsOS.INSTANCE))
                .forEach(f -> f.apply(
                        WindowsOS.INSTANCE, null
                ));
    }
}
