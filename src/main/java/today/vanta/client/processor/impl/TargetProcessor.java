package today.vanta.client.processor.impl;

import net.minecraft.entity.EntityLivingBase;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.ModuleDisableEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.impl.combat.AntiBot;
import today.vanta.client.module.impl.combat.KillAura;
import today.vanta.client.module.impl.player.Scaffold;
import today.vanta.client.processor.Processor;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.game.world.BlockCache;
import today.vanta.util.game.world.EntityUtil;

import java.util.ArrayList;
import java.util.List;

public class TargetProcessor extends Processor {

    public List<EntityLivingBase> list = new ArrayList<>();
    public List<String> friends = new ArrayList<>(), bots = new ArrayList<>();
    public EntityLivingBase target;
    public BlockCache cache;

    private KillAura killaura;
    private Scaffold scaffold;
    private AntiBot antiBot;

    @Override
    public void onInitialize() {
        super.onInitialize();

        killaura = Vanta.instance.moduleStorage.getT(KillAura.class);
        scaffold = Vanta.instance.moduleStorage.getT(Scaffold.class);
        antiBot = Vanta.instance.moduleStorage.getT(AntiBot.class);
    }

    @EventListen(priority = EventPriority.HIGHEST)
    private void onModuleDisable(ModuleDisableEvent event) {
        if (event.module.equals(killaura)) {
            target = null;
        }

        if (event.module.equals(scaffold)) {
            cache = null;
        }

        if (event.module.equals(antiBot)) {
            bots.clear();
        }
    }

    @EventListen(priority = EventPriority.HIGHEST)
    private void onUpdate(UpdateEvent event) {
        if (shouldLook()) {
            list.clear();

            mc.theWorld.getLoadedEntityList().stream()
                    .filter(e -> e instanceof EntityLivingBase)
                    .map(e -> (EntityLivingBase) e)
                    .filter(e -> EntityUtil.isValid(e, killaura.raytrace.getValue(), killaura.searchRange.getValue().floatValue(), killaura.entities))
                    .sorted(EntityUtil.getComparatorForSorting(killaura.sortMode.getValue()))
                    .forEachOrdered(list::add);

            target = list.isEmpty() ? null : list.get(0);
        }
    }

    public boolean shouldLook() {
        return killaura != null && killaura.isEnabled();
    }
}
