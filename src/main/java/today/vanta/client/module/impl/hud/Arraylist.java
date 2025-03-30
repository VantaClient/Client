package today.vanta.client.module.impl.hud;

import today.vanta.Vanta;
import today.vanta.client.event.impl.game.render.Render2DEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFontRenderer;
import today.vanta.util.game.render.font.CFonts;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Arraylist extends Module {
    private final StringSetting fontStyle = StringSetting.builder()
            .name("Font style")
            .value("Medium")
            .values("Light", "Italic", "Medium", "Semibold", "Bold", "Heavy")
            .listener((setting, oldValue, newValue) -> setFont())
            .build();

    private final NumberSetting fontSize = NumberSetting.builder()
            .name("Font size")
            .value(24)
            .min(10)
            .max(42)
            .places(0)
            .listener((setting, oldValue, newValue) -> setFont())
            .build();

    private final BooleanSetting
    fontShadow = BooleanSetting.builder()
            .name("Font shadow")
            .value(true)
            .build(),

    suffixes = BooleanSetting.builder()
            .name("Show suffixes")
            .value(true)
            .build(),

    background = BooleanSetting.builder()
            .name("Background")
            .value(true)
            .build();

    private final NumberSetting backgroundAlpha = NumberSetting.builder()
            .name("Background alpha")
            .value(100)
            .min(0)
            .max(255)
            .places(0)
            .build()
            .hide(() -> !background.getValue());


    public Arraylist() {
        super("Arraylist", "Draws an arraylist of modules.", Category.HUD);
        displayNames = new String[] {"Arraylist", "ArrayList", "Array List", "ModuleList", "Module List"};
        hideFromArraylist = true;
        setEnabled(true);
    }

    private CFontRenderer font = CFonts.SFPT_MEDIUM_24;

    private void setFont() {
        font = CFonts.getFont("SFPT-" + fontStyle.getValue(), fontSize.getValue().intValue());
    }

    @EventListen(priority = EventPriority.LOWEST)
    public void onRender(Render2DEvent event) {
        List<Module> modules = Vanta.instance.moduleStorage.list.stream().
                filter(m -> {
                    boolean add = m.isEnabled();

                    if (m.hideFromArraylist) {
                        add = false;
                    }

                    return add;
                }).sorted(Comparator.comparingDouble(m -> {
                    Module module = (Module) m;
                    String moduleName = module.displayName;

                    if (module.getSuffix() != null && module.addSuffix && suffixes.getValue()) {
                        moduleName += " " + module.getSuffix();
                    }

                    return font.getStringWidth(moduleName);
                }).reversed()).collect(Collectors.toList());

        float y = 3;
        for (Module module : modules) {
            String moduleName = module.displayName;

            if (module.getSuffix() != null && module.addSuffix && suffixes.getValue()) {
                moduleName += " §f" + module.getSuffix();
            }

            float scaleFactor = (float) Math.sqrt(Math.min(1.2F, (float) font.getStringWidth(moduleName) / 63.0F));
            float x = event.scaledResolution.getScaledWidth() - font.getStringWidth(moduleName) - 5;

            if (background.getValue()) {
                RenderUtil.rectangle(x - 2, y, font.getStringWidth(moduleName) + 4, font.getFontHeight() + 4, new Color(0, 0, 0, backgroundAlpha.getValue().intValue()));
            }

            if (fontShadow.getValue()) {
                font.drawStringWithShadow(moduleName, x, y + 0.7, Vanta.instance.moduleStorage.getT(Theme.class).colors[0]);
            } else {
                font.drawString(moduleName, x, y + 0.7f, Vanta.instance.moduleStorage.getT(Theme.class).colors[0]);
            }

            y += font.getFontHeight() + (background.getValue() ? 2 : 0) + 2;
        }
    }
}
