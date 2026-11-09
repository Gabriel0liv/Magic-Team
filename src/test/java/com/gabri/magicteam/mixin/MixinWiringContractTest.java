package com.gabri.magicteam.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dependency-free structural regression checks for global-first mixin wiring. */
public final class MixinWiringContractTest {
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");
    private static final Path UTIL_ROOT = Path.of("src/main/java/com/gabri/magicteam/util");
    private static final List<Path> MIXIN_CONFIGS = List.of(
            Path.of("src/main/resources/magic_team.mixins.json"),
            Path.of("src/main/resources/magic_team.traveloptics.mixins.json"),
            Path.of("src/main/resources/magic_team.geomancyplus.mixins.json"),
            Path.of("src/main/resources/magic_team.familiars.mixins.json"),
            Path.of("src/main/resources/magic_team.cataclysm.mixins.json")
    );
    private static final Path MOB_DISPATCHER = MIXIN_ROOT.resolve("AbstractSpellCastingMobDispatchMixin.java");
    private static final Pattern RUNTIME_METHOD_CALL = Pattern.compile("\\.m_\\d+_\\s*\\(");

    private MixinWiringContractTest() {
    }

    public static void main(String[] args) throws Exception {
        globalEntityAttributionIsWired();
        globalEffectAttributionIsWired();
        globalTargetGateIsWired();
        globalSideEffectGatesAreWired();
        retainedTransactionalExceptionsRemainNarrow();
        mixinBodiesUseMappedMinecraftCalls();
        allMixinSourcesAreRegisteredAndAllRegistrationsExist();
        mobDispatcherLetsMixinRemapTheVanillaOverride();
    }

    private static void globalEntityAttributionIsWired() throws IOException {
        Path mixin = MIXIN_ROOT.resolve("EntityMagicAttributionMixin.java");
        String source = Files.readString(mixin);
        Set<String> registered = readAllMixinRegistrations();
        check(registered.contains("EntityMagicAttributionMixin"), "global entity attribution mixin is not registered");
        check(source.contains("addFreshEntity"), "entity attribution must capture spawned magic entities");
        check(source.contains("method = \"tickNonPassenger\"")
                        && source.contains("@At(\"HEAD\")")
                        && source.contains("@At(\"RETURN\")"),
                "entity attribution must wrap the stable tickNonPassenger method boundary");
        check(source.contains("MagicAttributionIndex.refresh"), "active attributed entities must refresh TTL");
        check(source.contains("MAGIC_TEAM_TICK_SCOPES") && source.contains("MagicTeamEffectContext.pop"),
                "paired entity tick scope must only pop contexts it actually pushed");
    }

    private static void globalEffectAttributionIsWired() throws IOException {
        Path effectMixin = MIXIN_ROOT.resolve("MobEffectInstanceMagicAttributionMixin.java");
        Path livingMixin = MIXIN_ROOT.resolve("LivingEntityMixin.java");
        String effect = Files.readString(effectMixin);
        String living = Files.readString(livingMixin);
        Set<String> registered = readAllMixinRegistrations();
        check(registered.contains("MobEffectInstanceMagicAttributionMixin"),
                "delayed MobEffect attribution mixin is not registered");
        check(living.contains("MagicEffectAttributionIndex.record"),
                "successful magic effects must persist their attribution");
        check(effect.contains("MagicEffectAttributionIndex.get"),
                "effect ticks must recover persistent attribution");
        check(effect.contains("MagicTeamEffectContext.push") && effect.contains("MagicTeamEffectContext.pop"),
                "effect ticks must run inside restored magic context");
    }

    private static void globalTargetGateIsWired() throws IOException {
        String policy = Files.readString(UTIL_ROOT.resolve("MagicTargetingPolicy.java"));
        String spell = Files.readString(MIXIN_ROOT.resolve("AbstractSpellMixin.java"));
        String playerTick = Files.readString(MIXIN_ROOT.resolve("MagicManagerCastDispatchMixin.java"));
        String mob = Files.readString(MIXIN_ROOT.resolve("AbstractSpellCastingMobDispatchMixin.java"));

        check(policy.contains("TargetEntityCastData"),
                "standard Iron's selected-target data must be handled by shared policy");
        check(policy.contains("TeamUtils.shouldBlockFriendlyFire"),
                "global target policy must use central Magic Team policy");
        check(spell.contains("MagicTargetingPolicy.shouldBlockSelectedTarget"),
                "AbstractSpell player dispatch must use shared target policy");
        check(playerTick.contains("MagicTargetingPolicy.shouldBlockSelectedTarget"),
                "player channel tick dispatch must use shared target policy");
        check(mob.contains("MagicTargetingPolicy.shouldBlockSelectedTarget"),
                "mob virtual dispatch must use shared target policy");
        check(!Files.exists(MIXIN_ROOT.resolve("compat/traveloptics/OrbitalVoidFriendlyFireMixin.java")),
                "Orbital Void must not regain a fragile spell-specific target redirect");
        check(!Files.exists(MIXIN_ROOT.resolve("compat/traveloptics/TidalGraspFriendlyFireMixin.java")),
                "Tidal Grasp standard target handling must remain global-first");
    }

