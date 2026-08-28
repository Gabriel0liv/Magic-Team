package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Cursed Blast is an item ability, not an Iron's spell. Both the base weapon and
 * Evo 2 implementation directly call LivingEntity#hurt for the raycast victim
 * and nearby AOE targets. Evo 1 inherits the base method and Evo 3 inherits Evo 2.
 */
@Pseudo
@Mixin(
        targets = {
                "com.gametechbc.traveloptics.item.bossweapon.cursedwraithblade.CursedWraithbladeItem",
                "com.gametechbc.traveloptics.item.bossweapon.cursedwraithblade.CursedWraithbladeLevelTwoItem"
        },
        remap = false
)
public abstract class CursedWraithbladeFriendlyFireMixin {

    @Redirect(
            method = "executeCursedBlast(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    remap = false
            ),
            require = 2,
            remap = false
    )
    private boolean magicTeam$gateCursedBlastDamage(LivingEntity target,
                                                      DamageSource damageSource,
                                                      float amount,
                                                      Level level,
                                                      Player attacker,
                                                      ItemStack stack) {
        if (attacker != null && target != null && TeamUtils.shouldBlockFriendlyFire(attacker, target)) {
            return false;
        }
        return target.hurt(damageSource, amount);
    }
}
