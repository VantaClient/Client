package today.vanta.util.os.windows.natives;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import today.vanta.util.os.windows.WindowsEnhancements;

public interface NtDll extends StdCallLibrary {

    /* NTDLL */
    NtDll INSTANCE = Native.load("ntdll", NtDll.class);

    void RtlGetNtVersionNumbers(
            IntByReference MajorVersion,
            IntByReference MinorVersion,
            IntByReference BuildNumber
    );

    static void getBuildNumber() {
        // Get Windows Info
        final IntByReference majorVersion = new IntByReference();
        final IntByReference buildNumber = new IntByReference();
        INSTANCE.RtlGetNtVersionNumbers(majorVersion, new IntByReference(), buildNumber);

        // Write Info
        WindowsEnhancements.majorVersion = majorVersion.getValue();
        WindowsEnhancements.buildNumber = (buildNumber.getValue() & ~0xF0000000);
    }
}
