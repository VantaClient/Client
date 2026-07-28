package today.vanta.client.command;

import today.vanta.Vanta;
import today.vanta.util.game.Commons;

public abstract class Command implements Commons {
    public String name, description;

    public String[] aliases;

    public Command(String name, String description) {
        if (!description.endsWith(".")) {
            throw new IllegalArgumentException("No description or description missing period at the end!");
        }

        this.name = name;
        this.description = description;
        this.aliases = new String[]{name.toLowerCase()};

        Vanta.instance.commandStorage.context = this;
    }

    public abstract void execute(String[] args);

    public String[] getArgs() {
        return null;
    }
}