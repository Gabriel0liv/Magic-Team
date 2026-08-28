package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Travel Optics' Infernal Devastator ultimately spawns Cataclysm Flame_Jet_Entity
 * instances. Cataclysm normally rejects allied targets before hurt(), which makes
 * scoreboard friendlyFire=true ineffective. Only scoreboard-team relations are
 * replaced with Magic Team's offensive policy; every other Cataclysm alliance
 * keeps the original Entity#isAlliedTo semantics.
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Flame_Jet_Entity", remap = false)
public abstract class CataclysmFlameJetFriendlyFireMixin {

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
        return magicTeam$offensiveAlliance(source, target);
    }

    private static boolean magicTeam$offensiveAlliance(Entity source, Entity target) {
        if (source == null || target == null) {
            return source != null && source.isAlliedTo(target);
        }

        Team sourceTeam = source.getTeam();
        Team targetTeam = target.getTeam();
        if (sourceTeam != null && targetTeam != null && sourceTeam.isAlliedTo(targetTeam)) {
            return TeamUtils.shouldBlockFriendlyFire(source, target);
        }

        return source.isAlliedTo(target);
    }
}
