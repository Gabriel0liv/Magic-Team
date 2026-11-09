package com.gabri.magicteam.util;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MagicTeamEffectContext {
    /*
     * No initial value on purpose: read paths run on hot vanilla hooks
     * (setDeltaMovement, hurt, addEffect, isAlliedTo) for every entity, so an
     * empty context must be a plain null check with no allocation or
     * ThreadLocalMap mutation.
     */
    private static final ThreadLocal<Deque<Context>> CURRENT = new ThreadLocal<>();
    /** Contexts hidden by {@link #suspend()} so an unrelated nested tick cannot inherit them. */
    private static final ThreadLocal<Deque<Deque<Context>>> SUSPENDED = new ThreadLocal<>();
    private static final Deque<Context> EMPTY_SUSPENDED_MARKER = new ArrayDeque<>(0);
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
        Deque<Context> stack = CURRENT.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            CURRENT.set(stack);
        }
        stack.push(new Context(
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
        if (stack == null) {
            return;
        }

        if (!stack.isEmpty()) {
            stack.pop();
        }

        if (stack.isEmpty()) {
            CURRENT.remove();
        }
    }

    /**
     * Hides the whole current context stack until {@link #resume()}. Used when
     * the server ticks an entity nested inside another entity's attributed
     * tick (for example a passenger), which must not inherit that magic scope.
     */
    public static void suspend() {
        Deque<Deque<Context>> suspended = SUSPENDED.get();
        if (suspended == null) {
            suspended = new ArrayDeque<>();
            SUSPENDED.set(suspended);
        }

        Deque<Context> stack = CURRENT.get();
        suspended.push(stack == null || stack.isEmpty() ? EMPTY_SUSPENDED_MARKER : stack);
        CURRENT.remove();
    }

    /** Restores the stack hidden by the matching {@link #suspend()} call. */
    public static void resume() {
        Deque<Deque<Context>> suspended = SUSPENDED.get();
        if (suspended == null || suspended.isEmpty()) {
            SUSPENDED.remove();
            return;
        }

        Deque<Context> restored = suspended.pop();
        if (suspended.isEmpty()) {
            SUSPENDED.remove();
        }

        if (restored == EMPTY_SUSPENDED_MARKER) {
            CURRENT.remove();
        } else {
            CURRENT.set(restored);
        }
    }

    public static void clear() {
        CURRENT.remove();
        SUSPENDED.remove();
    }

    public static int getDepth() {
        Deque<Context> stack = CURRENT.get();
        return stack == null ? 0 : stack.size();
    }

    /** Number of hidden stacks still waiting for {@link #resume()}; non-zero at tick end means a leak. */
    public static int getSuspendedDepth() {
        Deque<Deque<Context>> suspended = SUSPENDED.get();
        return suspended == null ? 0 : suspended.size();
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
        Context context = stack == null ? null : stack.peek();
        if (context == null) {
            return "depth=0, suspended=" + getSuspendedDepth();
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
        return stack == null ? null : stack.peek();
    }

    private record Context(Entity source,
                           AbstractSpell spell,
                           CastSource castSource,
                           Origin origin,
                           InteractionType interactionType,
                           MagicAttribution persistentAttribution) {
    }
}
