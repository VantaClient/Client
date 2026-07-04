package today.vanta.util.os.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNullByDefault;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import today.vanta.Vanta;
import today.vanta.util.os.LWJGLImpl;

/**
 * Contains JNA stuff for setting the title bar to dark mode
 */
@NotNullByDefault
public final class DarkTitleBar {
    private static long getHWND() {
        final Object impl = LWJGLImpl.getDisplayImpl(); // WindowsDisplay
        final Class<? extends Object> c = impl.getClass();
        final Field hwnd;
        try {
            hwnd = c.getDeclaredField("hwnd");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        hwnd.setAccessible(true);
        try {
            return hwnd.getLong(impl);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public static void doIt() {
        Vanta.instance.logger.info("doing dark mode thing");
        final long hwnd = getHWND();
        Vanta.instance.logger.info("hwnd {}", hwnd);
        final WinNT.HRESULT hr = DwmAPI.INSTANCE.DwmSetWindowAttribute(
                new WinDef.HWND(new Pointer(hwnd)),
                DwmAPI.DwmWindowAttribute.DWMWA_USE_IMMERSIVE_DARK_MODE,
                new WinDef.BOOLByReference(new WinDef.BOOL(true)),
                DwmAPI.INT_SIZE
        );
        Vanta.instance.logger.info("dark mode HRESULT: {}", hr);
    }

    interface DwmAPI extends StdCallLibrary {
        DwmAPI INSTANCE = Native.load("dwmapi", DwmAPI.class);

        int INT_SIZE = 4;

        final class DwmWindowAttribute {
            private static final int DWMWA_NCRENDERING_ENABLED = 1;
            private static final int DWMWA_NCRENDERING_POLICY = 2;
            private static final int DWMWA_TRANSITIONS_FORCEDISABLED = 3;
            private static final int DWMWA_ALLOW_NCPAINT = 4;
            private static final int DWMWA_CAPTION_BUTTON_BOUNDS = 5;
            private static final int DWMWA_NONCLIENT_RTL_LAYOUT = 6;
            private static final int DWMWA_FORCE_ICONIC_REPRESENTATION = 7;
            private static final int DWMWA_FLIP3D_POLICY = 8;
            private static final int DWMWA_EXTENDED_FRAME_BOUNDS = 9;
            private static final int DWMWA_HAS_ICONIC_BITMAP = 10;
            private static final int DWMWA_DISALLOW_PEEK = 11;
            private static final int DWMWA_EXCLUDED_FROM_PEEK = 12;
            private static final int DWMWA_CLOAK = 13;
            private static final int DWMWA_CLOAKED = 14;
            private static final int DWMWA_FREEZE_REPRESENTATION = 15;
            private static final int DWMWA_PASSIVE_UPDATE_MODE = 16;
            private static final int DWMWA_USE_HOSTBACKDROPBRUSH = 17;
            // 3 are missing? lol
            private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
            private static final int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
            private static final int DWMWA_BORDER_COLOR = 34;
            private static final int DWMWA_CAPTION_COLOR = 35;
            private static final int DWMWA_TEXT_COLOR = 36;
            private static final int DWMWA_VISIBLE_FRAME_BORDER_THICKNESS = 37;
            private static final int DWMWA_SYSTEMBACKDROP_TYPE = 38;
            private static final int DWMWA_REDIRECTIONBITMAP_ALPHA = 39;
            private static final int DWMWA_BORDER_MARGINS = 40;
        }
        //https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmsetwindowattribute
        WinNT.HRESULT DwmSetWindowAttribute(
                WinDef.HWND hwnd,
                @MagicConstant(flagsFromClass = DwmWindowAttribute.class) int flag,
                WinDef.BOOLByReference attribute,
                int size
        );
    }
}
