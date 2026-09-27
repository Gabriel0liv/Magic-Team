package com.gabri.magicteam.util;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MagicTeamEffectContext {
    private static final ThreadLocal<Deque<Context>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);
    private static final long DEFAULT_ATTRIBUTION_TTL_TICKS = 20L * 60L * 5L;

    public enum Origin {
        SPELL,
        ENTITY_SCOPE,
        VANILLA_POTION
    }

    public enum InteractionType {
        GENERIC,
        HARMFUL,
        BENEFICIAL
    }

    private MagicTeamEffectContext() {
    }

    public static void push(Entity source, AbstractSpell spell, CastSource castSource) {
        push(source, spell, castSource, Origin.SPELL, InteractionType.GENERIC, null);
    }

    public static void push(Entity source, AbstractSpell spell, CastSource castSource, InteractionType interactionType) {
        push(source, spell, castSource, Origin.SPELL, interactionType, null);
    }

    public static void push(Entity source) {
        push(source, null, null, Origin.ENTITY_SCOPE, InteractionType.GENERIC, null);
    }

    public static void push(Entity source, InteractionType interactionType) {
        push(source, null, null, Origin.ENTITY_SCOPE, interactionType, null);
    }

    /**
     * Re-enters a transient interaction scope from persistent attribution.
     */
    public static void push(Entity source, MagicAttribution attribution) {
        if (attribution == null) {
            push(source);
            return;
        }
        push(source, null, null, Origin.ENTITY_SCOPE, attribution.interactionType(), attribution);
    }

    public static void pushVanillaPotion(Entity source) {
        push(source, null, null, Origin.VANILLA_POTION, InteractionType.GENERIC, null);
    }

    /**
     * Compatibility overload kept for existing callers while context origin is explicit internally.
     */
    public static void push(Entity source, AbstractSpell spell, CastSource castSource, boolean vanillaPotion) {
        Origin origin = vanillaPotion ? Origin.VANILLA_POTION : (spell != null ? Origin.SPELL : Origin.ENTITY_SCOPE);
        push(source, spell, castSource, origin, InteractionType.GENERIC, null);
    }

    private static void push(Entity source,
                             AbstractSpell spell,
                             CastSource castSource,
                             Origin origin,
                             InteractionType interactionType,
                             MagicAttribution persistentAttribution) {
        InteractionType normalizedInteraction = interactionType == null ? InteractionType.GENERIC : interactionType;
        CURRENT.get().push(new Context(
                source,
                spell,
                castSource,
                origin,
                normalizedInteraction,
                persistentAttribution
        ));
    }

    public static void pop() {
        Deque<Context> stack = CURRENT.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }

        if (stack.isEmpty()) {
            CURRENT.remove();
        }
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static int getDepth() {
        Deque<Context> stack = CURRENT.get();
        int depth = stack.size();
        if (depth == 0) {
            CURRENT.remove();
        }
        return depth;
    }

    public static boolean hasContext() {
        return getDepth() > 0;
    }

    public static Entity getSource() {
        Context context = current();
        return context != null ? context.source : null;
    }

    public static AbstractSpell getSpell() {
        Context context = current();
        return context != null ? context.spell : null;
    }

    public static CastSource getCastSource() {
        Context context = current();
        return context != null ? context.castSource : null;
    }

    public static Origin getOrigin() {
        Context context = current();
        return context != null ? context.origin : null;
    }

    public static InteractionType getInteractionType() {
        Context context = current();
        return context != null ? context.interactionType : null;
    }

    public static boolean isHarmfulInteraction() {
        return getInteractionType() == InteractionType.HARMFUL;
    }

    public static boolean isVanillaPotionApplication() {
        return getOrigin() == Origin.VANILLA_POTION;
    }

    /**
     * Produces a stable value snapshot for an interaction that may outlive this
     * thread-local scope. Returns null when the current scope does not prove a
     * spell/magic origin strongly enough to persist.
     */
    public static MagicAttribution currentAttribution() {
        Context context = current();
        if (context == null) {
            return null;
        }

        if (context.persistentAttribution != null) {
            return context.persistentAttribution;
        }

        if (context.source == null || context.spell == null || context.origin != Origin.SPELL) {
            return null;
        }

        Entity rootOwner = TeamUtils.getRootOwner(context.source);
        Entity effectiveRoot = rootOwner != null ? rootOwner : context.source;
        SpellBehavior behavior = TeamUtils.getSpellBehavior(context.spell);
        InteractionType interaction = context.interactionType == InteractionType.GENERIC
                ? MagicAttribution.interactionFor(behavior)
                : context.interactionType;
        long expiresAtTick = context.source.level().getGameTime() + DEFAULT_ATTRIBUTION_TTL_TICKS;

        return new MagicAttribution(
                context.source.getUUID(),
                effectiveRoot.getUUID(),
                context.spell.getSpellId(),
                behavior,
                interaction,
                expiresAtTick
        );
    }

    /**
     * Magic/spell scopes may influence LivingEntity#hurt unless the scope is
     * explicitly beneficial. Vanilla potion scopes always remain untouched.
     */
    public static boolean shouldFilterDamage() {
        Origin origin = getOrigin();
        InteractionType interactionType = getInteractionType();
        return origin != null
                && origin != Origin.VANILLA_POTION
                && interactionType != InteractionType.BENEFICIAL;
    }

    public static String describeCurrentContext() {
        Deque<Context> stack = CURRENT.get();
        Context context = stack.peek();
        if (context == null) {
            CURRENT.remove();
            return "depth=0";
        }

        String sourceType = context.source == null ? "null" : context.source.getClass().getName();
        String spellId = context.spell == null
                ? (context.persistentAttribution == null ? "null" : context.persistentAttribution.spellId())
                : context.spell.getSpellId();
        String castSource = context.castSource == null ? "null" : context.castSource.name();
        return "depth=" + stack.size()
                + ", origin=" + context.origin
                + ", interaction=" + context.interactionType
                + ", source=" + sourceType
                + ", spell=" + spellId
                + ", castSource=" + castSource;
    }

    private static Context current() {
        Deque<Context> stack = CURRENT.get();
        Context context = stack.peek();
        if (context == null) {
            CURRENT.remove();
        }
        return context;
    }

    private record Context(Entity source,
                           AbstractSpell spell,
                           CastSource castSource,
                           Origin origin,
                           InteractionType interactionType,
                           MagicAttribution persistentAttribution) {
    }
}
