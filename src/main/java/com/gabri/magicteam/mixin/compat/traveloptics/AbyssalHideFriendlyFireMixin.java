package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.AdapterContextBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Abyssal Hide armor picks its nearest target without any Iron's spell context.
 *
 * <p>Context bridge: the whole ability runs as hostile magic from the
 * wearer so the global gates protect allies (alliance checks, damage,
 * effects, knockback and fire). Depends only on the method name, not on how
 * its body is compiled; if the method disappears the adapter is skipped.</p>
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.armor.AbyssalHideArmorItem", remap = false)
public abstract class AbyssalHideFriendlyFireMixin {
    @Inject(method = "findNearestTarget(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/entity/LivingEntity;", at = @At("HEAD"), require = 0, remap = false)
    private void magicTeam$beginAbility(Level level, LivingEntity wearer, CallbackInfoReturnable<LivingEntity> ci) {
        AdapterContextBridge.beginHostile(wearer);
    }

    @Inject(method = "findNearestTarget(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/entity/LivingEntity;", at = @At("RETURN"), require = 0, remap = false)
    private void magicTeam$endAbility(Level level, LivingEntity wearer, CallbackInfoReturnable<LivingEntity> ci) {
        AdapterContextBridge.end();
    }
}
