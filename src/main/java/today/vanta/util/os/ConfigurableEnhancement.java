package today.vanta.util.os;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

@NotNullByDefault
public interface ConfigurableEnhancement<O extends OS, C> extends CommonEnhancement<O> {
    void apply(final O os, final @Nullable C config);
}
