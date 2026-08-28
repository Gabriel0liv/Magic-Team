package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.FlareVacuumAttribution;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Flare Vacuum is applied by Gyro Slash but creates Cataclysm Flame Jets later
 * with a null caster. Re-enter the stored attribution while the effect ticks and
 * restore that caster only at this effect's Flame Jet constructor call site.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.effects.FlareVacuumEffect", remap = false)
public abstract class FlareVacuumAttributionMixin {

    @Inject(
            method = "m_6742_(Lnet/minecraft/world/entity/LivingEntity;I)V",
            at = @At("HEAD"),
            remap = false
    )
    private void magicTeam$beginAttribution(LivingEntity entity, int amplifier, CallbackInfo ci) {
        FlareVacuumAttribution.begin(entity);
    }

    @Inject(
            method = "m_6742_(Lnet/minecraft/world/entity/LivingEntity;I)V",
            at = @At("RETURN"),
            remap = false
    )
    private void magicTeam$endAttribution(LivingEntity entity, int amplifier, CallbackInfo ci) {
        FlareVacuumAttribution.end();
    }

    @Inject(
            method = "pullEntityTowards(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$gatePull(LivingEntity affectedEntity, LivingEntity target, CallbackInfo ci) {
        LivingEntity originalCaster = FlareVacuumAttribution.getActiveSource();
        if (originalCaster != null && target != null && TeamUtils.shouldBlockFriendlyFire(originalCaster, target)) {
            ci.cancel();
        }
    }

    @ModifyArg(
            method = "spawnFangs(Lnet/minecraft/world/entity/LivingEntity;DDDDFII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/L_Ender/cataclysm/entity/projectile/Flame_Jet_Entity;<init>(Lnet/minecraft/world/level/Level;DDDFIFLnet/minecraft/world/entity/LivingEntity;)V",
                    remap = false
            ),
            index = 7,
            remap = false
    )
    private LivingEntity magicTeam$restoreOriginalCaster(LivingEntity originalCaster) {
        return originalCaster != null ? originalCaster : FlareVacuumAttribution.getActiveSource();
    }
}
