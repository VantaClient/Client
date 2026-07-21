package today.vanta.util.os.osx.enhancements.api;

import org.jetbrains.annotations.NotNullByDefault;
import today.vanta.util.os.CommonEnhancement;
import today.vanta.util.os.osx.enhancements.MacOS;

import java.util.Collections;
import java.util.List;

@NotNullByDefault
public final class OSXEnhancements {
    private static final List<CommonEnhancement<MacOS>> PRESETS = Collections.emptyList();

    public static void apply() {
        for (CommonEnhancement<MacOS> enhancement : PRESETS) {
            if (enhancement.canApply(MacOS.INSTANCE)) {
                enhancement.apply(MacOS.INSTANCE);
            }
        }
    }
}
