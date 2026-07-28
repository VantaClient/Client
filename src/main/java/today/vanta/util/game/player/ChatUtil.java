package today.vanta.util.game.player;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import today.vanta.util.game.Commons;

import java.util.regex.Matcher;

public class ChatUtil implements Commons {
    private static void addChatMessage(IChatComponent component, int line) {
        mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(component, line);
    }

    public static void send(Prefix prefix, String message, Object... args) {
        sendInternal(prefix, message, 1, args);
    }

    public static void sendNoLine(Prefix prefix, String message, Object... args) {
        sendInternal(prefix, message, 0, args);
    }

    public static void sendNoLineNoPrefix(String message, Object... args) {
        sendInternal(null, message, 0, args);
    }

    public static void sendNoPrefix(String message, Object... args) {
        sendInternal(null, message, 1, args);
    }

    public static void info(String message, Object... args) {
        send(Prefix.INFO, message, args);
    }

    public static void error(String message, Object... args) {
        send(Prefix.ERROR, message, args);
    }

    public static void warn(String message, Object... args) {
        send(Prefix.WARNING, message, args);
    }

    private static void sendInternal(Prefix prefix, String message, int line, Object... args) {
        if (message == null) return;

        for (Object arg : args) {
            message = message.replaceFirst("\\{}",
                    Matcher.quoteReplacement(String.valueOf(arg)));
        }

        if (prefix != null) {
            message = prefix.prefix + message;
        }

        message = message.replace('&', '§');

        addChatMessage(new ChatComponentText(message), line);
    }

    public enum Prefix {
        INFO("&dVanta &7:: &f"),
        ERROR("&cVanta &7:: &f"),
        WARNING("&eVanta &7:: &f");

        public final String prefix;

        Prefix(String prefix) {
            this.prefix = prefix;
        }
    }
}