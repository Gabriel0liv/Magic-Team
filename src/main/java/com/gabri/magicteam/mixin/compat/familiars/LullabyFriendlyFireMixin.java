package com.gabri.magicteam.mixin.compat.familiars;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lullaby applies the harmful SLEEPY effect through a private synthetic target
 * predicate that rejects caster.isAlliedTo(target). Scoreboard teammates with
 * friendly fire enabled must remain eligible hostile targets.
 */
@Pseudo
@Mixin(targets = "net.alshanex.alshanex_familiars.spells.LullabySpell", remap = false)
public abstract class LullabyFriendlyFireMixin {

    @Redirect(
            method = "lambda$applySleepy$0(Lnet/minecraft/world/entity/LivingEntity;DLnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_7307_(Lnet/minecraft/world/entity/Entity;)Z",
                    remap = false
            ),
            remap = false
    )
    private static boolean magicTeam$useFriendlyFirePolicy(LivingEntity caster, Entity target) {
        return TeamUtils.shouldBlockFriendlyFire(caster, target);
    }
}
