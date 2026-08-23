package today.vanta.client.module.impl.client;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.player.EntityPlayer;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;

public class Test extends Module {
    private TargetProcessor targetProcessor = TargetProcessor.getInstance();
    private final NumberSetting
            x = Setting.of("X position", 20, 0, 2000),
            y = Setting.of("Y position", 20, 0, 2000);

    public Test() {
        super("Test", "Test module for developers.", Category.CLIENT);
        hideFromArraylist = true;
    }


    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        float xAddition = 0;
        float yAddition = 0;
        float width = 135f;
        float height = 26f;
        float count = 0;
        float xaddValue =  width + 2;
        float totalWidth = xaddValue * count;

        for (int i = 0; i < targetProcessor.playerlist.size(); i++) {
            EntityPlayer entity = targetProcessor.playerlist.get(i);
            xAddition = xaddValue * i;
            if (count == 4) {
                return;
            } else {
                count++;
            }

            Rectangle
                    .create(x.getValue().floatValue() + xAddition, y.getValue().floatValue() + yAddition, width, height)
                    .color(new Color(10,10,10,150))
                    .push(event);
            float xDraw = x.getValue().floatValue() + xAddition;
            float yDraw = y.getValue().floatValue();
            RenderUtil.renderHead(event,entity,xDraw + 2, yDraw + 2, 17);
            float totalBarWidth = width - 4;
            float barWidth = totalBarWidth * (entity.getHealth() / entity.getMaxHealth());
            Rectangle.create(xDraw + 2, yDraw  + 21, totalBarWidth, 3f)
                    .color(new Color(10,10,10,255))
                    .push(event);
            Rectangle.create(xDraw + 2, yDraw  + 21, barWidth, 3f)
                    .color(Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0])
                    .push(event);
            CFonts.SFPT_REGULAR_18.drawStringWithShadow(entity.getName(), xDraw + 19,yDraw + 1,Color.white);
            float healthper = (entity.getHealth() / entity.getMaxHealth()) * 100;
            String healthperStr = String.format("%.1f", healthper) + "%";
            CFonts.getFont("SFPT-Regular", 16).drawStringWithShadow(healthperStr, xDraw + 19,yDraw + 10,Color.white);
            float length = CFonts.getFont("SFPT-Regular", 16).getStringWidth("#" + (i + 1));
            CFonts.getFont("SFPT-Regular", 16).drawStringWithShadow("#" + (i + 1), xDraw + width - length - 2,yDraw + 1,new Color(180,180,180,255));
            yAddition = 0;
        }
    }
}