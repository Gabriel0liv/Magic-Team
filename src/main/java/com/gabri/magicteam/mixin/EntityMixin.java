package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Relationship compatibility for proven magic interactions only.
 *
 * <p>When a spell context is active, or either participant carries persistent
 * magic attribution, Babel's root-owner/team relationship is authoritative for
 * {@code isAlliedTo}. This is relationship identity only: hostile permission is
 * still decided later by Magic Team's central policy.</p>
 */
@Mixin(value = Entity.class, priority = 2000)
public abstract class EntityMixin {

    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsAlliedTo(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled()) {
            return;
        }

        Entity self = (Entity) (Object) this;
        if (!isMagicRelevant(self, other)) {
            return;
        }

        cir.setReturnValue(TeamUtils.areAllies(self, other));
    }

    private static boolean isMagicRelevant(Entity self, Entity other) {
        if (MagicTeamEffectContext.hasContext()) {
            return true;
        }

        if (hasAttribution(self) || hasAttribution(other)) {
            return true;
        }

        return isIronsMagicEntity(self) || isIronsMagicEntity(other);
    }

    private static boolean hasAttribution(Entity entity) {
        if (entity == null) {
            return false;
        }
        return MagicAttributionIndex.get(entity, entity.level().getGameTime()) != null;
    }

    private static boolean isIronsMagicEntity(Entity entity) {
        if (entity == null) {
            return false;
        }

        String className = entity.getClass().getName();
        return className.startsWith("io.redspace.ironsspellbooks.");
    }
}
