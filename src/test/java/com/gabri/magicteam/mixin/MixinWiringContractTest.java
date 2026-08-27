package com.gabri.magicteam.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dependency-free structural regression checks for mixin wiring.
 *
 * <p>This intentionally reads source/resources from the checkout so it can run
 * without Forge, Iron's Spellbooks, or Babel Core on the classpath.</p>
 */
public final class MixinWiringContractTest {
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");
    private static final Path UTIL_ROOT = Path.of("src/main/java/com/gabri/magicteam/util");
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/magic_team.mixins.json");
    private static final Path MOD_ENTRY = Path.of("src/main/java/com/gabri/magicteam/MagicTeam.java");
    private static final Path MOB_DISPATCHER = MIXIN_ROOT.resolve("AbstractSpellCastingMobDispatchMixin.java");
    private static final Pattern RUNTIME_METHOD_CALL = Pattern.compile("\\.m_\\d+_\\s*\\(");
    private static final List<String> REQUIRED_DELAYED_HOSTILE_ADAPTERS = List.of(
            "compat.traveloptics.BanishFriendlyFireMixin",
            "compat.traveloptics.ReversalFriendlyFireMixin",
            "compat.traveloptics.CrimsonDescendFriendlyFireMixin",
            "compat.traveloptics.SpiritDamageHelperFriendlyFireMixin",
            "compat.traveloptics.CursedWraithbladeFriendlyFireMixin",
            "compat.traveloptics.HarbingersWrathFriendlyFireMixin",
            "compat.traveloptics.CataclysmFlameJetFriendlyFireMixin",
            "compat.traveloptics.CataclysmAmethystClusterFriendlyFireMixin",
            "compat.traveloptics.CataclysmVoidRuneFriendlyFireMixin",
            "compat.traveloptics.CataclysmAxeBladeFriendlyFireMixin",
            "compat.traveloptics.CataclysmLaserBeamFriendlyFireMixin",
            "compat.traveloptics.CataclysmPhantomArrowFriendlyFireMixin",
            "compat.traveloptics.CataclysmAncientDesertSteleFriendlyFireMixin",
            "compat.traveloptics.ArcaneArtilleryTargetingMixin",
            "compat.traveloptics.ScourgeOfTheSandsTargetingMixin",
            "compat.traveloptics.ScourgeOfTheSandsLevelThreeTargetingMixin",
            "compat.traveloptics.GyroSlashFriendlyFireMixin",
            "compat.traveloptics.DragonSpiritSpellFriendlyFireMixin",
            "compat.traveloptics.GalenaShatterFriendlyFireMixin",
            "compat.traveloptics.GalenaMarkFriendlyFireMixin",
            "compat.traveloptics.TidalGraspFriendlyFireMixin",
            "compat.traveloptics.FloodSlashFriendlyFireMixin"
    );

    private MixinWiringContractTest() {
    }

    public static void main(String[] args) throws Exception {
        confirmedDelayedHostileBypassesHaveAdapters();
        flareVacuumPreservesOriginalCaster();
        galenaMarkRechecksFriendlyFireDuringLifetime();
        tidalGraspUsesHostileContextForDelayedEffects();
        floodSlashDoesNotRewardBlockedHits();
        mixinBodiesUseMappedMinecraftCalls();
        allMixinSourcesAreRegisteredAndAllRegistrationsExist();
        mobDispatcherLetsMixinRemapTheVanillaOverride();
    }

    private static void confirmedDelayedHostileBypassesHaveAdapters() throws IOException {
        String json = Files.readString(MIXIN_CONFIG);
        Set<String> registered = readMixinRegistrations(json);

        for (String adapter : REQUIRED_DELAYED_HOSTILE_ADAPTERS) {
            Path source = MIXIN_ROOT.resolve(adapter.replace('.', '/') + ".java");
            check(Files.isRegularFile(source), "confirmed hostile bypass has no adapter source: " + adapter);
            check(registered.contains(adapter), "confirmed hostile bypass adapter is not registered: " + adapter);
            check(Files.readString(source).contains("TeamUtils.shouldBlockFriendlyFire"),
                    "confirmed hostile bypass adapter does not use friendly-fire policy: " + adapter);
        }
    }

