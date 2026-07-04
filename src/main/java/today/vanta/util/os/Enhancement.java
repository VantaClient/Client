package today.vanta.util.os;

import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public interface Enhancement<T> {
    void apply(T OS);
}
