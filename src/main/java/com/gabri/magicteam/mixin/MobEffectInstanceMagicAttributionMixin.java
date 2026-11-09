package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicEffectAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Re-enters the original magic context while an attributed MobEffect performs
 * its delayed tick.
 *
 * <p>This intentionally wraps the MobEffectInstance#tick method boundary rather
 * than redirecting its internal applyEffectTick call. Arclight and other
 * transformers may rewrite that internal invocation while preserving the public
 * tick contract.</p>
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMagicAttributionMixin {
    private static final ThreadLocal<Deque<Boolean>> MAGIC_TEAM_SCOPES =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "tick", at = @At("HEAD"))
    private void magicTeam$beginEffectAttribution(LivingEntity target,
                                                  Runnable onExpiration,
                                                  CallbackInfoReturnable<Boolean> cir) {
        boolean pushed = false;

        if (TeamUtils.isEnabled() && target != null) {
            MobEffect effect = ((MobEffectInstance) (Object) this).getEffect();
            MagicAttribution attribution = MagicEffectAttributionIndex.get(
                    target,
                    effect,
                    target.level().getGameTime()
            );

            if (attribution != null) {
                MagicTeamEffectContext.push(target, attribution);
                pushed = true;
            }
        }

        MAGIC_TEAM_SCOPES.get().push(pushed);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void magicTeam$endEffectAttribution(LivingEntity target,
                                                Runnable onExpiration,
                                                CallbackInfoReturnable<Boolean> cir) {
        Deque<Boolean> scopes = MAGIC_TEAM_SCOPES.get();
        boolean pushed = !scopes.isEmpty() && scopes.pop();

        if (pushed) {
            MagicTeamEffectContext.pop();
        }

        if (scopes.isEmpty()) {
            MAGIC_TEAM_SCOPES.remove();
        }
    }
}
