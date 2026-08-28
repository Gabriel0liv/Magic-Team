package com.gabri.magicteam.util;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MagicTeamEffectContext {
    private static final ThreadLocal<Deque<Context>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);

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
        push(source, spell, castSource, Origin.SPELL, InteractionType.GENERIC);
    }

    public static void push(Entity source, AbstractSpell spell, CastSource castSource, InteractionType interactionType) {
        push(source, spell, castSource, Origin.SPELL, interactionType);
    }

    public static void push(Entity source) {
        push(source, null, null, Origin.ENTITY_SCOPE, InteractionType.GENERIC);
    }

    public static void push(Entity source, InteractionType interactionType) {
        push(source, null, null, Origin.ENTITY_SCOPE, interactionType);
    }

    public static void pushVanillaPotion(Entity source) {
        push(source, null, null, Origin.VANILLA_POTION, InteractionType.GENERIC);
    }

    /**
     * Compatibility overload kept for existing callers while context origin is explicit internally.
     */
    public static void push(Entity source, AbstractSpell spell, CastSource castSource, boolean vanillaPotion) {
        Origin origin = vanillaPotion ? Origin.VANILLA_POTION : (spell != null ? Origin.SPELL : Origin.ENTITY_SCOPE);
        push(source, spell, castSource, origin, InteractionType.GENERIC);
    }

    private static void push(Entity source, AbstractSpell spell, CastSource castSource, Origin origin, InteractionType interactionType) {
        InteractionType normalizedInteraction = interactionType == null ? InteractionType.GENERIC : interactionType;
        CURRENT.get().push(new Context(source, spell, castSource, origin, normalizedInteraction));
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
        String spellId = context.spell == null ? "null" : context.spell.getSpellId();
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

    private record Context(Entity source, AbstractSpell spell, CastSource castSource, Origin origin, InteractionType interactionType) {
    }
}
