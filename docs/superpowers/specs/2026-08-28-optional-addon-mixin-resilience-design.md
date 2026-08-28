# Magic Team — Optional Addon Mixin Resilience Design

Date: 2026-08-28
Status: Approved design, pending implementation plan
Branch: `agent/friendly-fire-compat-2.3.3`

## Problem

Magic Team supports optional integrations for Iron's addons and related mods such as Travel Optics, GTBC Geomancy Plus, Alshanex Familiars and Cataclysm. Those integrations already avoid hard Java dependencies by using `@Pseudo` and string targets where appropriate, so an addon that is completely absent does not need to be installed.

The remaining failure mode is version drift while an addon is present. A future addon update can rename a target class, target method, descriptor or shadowed member. The current single `magic_team.mixins.json` is globally `required: true` with `defaultRequire: 1`, so a failed optional integration can become fatal even though the addon itself is not a mandatory dependency of Magic Team.

The goal is to preserve strict failure semantics for Magic Team's core Iron's/Minecraft hooks while isolating optional addon adapters so incompatibility in one addon does not prevent the server from starting.

## Invariants

1. Forge, Minecraft, Iron's Spellbooks and Babel Core remain mandatory dependencies.
2. Optional addons remain optional and must not become hard-linked Java dependencies.
3. Core Magic Team mixins remain strict: a broken required Iron's/Minecraft hook must still fail loudly rather than silently reducing protection.
4. Optional addon adapters must remain observable when incompatible. Do not hide broken injectors by globally changing them to `require = 0`.
5. Failure in one optional addon integration must not disable unrelated optional addon integrations.
6. Missing or renamed spell registry IDs must not crash startup; unknown spells continue to use Magic Team's safe default behavior.
7. This change must not alter friendly-fire, support, healing, ownership or summon semantics.

## Chosen architecture

Split mixin configuration by dependency boundary.

### Core config

`magic_team.mixins.json`

- `required: true`
- `injectors.defaultRequire: 1`
- contains only Minecraft, Iron's Spellbooks and Magic Team core adapters
- no class that targets Travel Optics, Geomancy Plus, Familiars or Cataclysm may remain here

This preserves fail-fast behavior for functionality that Magic Team cannot safely operate without.

### Optional configs

Create one optional mixin config per integration family:

- `magic_team.traveloptics.mixins.json`
- `magic_team.geomancyplus.mixins.json`
- `magic_team.familiars.mixins.json`
- `magic_team.cataclysm.mixins.json`

Each optional config uses:

```json
{
  "required": false,
  "minVersion": "0.8",
  "package": "com.gabri.magicteam.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "magic_team.refmap.json",
  "injectors": {
    "defaultRequire": 1
  }
}
```

`defaultRequire` intentionally stays `1`. When a known adapter no longer matches the installed addon version, Mixin should report the failure instead of silently pretending compatibility exists. Because the config is optional, that adapter/config failure is not treated as a fatal Magic Team requirement.

## Config membership

### Travel Optics

Move every `compat.traveloptics.*` adapter to `magic_team.traveloptics.mixins.json`.

Also move legacy/root adapters that target Travel Optics classes, including `AnnihilationSpellMixin`, out of the core config.

### Geomancy Plus

Move every `compat.geomancyplus.*` adapter to `magic_team.geomancyplus.mixins.json`.

### Familiars

Move every `compat.familiars.*` adapter to `magic_team.familiars.mixins.json`.

### Cataclysm

Move root Cataclysm adapters such as `CataclysmFlareBombMixin` and `CataclysmWitherHowitzerMixin`, plus Travel Optics adapters whose target classes are directly owned by Cataclysm only if they do not depend on Travel Optics-specific classes or behavior. The implementation must classify by actual runtime target/dependency, not merely by class name.

Where an adapter targets a Travel Optics class that internally interoperates with Cataclysm, it remains in the Travel Optics config because Travel Optics is the actual target dependency.

## Registration

All five mixin configs must be declared in the built JAR manifest through MixinGradle/manifest configuration so Forge loads them.

The core config remains first. Optional configs are added individually rather than through a single catch-all optional config, so an incompatibility in Travel Optics does not suppress Familiars, Geomancy Plus or Cataclysm coverage.

## Optional adapter class rules

Every class in an optional config must satisfy the existing architecture boundary:

- use `@Pseudo` when targeting a class outside mandatory dependencies;
- use `@Mixin(targets = "fully.qualified.Target", remap = false)` rather than importing the optional target class;
- do not add direct imports from optional addon packages;
- dependencies in method parameters, shadows and superclass types must remain limited to Minecraft/Forge/Iron's/Magic Team classes unless that adapter is intentionally compiled against a mandatory dependency.

Existing adapters that violate these rules must be corrected as part of the migration rather than merely moved to a new JSON file.

## Failure behavior

Expected runtime matrix:

| Situation | Expected result |
| --- | --- |
| Optional addon absent | Its adapters are skipped; server starts |
| Optional addon present and compatible | Its adapters apply normally |
| Optional addon present but a target method/descriptor changed | Mixin reports the incompatibility; optional integration is not a mandatory startup requirement |
| One optional integration fails | Other optional integration configs remain independent |
| Core Iron's/Minecraft mixin fails | Startup remains fail-fast because core config is required |
| Known support spell ID disappears/renames | No classloading failure; the new/unknown ID defaults to hostile until classified or overridden |

## Tests

Use TDD.

First extend the structural architecture contract so it fails against the current repository. The contract must verify:

1. `magic_team.mixins.json` remains `required: true` and `defaultRequire: 1`.
2. Core config contains no `compat.traveloptics`, `compat.geomancyplus`, `compat.familiars`, `AnnihilationSpellMixin`, `CataclysmFlareBombMixin` or `CataclysmWitherHowitzerMixin` entries after migration.
3. Each optional config exists, is `required: false`, and retains `defaultRequire: 1`.
4. Each expected optional adapter is present in exactly one optional config.
5. Optional `@Pseudo` mixins continue to have no hard imports from optional addon packages.
6. `build.gradle`/manifest registration includes all optional configs.

Verify the contract fails before production changes, then migrate the configs and registration until Structural Contracts pass.

Finally run Forge Compile and verify the generated refmap is still present. The compile check proves source/config packaging consistency; it does not replace real runtime testing against missing, compatible and deliberately incompatible addon versions.

## Out of scope

- Automatically supporting arbitrary future addon method layouts.
- Silently falling back to unprotected friendly-fire behavior without a log signal.
- Changing spell classification semantics.
- Making Iron's Spellbooks or Babel Core optional.
- Broad refactors unrelated to optional compatibility loading.
