package today.vanta.util.os.windows.natives;

import com.sun.jna.Native;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.win32.StdCallLibrary;
import org.intellij.lang.annotations.MagicConstant;

public interface DwmAPI extends StdCallLibrary {
    DwmAPI INSTANCE = Native.load("dwmapi", DwmAPI.class);

    int INT_SIZE = 4;

    final class WindowAttribute {
        public static final int NCRENDERING_ENABLED = 1;
        public static final int NCRENDERING_POLICY = 2;
        public static final int TRANSITIONS_FORCEDISABLED = 3;
        public static final int ALLOW_NCPAINT = 4;
        public static final int CAPTION_BUTTON_BOUNDS = 5;
        public static final int NONCLIENT_RTL_LAYOUT = 6;
        public static final int FORCE_ICONIC_REPRESENTATION = 7;
        public static final int FLIP3D_POLICY = 8;
        public static final int EXTENDED_FRAME_BOUNDS = 9;
        public static final int HAS_ICONIC_BITMAP = 10;
        public static final int DISALLOW_PEEK = 11;
        public static final int EXCLUDED_FROM_PEEK = 12;
        public static final int CLOAK = 13;
        public static final int CLOAKED = 14;
        public static final int FREEZE_REPRESENTATION = 15;
        public static final int PASSIVE_UPDATE_MODE = 16;
        public static final int USE_HOSTBACKDROPBRUSH = 17;
        // 3 are missing? lol
        /**
         * @since Windows 10 build 17763 / "Windows 10 October 2018 Update" / 1809
         */
        public static final int USE_IMMERSIVE_DARK_MODE_PRE_19041 = 19;
        /**
         * @since Windows 10 build 19041 / "Windows 10 May 2020 Update" / 2004
         */
        public static final int USE_IMMERSIVE_DARK_MODE = 20;
        public static final int WINDOW_CORNER_PREFERENCE = 33;
        public static final int BORDER_COLOR = 34;
        public static final int CAPTION_COLOR = 35;
        public static final int TEXT_COLOR = 36;
        public static final int VISIBLE_FRAME_BORDER_THICKNESS = 37;
        public static final int SYSTEMBACKDROP_TYPE = 38;
        public static final int REDIRECTIONBITMAP_ALPHA = 39;
        public static final int BORDER_MARGINS = 40;

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
}
