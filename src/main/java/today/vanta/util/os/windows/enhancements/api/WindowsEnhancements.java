package today.vanta.util.os.windows.enhancements.api;

import org.jetbrains.annotations.NotNullByDefault;
import today.vanta.util.os.CommonEnhancement;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.impl.*;

import java.util.Arrays;
import java.util.List;

@NotNullByDefault
public final class WindowsEnhancements {
    private static final List<CommonEnhancement<WindowsOS>> PRESETS = Arrays.asList(
            DarkTitleBar.INSTANCE,
            SystemBackdrop.INSTANCE,
            WindowCorner.INSTANCE,
            Transparent.INSTANCE,
            BackgroundBlur.INSTANCE
    );

    public static void apply() {
        PRESETS.stream()
                .filter(f -> f.canApply(WindowsOS.INSTANCE))
                .forEach(f -> f.apply(WindowsOS.INSTANCE));
    }
}
