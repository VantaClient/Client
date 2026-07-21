package today.vanta.util.os.osx.enhancements.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import today.vanta.util.os.ConfigurableEnhancement;
import today.vanta.util.os.osx.enhancements.MacOS;

@NotNullByDefault
public abstract class OSXConfigurableEnhancement<C> implements ConfigurableEnhancement<MacOS, C> {
    @Override
    public void apply(final MacOS os) {
        apply(os, null);
    }

    @Override
    public abstract void apply(final MacOS os, final @Nullable C config);
}