    private static void flareVacuumPreservesOriginalCaster() throws IOException {
        String adapter = "compat.traveloptics.FlareVacuumAttributionMixin";
        Path attribution = UTIL_ROOT.resolve("FlareVacuumAttribution.java");
        Path gyro = MIXIN_ROOT.resolve("compat/traveloptics/GyroSlashFriendlyFireMixin.java");
        Path flareVacuum = MIXIN_ROOT.resolve("compat/traveloptics/FlareVacuumAttributionMixin.java");
        Set<String> registered = readMixinRegistrations(Files.readString(MIXIN_CONFIG));

        check(Files.isRegularFile(attribution), "Flare Vacuum attribution tracker is missing");
        check(Files.isRegularFile(flareVacuum), "Flare Vacuum attribution mixin is missing");
        check(registered.contains(adapter), "Flare Vacuum attribution mixin is not registered");

        String gyroSource = Files.readString(gyro);
        check(gyroSource.contains("FlareVacuumAttribution.record"),
                "Gyro Slash must record the caster only after Flare Vacuum is applied");
        check(gyroSource.contains("if (applied)"),
                "Gyro Slash must not replace attribution when addEffect rejects the reapplication");

        String flareSource = Files.readString(flareVacuum);
        check(flareSource.contains("FlareVacuumAttribution.begin"),
                "Flare Vacuum tick must enter the stored caster scope");
        check(flareSource.contains("FlareVacuumAttribution.end"),
                "Flare Vacuum tick must always leave the stored caster scope");
        check(flareSource.contains("FlareVacuumAttribution.getActiveSource"),
                "Flare Vacuum Flame Jets must receive the active original caster");
        check(flareSource.contains("@ModifyArg"),
                "Flare Vacuum must restore caster at the Flame Jet constructor call site");

        String attributionSource = Files.readString(attribution);
        check(attributionSource.contains("getActiveDepth"),
                "Flare Vacuum attribution context must expose depth for leak detection");
        check(attributionSource.contains("clearActiveContext"),
                "Flare Vacuum attribution context must support emergency clearing");

        String modEntry = Files.readString(MOD_ENTRY);
        check(modEntry.contains("FlareVacuumAttribution.getActiveDepth"),
                "server tick leak guard must inspect Flare Vacuum attribution context");
        check(modEntry.contains("FlareVacuumAttribution.clearActiveContext"),
                "server tick leak guard must clear stale Flare Vacuum attribution context");
    }

    private static void galenaMarkRechecksFriendlyFireDuringLifetime() throws IOException {
        Path shatter = MIXIN_ROOT.resolve("compat/traveloptics/GalenaShatterFriendlyFireMixin.java");
        Path mark = MIXIN_ROOT.resolve("compat/traveloptics/GalenaMarkFriendlyFireMixin.java");

        check(Files.isRegularFile(shatter), "Galena Shatter application adapter is missing");
        check(Files.isRegularFile(mark), "Galena Mark lifetime adapter is missing");

        String shatterSource = Files.readString(shatter);
        check(shatterSource.contains("processStackedTarget"),
                "Galena Shatter must gate before consuming stacks and applying a mark");
        check(shatterSource.contains("setReturnValue(false)"),
                "protected Galena Shatter targets must be rejected before mark application");

        String markSource = Files.readString(mark);
        check(markSource.contains("@Invoker(\"getTarget\")"),
                "Galena Mark adapter must resolve its persisted target");
        check(markSource.contains("@Invoker(\"getCaster\")"),
                "Galena Mark adapter must resolve its persisted caster");
        check(markSource.contains("triggerMagneticBlast"),
                "Galena Mark must recheck friendly fire before the delayed magnetic blast");
        check(markSource.contains("method = \"m_8119_()V\""),
                "Galena Mark must recheck friendly fire during damage ticks");
        check(markSource.contains("require = 2"),
                "Galena Mark must gate both pull and push movement writes");
    }