    private static void globalSideEffectGatesAreWired() throws IOException {
        Set<String> registered = readAllMixinRegistrations();
        check(registered.contains("EntityMagicSideEffectMixin"),
                "global Entity side-effect gate is not registered");
        check(registered.contains("LivingEntityMagicSideEffectMixin"),
                "global LivingEntity side-effect gate is not registered");
        String entity = Files.readString(MIXIN_ROOT.resolve("EntityMagicSideEffectMixin.java"));
        String living = Files.readString(MIXIN_ROOT.resolve("LivingEntityMagicSideEffectMixin.java"));
        check(entity.contains("setDeltaMovement"), "forced movement must pass through global magic policy");
        check(entity.contains("setSecondsOnFire") || entity.contains("setRemainingFireTicks"),
                "magic fire side effects must pass through global policy");
        check(living.contains("removeEffect") && living.contains("removeAllEffects"),
                "hostile magic cleanse side effects must pass through global policy");
    }

    private static void retainedTransactionalExceptionsRemainNarrow() throws IOException {
        Path floodSlash = MIXIN_ROOT.resolve("compat/traveloptics/FloodSlashFriendlyFireMixin.java");
        Path tremorStep = MIXIN_ROOT.resolve("compat/geomancyplus/TremorStepFriendlyFireMixin.java");
        Path galenaShatter = MIXIN_ROOT.resolve("compat/traveloptics/GalenaShatterFriendlyFireMixin.java");

        check(Files.isRegularFile(floodSlash), "Flood Slash transaction exception is missing");
        String flood = Files.readString(floodSlash);
        check(flood.contains("victims.add") && flood.contains("ci.cancel()"),
                "Flood Slash exception must stop rewards while preserving victim bookkeeping");

        check(Files.isRegularFile(tremorStep), "Tremor Step side-effect exception is missing");
        String tremor = Files.readString(tremorStep);
        check(tremor.contains("getEntitiesOfClass") && tremor.contains("shouldBlockFriendlyFire"),
                "Tremor Step must filter before damage-adjacent invulnerability writes");

        check(Files.isRegularFile(galenaShatter), "Galena Shatter transaction exception is missing");
        String galena = Files.readString(galenaShatter);
        check(galena.contains("processStackedTarget") && galena.contains("setReturnValue(false)"),
                "Galena Shatter must gate stack consumption/mark creation before side effects");
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
        Set<String> registered = readAllMixinRegistrations();
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
                "class-level remap=false prevents mapping customServerAiStep");
        check(count(source, "method = \"customServerAiStep\"") == 2,
                "both mob tick redirects must target customServerAiStep");
        check(!source.contains("method = \"m_8024_()V\""),
                "runtime SRG name must not be hardcoded for the vanilla override");
    }

    private static Set<String> readAllMixinRegistrations() throws IOException {
        Set<String> result = new LinkedHashSet<>();
        for (Path config : MIXIN_CONFIGS) {
            check(Files.isRegularFile(config), "mixin config is missing: " + config);
            result.addAll(readMixinRegistrations(Files.readString(config)));
        }
        return result;
    }

    private static Set<String> readMixinRegistrations(String json) {
        int start = json.indexOf("\"mixins\"");
        int open = json.indexOf('[', start);
        int close = json.indexOf(']', open);
        check(start >= 0 && open >= 0 && close > open, "could not locate mixins array");
        Matcher matcher = Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(json.substring(open + 1, close));
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
        StringBuilder result = new StringBuilder(line.length());
        boolean inString = false;
        boolean escaped = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                result.append(' ');
            } else if (current == '"') {
                inString = true;
                result.append(' ');
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    private static int count(String source, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
