package com.gabri.magicteam.util;

/**
 * Dependency-free regression contract for MagicTeamEffectContext lifecycle semantics.
 */
public final class MagicTeamEffectContextContractTest {
    private MagicTeamEffectContextContractTest() {
    }

    public static void main(String[] args) {
        MagicTeamEffectContext.clear();
        check(MagicTeamEffectContext.getDepth() == 0, "context starts empty");

        MagicTeamEffectContext.push((net.minecraft.world.entity.Entity) null);
        check(MagicTeamEffectContext.getDepth() == 1, "entity scope increments depth");
        check(MagicTeamEffectContext.getOrigin() == MagicTeamEffectContext.Origin.ENTITY_SCOPE,
                "plain entity push is classified as ENTITY_SCOPE");
        check(MagicTeamEffectContext.shouldFilterDamage(),
                "entity scopes may filter synchronous magic damage");
        MagicTeamEffectContext.pop();

        MagicTeamEffectContext.pushVanillaPotion(null);
        check(MagicTeamEffectContext.getOrigin() == MagicTeamEffectContext.Origin.VANILLA_POTION,
                "vanilla potion scope is classified explicitly");
        check(!MagicTeamEffectContext.shouldFilterDamage(),
                "vanilla potion scope must not filter LivingEntity#hurt");

        MagicTeamEffectContext.clear();
        check(!MagicTeamEffectContext.hasContext(), "clear removes a leaked context");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
