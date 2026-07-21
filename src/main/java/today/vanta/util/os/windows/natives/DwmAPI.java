package today.vanta.util.os.windows.natives;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.win32.StdCallLibrary;
import lombok.AllArgsConstructor;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public interface DwmAPI extends StdCallLibrary {
    DwmAPI INSTANCE = Native.load("dwmapi", DwmAPI.class);

    int INT_SIZE = 4;

    final class WindowAttribute {
        public static final int NCRENDERING_ENABLED = 1,
        NCRENDERING_POLICY = 2,
        TRANSITIONS_FORCEDISABLED = 3,
        ALLOW_NCPAINT = 4,
        CAPTION_BUTTON_BOUNDS = 5,
        NONCLIENT_RTL_LAYOUT = 6,
        FORCE_ICONIC_REPRESENTATION = 7,
        FLIP3D_POLICY = 8,
        EXTENDED_FRAME_BOUNDS = 9,
        HAS_ICONIC_BITMAP = 10,
        DISALLOW_PEEK = 11,
        EXCLUDED_FROM_PEEK = 12,
        CLOAK = 13,
        CLOAKED = 14,
        FREEZE_REPRESENTATION = 15,
        PASSIVE_UPDATE_MODE = 16,
        USE_HOSTBACKDROPBRUSH = 17,
        // 3 are missing? lol
        /**
         * @since Windows 10 build 17763 / "Windows 10 October 2018 Update" / 1809
         */
        USE_IMMERSIVE_DARK_MODE_PRE_19041 = 19,
        /**
         * @since Windows 10 build 19041 / "Windows 10 May 2020 Update" / 2004
         */
        USE_IMMERSIVE_DARK_MODE = 20,
        WINDOW_CORNER_PREFERENCE = 33,
        BORDER_COLOR = 34,
        CAPTION_COLOR = 35,
        TEXT_COLOR = 36,
        VISIBLE_FRAME_BORDER_THICKNESS = 37,
        SYSTEMBACKDROP_TYPE = 38,
        REDIRECTIONBITMAP_ALPHA = 39,
        BORDER_MARGINS = 40,
        /**
         * Undocumented Mica
         * @since Windows 11 build 2200 (21H2)
         */
        MICA_EFFECT = 1029;

        /**
         * @param buildNumber Windows build number
         * @throws UnsupportedOperationException if buildNumber < 1809
         * @return flag
         */
        public static @MagicConstant(flagsFromClass = WindowAttribute.class)
        int immersiveDarkMode(int buildNumber) throws UnsupportedOperationException {
            if (buildNumber >= 19041) {
                return USE_IMMERSIVE_DARK_MODE;
            } else if (buildNumber >= 1809) {
                return USE_IMMERSIVE_DARK_MODE_PRE_19041;
            } else {
                throw new UnsupportedOperationException("Immersive dark mode is unsupported on buildNumber < 1809 (buildNumber = " + buildNumber + ")");
            }
        }
    }

    //https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute
    WinNT.HRESULT DwmSetWindowAttribute(
            WinDef.HWND hwnd,
            @MagicConstant(flagsFromClass = WindowAttribute.class) int flag,
            ByReference attribute,
            int size
    );
    final class WindowCompositionAttrib {
        public static final int WCA_UNDEFINED = 0x0,
            WCA_NCRENDERING_ENABLED = 0x1,
            WCA_NCRENDERING_POLICY = 0x2,
            WCA_TRANSITIONS_FORCEDISABLED = 0x3,
            WCA_ALLOW_NCPAINT = 0x4,
            WCA_CAPTION_BUTTON_BOUNDS = 0x5,
            WCA_NONCLIENT_RTL_LAYOUT = 0x6,
            WCA_FORCE_ICONIC_REPRESENTATION = 0x7,
            WCA_EXTENDED_FRAME_BOUNDS = 0x8,
            WCA_HAS_ICONIC_BITMAP = 0x9,
            WCA_THEME_ATTRIBUTES = 0xA,
            WCA_NCRENDERING_EXILED = 0xB,
            WCA_NCADORNMENTINFO = 0xC,
            WCA_EXCLUDED_FROM_LIVEPREVIEW = 0xD,
            WCA_VIDEO_OVERLAY_ACTIVE = 0xE,
            WCA_FORCE_ACTIVEWINDOW_APPEARANCE = 0xF,
            WCA_DISALLOW_PEEK = 0x10,
            WCA_CLOAK = 0x11,
            WCA_CLOAKED = 0x12,
            WCA_ACCENT_POLICY = 0x13,
            WCA_FREEZE_REPRESENTATION = 0x14,
            WCA_EVER_UNCLOAKED = 0x15,
            WCA_VISUAL_OWNER = 0x16,
            WCA_HOLOGRAPHIC = 0x17,
            WCA_EXCLUDED_FROM_DDA = 0x18,
            WCA_PASSIVEUPDATEMODE = 0x19,
            WCA_LAST = 0x1A;
    }
    @AllArgsConstructor
    class AccentPolicy extends Structure {
        @MagicConstant(flagsFromClass = AccentState.class) int state;
        int flags;
        int gradientColor;
        int animationId;

        public static final class AccentState {
            public static final int DISABLED = 0,
                    ENABLE_GRADIENT = 1,
                    ENABLE_TRANSPARENTGRADIENT = 2,
                    ENABLE_BLURBEHIND = 3,
                    // was called ACCENT_INVALID but window-vibrancy's enum names it otherwise?
                    ENABLE_ACRYLICBLURBEHIND = 4;
        }
        public static final class ByReference extends AccentPolicy implements Structure.ByReference {
            public ByReference(@MagicConstant(flagsFromClass = AccentState.class) int state, int flags, int gradientColor, int animationId) {
                super(state, flags, gradientColor, animationId);
            }
        }
    }
    @AllArgsConstructor
    class WindowCompositionAttribData extends Structure {
        // ts should NOT be 24 bytes
        public static final int SIZE = 24;
        @MagicConstant(flagsFromClass = WindowCompositionAttrib.class) int attrib;
        Structure.ByReference pData;
        WinDef.UINT size;
        public static final class ByReference extends WindowCompositionAttribData implements Structure.ByReference {
            public ByReference(@MagicConstant(flagsFromClass = WindowCompositionAttrib.class) int attrib, Structure.ByReference data, WinDef.UINT size) {
                super(attrib, data, size);
            }
        }
    }
    // technically in User32.dll but shut
    WinDef.BOOL SetWindowCompositionAttribute(
            WinDef.HWND hwnd,
            WindowCompositionAttribData.ByReference data
    );
    @AllArgsConstructor
    class BlurBehind extends Structure {
        @MagicConstant(flagsFromClass = Flags.class) int flags;
        WinDef.BOOL fEnable;
        WinDef.HRGN region;
        WinDef.BOOL transitionOnMaximized;
        public static final class Flags {
            public static final int DWM_BB_ENABLE = 0x00000001,
                    DWM_BB_BLURREGION = 0x00000002,
                    DWM_BB_TRANSITIONONMAXIMIZED = 4;
        }
        public static final class ByReference extends BlurBehind implements Structure.ByReference {
            public ByReference(int flags, WinDef.BOOL fEnable, WinDef.HRGN region, WinDef.BOOL transitionOnMaximized) {
                super(flags, fEnable, region, transitionOnMaximized);
            }
        }
    }
    WinNT.HRESULT DwmEnableBlurBehindWindow(
            WinDef.HWND hwnd,
            BlurBehind.ByReference data
    );
}
