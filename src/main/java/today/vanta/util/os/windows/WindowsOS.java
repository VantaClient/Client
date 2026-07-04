package today.vanta.util.os.windows;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import org.jetbrains.annotations.NotNullByDefault;
import today.vanta.util.os.OS;
import today.vanta.util.os.windows.enhancements.api.WindowsEnhancements;

import java.lang.reflect.Field;

@NotNullByDefault
public final class WindowsOS implements OS<Object> {
    public static final WindowsOS INSTANCE = new WindowsOS();
    public int majorVersion() {
        return WindowsEnhancements.majorVersion;
    }
    public int buildNumber() {
        return WindowsEnhancements.buildNumber;
    }

    public WinDef.HWND hwnd() {
        final Object impl = display();
        final Class<? extends Object> c = impl.getClass();
        final Field hwnd;
        try {
            hwnd = c.getDeclaredField("hwnd");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        hwnd.setAccessible(true);
        try {
            return new WinDef.HWND(new Pointer(hwnd.getLong(impl)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
