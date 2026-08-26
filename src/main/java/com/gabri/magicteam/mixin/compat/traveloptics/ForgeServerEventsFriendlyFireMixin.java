package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Travel Optics' Spider Aspect curio applies Poison directly from
 * LivingAttackEvent, before a later damage path can enforce team friendly fire.
 * Skip only that event handler for protected teammate attacks.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.events.ForgeServerEvents", remap = false)
public abstract class ForgeServerEventsFriendlyFireMixin {

    @Inject(
            method = "applyPoisonOnCurio(Lnet/minecraftforge/event/entity/living/LivingAttackEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void magicTeam$gateSpiderAspectPoison(LivingAttackEvent event, CallbackInfo ci) {
        if (event == null) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        Entity target = event.getEntity();
        if (attacker != null && target != null && TeamUtils.shouldBlockFriendlyFire(attacker, target)) {
            ci.cancel();
        }
    }
}
