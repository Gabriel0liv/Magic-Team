package com.gabri.magicteam.mixin.compat.familiars;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Alshanex's Familiars attaches retaliation side effects to LivingDamageEvent.
 *
 * <p>Normally Magic Team has already rejected protected magic damage before this
 * event can fire. These guards intentionally repeat the policy at the actual
 * retaliation sinks so another damage source/mod cannot make a protected ally
 * receive Scorcher fire or a Plague retaliation potion.</p>
 */
@Pseudo
@Mixin(targets = "net.alshanex.alshanex_familiars.event.ServerEvents", remap = false)
public abstract class ServerEventsRetaliationFriendlyFireMixin {

    @Redirect(
            method = "onDamageTaken(Lnet/minecraftforge/event/entity/living/LivingDamageEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_7311_(I)V"
            ),
            remap = false,
            require = 1
    )
    private static void magicTeam$guardScorcherRetaliation(LivingEntity retaliationTarget,
                                                            int fireTicks,
                                                            LivingDamageEvent event) {
        if (magicTeam$canRetaliate(event, retaliationTarget)) {
            retaliationTarget.setRemainingFireTicks(fireTicks);
        }
    }

    @Redirect(
            method = "onDamageTaken(Lnet/minecraftforge/event/entity/living/LivingDamageEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            remap = false,
            require = 1
    )
    private static boolean magicTeam$guardPlagueRetaliation(Level level,
                                                             Entity projectile,
                                                             LivingDamageEvent event) {
        Entity attacker = magicTeam$getAttacker(event);
        if (attacker != null && !magicTeam$canRetaliate(event, attacker)) {
            return false;
        }
        return level.addFreshEntity(projectile);
    }

    private static boolean magicTeam$canRetaliate(LivingDamageEvent event, Entity retaliationTarget) {
        if (event == null || retaliationTarget == null) {
            return true;
        }

        LivingEntity retaliator = event.getEntity();
        return retaliator == null || !TeamUtils.shouldBlockFriendlyFire(retaliator, retaliationTarget);
    }

    private static Entity magicTeam$getAttacker(LivingDamageEvent event) {
        if (event == null || event.getSource() == null) {
            return null;
        }
        return event.getSource().getEntity();
    }
}
