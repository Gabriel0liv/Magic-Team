package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free contract for final global magic damage/effect enforcement. */
public final class GlobalMagicEnforcementContractTest {
    private static final Path TEAM_UTILS = Path.of("src/main/java/com/gabri/magicteam/util/TeamUtils.java");
    private static final Path LIVING = Path.of("src/main/java/com/gabri/magicteam/mixin/LivingEntityMixin.java");
    private static final Path DAMAGE = Path.of("src/main/java/com/gabri/magicteam/mixin/DamageSourcesMixin.java");
    private static final Path CLOUD = Path.of("src/main/java/com/gabri/magicteam/mixin/AreaEffectCloudMixin.java");
    private static final Path POTION = Path.of("src/main/java/com/gabri/magicteam/mixin/ThrownPotionMixin.java");

    private GlobalMagicEnforcementContractTest() {
    }

    public static void main(String[] args) throws Exception {
        String teamUtils = Files.readString(TEAM_UTILS);
        String living = Files.readString(LIVING);
        String damage = Files.readString(DAMAGE);
        String cloud = Files.readString(CLOUD);
        String potion = Files.readString(POTION);

        check(teamUtils.contains("resolveMagicBehavior"),
                "global enforcement must centralize support/hostile behavior resolution");
        check(teamUtils.contains("resolveMagicSource"),
                "global enforcement must centralize original/root caster resolution");
        check(teamUtils.contains("if (!isEnabled())"),
                "disabled Magic Team must leave gameplay transparent");

        check(living.contains("hasMagicEvidence"),
                "generic LivingEntity damage gate must require positive magic evidence");
        check(living.contains("spell != null") && living.contains("attribution != null"),
                "live spells and persistent attribution must both count as magic evidence");
        check(living.contains("MagicEffectAttributionIndex.record"),
                "successful magic effects must persist delayed-effect attribution");

        check(damage.contains("instanceof SpellDamageSource"),
                "Iron's native SpellDamageSource must remain preferred evidence");
        check(damage.contains("MagicAttributionIndex.get"),
                "custom/delayed damage must be able to fall back to persistent attribution");

        check(cloud.contains("MagicAttributionIndex.get") && potion.contains("MagicAttributionIndex.get"),
                "vanilla cloud/potion entities must preserve magic attribution when spawned by spells");
        check(teamUtils.contains("isVanillaPotionSource(source) && attribution == null"),
                "plain vanilla potions must bypass Magic Team while attributed magic potions do not");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