    private static void tidalGraspUsesHostileContextForDelayedEffects() throws IOException {
        String adapter = "compat.traveloptics.TidalGraspEffectContextMixin";
        Path spell = MIXIN_ROOT.resolve("compat/traveloptics/TidalGraspFriendlyFireMixin.java");
        Path effect = MIXIN_ROOT.resolve("compat/traveloptics/TidalGraspEffectContextMixin.java");
        Set<String> registered = readMixinRegistrations(Files.readString(MIXIN_CONFIG));

        check(Files.isRegularFile(spell), "Tidal Grasp spell adapter is missing");
        check(Files.isRegularFile(effect), "Tidal Grasp delayed-effect context adapter is missing");
        check(registered.contains(adapter), "Tidal Grasp delayed-effect context adapter is not registered");

        String spellSource = Files.readString(spell);
        check(spellSource.contains("checkPreCastConditions"),
                "Tidal Grasp must reject a protected target before the cast starts");
        check(spellSource.contains("onServerCastTick"),
                "Tidal Grasp must recheck the target while channeling");
        check(spellSource.contains("onCast"),
                "Tidal Grasp must recheck helper/teleport operations at release");

        String effectSource = Files.readString(effect);
        check(effectSource.contains("InteractionType.HARMFUL"),
                "Tidal Grasp detonation must classify stun/wet/damage as harmful");
        check(effectSource.contains("MagicTeamEffectContext.push"),
                "Tidal Grasp detonation must enter harmful effect context");
        check(effectSource.contains("MagicTeamEffectContext.pop"),
                "Tidal Grasp detonation must leave harmful effect context");
    }

    private static void floodSlashDoesNotRewardBlockedHits() throws IOException {
        Path adapter = MIXIN_ROOT.resolve("compat/traveloptics/FloodSlashFriendlyFireMixin.java");
        check(Files.isRegularFile(adapter), "Flood Slash adapter is missing");
        String source = Files.readString(adapter);
        check(source.contains("@Shadow") && source.contains("victims"),
                "Flood Slash must preserve victim bookkeeping for blocked targets");
        check(source.contains("victims.add"),
                "Flood Slash must mark protected targets as processed to prevent retry loops");
        check(source.contains("ci.cancel()"),
                "Flood Slash must stop damage, Wet, mana and Replenish rewards on a blocked hit");
    }

    private static void mixinBodiesUseMappedMinecraftCalls() throws IOException {
        try (var paths = Files.walk(MIXIN_ROOT)) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
                int lineNumber = 0;
                for (String line : Files.readAllLines(path)) {
                    lineNumber++;
                    String withoutStringLiterals = stripStringLiterals(line);
                    check(!RUNTIME_METHOD_CALL.matcher(withoutStringLiterals).find(),
                            "raw runtime Minecraft method call in Java body: " + path + ":" + lineNumber);
                }
            }
        }
    }

    private static void allMixinSourcesAreRegisteredAndAllRegistrationsExist() throws IOException {
        String json = Files.readString(MIXIN_CONFIG);
        Set<String> registered = readMixinRegistrations(json);
        Set<String> sources = new LinkedHashSet<>();

        try (var paths = Files.walk(MIXIN_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> readUnchecked(path).contains("@Mixin"))
                    .map(MixinWiringContractTest::toMixinName)
                    .forEach(sources::add);
        }

        for (String source : sources) {
            check(registered.contains(source), "mixin source is not registered: " + source);
        }

        for (String registration : registered) {
            Path source = MIXIN_ROOT.resolve(registration.replace('.', '/') + ".java");
            check(Files.isRegularFile(source), "mixin registration has no source file: " + registration);
        }
    }

    private static void mobDispatcherLetsMixinRemapTheVanillaOverride() throws IOException {
        String source = Files.readString(MOB_DISPATCHER);

        check(!source.contains("@Mixin(value = AbstractSpellCastingMob.class, remap = false)"),
                "class-level remap=false prevents mapping customServerAiStep in dev/reobf environments");
        check(count(source, "method = \"customServerAiStep\"") == 2,
                "both mob tick redirects must target the Mojmap customServerAiStep selector");
        check(!source.contains("method = \"m_8024_()V\""),
                "runtime SRG name must not be hardcoded for the vanilla override");
    }

    private static Set<String> readMixinRegistrations(String json) {
        int start = json.indexOf("\"mixins\"");
        int open = json.indexOf('[', start);
        int close = json.indexOf(']', open);
        check(start >= 0 && open >= 0 && close > open, "could not locate mixins array");

        String body = json.substring(open + 1, close);
        Matcher matcher = Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(body);
        Set<String> result = new LinkedHashSet<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private static String toMixinName(Path path) {
        String relative = MIXIN_ROOT.relativize(path).toString();
        return relative.substring(0, relative.length() - ".java".length()).replace('\\', '.').replace('/', '.');
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("could not read " + path, exception);
        }
    }

    private static String stripStringLiterals(String line) {
        return line.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"");
    }

    private static int count(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
