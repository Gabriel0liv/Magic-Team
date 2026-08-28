package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Direct trident damage uses a vanilla DamageSource, so gate it at hit entry. */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.aqua_trident.EternalMaelstromTridentEntity", remap = false)
public abstract class EternalMaelstromTridentFriendlyFireMixin {

    @Inject(
            method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$gateDirectHit(EntityHitResult hitResult, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Entity owner = TeamUtils.getRootOwner(self);
        Entity target = hitResult == null ? null : hitResult.getEntity();
        if (owner != null && target != null && TeamUtils.shouldBlockFriendlyFire(owner, target)) {
            ci.cancel();
        }
    }
}
