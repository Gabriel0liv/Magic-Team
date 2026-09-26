package com.gabri.magicteam.util;

import java.util.Objects;
import java.util.UUID;

/**
 * Addon-neutral snapshot describing a magic interaction that outlives the
 * synchronous spell call stack.
 *
 * <p>Only stable value data is stored here. In particular, this type never
 * retains addon spell/entity classes.</p>
 */
public record MagicAttribution(
        UUID sourceEntityId,
        UUID rootCasterId,
        String spellId,
        SpellBehavior behavior,
        MagicTeamEffectContext.InteractionType interactionType,
        long expiresAtTick
) {
    public MagicAttribution {
        Objects.requireNonNull(sourceEntityId, "sourceEntityId");
        rootCasterId = rootCasterId == null ? sourceEntityId : rootCasterId;
        spellId = spellId == null ? "" : spellId.trim().toLowerCase();
        Objects.requireNonNull(behavior, "behavior");
        interactionType = interactionType == null
                ? interactionFor(behavior)
                : interactionType;
    }

    public boolean isExpired(long currentTick) {
        return expiresAtTick != Long.MAX_VALUE && currentTick >= expiresAtTick;
    }

    public MagicAttribution withExpiry(long expiresAtTick) {
        return new MagicAttribution(
                sourceEntityId,
                rootCasterId,
                spellId,
                behavior,
                interactionType,
                expiresAtTick
        );
    }

    public static MagicTeamEffectContext.InteractionType interactionFor(SpellBehavior behavior) {
        return behavior == SpellBehavior.SUPPORT
                ? MagicTeamEffectContext.InteractionType.BENEFICIAL
                : MagicTeamEffectContext.InteractionType.HARMFUL;
    }
}
