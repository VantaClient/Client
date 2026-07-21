package today.vanta.client.module.impl.client;

import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.RunTickEvent;
import today.vanta.client.event.impl.game.network.ReceivePacketEvent;
import today.vanta.client.event.impl.game.player.ChangeWorldEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.movement.Fly;
import today.vanta.client.module.impl.movement.Speed;
import today.vanta.client.module.impl.player.Scaffold;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.system.math.Counter;

public class AutoDisable extends Module {
    private final MultiStringSetting modules = Setting.of("Modules", new String[]{"Scaffold", "KillAura"}, new String[]{"Scaffold", "KillAura", "Speed", "Fly"});

    private final BooleanSetting
            onDeath = Setting.of("On death", true),
            onWorldChange = Setting.of("On world change", true),
            onTeleport = Setting.of("On teleport (flag)", false),
            warn = Setting.of("Warn", true);

    private final Counter disableCooldown = new Counter();

    public AutoDisable() {
        super("AutoDisable", "Automatically disables modules in emergencies.", Category.CLIENT);
        hideFromArraylist = true;
    }

    @EventListen
    private void onRunTick(RunTickEvent event) {
        if (mc.thePlayer == null) return;

        if (!mc.thePlayer.isEntityAlive() && onDeath.getValue()) {
            tryDisableModules(Reason.DEATH);
        }
    }

    @EventListen
    private void onReceivePacket(ReceivePacketEvent event) {
        if (mc.thePlayer == null) return;

        if (event.packet instanceof S08PacketPlayerPosLook && onTeleport.getValue()) {
            tryDisableModules(Reason.FLAG);
        }
    }

    @EventListen
    private void onChangeWorld(ChangeWorldEvent event) {
        if (onWorldChange.getValue()) {
            tryDisableModules(Reason.WORLD);
        }
    }

    private void tryDisableModules(Reason reason) {
        if (!disableCooldown.hasElapsed(2500, true)) {
            return;
        }

        disableModules();

        if (warn.getValue()) {
            switch (reason) {
                case FLAG:
                    ChatUtil.warn("Disabled modules because you got flagged!");
                    break;
                case DEATH:
                    ChatUtil.warn("Disabled modules because you died!");
                    break;
                case WORLD:
                    ChatUtil.warn("Disabled modules because you changed worlds!");
                    break;
            }
        }
    }

    private void disableModules() {
        if (modules.isEnabled("Scaffold")) {
            Vanta.instance.moduleStorage.getT(Scaffold.class).setEnabled(false);
        }

        if (modules.isEnabled("KillAura")) {
            TargetProcessor.getInstance().killaura.setEnabled(false);
        }

        if (modules.isEnabled("Speed")) {
            Vanta.instance.moduleStorage.getT(Speed.class).setEnabled(false);
        }

        if (modules.isEnabled("Fly")) {
            Vanta.instance.moduleStorage.getT(Fly.class).setEnabled(false);
        }
    }

    private enum Reason {
        DEATH, WORLD, FLAG
    }
}