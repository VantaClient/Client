package today.vanta.util.os.windows;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import org.jetbrains.annotations.NotNullByDefault;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.Display;
import today.vanta.util.os.OS;
import today.vanta.util.os.windows.enhancements.api.WindowsEnhancements;

@NotNullByDefault
public final class WindowsOS implements OS {
    public static final WindowsOS INSTANCE = new WindowsOS();
    @SuppressWarnings("unused") // shut
    public int majorVersion() {
        return WindowsEnhancements.majorVersion;
    }
    public int buildNumber() {
        return WindowsEnhancements.buildNumber;
    }

    public WinDef.HWND hwnd() {
        return new WinDef.HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(
                Display.getWindowHandle()
        )));
    }
}
