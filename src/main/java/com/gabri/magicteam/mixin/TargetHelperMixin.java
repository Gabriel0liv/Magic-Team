package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Using string-based target to avoid compilation errors if TargetHelper is not in the classpath.
@Mixin(targets = "io.redspace.ironsspellbooks.api.util.TargetHelper", remap = false)
public abstract class TargetHelperMixin {

    /**
     * Common hook for Iron's Spells targeting.
     * signature: canEntityHitEntity(Entity, Entity, boolean)
     */
    @Inject(method = "canEntityHitEntity(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("HEAD"), cancellable = true)
    private static void onCanEntityHitEntity(Entity attacker, Entity target, boolean canHitAllies, CallbackInfoReturnable<Boolean> cir) {
        if (!canHitAllies && TeamUtils.areAllies(attacker, target)) {
            // DEBUG LOGGING
            System.out.println("[MagicTeam] TargetHelperMixin: BLOCKING targeting from " + attacker.getName().getString() + " to " + target.getName().getString());
            
            // Visual Feedback
            TeamUtils.sendProtectionMessage(attacker);
            
            cir.setReturnValue(false);
        }
    }
}
