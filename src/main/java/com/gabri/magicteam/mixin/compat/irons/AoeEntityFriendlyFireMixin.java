package com.gabri.magicteam.mixin.compat.irons;

import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * AoeEntity pre-filters targets before damage/effects reach Magic Team's central gates.
 * Replace only that offensive relationship check; alliance semantics elsewhere remain intact.
 */
@Mixin(AoeEntity.class)
public abstract class AoeEntityFriendlyFireMixin {

    @Redirect(
            method = "canHitEntity(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private boolean magicTeam$useFriendlyFirePolicy(Entity owner, Entity target) {
        return TeamUtils.shouldBlockFriendlyFire(owner, target);
    }
}
