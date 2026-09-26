package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tidal Grasp marks and teleports its selected target without going through
 * damage. Reject protected teammates at pre-cast, recheck while channeling, and
 * cancel the release before addon side effects in case team state changed.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.spells.aqua.TidalGraspSpell", remap = false)
public abstract class TidalGraspFriendlyFireMixin {

    @Inject(
            method = "checkPreCastConditions",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$rejectProtectedPreCast(Level level,
                                                   int spellLevel,
                                                   LivingEntity caster,
                                                   MagicData playerMagicData,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        LivingEntity target = magicTeam$getSelectedTarget(level, playerMagicData);
        if (target != null && TeamUtils.shouldBlockFriendlyFire(caster, target)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "onServerCastTick",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$skipProtectedChannelTick(Level level,
                                                     int spellLevel,
                                                     LivingEntity caster,
                                                     MagicData playerMagicData,
                                                     CallbackInfo ci) {
        LivingEntity target = magicTeam$getSelectedTarget(level, playerMagicData);
        if (target != null && TeamUtils.shouldBlockFriendlyFire(caster, target)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onCast",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$rejectProtectedRelease(Level level,
                                                   int spellLevel,
                                                   LivingEntity caster,
                                                   io.redspace.ironsspellbooks.api.spells.CastSource castSource,
                                                   MagicData playerMagicData,
                                                   CallbackInfo ci) {
        LivingEntity target = magicTeam$getSelectedTarget(level, playerMagicData);
        if (target != null && TeamUtils.shouldBlockFriendlyFire(caster, target)) {
            ci.cancel();
        }
    }

    private static LivingEntity magicTeam$getSelectedTarget(Level level, MagicData playerMagicData) {
        if (!(level instanceof ServerLevel serverLevel) || playerMagicData == null) {
            return null;
        }

        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            Entity target = targetData.getTarget(serverLevel);
            if (target instanceof LivingEntity livingTarget) {
                return livingTarget;
            }
        }
        return null;
    }
}
