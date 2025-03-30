package today.vanta.util.game.world;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import today.vanta.Vanta;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.util.game.IMinecraft;
import today.vanta.util.system.math.ColorUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EntityUtil implements IMinecraft {
    private static final Map<String, Float> skinCache = new ConcurrentHashMap<>();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);


    public static Comparator<EntityLivingBase> getComparatorForSorting(String mode) {
        switch (mode) {
            case "Health":
                return Comparator.comparingDouble(EntityLivingBase::getHealth);

            case "Armor":
                return Comparator.comparingDouble(EntityLivingBase::getTotalArmorValue);

            case "Hurt-time":
                return Comparator.comparingDouble(EntityLivingBase::getHurtTime);

            case "Ticks":
                return Comparator.comparingDouble(EntityLivingBase::getTicksExisted);

            case "Skin color":
                return Comparator.comparingDouble(EntityUtil::getDarkPixelPercentage);

            default:
                return Comparator.comparingDouble(e -> e.getDistanceToEntity(mc.thePlayer));
        }
    }

    public static float getDarkPixelPercentage(EntityLivingBase entity) {
        if (!(entity instanceof AbstractClientPlayer)) {
            return 0.0f;
        }

        AbstractClientPlayer player = (AbstractClientPlayer) entity;
        String uuid = player.getUniqueID().toString().replaceAll("-", "");

        if (skinCache.containsKey(uuid)) {
            return skinCache.get(uuid);
        }

        executorService.execute(() -> {
            float percentage = analyzeSkin(uuid);
            skinCache.put(uuid, percentage);
        });

        return 0.0f;
    }

    private static float analyzeSkin(String uuid) {
        BufferedImage skinImage = downloadSkinImage(uuid);
        return (skinImage != null) ? ColorUtil.calculateDarkPixelRatio(skinImage) : 0.0f;
    }

    public static boolean isValid(EntityLivingBase entity, boolean raytrace, float searchRange, MultiStringSetting entities) {
        if (entity == mc.thePlayer ||
                entity.isDead ||
                !entity.isEntityAlive() ||
                entity.getName().isEmpty() ||
                entity.isInvisible() ||
                (raytrace && !mc.thePlayer.canEntityBeSeen(entity)) ||
                Vanta.instance.processorStorage.getT(TargetProcessor.class).friends.contains(entity.getDisplayName().getUnformattedText()) ||
                Vanta.instance.processorStorage.getT(TargetProcessor.class).bots.contains(entity.getDisplayName().getUnformattedText()) ||
                entity.getDistanceToEntity(mc.thePlayer) > searchRange) {
            return false;
        }

        boolean validType = false;

        if (entity instanceof EntityPlayer && entities.isEnabled("Players")) {
            validType = true;
        }

        if ((entity instanceof EntityAnimal || entity instanceof EntityVillager) && entities.isEnabled("Animals")) {
            validType = true;
        }

        if (entity instanceof EntityMob && entities.isEnabled("Monsters")) {
            validType = true;
        }

        return validType;
    }

    public static BufferedImage downloadSkinImage(String uuid) {
        try {
            String skinUrl = "https://crafatar.com/skins/" + uuid;
            return ImageIO.read(new URL(skinUrl));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
