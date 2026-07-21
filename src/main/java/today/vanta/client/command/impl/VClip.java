package today.vanta.client.command.impl;

import today.vanta.client.command.Command;
import today.vanta.util.game.player.ChatUtil;

public class VClip extends Command {
    public VClip() {
        super("VClip", "Clip vertically.");
        aliases = new String[]{"vclip", "vc"};
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            ChatUtil.error("Missing argument &c<blocks>");
            return;
        }

        String blocksString = args[0].toLowerCase();
        if (blocksString.isEmpty()) {
            ChatUtil.error("Module name can't be empty!");
            return;
        }

        int blocks = Integer.parseInt(blocksString);
        String direction = blocks > 0 ? "up" : blocks < 0 ? "down" : "";
        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + blocks, mc.thePlayer.posZ);
        send("Teleported you &e{}&r blocks {}!", Math.abs(blocks), direction);
    }

    @Override
    public String[] getArgs() {
        return new String[]{"vclip <blocks>"};
    }
}
