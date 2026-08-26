package com.gabri.magicteam.mixin.compat.familiars;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hiken manually skips allied entities in both block-impact and entity-impact AOE loops
 * before hurt() is called. Redirect both checks to the hostile friendly-fire policy.
 */
@Pseudo
@Mixin(targets = "net.alshanex.alshanex_familiars.entity.misc.HikenEntity", remap = false)
public abstract class HikenFriendlyFireMixin {

    @Redirect(
            method = "m_6532_(Lnet/minecraft/world/phys/HitResult;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_7307_(Lnet/minecraft/world/entity/Entity;)Z",
                    remap = false
            ),
            require = 2,
            remap = false
    )
    private boolean magicTeam$useFriendlyFirePolicy(Entity owner, Entity target) {
        return TeamUtils.shouldBlockFriendlyFire(owner, target);
    }
}
