package com.gabri.magicteam.util;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.Entity;

/** Shared gate for hostile magic side effects that are not damage or MobEffect application. */
public final class MagicSideEffectPolicy {
    private MagicSideEffectPolicy() {
    }

    public static boolean shouldBlock(Entity target) {
        if (!TeamUtils.isEnabled() || target == null) {
            return false;
        }

        AbstractSpell spell = MagicTeamEffectContext.getSpell();
        MagicAttribution attribution = MagicTeamEffectContext.currentAttribution();
        MagicTeamEffectContext.InteractionType interactionType = MagicTeamEffectContext.getInteractionType();
        SpellBehavior behavior = TeamUtils.resolveMagicBehavior(spell, attribution, interactionType);
        if (behavior != SpellBehavior.HOSTILE) {
            return false;
        }

        Entity source = MagicTeamEffectContext.getSource();
        if (source == null) {
            return false;
        }

        Entity effectiveSource = TeamUtils.resolveMagicSource(source, attribution);
        return effectiveSource != null && TeamUtils.shouldBlockFriendlyFire(effectiveSource, target);
    }
}
