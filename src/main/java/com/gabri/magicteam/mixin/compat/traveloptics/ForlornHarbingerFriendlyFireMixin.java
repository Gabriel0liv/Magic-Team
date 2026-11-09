package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.AdapterContextBridge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forlorn Harbinger armor ability affects nearby entities outside the spell API.
 *
 * <p>Context bridge: the whole ability runs as hostile magic from the
 * player so the global gates protect allies (alliance checks, damage,
 * effects, knockback and fire). Depends only on the method name, not on how
 * its body is compiled; if the method disappears the adapter is skipped.</p>
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.armor.ForlornHarbingerArmorItem", remap = false)
public abstract class ForlornHarbingerFriendlyFireMixin {
    @Inject(method = "activateNocturnalUplift(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;ZII)V", at = @At("HEAD"), require = 0, remap = false)
    private void magicTeam$beginAbility(Player player, ItemStack stack, boolean flag, int a, int b, CallbackInfo ci) {
        AdapterContextBridge.beginHostile(player);
    }

    @Inject(method = "activateNocturnalUplift(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;ZII)V", at = @At("RETURN"), require = 0, remap = false)
    private void magicTeam$endAbility(Player player, ItemStack stack, boolean flag, int a, int b, CallbackInfo ci) {
        AdapterContextBridge.end();
    }
}
