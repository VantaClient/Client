package today.vanta.util.os.windows;

import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public final class WindowsEnhancements {
    public static final int MINIMUM_BUILD_NUM = 22000;
    public static final int BACKDROP_BUILD_NUM = 22621;

    public static int majorVersion = Integer.MIN_VALUE;
    public static int buildNumber = Integer.MIN_VALUE;

    public static void apply() {

    }
}
