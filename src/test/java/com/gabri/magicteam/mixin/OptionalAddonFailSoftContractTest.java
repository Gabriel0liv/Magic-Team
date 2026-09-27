package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dependency-free contract for fail-soft optional addon mechanism exceptions. */
public final class OptionalAddonFailSoftContractTest {
    private static final Path RESOURCE_ROOT = Path.of("src/main/resources");
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");
    private static final Path UTIL_ROOT = Path.of("src/main/java/com/gabri/magicteam/util");
    private static final Path CORE_CONFIG = RESOURCE_ROOT.resolve("magic_team.mixins.json");
    private static final List<Path> OPTIONAL_CONFIGS = List.of(
            RESOURCE_ROOT.resolve("magic_team.traveloptics.mixins.json"),
            RESOURCE_ROOT.resolve("magic_team.geomancyplus.mixins.json"),
            RESOURCE_ROOT.resolve("magic_team.familiars.mixins.json"),
            RESOURCE_ROOT.resolve("magic_team.cataclysm.mixins.json")
    );
    private static final Pattern POSITIVE_REQUIRE = Pattern.compile("\\brequire\\s*=\\s*([1-9][0-9]*)");
    private static final Pattern MIXIN_ENTRY = Pattern.compile("\\\"([^\\\"]+)\\\"");
    private static final String DIAGNOSTIC_PLUGIN =
            "com.gabri.magicteam.mixin.compat.OptionalAddonMixinPlugin";

    private OptionalAddonFailSoftContractTest() {
    }

    public static void main(String[] args) throws Exception {
        coreRemainsStrict();
        optionalConfigsAreFailSoft();
        optionalAdaptersCannotReintroduceFatalRequirements();
        diagnosticsDistinguishOptionalFromCore();
        orbitalVoidRegressionIsRemovedFromOptionalLayer();
    }

    private static void coreRemainsStrict() throws Exception {
        String core = Files.readString(CORE_CONFIG);
        check(core.contains("\"required\": true"), "core mixin config must remain required");
        check(core.contains("\"defaultRequire\": 1"), "core mixin config must remain strict");
        check(!core.contains("\"plugin\": \"" + DIAGNOSTIC_PLUGIN + "\""),
                "optional degradation plugin must not be attached to the strict core config");
    }

    private static void optionalConfigsAreFailSoft() throws Exception {
        for (Path configPath : OPTIONAL_CONFIGS) {
            String config = Files.readString(configPath);
            check(config.contains("\"required\": false"),
                    "optional config must remain non-required: " + configPath);
            check(config.contains("\"defaultRequire\": 0"),
                    "optional config must make zero-match injectors non-fatal: " + configPath);
            check(config.contains("\"plugin\": \"" + DIAGNOSTIC_PLUGIN + "\""),
                    "optional config must install degradation diagnostics: " + configPath);
        }
    }

    private static void optionalAdaptersCannotReintroduceFatalRequirements() throws Exception {
        List<String> offenders = new ArrayList<>();
        for (Path configPath : OPTIONAL_CONFIGS) {
            for (String adapter : readMixinRegistrations(Files.readString(configPath))) {
                Path source = MIXIN_ROOT.resolve(adapter.replace('.', '/') + ".java");
                check(Files.isRegularFile(source), "optional adapter source is missing: " + adapter);
                Matcher matcher = POSITIVE_REQUIRE.matcher(Files.readString(source));
                if (matcher.find()) {
                    offenders.add(adapter + " (require=" + matcher.group(1) + ")");
                }
            }
        }
        check(offenders.isEmpty(),
                "optional adapters must not declare positive injector require values: " + offenders);
    }

    private static void diagnosticsDistinguishOptionalFromCore() throws Exception {
        Path plugin = MIXIN_ROOT.resolve("compat/OptionalAddonMixinPlugin.java");
        Path handler = MIXIN_ROOT.resolve("compat/OptionalAddonMixinErrorHandler.java");
        check(Files.isRegularFile(plugin), "optional mixin diagnostic plugin is missing");
        check(Files.isRegularFile(handler), "optional mixin error handler is missing");

        String pluginSource = Files.readString(plugin);
        String handlerSource = Files.readString(handler);
        check(pluginSource.contains("Mixins.registerErrorHandlerClass"),
                "optional config plugin must register the error handler before application");
        check(handlerSource.contains("return ErrorAction.WARN"),
                "prepare-time optional failures must be skippable when safe");
        check(handlerSource.contains("onApplyError") && handlerSource.contains("return action"),
                "apply-time failures must preserve Mixin's original safety action");
        check(handlerSource.contains("isOptionalMixin"),
                "error handler must explicitly classify optional adapters instead of downgrading all mixins");
    }

    private static void orbitalVoidRegressionIsRemovedFromOptionalLayer() throws Exception {
        String travelConfig = Files.readString(OPTIONAL_CONFIGS.get(0));
        check(!travelConfig.contains("OrbitalVoidFriendlyFireMixin"),
                "Orbital Void must not regain the fragile redirect that caused the 0/1 InjectionError");
        check(!Files.exists(MIXIN_ROOT.resolve("compat/traveloptics/OrbitalVoidFriendlyFireMixin.java")),
                "obsolete Orbital Void adapter source must stay deleted");
        check(Files.readString(UTIL_ROOT.resolve("MagicTargetingPolicy.java")).contains("TargetEntityCastData"),
                "standard targeted spell protection must live in the shared global targeting layer");
    }

    private static Set<String> readMixinRegistrations(String json) {
        int marker = json.indexOf("\"mixins\"");
        int open = json.indexOf('[', marker);
        int close = json.indexOf(']', open);
        check(marker >= 0 && open >= 0 && close > open, "could not locate optional mixins array");
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = MIXIN_ENTRY.matcher(json.substring(open + 1, close));
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
