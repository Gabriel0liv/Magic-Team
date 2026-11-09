package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.effect.ImmolateEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Restores the published 2.3.1 afflicter bridge for Immolate. addImmolateStack
 * applies its MobEffect without passing the afflicter to LivingEntity#addEffect,
 * so the generic effect gate otherwise has no source when this helper is entered
 * outside an already-attributed spell scope.
 */
@Mixin(value = ImmolateEffect.class, remap = false)
public class ImmolateEffectMixin {
    private static final ThreadLocal<Deque<Boolean>> MAGIC_TEAM_IMMOLATE_SCOPES =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "addImmolateStack", at = @At("HEAD"), remap = false)
    private static void magicTeam$beginImmolateStack(LivingEntity entity,
                                                     @Nullable Entity afflicter,
                                                     CallbackInfoReturnable<MobEffectInstance> cir) {
        boolean pushed = TeamUtils.isEnabled() && afflicter != null;
        MAGIC_TEAM_IMMOLATE_SCOPES.get().push(pushed);
        if (pushed) {
            MagicTeamEffectContext.push(afflicter, MagicTeamEffectContext.InteractionType.HARMFUL);
        }
    }

    @Inject(method = "addImmolateStack", at = @At("RETURN"), remap = false)
    private static void magicTeam$endImmolateStack(LivingEntity entity,
                                                   @Nullable Entity afflicter,
                                                   CallbackInfoReturnable<MobEffectInstance> cir) {
        Deque<Boolean> scopes = MAGIC_TEAM_IMMOLATE_SCOPES.get();
        boolean pushed = !scopes.isEmpty() && scopes.pop();
        if (pushed) {
            MagicTeamEffectContext.pop();
        }
        if (scopes.isEmpty()) {
            MAGIC_TEAM_IMMOLATE_SCOPES.remove();
        }
    }
}
