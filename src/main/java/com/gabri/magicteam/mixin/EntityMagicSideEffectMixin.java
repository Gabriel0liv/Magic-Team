package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicSideEffectPolicy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Global gate for common hostile magic side effects on Entity. */
@Mixin(Entity.class)
public abstract class EntityMagicSideEffectMixin {

    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void magicTeam$gateForcedMovement(Vec3 movement, CallbackInfo ci) {
        Entity target = (Entity) (Object) this;
        if (MagicSideEffectPolicy.shouldBlock(target)) {
            ci.cancel();
        }
    }

    @Inject(method = "setSecondsOnFire(I)V", at = @At("HEAD"), cancellable = true)
    private void magicTeam$gateSetSecondsOnFire(int seconds, CallbackInfo ci) {
        Entity target = (Entity) (Object) this;
        if (seconds > 0 && MagicSideEffectPolicy.shouldBlock(target)) {
            ci.cancel();
        }
    }

    @Inject(method = "setRemainingFireTicks(I)V", at = @At("HEAD"), cancellable = true)
    private void magicTeam$gateSetRemainingFireTicks(int ticks, CallbackInfo ci) {
        Entity target = (Entity) (Object) this;

        // Reducing/extinguishing fire is beneficial even when a mixed spell also
        // performs hostile work. Only increases are subject to hostile filtering.
        if (ticks <= target.getRemainingFireTicks()) {
            return;
        }

        if (MagicSideEffectPolicy.shouldBlock(target)) {
            ci.cancel();
        }
    }
}
