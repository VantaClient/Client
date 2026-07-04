package today.vanta.util.os.windows.enhancements.impl;

import com.sun.jna.ptr.IntByReference;
import org.jetbrains.annotations.NotNullByDefault;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.api.WindowsEnhancement;
import today.vanta.util.os.windows.natives.DwmAPI;

@NotNullByDefault
public final class MicaBackdrop extends WindowsEnhancement {
    public static final MicaBackdrop INSTANCE = new MicaBackdrop();

    @Override
    public boolean shouldApply(WindowsOS OS) {
        return OS.buildNumber() >= 22621;
    }

    @Override
    public void apply(WindowsOS OS) {
        DwmAPI.INSTANCE.DwmSetWindowAttribute(
                OS.hwnd(),
                DwmAPI.WindowAttribute.SYSTEMBACKDROP_TYPE,
                new IntByReference(0),
                DwmAPI.INT_SIZE
        );
    }
}
