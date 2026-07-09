package today.vanta.client.module.impl.render;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.ImageUtil;
import today.vanta.util.game.render.ProjectionUtil;
import today.vanta.util.game.render.shape.impl.ImageRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;

public class ESP extends Module {
    private final MultiStringSetting entities = Setting.of("Entities", new String[]{"Players"}, new String[]{"Players", "Monsters", "Animals", "Local", "Invisibles"});
    private final StringSetting image = Setting.of("Image", "None","ermwhat", "longboy", "silly", "cousin", "None");

    public ESP() {
        super("ESP", "Extra-sensory perception.", Category.RENDER);
        displayNames = new String[]{"ESP", "ChildESP"};
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        float ticks = event.partialTicks;
        ScaledResolution sr = event.scaledResolution;
        Color color = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            if (!canRender(entity)) continue;
            if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) continue;

            ProjectionUtil.ScreenBounds bounds = ProjectionUtil.projectBoundingBox(entity, ticks, sr);
            if (bounds == null) continue;

            float x = (float) bounds.minX;
            float y = (float) bounds.minY;
            float width = (float) (bounds.maxX - bounds.minX);
            float height = (float) (bounds.maxY - bounds.minY);
            if (width <= 0.0F || height <= 0.0F) continue;

            Rectangle
                    .create(x - 0.5f, y - 0.5f, width + 1, height + 1)
                    .outline(true)
                    .color(Color.BLACK)
                    .outlineWidth(1.0f)
                    .push(event);

            Rectangle
                    .create(x, y, width, height)
                    .outline(true)
                    .color(color)
                    .outlineWidth(1.0f)
                    .push(event);

            Rectangle
                    .create(x + 0.5f, y + 0.5f, width - 1, height - 1)
                    .outline(true)
                    .color(Color.BLACK)
                    .outlineWidth(1.0f)
                    .push(event);

            if (!image.isValue("None")) {
                String texture = image.getValue() +".png";
                ImageRectangle
                        .create(x + 2,y + 2,width - 4,height - 4, -1)
                        .resource(ImageUtil.getTexture(texture))
                        .push(event);
            }
        }
    }

    private boolean canRender(Entity living) {
        if (living == mc.thePlayer && entities.isEnabled("Local")) return true;
        if (living instanceof EntityAnimal && entities.isEnabled("Animals")) return true;
        if (living instanceof IMob && entities.isEnabled("Monsters")) return true;
        if (living.isInvisible() && entities.isEnabled("Invisibles")) return true;
        if (living instanceof EntityPlayer && entities.isEnabled("Player")) return true;
        if (TargetProcessor.getInstance().bots.contains(living.getName())) return false;
        return false;
    }
}