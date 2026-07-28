package today.vanta.client.module.impl.render;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.game.render.RenderNametagsEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.ProjectionUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Nametags extends Module {
    private final MultiStringSetting entities = Setting.of("Entities", new String[]{"Players"}, new String[]{"Players", "Monsters", "Animals", "Local", "Invisibles"});
    private final BooleanSetting
            belowPlayer = Setting.of("Below player", false),
            distance = Setting.of("Distance", true),
            health = Setting.of("Health", true),
            equipment = Setting.of("Equipment", false),
            background = Setting.of("Draw background", true),
            healthbar = Setting.of("Draw health", true);

    private final StringSetting colMode = Setting.of("Health color", "Gradient", "Health", "Gradient").hide(() -> !healthbar.getValue());

    private static final Color BACKGROUND = new Color(20, 20, 20, 200);

    public Nametags() {
        super("Nametags", "Renders better styled nametags than vanilla above entities.", Category.RENDER);
    }

    @EventListen
    private void onRenderNametags(RenderNametagsEvent event) {
        if (canRender(event.entity)) {
            event.cancelled = true;
        }
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        float ticks = event.partialTicks;
        ScaledResolution sr = event.scaledResolution;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            if (!canRender(entity)) continue;
            if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) continue;
            EntityLivingBase living = (EntityLivingBase) entity;
            boolean localPlayer = entity == mc.thePlayer;

            ProjectionUtil.ScreenBounds bounds = ProjectionUtil.projectBoundingBox(entity, ticks, sr);
            if (bounds == null) continue;

            float x = (float) bounds.minX;
            float y = (float) bounds.minY;
            float width = (float) (bounds.maxX - bounds.minX);
            float height = (float) (bounds.maxY - bounds.minY);
            if (width <= 0.0F || height <= 0.0F) continue;

            GlyphFontRenderer font = CFonts.SFPT_MEDIUM_18;

            float distance = mc.thePlayer.getDistanceToEntity(entity);
            float health = living.getHealth();

            String distanceText = (!localPlayer && this.distance.getValue())
                    ? String.format("%.0fm ", distance)
                    : "";

            String nameText = entity.getName();

            String healthText = this.health.getValue()
                    ? String.format(" %.0fhp", health)
                    : "";

            Color distanceColor;
            if (distance <= 10) {
                distanceColor = Color.GREEN;
            } else if (distance <= 30) {
                distanceColor = Color.YELLOW;
            } else if (distance <= 60) {
                distanceColor = Color.ORANGE;
            } else {
                distanceColor = Color.RED;
            }

            float maxHealth = living.getMaxHealth();
            float healthPercent = health / maxHealth;

            Color healthColor;
            if (healthPercent <= 0.25f) {
                healthColor = Color.RED;
            } else if (healthPercent <= 0.5f) {
                healthColor = Color.ORANGE;
            } else if (healthPercent <= 0.75f) {
                healthColor = Color.YELLOW;
            } else {
                healthColor = Color.GREEN;
            }

            Color gradcolor = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
            Color gradcolor2 = Vanta.instance.moduleStorage.getT(Theme.class).colors[1];

            float distanceWidth = font.getStringWidth(distanceText);
            float nameWidth = font.getStringWidth(nameText);
            float healthWidth = font.getStringWidth(healthText);

            float totalWidth = distanceWidth + nameWidth + healthWidth;

            float startX = x + width / 2f - totalWidth / 2f;
            float textY = y - font.getFontHeight() - 10;

            float idk = totalWidth + 2;
            float idkbar1 = (idk / 2) * (((EntityLivingBase) entity).getHealth() / ((EntityLivingBase) entity).getMaxHealth());
            float idkbar2 = (idk / 2) * (((EntityLivingBase) entity).getHealth() / ((EntityLivingBase) entity).getMaxHealth()) * -1;


            if (belowPlayer.getValue()) {
                textY = y + height + 5;
            }
            if (background.getValue()) {
                Rectangle
                        .create(startX, textY - 1, totalWidth + 2, 14)
                        .color(BACKGROUND)
                        .push(event);
            }

            if (healthbar.getValue()) {
                if (colMode.isValue("Gradient")) {
                    GradientRectangle
                            .create(startX + (idk / 2), textY + 12, idkbar1, 1)
                            .firstColor(gradcolor2)
                            .secondColor(gradcolor)
                            .push(event);
                    GradientRectangle
                            .create(startX + (idk / 2), textY + 12, idkbar2, 1)
                            .firstColor(gradcolor2)
                            .secondColor(gradcolor)
                            .push(event);
                } else {
                    Rectangle
                            .create(startX + (idk / 2), textY + 12, idkbar1, 1)
                            .color(healthColor)
                            .push(event);
                    Rectangle
                            .create(startX + (idk / 2), textY + 12, idkbar2, 1)
                            .color(healthColor)
                            .push(event);
                }
            }

            if (!distanceText.isEmpty()) {
                font.drawStringWithShadow(
                        distanceText,
                        startX,
                        textY,
                        distanceColor
                );
            }

            font.drawStringWithShadow(
                    nameText,
                    startX + distanceWidth,
                    textY,
                    Color.WHITE
            );

            if (!healthText.isEmpty()) {
                font.drawStringWithShadow(
                        healthText,
                        startX + distanceWidth + nameWidth,
                        textY,
                        healthColor
                );
            }

            if (equipment.getValue()) {
                List<ItemStack> items = new ArrayList<>();

                if (living.getHeldItem() != null)
                    items.add(living.getHeldItem());

                for (int i = 3; i >= 0; i--) {
                    ItemStack armor = living.getCurrentArmor(i);
                    if (armor != null)
                        items.add(armor);
                }

                if (!items.isEmpty()) {
                    float scale = 0.75f;
                    float itemSize = 15f * scale;
                    float spacing = 0;

                    float totalWidthItems = items.size() * itemSize +
                            (items.size() - 1) * spacing;

                    float itemX = x + width / 2f - totalWidthItems / 2f;
                    float itemY = belowPlayer.getValue()
                            ? textY + font.getFontHeight() + 5
                            : textY - itemSize - 2;

                    for (ItemStack stack : items) {
                        RenderUtil.renderScaledItem(stack, itemX, itemY, scale);
                        itemX += itemSize + spacing;
                    }
                }
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