package com.gabri.magicteam.mixin.compat.geomancyplus;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

/**
 * Tremor Step applies spell damage and then unconditionally resets the target's
 * invulnerability timer. Filter protected teammates before both operations so a
 * blocked friendly-fire hit has no offensive side effects.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.gtbcs_geomancy_plus.effects.TremorStepEffect", remap = false)
public abstract class TremorStepFriendlyFireMixin {

    @Redirect(
            method = "triggerTremorShockwave(Lnet/minecraft/world/entity/player/Player;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_6443_(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
                    remap = false
            ),
            remap = false
    )
    private <T extends Entity> List<T> magicTeam$filterProtectedTargets(Level level,
                                                                        Class<T> entityClass,
                                                                        AABB box,
                                                                        Predicate<? super T> predicate,
                                                                        Player caster,
                                                                        int amplifier) {
        return level.getEntitiesOfClass(
                entityClass,
                box,
                target -> predicate.test(target) && !TeamUtils.shouldBlockFriendlyFire(caster, target)
        );
    }
}
