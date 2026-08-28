# Optional Addon Mixin Resilience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Isolate Magic Team's optional addon mixins so an absent or internally changed addon cannot turn an optional integration into a mandatory startup dependency, while keeping core Iron's/Minecraft hooks fail-fast.

**Architecture:** Keep `magic_team.mixins.json` as the strict core config (`required: true`, `defaultRequire: 1`). Move Travel Optics, Geomancy Plus, Familiars and Cataclysm adapters into four independent optional configs (`required: false`, `defaultRequire: 1`) and register all configs in MixinGradle/the JAR manifest. Structural contracts enforce the dependency boundary and config membership.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, SpongePowered Mixin 0.8.5, Gradle 8.8, dependency-free structural contract tests.

**Spec:** `docs/superpowers/specs/2026-08-28-optional-addon-mixin-resilience-design.md`

## Global Constraints

- Forge, Minecraft, Iron's Spellbooks and Babel Core remain mandatory dependencies.
- Optional addons remain optional and must not become hard-linked Java dependencies.
- Core Magic Team mixins remain strict: `required: true`, `injectors.defaultRequire: 1`.
- Optional addon configs use `required: false`, but retain `injectors.defaultRequire: 1` so incompatibilities remain observable.
- Failure in one optional addon integration must not suppress unrelated optional integrations.
- No friendly-fire, support, healing, ownership, summon or spell-classification semantics change in this work.
- Optional adapters continue using `@Pseudo` and string targets without imports from optional addon packages.

---

### Task 1: Add a failing structural contract for optional config isolation

**Files:**
- Modify: `src/test/java/com/gabri/magicteam/mixin/ArchitectureBoundaryContractTest.java`
- Existing workflow: `.github/workflows/structural-contracts.yml`

**Interfaces:**
- Consumes: existing source/resource tree and dependency-free Java test runner.
- Produces: structural assertions that define the required config split and registration.

- [ ] **Step 1: Extend the architecture contract**

Add resource paths for:

```java
private static final Path CORE_MIXINS = Path.of("src/main/resources/magic_team.mixins.json");
private static final Path TRAVELOPTICS_MIXINS = Path.of("src/main/resources/magic_team.traveloptics.mixins.json");
private static final Path GEOMANCY_MIXINS = Path.of("src/main/resources/magic_team.geomancyplus.mixins.json");
private static final Path FAMILIARS_MIXINS = Path.of("src/main/resources/magic_team.familiars.mixins.json");
private static final Path CATACLYSM_MIXINS = Path.of("src/main/resources/magic_team.cataclysm.mixins.json");
private static final Path BUILD_GRADLE = Path.of("build.gradle");
```

Call a new `optionalAddonMixinConfigsAreIsolated()` method from `main`. The method must assert:

```java
String core = Files.readString(CORE_MIXINS);
check(core.contains("\"required\": true"), "core mixin config must remain required");
check(core.contains("\"defaultRequire\": 1"), "core mixin config must remain strict");

for (String forbidden : List.of(
        "compat.traveloptics.",
        "compat.geomancyplus.",
        "compat.familiars.",
        "AnnihilationSpellMixin",
        "CataclysmFlareBombMixin",
        "CataclysmWitherHowitzerMixin")) {
    check(!core.contains(forbidden), "optional adapter leaked into required core config: " + forbidden);
}
```

For each optional config, read the file and assert `"required": false` and `"defaultRequire": 1`. Assert representative/complete family membership and that each known optional adapter appears in exactly one optional config. At minimum, verify the complete entry sets currently listed in `magic_team.mixins.json` for `compat.traveloptics.*`, `compat.geomancyplus.*`, `compat.familiars.*`, plus root `AnnihilationSpellMixin`, `CataclysmFlareBombMixin`, and `CataclysmWitherHowitzerMixin`.

Read `build.gradle` and assert it contains all five config names:

```text
magic_team.mixins.json
magic_team.traveloptics.mixins.json
magic_team.geomancyplus.mixins.json
magic_team.familiars.mixins.json
magic_team.cataclysm.mixins.json
```

- [ ] **Step 2: Run Structural Contracts and verify RED**

Run the GitHub `Structural Contracts` workflow on the branch head.

Expected: `ArchitectureBoundaryContractTest` fails because the optional JSON configs do not exist and/or optional entries are still present in the required core config.

- [ ] **Step 3: Commit the RED contract**

Commit only the test change with message:

```text
test: require isolated optional addon mixin configs
```

---

### Task 2: Split optional addon configs and register them

**Files:**
- Modify: `src/main/resources/magic_team.mixins.json`
- Create: `src/main/resources/magic_team.traveloptics.mixins.json`
- Create: `src/main/resources/magic_team.geomancyplus.mixins.json`
- Create: `src/main/resources/magic_team.familiars.mixins.json`
- Create: `src/main/resources/magic_team.cataclysm.mixins.json`
- Modify: `build.gradle`
- Test: `src/test/java/com/gabri/magicteam/mixin/ArchitectureBoundaryContractTest.java`

