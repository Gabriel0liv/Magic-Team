package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Travel Optics uses Cataclysm void runes from both spells and boss weapons.
 * Cataclysm performs two Entity#isAlliedTo checks before damage, which blocks
 * scoreboard teammates even when their team allows friendly fire. Replace only
 * scoreboard-team relations with Magic Team's offensive policy and preserve
 * native Cataclysm alliance semantics for every other relation.
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Void_Rune_Entity", remap = false)
public abstract class CataclysmVoidRuneFriendlyFireMixin {

    @Redirect(
            method = "damage(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_7307_(Lnet/minecraft/world/entity/Entity;)Z",
                    remap = false
            ),
            require = 2,
            remap = false
    )
    private boolean magicTeam$scoreboardAwareAlliance(Entity source, Entity target) {
        if (source == null || target == null) {
            return false;
        }

        Team sourceTeam = source.getTeam();
        Team targetTeam = target.getTeam();
        if (sourceTeam != null && targetTeam != null && sourceTeam.isAlliedTo(targetTeam)) {
            return TeamUtils.shouldBlockFriendlyFire(source, target);
        }

        return source.isAlliedTo(target);
    }
}
