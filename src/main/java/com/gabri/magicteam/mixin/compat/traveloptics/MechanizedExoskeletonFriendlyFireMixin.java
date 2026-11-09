package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.AdapterContextBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mechanized Exoskeleton armor selects its missile target before any magic entity exists.
 *
 * <p>Context bridge: the whole ability runs as hostile magic from the
 * player so the global gates protect allies (alliance checks, damage,
 * effects, knockback and fire). Depends only on the method name, not on how
 * its body is compiled; if the method disappears the adapter is skipped.</p>
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.armor.MechanizedExoskeletonArmorItem", remap = false)
public abstract class MechanizedExoskeletonFriendlyFireMixin {
    @Inject(method = "findMissileTarget(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/entity/LivingEntity;", at = @At("HEAD"), require = 0, remap = false)
    private void magicTeam$beginAbility(Player player, CallbackInfoReturnable<LivingEntity> ci) {
        AdapterContextBridge.beginHostile(player);
    }

    @Inject(method = "findMissileTarget(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/entity/LivingEntity;", at = @At("RETURN"), require = 0, remap = false)
    private void magicTeam$endAbility(Player player, CallbackInfoReturnable<LivingEntity> ci) {
        AdapterContextBridge.end();
    }
}
