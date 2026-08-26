package com.gabri.magicteam.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
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
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/magic_team.mixins.json");
    private static final Path MOB_DISPATCHER = MIXIN_ROOT.resolve("AbstractSpellCastingMobDispatchMixin.java");

    private MixinWiringContractTest() {
    }

    public static void main(String[] args) throws Exception {
        allMixinSourcesAreRegisteredAndAllRegistrationsExist();
        mobDispatcherLetsMixinRemapTheVanillaOverride();
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
