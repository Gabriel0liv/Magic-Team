package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free regression checks for Alshanex's Familiars compatibility. */
public final class FamiliarsCompatibilityContractTest {
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/magic_team.familiars.mixins.json");

    private FamiliarsCompatibilityContractTest() {
    }

    public static void main(String[] args) throws Exception {
        mayhemDirectHitsBlockSideEffectsBeforeDamage();
        dragonEggDoesNotResetProtectedTargetInvulnerability();
        hostileBardSpellsHonorFriendlyFire();
        harpExplosionTreatsDamageControlAndEffectsAsOneHostileTransaction();
        retaliatoryPetEffectsHonorFriendlyFire();
        existingFamiliarsAdaptersRemainRegistered();
    }

    private static void mayhemDirectHitsBlockSideEffectsBeforeDamage() throws Exception {
        String adapter = "compat.familiars.MayhemDirectHitFriendlyFireMixin";
        Path sourcePath = MIXIN_ROOT.resolve("compat/familiars/MayhemDirectHitFriendlyFireMixin.java");
        String config = Files.readString(MIXIN_CONFIG);

        check(Files.isRegularFile(sourcePath), "Mayhem direct-hit adapter is missing");
        check(config.contains("\"" + adapter + "\""), "Mayhem direct-hit adapter is not registered");

        String source = Files.readString(sourcePath);
        check(source.contains("EndStoneEntity"), "End Stone knockback must be covered");
        check(source.contains("PurpurPilarEntity"), "Purpur Pillar knockback must be covered");
        check(source.contains("PurpurBricksEntity"), "Purpur Bricks knockback must be covered");
        check(source.contains("ChorusFlowerEntity"), "Chorus Flower teleport must be covered");
        check(source.contains("AbstractMagicProjectile;m_5790_"),
                "Mayhem adapter must let the Iron projectile superclass handle the impact before gating addon side effects");
        check(source.contains("Shift.AFTER"),
                "Mayhem adapter must gate only after the superclass impact hook");
        check(source.contains("TeamUtils.shouldBlockFriendlyFire"),
                "Mayhem direct-hit side effects must use the offensive friendly-fire policy");
        check(source.contains("discard()") && source.contains("ci.cancel()"),
                "protected Mayhem hits must still consume the projectile while skipping knockback/teleport");
    }

    private static void dragonEggDoesNotResetProtectedTargetInvulnerability() throws Exception {
        String adapter = "compat.familiars.DragonEggFriendlyFireMixin";
        Path sourcePath = MIXIN_ROOT.resolve("compat/familiars/DragonEggFriendlyFireMixin.java");
        String config = Files.readString(MIXIN_CONFIG);

        check(Files.isRegularFile(sourcePath), "Dragon Egg direct-hit adapter is missing");
        check(config.contains("\"" + adapter + "\""), "Dragon Egg direct-hit adapter is not registered");

        String source = Files.readString(sourcePath);
        check(source.contains("f_19802_"),
                "Dragon Egg adapter must intercept the direct target invulnerability-time write");
        check(source.contains("invulnerableTime"),
                "Dragon Egg adapter must preserve the mapped field write for allowed targets");
        check(source.contains("TeamUtils.shouldBlockFriendlyFire"),
                "Dragon Egg invulnerability reset must respect friendly-fire policy");
    }

    private static void hostileBardSpellsHonorFriendlyFire() throws Exception {
        String config = Files.readString(MIXIN_CONFIG);
        String[] adapters = {
                "compat.familiars.LullabyFriendlyFireMixin",
                "compat.familiars.SonataFriendlyFireMixin"
        };

        for (String adapter : adapters) {
            Path sourcePath = MIXIN_ROOT.resolve(adapter.replace('.', '/') + ".java");
            check(Files.isRegularFile(sourcePath), "hostile Bard adapter is missing: " + adapter);
            check(config.contains("\"" + adapter + "\""), "hostile Bard adapter is not registered: " + adapter);
            String source = Files.readString(sourcePath);
            check(source.contains("TeamUtils.shouldBlockFriendlyFire"),
                    "hostile Bard targeting must use friendly-fire policy: " + adapter);
            check(source.contains("m_7307_"),
                    "hostile Bard targeting must replace the runtime allied prefilter: " + adapter);
        }

        String lullaby = Files.readString(MIXIN_ROOT.resolve("compat/familiars/LullabyFriendlyFireMixin.java"));
        check(lullaby.contains("lambda$applySleepy$0"),
                "Lullaby must patch the SLEEPY target predicate rather than global alliance semantics");

        String sonata = Files.readString(MIXIN_ROOT.resolve("compat/familiars/SonataFriendlyFireMixin.java"));
        check(sonata.contains("lambda$shootNotes$0"),
                "Sonata must patch the GUIDING_BOLT target predicate rather than global alliance semantics");
    }

    private static void harpExplosionTreatsDamageControlAndEffectsAsOneHostileTransaction() throws Exception {
        String adapter = "compat.familiars.HarpExplosionFriendlyFireMixin";
        Path sourcePath = MIXIN_ROOT.resolve("compat/familiars/HarpExplosionFriendlyFireMixin.java");
        String config = Files.readString(MIXIN_CONFIG);

        check(Files.isRegularFile(sourcePath), "Harp Explosion adapter is missing");
        check(config.contains("\"" + adapter + "\""), "Harp Explosion adapter is not registered");

        String source = Files.readString(sourcePath);
        check(source.contains("Lnet/minecraft/world/level/Level;m_6249_"),
                "Harp Explosion must filter its custom age-26 target list before damage and side effects");
        check(source.contains("TeamUtils.shouldBlockFriendlyFire"),
                "Harp Explosion target filtering must use friendly-fire policy");
        check(source.contains("Lnet/minecraft/world/level/Explosion;m_46061_()V"),
                "Harp Explosion must scope the synchronous explosion damage");
        check(source.contains("InteractionType.HARMFUL"),
                "Harp Explosion explosion must execute under harmful interaction context");
        check(source.contains("try") && source.contains("finally"),
                "Harp Explosion explosion context must always be popped");
    }

    private static void retaliatoryPetEffectsHonorFriendlyFire() throws Exception {
        String adapter = "compat.familiars.ServerEventsRetaliationFriendlyFireMixin";
        Path sourcePath = MIXIN_ROOT.resolve("compat/familiars/ServerEventsRetaliationFriendlyFireMixin.java");
        String config = Files.readString(MIXIN_CONFIG);

        check(Files.isRegularFile(sourcePath), "Familiars ServerEvents retaliation adapter is missing");
        check(config.contains("\"" + adapter + "\""),
                "Familiars ServerEvents retaliation adapter is not registered");

        String source = Files.readString(sourcePath);
        check(source.contains("onDamageTaken"),
                "retaliation compatibility must be scoped to the LivingDamageEvent handler");
        check(source.contains("m_7311_"),
                "Scorcher retaliation fire ticks must be guarded");
        check(source.contains("Lnet/minecraft/world/level/Level;m_7967_"),
                "Plague retaliatory potion spawn must be guarded");
        check(source.contains("TeamUtils.shouldBlockFriendlyFire"),
                "retaliation must use the offensive friendly-fire policy");
    }

    private static void existingFamiliarsAdaptersRemainRegistered() throws Exception {
        String config = Files.readString(MIXIN_CONFIG);
        check(config.contains("\"compat.familiars.HikenFriendlyFireMixin\""),
                "Hiken compatibility must remain registered");
        check(config.contains("\"compat.familiars.IllusionistDecoyContextMixin\""),
                "Illusionist Decoy explosion context must remain registered");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
