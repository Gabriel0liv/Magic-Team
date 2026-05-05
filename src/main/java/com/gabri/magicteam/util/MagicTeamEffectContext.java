package com.gabri.magicteam.util;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MagicTeamEffectContext {
    private static final ThreadLocal<Deque<Context>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);

    private MagicTeamEffectContext() {
    }

    public static void push(Entity source, AbstractSpell spell, CastSource castSource) {
        CURRENT.get().push(new Context(source, spell, castSource));
    }

    public static void push(Entity source) {
        push(source, null, null);
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

    public static Entity getSource() {
        Context context = CURRENT.get().peek();
        return context != null ? context.source : null;
    }

    public static AbstractSpell getSpell() {
        Context context = CURRENT.get().peek();
        return context != null ? context.spell : null;
    }

    public static CastSource getCastSource() {
        Context context = CURRENT.get().peek();
        return context != null ? context.castSource : null;
    }

    private record Context(Entity source, AbstractSpell spell, CastSource castSource) {
    }
}
