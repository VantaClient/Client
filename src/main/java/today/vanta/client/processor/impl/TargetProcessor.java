package today.vanta.client.processor.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.ModuleDisableEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.impl.combat.AntiBot;
import today.vanta.client.module.impl.combat.KillAura;
import today.vanta.client.processor.Processor;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.game.world.EntityUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TargetProcessor extends Processor {
    public List<EntityLivingBase> list = new ArrayList<>();
    public List<EntityPlayer> playerlist = new ArrayList<>();
    public List<String> friends = new ArrayList<>(), bots = new ArrayList<>();
    public EntityLivingBase target;

    public KillAura killaura;
    public AntiBot antiBot;

    @Override
    public void onInitialize() {
        super.onInitialize();

        killaura = Vanta.instance.moduleStorage.getT(KillAura.class);
        antiBot = Vanta.instance.moduleStorage.getT(AntiBot.class);
    }

    @EventListen(priority = EventPriority.HIGHEST)
    private void onModuleDisable(ModuleDisableEvent event) {
        if (event.module.equals(killaura)) {
            target = null;
        }

        if (event.module.equals(antiBot)) {
            bots.clear();
        }
    }

    @EventListen(priority = EventPriority.HIGHEST)
    private void onUpdate(UpdateEvent event) {
        if (!shouldLook()) {
            list.clear();
            playerlist.clear();
            target = null;
            return;
        }

        list.clear();
        playerlist.clear();

        for (Entity entity : mc.theWorld.getLoadedEntityList()) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;

                if (EntityUtil.isValid(
                        living,
                        killaura.raytrace.getValue(),
                        killaura.searchRange.getValue().floatValue(),
                        killaura.entities)) {

                    list.add(living);
                }
            }

            if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                EntityPlayer player = (EntityPlayer) entity;

                if (mc.thePlayer.getDistanceToEntity(player) < killaura.attackRange.getValue().floatValue()) {
                    playerlist.add(player);
                }
            }
        }

        Comparator<EntityLivingBase> livingComparator =
                EntityUtil.getComparatorForSorting(killaura.sortMode.getValue());

        Comparator<EntityPlayer> playerComparator =
                EntityUtil.getComparatorForSorting(killaura.sortMode.getValue());

        list.sort(livingComparator);
        playerlist.sort(playerComparator);

        target = list.isEmpty() ? null : list.get(0);
    }

    public boolean shouldLook() {
        return killaura != null && killaura.isEnabled();
    }

    public static TargetProcessor getInstance() {
        return Vanta.instance.processorStorage.getT(TargetProcessor.class);
    }
}