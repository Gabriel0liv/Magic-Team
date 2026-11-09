package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DamageSources.class, remap = false)
public class DamageSourcesMixin {

    /**
     * Replaces Iron's magic friendly-fire decision while Magic Team is enabled.
     * Babel resolves projectile/summon ownership and alliance identity; vanilla
     * scoreboard friendlyFire is intentionally not a permission input for magic.
     */
    @Inject(method = "isFriendlyFireBetween", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onIsFriendlyFireBetween(Entity attacker, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled()) {
            return;
        }
        cir.setReturnValue(TeamUtils.shouldBlockFriendlyFire(attacker, target));
    }

    /**
     * Iron's native SpellDamageSource is preferred evidence. Persistent generic
     * attribution is only consulted as a fallback for delayed/custom entities.
     */
    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onApplyDamage(Entity target,
                                      float baseAmount,
                                      net.minecraft.world.damagesource.DamageSource damageSource,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled() || damageSource == null || target == null) {
            return;
        }

        Entity attacker = damageSource.getEntity();
        if (attacker == null) {
            attacker = damageSource.getDirectEntity();
        }
        if (attacker == null) {
            return;
        }

        io.redspace.ironsspellbooks.api.spells.AbstractSpell spell = null;
        if (damageSource instanceof SpellDamageSource spellDamageSource) {
            spell = spellDamageSource.spell();
        }

        MagicAttribution attribution = spell == null
                ? MagicAttributionIndex.get(attacker, attacker.level().getGameTime())
                : null;

        if (spell == null && attribution == null) {
            return;
        }

        if (TeamUtils.shouldBlockMagicDamage(
                attacker,
                target,
                spell,
                attribution,
                MagicTeamEffectContext.getInteractionType())) {
            TeamUtils.sendBlockedMessage(TeamUtils.resolveMagicSource(attacker, attribution));
            cir.setReturnValue(false);
        }
    }
}
