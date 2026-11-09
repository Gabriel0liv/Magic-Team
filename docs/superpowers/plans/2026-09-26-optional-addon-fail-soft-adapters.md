# Optional Addon Fail-Soft Adapters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every optional Magic Team addon adapter fail-soft: an incompatible adapter must not crash server startup, must preserve generic Magic Team protection where independently applicable, must fall back to addon-native behavior only for the unavailable compatibility path, and must emit a WARN-level degradation signal.

**Architecture:** Keep `magic_team.mixins.json` strict for mandatory Minecraft/Iron's core hooks. Make optional addon mixin configs non-fatal by setting `injectors.defaultRequire` to `0`, prohibit positive per-injector `require` values in optional adapters, and add an optional compatibility diagnostics layer that warns when an adapter/config is skipped or degraded without weakening core failures.

**Tech Stack:** Java 17, Forge 1.20.1, Sponge Mixin 0.8.5/MixinGradle, GitHub Actions structural contracts and Forge compile.

**Spec:** `docs/superpowers/specs/2026-08-28-optional-addon-mixin-resilience-design.md`

## Global Constraints

- Forge, Minecraft, Iron's Spellbooks and Babel Core remain mandatory dependencies.
- `magic_team.mixins.json` remains `required: true` with `injectors.defaultRequire: 1`.
- Optional addon configs remain `required: false` and must use `injectors.defaultRequire: 0`.
- No optional adapter may override injector `require` with a positive value.
- Generic Magic Team protection remains active wherever the spell still traverses generic hooks.
- A failed optional adapter may revert only its adapter-specific path to addon-native behavior.
- Optional degradation must produce a WARN signal and must not be represented as healthy compatibility.
- Core failures must never be downgraded to WARN/fallback.
- Friendly-fire classification, SUPPORT/HOSTILE overrides, ownership, healing and summon semantics are unchanged.

## Review Focus

- Optional injector target disappears: startup continues, adapter no-ops, WARN emitted.
- Optional method descriptor changes: failure is downgraded only when Mixin can safely continue; unrelated adapters remain active.
- Core Iron's target disappears: startup remains strict/fatal.
- Multiple optional adapters in one addon: one degradation does not suppress working siblings.
- Generic protection still blocks allies where the failing spell later reaches a generic Magic Team hook.

---

### Task 1: Pin the fail-soft policy with structural contracts

**Files:**
- Modify: `src/test/java/com/gabri/magicteam/mixin/OptionalAddonMixinConfigContractTest.java` or the existing structural mixin-config contract that owns these assertions
- Modify: `src/test/java/com/gabri/magicteam/mixin/RuntimeJarCrashRegressionContractTest.java`

**Interfaces:**
- Consumes: optional mixin JSON files under `src/main/resources`
- Produces: regression assertions that optional configs are non-fatal and core remains strict

- [ ] **Step 1: Write failing assertions**

Assert all optional configs have `"required": false` and `"defaultRequire": 0`; assert core retains `"required": true` and `"defaultRequire": 1`; scan optional adapter sources and fail if a positive `require = N` is present.

- [ ] **Step 2: Add the latest runtime regression**

Add Orbital Void to `RuntimeJarCrashRegressionContractTest` so the test documents that a missing optional redirect target must not be fatal under the optional config policy.

- [ ] **Step 3: Run Structural Contracts and verify RED**

Expected: failure because optional configs currently still use `defaultRequire: 1` and/or optional sources still contain positive `require` overrides.

- [ ] **Step 4: Commit the RED contract**

Commit message: `test: require fail-soft optional addon adapters`

### Task 2: Make optional injector misses non-fatal

**Files:**
- Modify: `src/main/resources/magic_team.traveloptics.mixins.json`
- Modify: `src/main/resources/magic_team.geomancyplus.mixins.json`
- Modify: `src/main/resources/magic_team.familiars.mixins.json`
- Modify: `src/main/resources/magic_team.cataclysm.mixins.json`
- Modify: optional adapter Java sources containing positive `require` overrides

