package com.gabri.magicteam.mixin.compat.familiars;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sonata's radial GUIDING_BOLT application uses a private synthetic predicate
 * that rejects caster.isAlliedTo(target). GUIDING_BOLT is harmful, so the
 * scoreboard friendly-fire permission must decide whether a teammate is valid.
 */
@Pseudo
@Mixin(targets = "net.alshanex.alshanex_familiars.spells.SonataSpell", remap = false)
public abstract class SonataFriendlyFireMixin {

    @Redirect(
            method = "lambda$shootNotes$0(Lnet/minecraft/world/entity/LivingEntity;DLnet/minecraft/world/entity/LivingEntity;)Z",
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
