package com.gabri.magicteam.mixin;

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
     * Blocks hostile spell damage between distinct allies whenever Magic Team is enabled.
     * Support-classified spells keep their existing ally-allowed behavior.
     */
    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onApplyDamage(Entity target, float baseAmount, net.minecraft.world.damagesource.DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled()) {
            return;
        }

        Entity attacker = damageSource.getEntity();
        if (attacker == null || target == null) {
            return;
        }

        if (damageSource instanceof SpellDamageSource spellDamageSource
                && TeamUtils.shouldBlockMagicDamage(attacker, target, spellDamageSource.spell())) {
            TeamUtils.sendBlockedMessage(attacker);
            cir.setReturnValue(false);
        }
    }
}
