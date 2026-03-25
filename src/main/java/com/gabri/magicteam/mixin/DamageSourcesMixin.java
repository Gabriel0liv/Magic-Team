package com.gabri.magicteam.mixin;

import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
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
     * Injetamos no HEAD e resolvemos os summoners manualmente para garantir
     * compatibilidade com diferentes versões e ambientes (reobf).
     */
    @Inject(method = "isFriendlyFireBetween", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onIsFriendlyFireBetween(Entity attacker, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!com.gabri.magicteam.MagicTeamConfig.SERVER.enableDamageBlock.get()) return;

        if (attacker == null || target == null) return;

        // Resolução manual de Summoners (replicando a lógica do ISS no HEAD)
        Entity resolvedAttacker = attacker;
        if (attacker instanceof IMagicSummon summon) {
            Entity owner = summon.getSummoner();
            if (owner != null) resolvedAttacker = owner;
        }

        Entity resolvedTarget = target;
        if (target instanceof IMagicSummon summon) {
            Entity owner = summon.getSummoner();
            if (owner != null) resolvedTarget = owner;
        }

        if (TeamUtils.areAllies(resolvedAttacker, resolvedTarget)) {
            // Se forem aliados de equipe, forçamos Friendly Fire = TRUE (bloqueia o dano/efeito).
            cir.setReturnValue(true);
        }
    }
}
