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
     * Intercepta a verificação de Friendly Fire do Iron's Spells.
     * Retornamos SEMPRE FALSE para permitir que a lógica interna das magias 
     * (como aplicação de efeitos de Slow) prossiga sem ser abortada.
     */
    @Inject(method = "isFriendlyFireBetween", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onIsFriendlyFireBetween(Entity attacker, Entity target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Gerencia o contexto de dano para permitir atingir aliados.
     */
    @Inject(method = "applyDamage", at = @At("HEAD"), remap = false)
    private static void onApplyDamageHead(Entity target, float baseAmount, net.minecraft.world.damagesource.DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Entity attacker = damageSource.getEntity();
        if (attacker == null || target == null) return;

        if (TeamUtils.areAllies(attacker, target)) {
            // Se o bloqueio de dano está DESATIVADO, forçamos o Minecraft a ver o alvo como inimigo
            // durante a execução do applyDamage (isso libera o hurt() vanilla).
            if (!com.gabri.magicteam.MagicTeamConfig.SERVER.enableDamageBlock.get()) {
                TeamUtils.FORCE_ENEMIES_SCOPE.set(true);
            }
        }
    }

    @Inject(method = "applyDamage", at = @At("RETURN"), remap = false)
    private static void onApplyDamageTail(Entity target, float baseAmount, net.minecraft.world.damagesource.DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        // Limpa o contexto ao sair
        TeamUtils.FORCE_ENEMIES_SCOPE.set(false);
    }

    /**
     * Bloqueio final de segurança se o dano ainda assim passar mas o toggle estiver ON.
     */
    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onApplyDamageBlock(Entity target, float baseAmount, net.minecraft.world.damagesource.DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Entity attacker = damageSource.getEntity();
        if (attacker == null || target == null) return;

        if (TeamUtils.areAllies(attacker, target)) {
            if (com.gabri.magicteam.MagicTeamConfig.SERVER.enableDamageBlock.get()) {
                TeamUtils.sendBlockedMessage(attacker);
                cir.setReturnValue(false);
            }
        }
    }
}
