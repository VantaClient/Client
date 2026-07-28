package today.vanta.storage.impl;

import today.vanta.client.command.Command;
import today.vanta.client.command.impl.*;
import today.vanta.client.processor.impl.ChatProcessor;
import today.vanta.storage.Storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandStorage extends Storage<Command> {
    public Command context;

    @Override
    public void subscribe() {
        super.subscribe();

        list.add(new Help());
        list.add(new Toggle());
        list.add(new Clear());
        list.add(new Bind());
        list.add(new Config());
        list.add(new Teleport());
        list.add(new VClip());

        this.context = null;
    }

    public Command getCommand(String input) {
        return this.list.stream()
                .filter(cmd ->
                        cmd.name.equalsIgnoreCase(input) ||
                                (cmd.aliases != null && Arrays.stream(cmd.aliases)
                                        .anyMatch(a -> a.equalsIgnoreCase(input)))
                )
                .findFirst()
                .orElse(null);
    }

    public String getInlineSuggestion(String input) {
        if (input == null || !input.startsWith(ChatProcessor.COMMAND_PREFIX)) {
            return null;
        }

        String withoutPrefix = input.substring(ChatProcessor.COMMAND_PREFIX.length());
        String[] parts = withoutPrefix.split("\\s+");
        String typed = parts[0].toLowerCase();

        if (typed.isEmpty()) {
            return null;
        }

        Command command = this.list.stream()
                .filter(cmd -> cmd.aliases != null &&
                        Arrays.stream(cmd.aliases).anyMatch(a -> a.toLowerCase().startsWith(typed)))
                .findFirst()
                .orElse(null);

        if (command == null) {
            return null;
        }

        String matchedAlias = Arrays.stream(command.aliases)
                .filter(a -> a.toLowerCase().startsWith(typed))
                .findFirst()
                .orElse(command.aliases[0]);
        String primary = matchedAlias.toLowerCase();
        String completion = primary.substring(typed.length());

        String[] args = command.getArgs();
        if (args == null || args.length == 0) {
            return completion.isEmpty() ? null : completion;
        }

        String usage = args[0];
        for (String arg : args) {
            String[] argParts = arg.split("\\s+");
            if (argParts.length > 0 && argParts[0].toLowerCase().equals(primary)) {
                usage = arg;
                break;
            }
        }

        String[] usageParts = usage.split("\\s+");
        if (usageParts.length <= 1) {
            return completion.isEmpty() ? null : completion;
        }

        int typedArgCount = Math.max(0, parts.length - 1);
        int usageArgCount = usageParts.length - 1;
        int remainingStart = 1 + Math.min(typedArgCount, usageArgCount);

        if (remainingStart < usageParts.length) {
            completion += " " + String.join(" ", Arrays.copyOfRange(usageParts, remainingStart, usageParts.length));
        }

        return completion.isEmpty() ? null : completion;
    }

    public List<String> getMatchingCommands(String input) {
        List<String> result = new ArrayList<>();

        if (input == null || !input.startsWith(ChatProcessor.COMMAND_PREFIX)) {
            return result;
        }

        String withoutPrefix = input.substring(ChatProcessor.COMMAND_PREFIX.length());
        String[] parts = withoutPrefix.split("\\s+");
        String typed = parts[0].toLowerCase();

        if (typed.isEmpty()) {
            return result;
        }

        return this.list.stream()
                .filter(cmd -> cmd.aliases != null &&
                        Arrays.stream(cmd.aliases).anyMatch(a -> a.toLowerCase().startsWith(typed)))
                .map(cmd -> {
                    String matchedAlias = Arrays.stream(cmd.aliases)
                            .filter(a -> a.toLowerCase().startsWith(typed))
                            .findFirst()
                            .orElse(cmd.aliases[0]);
                    String primary = matchedAlias.toLowerCase();
                    String suggestion = ChatProcessor.COMMAND_PREFIX + primary;
                    String[] args = cmd.getArgs();
                    if (args != null && args.length > 0) {
                        String usage = args[0];
                        for (String arg : args) {
                            String[] argParts = arg.split("\\s+");
                            if (argParts.length > 0 && argParts[0].toLowerCase().equals(primary)) {
                                usage = arg;
                                break;
                            }
                        }
                        String[] usageParts = usage.split("\\s+");
                        if (usageParts.length > 1) {
                            suggestion += " " + String.join(" ", Arrays.copyOfRange(usageParts, 1, usageParts.length));
                        }
                    }
                    return suggestion;
                })
                .collect(Collectors.toList());
    }
}
