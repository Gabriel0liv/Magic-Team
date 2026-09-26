package com.gabri.magicteam.util;

import com.gabri.babel.core.gameplay.entity.BabelEntityRelations;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeamUtils {
    private static final BabelEntityRelations ENTITY_RELATIONS = BabelEntityRelations.INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger("magic_team");
    private static final Map<UUID, Long> LAST_MESSAGE_TIME = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 1000;
    private static final Set<String> DEFAULT_SUPPORT_SPELLS = Set.of(
            "irons_spellbooks:fortify",
            "irons_spellbooks:haste",
            "irons_spellbooks:cloud_of_regeneration",
            "irons_spellbooks:cleanse",
            "irons_spellbooks:blessing_of_life",
            "irons_spellbooks:healing_circle",
            "irons_spellbooks:wisp"
    );

    private static volatile boolean debugEnabled;

    public static boolean isEnabled() {
        return MagicTeamConfig.SERVER.enabled.get();
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    /**
     * Determina se duas entidades devem ser tratadas como aliadas no momento atual.
     * Aliança e proteção mágica são conceitos separados.
     */
    public static boolean areAllies(Entity a, Entity b) {
        if (a == null || b == null) {
            return false;
        }

        return ENTITY_RELATIONS.areAllies(a, b);
    }

    /**
     * Resolves the true owner of an entity (projectiles, summons, AOE owners, etc.)
     * through Babel Core's current entity-relations domain.
     */
    public static Entity getRootOwner(Entity entity) {
        return ENTITY_RELATIONS.getRootOwner(entity);
    }

    /**
     * Decides whether Magic Team must block an offensive magical interaction.
     * Vanilla scoreboard friendlyFire is intentionally irrelevant here: vanilla
     * combat remains Minecraft's responsibility, while this method is called only
     * from Magic Team's magic/addon interception points.
     */
    public static boolean shouldBlockFriendlyFire(Entity attacker, Entity target) {
        if (!isEnabled()) {
            return false;
        }

        if (attacker == null || target == null) {
            return false;
        }

        Entity rootAttacker = resolveComparisonEntity(attacker);
        Entity rootTarget = resolveComparisonEntity(target);
        if (rootAttacker == null || rootTarget == null) {
            return false;
        }

        boolean sameResolvedEntity = rootAttacker == rootTarget;
        boolean allied = ENTITY_RELATIONS.areAllies(rootAttacker, rootTarget);
        boolean blocked = FriendlyFirePolicy.shouldBlock(sameResolvedEntity, allied);

        debugDecision(attacker, target, blocked, allied, sameResolvedEntity);
        return blocked;
    }

    /** In-game action-bar feedback for blocked allied interactions. */
    public static void sendBlockedMessage(Entity entity) {
        if (!isEnabled() || !MagicTeamConfig.SERVER.blockedMessageEnabled.get()) {
            return;
        }

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        long now = System.currentTimeMillis();
        long last = LAST_MESSAGE_TIME.getOrDefault(player.getUUID(), 0L);
        if (now - last <= MESSAGE_COOLDOWN_MS) {
            return;
        }

        Component message;
        try {
            message = parseBlockedMessage(MagicTeamConfig.SERVER.blockedMessage.get());
        } catch (RuntimeException exception) {
            LOGGER.warn("Invalid configured blocked message; using Magic Team default", exception);
            message = Component.literal("Você não pode ferir um aliado.");
        }

        player.sendSystemMessage(message, true);
        LAST_MESSAGE_TIME.put(player.getUUID(), now);
    }

    /** Parses plain text or vanilla tellraw-style JSON into a server-resolved component. */
    public static Component parseBlockedMessage(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("{") || value.startsWith("[")) {
            Component parsed = Component.Serializer.fromJson(value);
            if (parsed == null) {
                throw new IllegalArgumentException("JSON component resolved to null");
            }
            return parsed;
        }
        return Component.literal(raw == null ? "" : raw);
    }

    public static boolean shouldAllowHealing(Entity healer, Entity target) {
        if (!isEnabled()) {
            return true;
        }

        if (healer == null || target == null) {
            return true;
        }

        return areAllies(healer, target);
    }

    public static boolean shouldAllowTarget(LivingEntity caster, LivingEntity target, AbstractSpell spell) {
        if (!isEnabled()) {
            return true;
        }

        if (caster == null || target == null) {
            return true;
        }

        if (!isHarmful(spell)) {
            return true;
        }

        return !shouldBlockFriendlyFire(caster, target);
    }

    public static boolean shouldAllowEffect(Entity source, Entity target, MobEffectInstance effectInstance) {
        return shouldAllowEffect(
                source,
                target,
                effectInstance,
                null,
                MagicTeamEffectContext.currentAttribution(),
                MagicTeamEffectContext.getInteractionType()
        );
    }

    public static boolean shouldAllowEffect(Entity source, Entity target, MobEffectInstance effectInstance, AbstractSpell spell) {
        return shouldAllowEffect(
                source,
                target,
                effectInstance,
                spell,
                MagicTeamEffectContext.currentAttribution(),
                MagicTeamEffectContext.getInteractionType()
        );
    }

    public static boolean shouldAllowEffect(Entity source, Entity target, MobEffectInstance effectInstance,
                                            AbstractSpell spell, MagicTeamEffectContext.InteractionType interactionType) {
        return shouldAllowEffect(
                source,
                target,
                effectInstance,
                spell,
                MagicTeamEffectContext.currentAttribution(),
                interactionType
        );
    }

    public static boolean shouldAllowEffect(Entity source,
                                            Entity target,
                                            MobEffectInstance effectInstance,
                                            AbstractSpell spell,
                                            MagicAttribution attribution,
                                            MagicTeamEffectContext.InteractionType interactionType) {
        if (!isEnabled()) {
            return true;
        }

        if (source == null || target == null || effectInstance == null) {
            return true;
        }

        if (MagicTeamEffectContext.isVanillaPotionApplication()) {
            return true;
        }

        // A plain vanilla potion/cloud remains outside Magic Team. If the same
        // vanilla entity was spawned by an attributed spell, however, the
        // attribution is positive evidence that it belongs to the magic path.
        if (isVanillaPotionSource(source) && attribution == null) {
            return true;
        }

        SpellBehavior behavior = resolveMagicBehavior(spell, attribution, interactionType);
        if (behavior == null) {
            return true;
        }

        Entity effectiveSource = resolveMagicSource(source, attribution);
        if (effectiveSource == null) {
            return true;
        }

        if (behavior == SpellBehavior.SUPPORT) {
            return effectiveSource == target || areAllies(effectiveSource, target);
        }

        return !shouldBlockFriendlyFire(effectiveSource, target);
    }

    public static boolean shouldBlockMagicDamage(Entity attacker, Entity target) {
        return shouldBlockMagicDamage(
                attacker,
                target,
                null,
                MagicTeamEffectContext.currentAttribution(),
                MagicTeamEffectContext.getInteractionType()
        );
    }

    public static boolean shouldBlockMagicDamage(Entity attacker, Entity target, AbstractSpell spell) {
        return shouldBlockMagicDamage(
                attacker,
                target,
                spell,
                MagicTeamEffectContext.currentAttribution(),
                MagicTeamEffectContext.getInteractionType()
        );
    }

    public static boolean shouldBlockMagicDamage(Entity attacker,
                                                 Entity target,
                                                 AbstractSpell spell,
                                                 MagicAttribution attribution,
                                                 MagicTeamEffectContext.InteractionType interactionType) {
        if (!isEnabled() || attacker == null || target == null) {
            return false;
        }

        SpellBehavior behavior = resolveMagicBehavior(spell, attribution, interactionType);
        if (behavior == null || behavior == SpellBehavior.SUPPORT) {
            return false;
        }

        Entity effectiveAttacker = resolveMagicSource(attacker, attribution);
        return effectiveAttacker != null && shouldBlockFriendlyFire(effectiveAttacker, target);
    }

    /**
     * Resolves the effective spell behavior from the strongest available magic evidence.
     * Returning null means Magic Team has no proof that the interaction is magic and
     * therefore must leave original gameplay untouched.
     */
    public static SpellBehavior resolveMagicBehavior(AbstractSpell spell,
                                                     MagicAttribution attribution,
                                                     MagicTeamEffectContext.InteractionType interactionType) {
        if (spell != null) {
            return getSpellBehavior(spell);
        }

        if (attribution != null) {
            SpellBehavior currentOverride = getSpellOverride(attribution.spellId());
            return currentOverride != null ? currentOverride : attribution.behavior();
        }

        if (interactionType == MagicTeamEffectContext.InteractionType.HARMFUL) {
            return SpellBehavior.HOSTILE;
        }

        if (interactionType == MagicTeamEffectContext.InteractionType.BENEFICIAL) {
            return SpellBehavior.SUPPORT;
        }

        return null;
    }

    /**
     * Resolves the original/root caster from persistent attribution when possible,
     * then falls back to Babel's live root-owner relationship for the interaction entity.
     */
    public static Entity resolveMagicSource(Entity interactionSource, MagicAttribution attribution) {
        if (interactionSource == null) {
            return null;
        }

        if (attribution != null && interactionSource.level() instanceof ServerLevel serverLevel) {
            Entity rootCaster = serverLevel.getEntity(attribution.rootCasterId());
            if (rootCaster != null) {
                return rootCaster;
            }

            Entity originalSource = serverLevel.getEntity(attribution.sourceEntityId());
            if (originalSource != null) {
                Entity root = getRootOwner(originalSource);
                return root != null ? root : originalSource;
            }
        }

        return resolveComparisonEntity(interactionSource);
    }

    public static SpellBehavior getDefaultSpellBehavior(AbstractSpell spell) {
        if (spell == null) {
            return SpellBehavior.HOSTILE;
        }

        String spellId = normalizeSpellId(spell.getSpellId());
        return DEFAULT_SUPPORT_SPELLS.contains(spellId) ? SpellBehavior.SUPPORT : SpellBehavior.HOSTILE;
    }

    public static SpellBehavior getSpellBehavior(AbstractSpell spell) {
        if (spell == null) {
            return SpellBehavior.HOSTILE;
        }

        SpellBehavior override = getSpellOverride(spell.getSpellId());
        return override != null ? override : getDefaultSpellBehavior(spell);
    }

    public static SpellBehavior getSpellOverride(String spellId) {
        String normalizedId = normalizeSpellId(spellId);
        for (String entry : MagicTeamConfig.SERVER.spellOverrides.get()) {
            if (entry == null) {
                continue;
            }

            int separator = entry.lastIndexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                continue;
            }

            String configuredId = normalizeSpellId(entry.substring(0, separator));
            if (!configuredId.equals(normalizedId)) {
                continue;
            }

            return SpellBehavior.parse(entry.substring(separator + 1));
        }
        return null;
    }

    public static boolean isHarmful(AbstractSpell spell) {
        return getSpellBehavior(spell) == SpellBehavior.HOSTILE;
    }

    public static boolean isSpellBeneficial(AbstractSpell spell) {
        return getSpellBehavior(spell) == SpellBehavior.SUPPORT;
    }

    private static String normalizeSpellId(String spellId) {
        return spellId == null ? "" : spellId.trim().toLowerCase();
    }

    private static Entity resolveComparisonEntity(Entity entity) {
        if (entity == null) {
            return null;
        }

        Entity root = ENTITY_RELATIONS.getRootOwner(entity);
        return root != null ? root : entity;
    }

    private static boolean isVanillaPotionSource(Entity source) {
        return source instanceof ThrownPotion || source instanceof AreaEffectCloud;
    }

    private static void debugDecision(Entity attacker, Entity target, boolean blocked,
                                      boolean allied, boolean sameResolvedEntity) {
        if (!debugEnabled) {
            return;
        }

        LOGGER.info(
                "Magic Team decision: action={}, attacker={}, target={}, allied={}, sameResolvedEntity={}",
                blocked ? "BLOCKED" : "ALLOWED",
                attacker.getScoreboardName(),
                target.getScoreboardName(),
                allied,
                sameResolvedEntity
        );
    }
}
