package today.vanta.client.module.impl.hud;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.shape.impl.Triangle;
import today.vanta.util.game.world.EntityUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Arrows extends Module {
    private final List<EntityPlayer> list = new ArrayList<>();

    public Arrows() {
        super("Arrows", "Triangles pointing to players.", Category.HUD);
        hideFromArraylist = true;
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        list.clear();
        mc.theWorld.getLoadedEntityList().stream()
                .filter(entity -> entity instanceof EntityPlayer && entity != mc.thePlayer && !entity.isDead)
                .map(entity -> (EntityPlayer) entity)
                .sorted(EntityUtil.getComparatorForSorting("Range"))
                .forEachOrdered(list::add);

        EntityPlayer target = list.isEmpty() ? null : list.get(0);

        if (target != null) {
            if (mc.gameSettings.thirdPersonView != 0) return;
            // yes this is vibecoded, I couldn't figure it out :sob:
            float centerX = event.scaledResolution.getScaledWidth() / 2f;
            float centerY = event.scaledResolution.getScaledHeight() / 2f;
            float radius = 60f;

            double dx = target.posX - mc.thePlayer.posX;
            double dz = target.posZ - mc.thePlayer.posZ;
            double angleToEntity = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
            double relativeYaw = MathHelper.wrapAngleTo180_double(angleToEntity - mc.thePlayer.rotationYaw);


            double rad = Math.toRadians(relativeYaw - 90);
            float posX = centerX + (float) (radius * Math.cos(rad));
            float posY = centerY + (float) (radius * Math.sin(rad));
            float width = 20;
            float height = 15;
            float outlineWidth = 22;
            float outlineHeight = 17;
            Triangle.create(posX - outlineWidth / 2 + 0.5f, posY - outlineWidth / 2 + 0.5f, outlineWidth, outlineHeight)
                            .color(Color.black)
                            .rotate((float) relativeYaw)
                            .push(event);
            Triangle.create(posX - width / 2, posY - width / 2, width, height)
                    .color(Vanta.instance.moduleStorage.getT(Theme.class).colors[0])
                    .rotate((float) relativeYaw) // adjust +90/-90 offset until the tip points the right way
                    .push(event);
        }

    }
}
