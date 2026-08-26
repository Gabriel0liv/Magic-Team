package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Evo 3's Mini EM-Pulse excludes scoreboard allies before applying its stun.
 * Replace only that team-relation branch with the offensive FF policy; the
 * separate tamed-animal protection in isAllyOrTamed remains untouched.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.bossweapon.harbingerwrath.HarbingersWrathLevelThreeItem", remap = false)
public abstract class HarbingersWrathFriendlyFireMixin {

    @Redirect(
            method = "isAllyOrTamed(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/scores/Team;m_83536_(Lnet/minecraft/world/scores/Team;)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean magicTeam$useFriendlyFireForTeamBranch(Team attackerTeam,
                                                            Team targetTeam,
                                                            LivingEntity target,
                                                            LivingEntity attacker) {
        return TeamUtils.shouldBlockFriendlyFire(attacker, target);
    }
}
