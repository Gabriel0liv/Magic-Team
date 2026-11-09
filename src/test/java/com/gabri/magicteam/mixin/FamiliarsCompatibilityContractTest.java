package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free regression checks for the reduced Familiars compatibility layer. */
public final class FamiliarsCompatibilityContractTest {
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/magic_team.familiars.mixins.json");

    private FamiliarsCompatibilityContractTest() {
    }

    public static void main(String[] args) throws Exception {
        ordinaryFamiliarsMagicUsesGlobalHooks();
        retaliationEventRemainsNarrowException();
    }

    private static void ordinaryFamiliarsMagicUsesGlobalHooks() throws Exception {
        String config = Files.readString(MIXIN_CONFIG);
        for (String removed : new String[]{
                "HikenFriendlyFireMixin",
                "IllusionistDecoyContextMixin",
                "MayhemDirectHitFriendlyFireMixin",
                "DragonEggFriendlyFireMixin",
                "LullabyFriendlyFireMixin",
                "SonataFriendlyFireMixin",
                "HarpExplosionFriendlyFireMixin"
        }) {
            check(!config.contains(removed), "ordinary Familiars magic must use global hooks: " + removed);
        }

        check(Files.isRegularFile(MIXIN_ROOT.resolve("EntityMagicAttributionMixin.java")),
                "global entity attribution is required for custom Familiars entities");
        check(Files.isRegularFile(MIXIN_ROOT.resolve("MobEffectInstanceMagicAttributionMixin.java")),
                "global effect attribution is required for delayed Familiars effects");
        check(Files.isRegularFile(MIXIN_ROOT.resolve("EntityMagicSideEffectMixin.java")),
                "global hostile side-effect gate is required for Familiars knockback/fire behavior");
    }

    private static void retaliationEventRemainsNarrowException() throws Exception {
        String adapter = "compat.familiars.ServerEventsRetaliationFriendlyFireMixin";
        Path sourcePath = MIXIN_ROOT.resolve("compat/familiars/ServerEventsRetaliationFriendlyFireMixin.java");
        String config = Files.readString(MIXIN_CONFIG);

        check(Files.isRegularFile(sourcePath), "Familiars retaliation event exception is missing");
        check(config.contains("\"" + adapter + "\""), "Familiars retaliation event exception is not registered");

        String source = Files.readString(sourcePath);
        check(source.contains("onDamageTaken"), "retaliation exception must remain scoped to LivingDamageEvent");
        check(source.contains("TeamUtils.shouldBlockFriendlyFire"),
                "retaliation exception must delegate to central friendly-fire policy");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
