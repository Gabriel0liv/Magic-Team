package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Travel Optics uses Cataclysm laser beams from spells and boss weapons. Their
 * hits happen after the original cast/item execution has ended, so no spell
 * context remains. Gate the delayed hit through the projectile's persisted
 * vanilla owner relation before Cataclysm mutates fire ticks or deals damage.
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Laser_Beam_Entity", remap = false)
public abstract class CataclysmLaserBeamFriendlyFireMixin {

    @Inject(
            method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$gateDelayedHit(EntityHitResult hitResult, CallbackInfo ci) {
        if (hitResult != null
                && TeamUtils.shouldBlockFriendlyFire((Entity) (Object) this, hitResult.getEntity())) {
            ci.cancel();
        }
    }
}
