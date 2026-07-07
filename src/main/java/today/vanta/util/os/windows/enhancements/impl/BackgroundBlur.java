package today.vanta.util.os.windows.enhancements.impl;

import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import org.jetbrains.annotations.NotNullByDefault;
import today.vanta.Vanta;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.api.WindowsEnhancement;
import today.vanta.util.os.windows.natives.DwmAPI;

// TODO: make the background blur disable when you leave the main menu
@NotNullByDefault
public final class BackgroundBlur extends WindowsEnhancement {
    // mfs be saying "if it works don't touch it", but what if my code is absolute trash? do I not touch it?
    public static final BackgroundBlur INSTANCE = new BackgroundBlur();
    private static boolean isBlurBehindSupported(final WindowsOS OS) {
        // Vista is 6.0.*.*, 7 is 6.1.*.*, 8 is 6.2.*.*
        return OS.version.major == 6 && OS.version.minor < 2;
    }
    private static boolean isSWCASupported(final WindowsOS OS) {
        return OS.version.atLeastB(17763);
    }
    private static boolean isUndocumentedMicaSupported(final WindowsOS OS) {
        return OS.version.atLeastB(22000);
    }
    private static boolean isBackdropTypeSupported(final WindowsOS OS) {
        return OS.version.atLeastB(22523);
    }

    private static boolean blurSupported(final WindowsOS OS) {
        return isBlurBehindSupported(OS) || isSWCASupported(OS);
    }

    /**
     * Tries to apply basic blur to the window.
     */
    private static boolean applyBlur(final WindowsOS OS) {
        if (isBlurBehindSupported(OS))
            return DwmAPI.INSTANCE.DwmEnableBlurBehindWindow(
                    OS.hwnd(),
                    new DwmAPI.BlurBehind.ByReference(
                            DwmAPI.BlurBehind.Flags.DWM_BB_ENABLE,
                            new WinDef.BOOL(true),
                            new WinDef.HRGN(),
                            new WinDef.BOOL()
                    )).equals(WinNT.S_OK);
        else if (isSWCASupported(OS))
            return DwmAPI.INSTANCE.SetWindowCompositionAttribute(
                    OS.hwnd(),
                    new DwmAPI.WindowCompositionAttribData.ByReference(
                            DwmAPI.WindowCompositionAttrib.WCA_ACCENT_POLICY,
                            new DwmAPI.AccentPolicy.ByReference(
                                    DwmAPI.AccentPolicy.AccentState.ENABLE_BLURBEHIND,
                                    0,
                                    0,
                                    0
                            ),
                            new WinDef.UINT(4)
                    )
            ).booleanValue();
        return false;
    }

    private static boolean acrylicSupported(final WindowsOS OS) {
        return isBackdropTypeSupported(OS) || isSWCASupported(OS);
    }

    public enum BackdropType {
        AUTO("Auto"), // 0 Auto
        NONE("None"), // 1 None
        MICA("Mica"), // 2 Mica
        ACRYLIC("Acrylic"), // 3 Acrylic
        TABBED("Tabbed"); // 4 Tabbed
        public final String name;

        BackdropType(final String name) {
            this.name = name;
        }
        public IntByReference value() {
            return new IntByReference(this.ordinal());
        }
    }
    private static boolean applyAcrylic(final WindowsOS OS) {
        if (isBackdropTypeSupported(OS))
            DwmAPI.INSTANCE.DwmSetWindowAttribute(
                    OS.hwnd(),
                    DwmAPI.WindowAttribute.SYSTEMBACKDROP_TYPE,
                    BackdropType.ACRYLIC.value(),
                    DwmAPI.INT_SIZE
            );
        else if (isSWCASupported(OS)) {
            DwmAPI.INSTANCE.SetWindowCompositionAttribute(
                    OS.hwnd(),
                    new DwmAPI.WindowCompositionAttribData.ByReference(
                            DwmAPI.WindowCompositionAttrib.WCA_ACCENT_POLICY,
                            new DwmAPI.AccentPolicy.ByReference(
                                    DwmAPI.AccentPolicy.AccentState.ENABLE_ACRYLICBLURBEHIND,
                                    0,
                                    0,
                                    0
                            ),
                            new WinDef.UINT(4)
                    )
            );
        } else return false;
        return true;
    }

    private static boolean micaSupported(final WindowsOS OS) {
        return isBackdropTypeSupported(OS) || isUndocumentedMicaSupported(OS);
    }

    private static boolean applyMica(final WindowsOS OS) {
        if (isBackdropTypeSupported(OS))
            return DwmAPI.INSTANCE.DwmSetWindowAttribute(
                    OS.hwnd(),
                    DwmAPI.WindowAttribute.SYSTEMBACKDROP_TYPE,
                    BackdropType.MICA.value(),
                    DwmAPI.INT_SIZE
            ).equals(WinNT.S_OK);
        else if (isUndocumentedMicaSupported(OS))
            return DwmAPI.INSTANCE.DwmSetWindowAttribute(
                    OS.hwnd(),
                    DwmAPI.WindowAttribute.MICA_EFFECT,
                    new WinDef.BOOLByReference(new WinDef.BOOL(true)),
                    DwmAPI.INT_SIZE
            ).equals(WinNT.S_OK);
        return false;
    }

    @Override
    public boolean canApply(final WindowsOS OS) {
        return micaSupported(OS) || acrylicSupported(OS) || blurSupported(OS);
    }

    /** Logs "Failed to {message}" if result isn't `true` **/
    private static void logIfFail(boolean result, String message) {
        if (!result) {
            Vanta.instance.logger.warn("Failed to {}", message);
        }
//        return result;
    }

    @Override
    public void apply(final WindowsOS OS) {
        // Windows 11 -> Windows 10 -> Windows Vista until Windows 8 -> update son
        if (micaSupported(OS)) logIfFail(applyMica(OS), "apply Mica blur");
        else if (acrylicSupported(OS)) logIfFail(applyAcrylic(OS), "apply Acrylic blur");
        else if (blurSupported(OS)) logIfFail(applyBlur(OS), "apply blur");
    }
}