**Interfaces:**
- Consumes: policy contract from Task 1
- Produces: zero-match optional injectors become no-ops instead of `InjectionError`

- [ ] **Step 1: Change each optional config to `defaultRequire: 0`**

Do not change the core config.

- [ ] **Step 2: Remove positive `require` overrides from optional adapters**

For each optional adapter, rely on the optional config default unless an injector already explicitly uses `require = 0`; preserve ordinals, slices and other targeting details.

- [ ] **Step 3: Run Structural Contracts**

Expected: optional policy assertions pass; existing architecture/friendly-fire/Familiars/command contracts remain green.

- [ ] **Step 4: Commit**

Commit message: `fix: make optional addon injectors fail soft`

### Task 3: Add observable optional-adapter degradation warnings

**Files:**
- Create or modify: a focused class under `src/main/java/com/gabri/magicteam/mixin/compat/` for optional mixin diagnostics
- Modify: optional mixin config JSON files only if a Mixin plugin/error handler declaration is required
- Modify: structural test class owning optional mixin diagnostics

**Interfaces:**
- Produces: WARN-level message naming addon family and adapter/mixin when an optional compatibility hook is skipped/degraded
- Must not intercept or downgrade failures from `magic_team.mixins.json`

- [ ] **Step 1: Write failing diagnostics contract**

Assert the diagnostics component distinguishes optional config/mixin names from core and formats a warning containing adapter identity plus fallback wording.

- [ ] **Step 2: Implement the smallest safe Mixin diagnostic hook**

Use Mixin plugin/error-handler facilities only where they can observe optional application degradation without hard-linking optional addon classes. Do not claim recovery from transformation failures that Mixin itself cannot safely continue after.

- [ ] **Step 3: Verify optional/core separation in tests**

Assert optional degradation maps to WARN/fallback while a simulated core failure remains strict.

- [ ] **Step 4: Commit**

Commit message: `feat: warn on degraded optional adapters`

### Task 4: Verify fallback semantics and packaging

**Files:**
- Modify: `README.md` only if current compatibility documentation still implies all adapters are guaranteed active after startup
- Modify: existing structural contracts as needed for packaging assertions

**Interfaces:**
- Consumes: Tasks 2-3
- Produces: buildable JAR whose optional configs are registered and whose generic policy remains unchanged

- [ ] **Step 1: Run all Structural Contracts**

Expected: all contracts pass, including friendly-fire policy, runtime crash regression, architecture boundary, Familiars and command/config UX.

- [ ] **Step 2: Run Forge Compile**

Expected: compile/build succeeds and generated `magic_team.refmap.json` remains present.

- [ ] **Step 3: Inspect built JAR resources**

Verify all optional mixin configs are packaged and registered alongside the strict core config.

- [ ] **Step 4: Commit documentation/verification changes if needed**

Commit message: `docs: document optional adapter fallback semantics`

### Task 5: Real Arclight smoke-test handoff

**Files:**
- No source changes unless the runtime exposes a new defect.

**Interfaces:**
- Consumes: built `magic_team-2.4.0.jar`
- Produces: runtime evidence that sequential optional adapter mismatches no longer stop startup

- [ ] **Step 1: Build locally against the user's Babel-Core path override**

Run `./gradlew clean test build --no-daemon --max-workers=1` (PowerShell equivalent on Windows).

- [ ] **Step 2: Replace only the Magic Team JAR in the same Arclight/Forge modpack**

Use the exact environment that produced Aqua Missiles, Solar Storm, Tidal Grasp and Orbital Void crashes.

- [ ] **Step 3: Confirm startup behavior**

Expected: missing optional targets produce WARN/fallback rather than `Critical injection failure`; server proceeds past Travel Optics/Geomancy optional adapters.

- [ ] **Step 4: Runtime gameplay checks**

Verify generic ally protection still works for paths covered by core hooks, working optional adapters still apply, and only degraded adapter-specific behavior falls back to the addon's native implementation.

- [ ] **Step 5: If another optional adapter mismatch appears**

Treat a server-stopping optional mismatch as a policy regression, not as another spell-by-spell patch; extend the regression contract before changing production code.
