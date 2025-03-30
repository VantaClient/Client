package today.vanta.util.game.events.exception;

public class EventCallException extends RuntimeException {
    public EventCallException() {
    }

    public EventCallException(String message) {
        super(message);
    }

    public EventCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
