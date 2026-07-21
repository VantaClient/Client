package today.vanta.util.os;

import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public interface CommonEnhancement<O extends OS> {
    boolean canApply(final O os);
    void apply(final O os);
}
