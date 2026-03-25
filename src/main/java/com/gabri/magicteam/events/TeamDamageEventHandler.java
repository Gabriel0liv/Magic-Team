package com.gabri.magicteam.events;

import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TeamDamageEventHandler {

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource() instanceof SpellDamageSource spellSource) {
            Entity attacker = spellSource.getEntity();
            Entity target = event.getEntity();

            if (TeamUtils.areAllies(attacker, target)) {
                // BLOCK DAMAGE
                event.setCanceled(true);
                
                // Visual Feedback
                TeamUtils.sendBlockedMessage(attacker);
            }
        }
    }
}
