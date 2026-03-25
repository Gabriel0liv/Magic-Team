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
     * Injetamos no HEAD e resolvemos os summoners manualmente para garantir
     * compatibilidade com diferentes versões e ambientes (reobf).
     */
    @Inject(method = "isFriendlyFireBetween", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onIsFriendlyFireBetween(Entity attacker, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (attacker == null || target == null) return;

        Entity rootAttacker = TeamUtils.getRootOwner(attacker);
        Entity rootTarget = TeamUtils.getRootOwner(target);

        if (TeamUtils.areAllies(rootAttacker, rootTarget)) {
            boolean blockEnabled = com.gabri.magicteam.MagicTeamConfig.SERVER.enableDamageBlock.get();
            // Friendly Fire is TRUE if blocking is ENABLED.
            // Friendly Fire is FALSE if blocking is DISABLED (allows damage).
            cir.setReturnValue(blockEnabled);
        } else {
            // Se NÃO são aliados (Bypass ou times diferentes), NÃO é friendly fire.
            cir.setReturnValue(false);
        }
    }
}
