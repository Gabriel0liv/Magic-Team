package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.AdapterContextBridge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cursed Wraithblade item ability damages targets directly, without spell attribution.
 *
 * <p>Context bridge: the whole ability runs as hostile magic from the
 * player so the global gates protect allies (alliance checks, damage,
 * effects, knockback and fire). Depends only on the method name, not on how
 * its body is compiled; if the method disappears the adapter is skipped.</p>
 */
@Pseudo
@Mixin(targets = {
                "com.gametechbc.traveloptics.item.bossweapon.cursedwraithblade.CursedWraithbladeItem",
                "com.gametechbc.traveloptics.item.bossweapon.cursedwraithblade.CursedWraithbladeLevelTwoItem"
        }, remap = false)
public abstract class CursedWraithbladeFriendlyFireMixin {
    @Inject(method = "executeCursedBlast(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), require = 0, remap = false)
    private void magicTeam$beginAbility(Level level, Player player, ItemStack stack, CallbackInfo ci) {
        AdapterContextBridge.beginHostile(player);
    }

    @Inject(method = "executeCursedBlast(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"), require = 0, remap = false)
    private void magicTeam$endAbility(Level level, Player player, ItemStack stack, CallbackInfo ci) {
        AdapterContextBridge.end();
    }
}
