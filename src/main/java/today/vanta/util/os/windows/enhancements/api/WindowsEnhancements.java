package today.vanta.util.os.windows.enhancements.api;

import org.jetbrains.annotations.NotNullByDefault;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.impl.DarkTitleBar;
import today.vanta.util.os.windows.enhancements.impl.MicaBackdrop;
import today.vanta.util.os.windows.natives.NtDll;

import java.util.Arrays;

@NotNullByDefault
public final class WindowsEnhancements {
    public static int majorVersion = Integer.MIN_VALUE;
    public static int buildNumber = Integer.MIN_VALUE;
    private static final WindowsEnhancement[] ENHANCEMENTS = new WindowsEnhancement[]{
            DarkTitleBar.INSTANCE,
            MicaBackdrop.INSTANCE
            // TODO: more enhancements
    };

    public static void apply() {
        NtDll.setBuildInfo();
        Arrays.stream(ENHANCEMENTS)
                .filter(f -> f.shouldApply(WindowsOS.INSTANCE))
                .forEach(f -> f.apply(WindowsOS.INSTANCE));
    }
}
