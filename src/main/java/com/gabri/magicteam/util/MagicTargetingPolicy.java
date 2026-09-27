package com.gabri.magicteam.util;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Addon-neutral gate for hostile spells that use Iron's standard selected-target
 * cast data. The same policy is reused by every virtual dispatch boundary so an
 * addon override cannot bypass protection simply by not calling super.
 */
public final class MagicTargetingPolicy {
    private MagicTargetingPolicy() {
    }

    public static boolean shouldBlockSelectedTarget(Level level,
                                                    LivingEntity caster,
                                                    MagicData magicData,
                                                    AbstractSpell spell) {
        if (!TeamUtils.isEnabled() || caster == null || magicData == null || !TeamUtils.isHarmful(spell)) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!(magicData.getAdditionalCastData() instanceof TargetEntityCastData targetData)) {
            return false;
        }

        Entity target = targetData.getTarget(serverLevel);
        return target != null && TeamUtils.shouldBlockFriendlyFire(caster, target);
    }
}
