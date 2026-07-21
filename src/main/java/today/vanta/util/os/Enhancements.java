package today.vanta.util.os;

import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import today.vanta.util.os.osx.enhancements.api.OSXEnhancements;
import today.vanta.util.os.windows.enhancements.api.WindowsEnhancements;

@NotNullByDefault
public final class Enhancements {
    private static final @Nullable Runnable applyFn = makeApplyFn();
    private static @Nullable Runnable makeApplyFn() {
        switch (Util.getOSType()) {
            case WINDOWS:
                return WindowsEnhancements::apply;
            case OSX:
                return OSXEnhancements::apply;
            case LINUX:
            case UNKNOWN:
            case SOLARIS:
        }
        return null;
    }

    public static boolean supportsWindowBlur() {
        return Util.getOSType() == Util.EnumOS.WINDOWS;
    }

    public static void apply() {
        if (applyFn != null) applyFn.run();
    }
}
