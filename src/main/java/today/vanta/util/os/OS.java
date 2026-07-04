package today.vanta.util.os;

public interface OS<T> {
    @SuppressWarnings("unchecked")
    default T display() {
        return (T)LWJGLInternals.displayImpl();
    }
}
