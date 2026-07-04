package today.vanta.util.os.windows;

import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class WindowsOS {
    public final Object display;
    public WindowsOS(Object display) {
        this.display = display;
    }
    public int majorVersion() {
        return WindowsEnhancements.majorVersion;
    }
    public int buildNumber() {
        return WindowsEnhancements.buildNumber;
    }
}
