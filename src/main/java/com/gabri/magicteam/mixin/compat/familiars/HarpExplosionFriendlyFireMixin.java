package com.gabri.magicteam.mixin.compat.familiars;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

/**
 * Harp Explosion performs a custom delayed AOE outside AoeEntity#checkHits. It
 * ignores applyDamage's boolean result and then pushes the target and applies
 * three effects, so protected teammates must be removed before the transaction.
 * Its optional vanilla Explosion is also scoped as harmful so synchronous
 * explosion damage uses the same owner-aware friendly-fire decision.
 */
@Pseudo
@Mixin(targets = "net.alshanex.alshanex_familiars.entity.sound.HarpExplosionEntity", remap = false)
public abstract class HarpExplosionFriendlyFireMixin {

    @Shadow(remap = false)
    public abstract LivingEntity getOwner();

    @Redirect(
            method = "m_8119_()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_6249_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
                    remap = false
            ),
            remap = false
    )
    private List<Entity> magicTeam$filterProtectedTargets(Level level,
                                                           Entity except,
                                                           AABB box,
                                                           Predicate<? super Entity> predicate) {
        LivingEntity owner = getOwner();
        return level.getEntities(
                except,
                box,
                target -> predicate.test(target)
                        && (owner == null || !TeamUtils.shouldBlockFriendlyFire(owner, target))
        );
    }

    @Redirect(
            method = "m_8119_()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Explosion;m_46061_()V",
                    remap = false
            ),
            remap = false
    )
    private void magicTeam$scopeExplosionDamage(Explosion explosion) {
        LivingEntity owner = getOwner();
        MagicTeamEffectContext.push(
                owner != null ? owner : (Entity) (Object) this,
                MagicTeamEffectContext.InteractionType.HARMFUL
        );
        try {
            explosion.explode();
        } finally {
            MagicTeamEffectContext.pop();
        }
    }
}
