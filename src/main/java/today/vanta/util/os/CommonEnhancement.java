package today.vanta.util.os;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

@NotNullByDefault
public interface CommonEnhancement<O extends OS<D>, D, C> {
    boolean canApply(final O os);
    void apply(final O OS, final @Nullable C config);
}
