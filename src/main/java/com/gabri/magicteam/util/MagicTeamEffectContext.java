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

    private MagicTeamEffectContext() {
    }

    public static void push(Entity source, AbstractSpell spell, CastSource castSource) {
        push(source, spell, castSource, Origin.SPELL);
    }

    public static void push(Entity source) {
        push(source, null, null, Origin.ENTITY_SCOPE);
    }

    public static void pushVanillaPotion(Entity source) {
        push(source, null, null, Origin.VANILLA_POTION);
    }

    /**
     * Compatibility overload kept for existing callers while context origin is explicit internally.
     */
    public static void push(Entity source, AbstractSpell spell, CastSource castSource, boolean vanillaPotion) {
        Origin origin = vanillaPotion ? Origin.VANILLA_POTION : (spell != null ? Origin.SPELL : Origin.ENTITY_SCOPE);
        push(source, spell, castSource, origin);
    }

    private static void push(Entity source, AbstractSpell spell, CastSource castSource, Origin origin) {
        CURRENT.get().push(new Context(source, spell, castSource, origin));
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

    public static boolean isVanillaPotionApplication() {
        return getOrigin() == Origin.VANILLA_POTION;
    }

    /**
     * Only magic/spell scopes are allowed to influence LivingEntity#hurt.
     * Vanilla potion scopes exist solely to keep normal potion behavior untouched.
     */
    public static boolean shouldFilterDamage() {
        Origin origin = getOrigin();
        return origin != null && origin != Origin.VANILLA_POTION;
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

    private record Context(Entity source, AbstractSpell spell, CastSource castSource, Origin origin) {
    }
}
