package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blackout's movement punishment runs after AoeEntity#tick returns, so it is
 * outside the base AOE scope. Keep the whole subclass tick in a harmful scope
 * to cover that direct damage without changing AOE relation semantics.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.BlackoutAntiMagicField", remap = false)
public abstract class BlackoutAntiMagicFieldContextMixin {

    @Inject(method = "m_8119_()V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginTick(CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "m_8119_()V", at = @At("RETURN"), remap = false)
    private void magicTeam$endTick(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
