package today.vanta.client.module.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.RunTickEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;

public class AntiBot extends Module {
    private final StringSetting mode = StringSetting.builder()
            .name("Mode")
            .value("Basic")
            .values("Basic")
            .build();

    public AntiBot() {
        super("AntiBot", "Prevents attacking bots.", Category.COMBAT);
    }

    @EventListen
    private void onRunTick(RunTickEvent event) {
        if (mc.theWorld == null)
            return;

        switch (mode.getValue()) {
            case "Basic":
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (!(entity instanceof EntityPlayer)) continue;

                    EntityPlayer player = (EntityPlayer) entity;

                    if (player.getDisplayName().getUnformattedText().toLowerCase().contains("[npc]") ||
                            player.getDisplayName().getUnformattedText().toLowerCase().contains("[bot]")) {
                        Vanta.instance.processorStorage.getT(TargetProcessor.class).bots.add(player.getDisplayName().getUnformattedText());
                    }
                }
                break;
        }
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}
