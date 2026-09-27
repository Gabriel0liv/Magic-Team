package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.armor.ForlornHarbingerArmorItem", remap = false)
public abstract class ForlornHarbingerFriendlyFireMixin {
    @Redirect(
            method = "activateNocturnalUplift(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;ZII)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_7307_(Lnet/minecraft/world/entity/Entity;)Z", remap = false),
            remap = false
    )
    private boolean magicTeam$useFriendlyFirePolicy(Entity affected, Entity player) {
        return TeamUtils.shouldBlockFriendlyFire(player, affected);
    }
}
