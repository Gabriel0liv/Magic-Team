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
 * Extended Phantom Arrow sets fire before calling hurt(), so damage-only
 * filtering is too late. Cancel the hostile hit at the method boundary when
 * scoreboard friendly fire is disabled.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.extended_projectiles.ExtendedPhantomArrowEntity", remap = false)
public abstract class ExtendedPhantomArrowFriendlyFireMixin {

    @Inject(
            method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$gateHit(EntityHitResult hitResult, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Entity owner = TeamUtils.getRootOwner(self);
        Entity target = hitResult == null ? null : hitResult.getEntity();
        if (owner != null && target != null && TeamUtils.shouldBlockFriendlyFire(owner, target)) {
            ci.cancel();
        }
    }
}
