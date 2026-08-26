package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Vortex Punch creates Cataclysm's plain-Entity vortex. Babel's generic root-owner
 * resolver intentionally does not reflect arbitrary getOwner() methods, so this
 * adapter uses the vortex's real owner directly for pull and explosion policy.
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.effect.Void_Vortex_Entity", remap = false)
public abstract class CataclysmVoidVortexFriendlyFireMixin {

    @Shadow(remap = false)
    @Nullable
    public abstract LivingEntity getOwner();

    @Inject(method = "m_8119_()V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginTick(CallbackInfo ci) {
        LivingEntity owner = getOwner();
        MagicTeamEffectContext.push(owner != null ? owner : (Entity) (Object) this,
                MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "m_8119_()V", at = @At("RETURN"), remap = false)
    private void magicTeam$endTick(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }

    @Redirect(
            method = "m_8119_()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"),
            remap = false
    )
    private void magicTeam$gatePersistentPull(Entity target, Vec3 motion) {
        LivingEntity owner = getOwner();
        if (owner == null || target == null || !TeamUtils.shouldBlockFriendlyFire(owner, target)) {
            target.setDeltaMovement(motion);
        }
    }
}
