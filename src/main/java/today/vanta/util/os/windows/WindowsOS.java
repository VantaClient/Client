package today.vanta.util.os.windows;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import org.jetbrains.annotations.NotNullByDefault;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.Display;
import today.vanta.util.os.OS;
import lombok.Value;
import today.vanta.util.os.windows.natives.NtDll;

@NotNullByDefault
public final class WindowsOS implements OS {
    public static final WindowsOS INSTANCE = new WindowsOS();
    public final Version version;

    public WindowsOS() {
        final IntByReference majorVersion = new IntByReference();
        final IntByReference minorVersion = new IntByReference();
        final IntByReference buildNumber = new IntByReference();
        NtDll.INSTANCE.RtlGetNtVersionNumbers(
                majorVersion, minorVersion, buildNumber
        );
        version = new WindowsOS.Version(
                majorVersion.getValue(),
                minorVersion.getValue(),
                buildNumber.getValue() & ~0xF0000000
        );
    }

    @Value
    public static class Version implements Comparable<Version> {
        public int major;
        public int minor;
        public int build;

        /**
         * this major >= input major
         * @param major the required major version
         * @return (this) major >= (input) major
         */
        public boolean atLeastM(int major) {
            return this.major >= major;
        }

        /**
         * @param build the required build version
         * @return (this) build >= (input) build
         */
        public boolean atLeastB(int build) {
            return this.build >= build;
        }
        public boolean atLeast(int major, int minor) {
            return major() >= major && minor() >= minor;
        }
        public boolean atLeast(int major, int minor, int build) {
            return atLeast(major, minor) && this.build >= build;
        }
        @Override
        public int compareTo(Version o) {
            int result = Integer.compare(major, o.major);
            if (result != 0) return result;

            result = Integer.compare(minor, o.minor);
            if (result != 0) return result;

            return Integer.compare(build, o.build);
        }
    }

    public int major() {
        return this.version.major();
    }
    public int minor() {
        return this.version.minor();
    }
    public boolean atLeast(int major, int minor) {
        return this.version.atLeast(major, minor);
    }
    public boolean atLeast(int major, int minor, int build) {
        return this.version.atLeast(major, minor, build);
    }
    public int build() {
        return this.version.build();
    }

    public WinDef.HWND hwnd() {
        return new WinDef.HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(
                Display.getWindowHandle()
        )));
    }
}
