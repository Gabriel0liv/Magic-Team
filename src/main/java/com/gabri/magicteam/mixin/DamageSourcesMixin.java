package com.gabri.magicteam.mixin;

import io.redspace.ironsspellbooks.damage.DamageSources;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DamageSources.class, remap = false)
public class DamageSourcesMixin {

    /**
     * Propaga o estado de aliança do Magic-Team para o Iron's Spells.
     */
    @Inject(method = "isFriendlyFireBetween", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onIsFriendlyFireBetween(Entity attacker, Entity target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TeamUtils.areAllies(attacker, target));
    }

    /**
     * Bloqueio de dano simplificado baseado em aliança.
     */
    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onApplyDamage(Entity target, float baseAmount, net.minecraft.world.damagesource.DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Entity attacker = damageSource.getEntity();
        if (attacker == null || target == null) return;

        // Se o sistema diz que são aliados, bloqueamos o dano.
        // Se a Aliança Global estiver OFF, areAllies retornará false e o dano passará.
        if (TeamUtils.areAllies(attacker, target)) {
            TeamUtils.sendBlockedMessage(attacker);
            cir.setReturnValue(false);
        }
    }
}
