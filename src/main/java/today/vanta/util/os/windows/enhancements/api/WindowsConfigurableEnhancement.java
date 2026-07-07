package today.vanta.util.os.windows.enhancements.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import today.vanta.util.os.ConfigurableEnhancement;
import today.vanta.util.os.windows.WindowsOS;

@NotNullByDefault
public abstract class WindowsConfigurableEnhancement<C> implements ConfigurableEnhancement<WindowsOS, C> {
    @Override
    public void apply(final WindowsOS os) {
        apply(os, null);
    }

    @Override
    public abstract void apply(final WindowsOS os, final @Nullable C config);
}
