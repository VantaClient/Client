package today.vanta.util.os;

import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public interface CommonEnhancement<T extends OS<D>, D> {
    boolean shouldApply(T os);
    void apply(T OS);
}