**Interfaces:**
- Consumes: existing mixin class names and shared `magic_team.refmap.json`.
- Produces: five independently registered mixin configs with strict core and optional addon boundaries.

- [ ] **Step 1: Reduce the core config to mandatory targets**

Keep these entries in `magic_team.mixins.json`:

```text
AbstractMagicProjectileMixin
AbstractSpellMixin
AbstractSpellCastingMobDispatchMixin
AoeEntityMixin
EntityMixin
AreaEffectCloudMixin
ThrownPotionMixin
PoisonCloudMixin
UtilsMixin
DamageSourcesMixin
LivingEntityMixin
MagicManagerCastDispatchMixin
compat.irons.AoeEntityFriendlyFireMixin
```

Keep:

```json
"required": true,
"injectors": { "defaultRequire": 1 }
```

Remove all Travel Optics, Geomancy Plus, Familiars and Cataclysm entries from this file.

- [ ] **Step 2: Create the Travel Optics optional config**

Create `magic_team.traveloptics.mixins.json` using package `com.gabri.magicteam.mixin`, the shared refmap, `required: false`, and `defaultRequire: 1`.

Its `mixins` array contains `AnnihilationSpellMixin` plus every current `compat.traveloptics.*` entry from the old core config. Entries whose class names mention Cataclysm but whose actual target class lives under `com.gametechbc.traveloptics...` remain here.

- [ ] **Step 3: Create Geomancy Plus, Familiars and Cataclysm optional configs**

`magic_team.geomancyplus.mixins.json` contains every current `compat.geomancyplus.*` entry.

`magic_team.familiars.mixins.json` contains every current `compat.familiars.*` entry.

`magic_team.cataclysm.mixins.json` contains:

```text
CataclysmFlareBombMixin
CataclysmWitherHowitzerMixin
```

Each config uses:

```json
"required": false,
"minVersion": "0.8",
"package": "com.gabri.magicteam.mixin",
"compatibilityLevel": "JAVA_17",
"refmap": "magic_team.refmap.json",
"injectors": { "defaultRequire": 1 }
```

- [ ] **Step 4: Register every config in Gradle and manifest**

Extend the existing MixinGradle block:

```gradle
mixin {
    add sourceSets.main, "${mod_id}.refmap.json"
    config "${mod_id}.mixins.json"
    config "${mod_id}.traveloptics.mixins.json"
    config "${mod_id}.geomancyplus.mixins.json"
    config "${mod_id}.familiars.mixins.json"
    config "${mod_id}.cataclysm.mixins.json"
}
```

Set the JAR manifest `MixinConfigs` attribute to the comma-separated five config names so the built JAR declares them explicitly:

```gradle
'MixinConfigs': "magic_team.mixins.json,magic_team.traveloptics.mixins.json,magic_team.geomancyplus.mixins.json,magic_team.familiars.mixins.json,magic_team.cataclysm.mixins.json"
```

- [ ] **Step 5: Run Structural Contracts and verify GREEN**

Expected: all structural contracts pass, including the new optional-config isolation assertions and existing no-hard-import checks.

- [ ] **Step 6: Run Forge Compile**

Expected: Forge compilation succeeds and generated refmap verification passes.

- [ ] **Step 7: Commit the implementation**

Commit config/resource/build changes with message:

```text
fix: isolate optional addon mixin configs
```

---

### Task 3: Final verification and runtime handoff

**Files:**
- No production file changes unless verification exposes a defect.

**Interfaces:**
- Consumes: branch head after Task 2.
- Produces: verified CI evidence and a focused runtime test matrix.

- [ ] **Step 1: Verify fresh branch status and CI**

Confirm both `Structural Contracts` and `Forge Compile` succeed on the final head.

- [ ] **Step 2: Inspect final config membership**

Verify no optional addon adapter remains in `magic_team.mixins.json`, each optional adapter appears in exactly one optional config, and all configs retain the shared refmap.

- [ ] **Step 3: Runtime handoff**

Document that CI proves source/config/build consistency, not Forge runtime behavior. Test in a real server with these cases:

```text
Iron's + Babel Core + Magic Team only -> starts
+ Travel Optics compatible version -> starts and adapter behavior works
+ Geomancy Plus compatible version -> starts and adapter behavior works
+ Familiars compatible version -> starts and adapter behavior works
+ Cataclysm compatible version -> starts and adapter behavior works
optional addon absent -> starts
one deliberately incompatible addon build/version -> startup should survive that optional integration failure and log the incompatibility
```

Do not mark PR ready or merge solely from compile/structural CI.