package today.vanta.util.os.windows.natives;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import today.vanta.util.os.windows.WindowsOS;
import today.vanta.util.os.windows.enhancements.api.WindowsEnhancements;

public interface NtDll extends StdCallLibrary {

    /* NTDLL */
    NtDll INSTANCE = Native.load("ntdll", NtDll.class);

    void RtlGetNtVersionNumbers(
            IntByReference MajorVersion,
            IntByReference MinorVersion,
            IntByReference BuildNumber
    );

    static void setBuildInfo() {
    }
}
