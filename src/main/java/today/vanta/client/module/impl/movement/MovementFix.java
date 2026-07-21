package today.vanta.client.module.impl.movement;

import today.vanta.Vanta;
import today.vanta.client.event.impl.game.player.JumpEvent;
import today.vanta.client.event.impl.game.player.MoveFlyingEvent;
import today.vanta.client.event.impl.game.player.MoveInputEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.player.Scaffold;
import today.vanta.client.processor.impl.RotationProcessor;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.game.player.constructors.Rotation;

public class MovementFix extends Module {
    private final MultiStringSetting exemptions = Setting.of("Exemptions", new String[]{}, new String[]{"KillAura", "Scaffold"});

    public MovementFix() {
        super("MovementFix", "Fixes your movement when in combat.", Category.MOVEMENT);
        displayNames = new String[]{"MovementFix", "MovementCorrection", "CorrectMovement", "MoveFix"};
    }

    @EventListen
    private void onMoveInput(MoveInputEvent event) {
        if (getRotations() == null) return;
        if (isExempted()) return;
        MovementUtil.correctMovement(event, getRotations().yaw);
    }

    @EventListen
    private void onMoveFlying(MoveFlyingEvent event) {
        if (getRotations() == null) return;
        if (isExempted()) return;
        event.yaw = getRotations().yaw;
    }

    @EventListen
    private void onJump(JumpEvent event) {
        if (getRotations() == null) return;
        if (isExempted()) return;
        event.yaw = getRotations().yaw;
    }

    private Rotation getRotations() {
        RotationProcessor processor = RotationProcessor.getInstance();
        if (!processor.isActive()) {
            return null;
        }
        return processor.getCurrentRotation();
    }

    private boolean isExempted() {
        boolean scaffoldEnabled = exemptions.isEnabled("Scaffold")
                && Vanta.instance.moduleStorage.getT(Scaffold.class).isEnabled();

        boolean killAuraEnabled = exemptions.isEnabled("KillAura")
                && TargetProcessor.getInstance().killaura.isEnabled();

        if (scaffoldEnabled) {
            return true;
        }

        return killAuraEnabled;
    }
}