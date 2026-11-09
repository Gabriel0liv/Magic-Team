# Global-First Magic Protection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor Magic Team so shared Iron's/global interception and generic attribution protect most addon magic without one adapter per spell.

**Architecture:** Keep the current strict global Iron's hooks as the primary enforcement path, add addon-neutral persistent magic attribution for interactions that outlive the cast stack, then migrate repeated addon adapters into shared mechanism-level hooks and remove adapters proven redundant. Spell-specific adapters remain only as documented fail-soft exceptions.

**Tech Stack:** Java 17, Forge 1.20.1, Sponge Mixin 0.8.5, Iron's Spells 'n Spellbooks 3.15.3 APIs, Babel Core entity relations, dependency-free structural contract tests.

**Spec:** `docs/superpowers/specs/2026-09-26-global-first-magic-protection-design.md`

## Global Constraints

- Global Iron's/Minecraft integration is the primary protection model; adapters are fallback only.
- New normal Iron's-compatible spells should not require Magic Team source changes.
- Vanilla `/team friendlyFire` remains independent from Magic Team's magic-only policy.
- Magic Team disabled mode must remain transparent to gameplay filtering.
- Babel Core remains canonical for root-owner/alliance resolution.
- Unknown registered spells remain hostile-by-default unless classified/overridden as support.
- Optional addon compatibility must remain fail-soft.
- Do not classify arbitrary vanilla/mod interactions as magic without attribution evidence.
- Do not remove an adapter until an equivalent shared path is identified and regression-tested.
- Do not use GitHub Actions unless the user explicitly requests it.

## Review Focus

- A delayed entity survives after `AbstractSpell.onCast` returns: attribution must still resolve caster/spell without leaking indefinitely.
- An unrelated vanilla/mod entity exists with no magic attribution: it must not be treated as hostile magic.
- A support spell creates a delayed projectile/effect: persisted attribution must preserve support behavior rather than defaulting to hostile.
- Nested spell/entity contexts occur in one server thread: push/pop and persistent attribution must not cross-contaminate interactions.
- Magic Team is disabled after attribution already exists: all enforcement paths must become transparent immediately.

---

### Task 1: Pin the Global-First Architecture with Contracts

**Files:**
- Create: `src/test/java/com/gabri/magicteam/mixin/GlobalFirstArchitectureContractTest.java`
- Modify: `src/test/java/com/gabri/magicteam/mixin/ArchitectureBoundaryContractTest.java`
- Modify: `src/test/java/com/gabri/magicteam/mixin/MixinWiringContractTest.java`

**Interfaces:**
- Consumes: current core mixin registrations and addon compatibility configs.
- Produces: structural rules that later tasks must satisfy before adapter deletion is accepted.

- [ ] **Step 1: Write the failing global-first contract**

Add assertions that:
- core/global mixins are registered in `magic_team.mixins.json`;
- addon-specific configs are treated as optional compatibility, not primary policy;
- no new spell-specific adapter is accepted without an explicit `GLOBAL_FIRST_EXCEPTION:` justification comment;
- generic attribution classes must live outside addon-specific packages and must not reference Travel Optics/Familiars/Geomancy/Cataclysm classes.

- [ ] **Step 2: Run the dependency-free contract locally and verify RED**

Run:

```powershell
javac -d build\structural-contracts src\test\java\com\gabri\magicteam\mixin\GlobalFirstArchitectureContractTest.java
java -cp build\structural-contracts com.gabri.magicteam.mixin.GlobalFirstArchitectureContractTest
```

Expected: FAIL because the attribution layer and adapter exception rules do not exist yet.

- [ ] **Step 3: Update existing architecture/wiring tests to stop requiring spell-specific adapters as permanent architecture**

Remove assertions that encode specific Travel Optics spell adapters as mandatory end-state coverage. Replace them with assertions for shared global/mechanism-level coverage.

- [ ] **Step 4: Re-run the structural contracts and preserve the intentional RED only for missing implementation**

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/gabri/magicteam/mixin
git commit -m "test: define global-first magic architecture"
```

---

### Task 2: Add Generic Persistent Magic Attribution

**Files:**
- Create: `src/main/java/com/gabri/magicteam/util/MagicAttribution.java`
- Create: `src/main/java/com/gabri/magicteam/util/MagicAttributionIndex.java`
- Create: `src/test/java/com/gabri/magicteam/util/MagicAttributionIndexContractTest.java`
- Modify: `src/main/java/com/gabri/magicteam/util/MagicTeamEffectContext.java`

**Interfaces:**
- Consumes: `Entity`, `AbstractSpell`, `CastSource`, `SpellBehavior`, `MagicTeamEffectContext.InteractionType`.
- Produces:
  - `record MagicAttribution(UUID sourceEntityId, UUID rootCasterId, String spellId, SpellBehavior behavior, MagicTeamEffectContext.InteractionType interactionType, long expiresAtTick)`
  - `MagicAttributionIndex.record(Entity entity, MagicAttribution attribution)`
  - `MagicAttributionIndex.get(Entity entity, long currentTick)` returning `MagicAttribution` or `null`
  - `MagicAttributionIndex.remove(Entity entity)`
  - `MagicAttributionIndex.cleanup(long currentTick)`
  - `MagicTeamEffectContext.currentAttribution()` returning a normalized attribution snapshot when a live spell context can provide one.

- [ ] **Step 1: Write failing attribution tests**

Cover:
- record/get;
- expiry cleanup;
- replacement of stale attribution;
- support behavior preserved;
- unknown/no attribution returns `null`;
- nested contexts produce the current/top spell attribution only.

- [ ] **Step 2: Run the attribution contract and verify RED**

Expected: FAIL because `MagicAttribution`/`MagicAttributionIndex` do not exist.

- [ ] **Step 3: Implement the minimal addon-neutral attribution model**

Use UUID-keyed storage with explicit expiry. Do not store live addon classes. Store spell identity/classification as stable primitive/value data.

- [ ] **Step 4: Extend `MagicTeamEffectContext` with `currentAttribution()`**

The snapshot must derive root/source identity and classification from the active context without changing existing push/pop semantics.

- [ ] **Step 5: Run attribution and existing context contracts**

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/gabri/magicteam/util src/test/java/com/gabri/magicteam/util
git commit -m "feat: add generic persistent magic attribution"
```

---

### Task 3: Propagate Attribution through Shared Entity/Projectile Boundaries

**Files:**
- Create: `src/main/java/com/gabri/magicteam/mixin/EntityMagicAttributionMixin.java`
- Modify: `src/main/java/com/gabri/magicteam/mixin/AbstractMagicProjectileMixin.java`
- Modify: `src/main/java/com/gabri/magicteam/mixin/AoeEntityMixin.java`
- Modify: `src/main/resources/magic_team.mixins.json`
- Create: `src/test/java/com/gabri/magicteam/mixin/MagicAttributionPropagationContractTest.java`

**Interfaces:**
- Consumes: `MagicTeamEffectContext.currentAttribution()`, `MagicAttributionIndex.record/get`.
- Produces: generic propagation from active spell context into entities that are spawned/processed within Magic Team's known magic boundaries.

- [ ] **Step 1: Write failing propagation contracts**

Assert that:
- Iron's projectiles and AOEs record attribution from active spell context;
- an entity with no active magic context is not attributed;
- support attribution remains support;
- disabled Magic Team does not create enforcement-only attribution that can later block gameplay.

- [ ] **Step 2: Run the propagation contract and verify RED**

- [ ] **Step 3: Implement the smallest safe shared propagation hooks**

Prefer existing common Iron's lifecycle/ownership points. Do not intercept every vanilla `Entity` construction indiscriminately.

- [ ] **Step 4: Re-enter transient context from persistent attribution during shared projectile/AOE processing**

When a delayed attributed entity later executes a known magic interaction, establish a temporary context derived from its attribution, then reliably pop it in `finally`/paired mixin scope.

- [ ] **Step 5: Run propagation, context, and wiring contracts**

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/gabri/magicteam/mixin src/main/resources/magic_team.mixins.json src/test/java/com/gabri/magicteam/mixin
git commit -m "feat: propagate magic attribution through shared entities"
```

---

### Task 4: Make Final Damage and Effect Gates Attribution-Aware

**Files:**
- Modify: `src/main/java/com/gabri/magicteam/mixin/LivingEntityMixin.java`
- Modify: `src/main/java/com/gabri/magicteam/mixin/DamageSourcesMixin.java`
- Modify: `src/main/java/com/gabri/magicteam/util/TeamUtils.java`
- Modify: `src/test/java/com/gabri/magicteam/util/FriendlyFirePolicyContractTest.java`
- Create: `src/test/java/com/gabri/magicteam/mixin/GlobalMagicEnforcementContractTest.java`

**Interfaces:**
- Consumes: active `MagicTeamEffectContext`, persistent `MagicAttribution`, Babel root owner, `SpellBehavior`.
- Produces: one central resolution path for hostile/support decisions regardless of whether attribution came from live cast context or a delayed entity.

- [ ] **Step 1: Write failing enforcement tests**

Cover:
- hostile direct spell on ally -> blocked;
- support direct spell on ally -> allowed;
- hostile delayed attributed entity on ally -> blocked;
- support delayed attributed entity on ally -> allowed;
- no attribution -> leave original behavior untouched;
- Magic Team disabled -> leave original behavior untouched even when attribution exists;
- stale/mismatched attribution must not classify unrelated damage;
- harmful effect path and beneficial/support effect path remain distinct.

- [ ] **Step 2: Run enforcement contracts and verify RED for delayed attribution cases**

- [ ] **Step 3: Add one normalized interaction-resolution helper in `TeamUtils`**

Define a single helper that resolves effective magic behavior from explicit spell/context/attribution evidence and delegates ally ownership to Babel. Avoid duplicating classification logic in mixins.

- [ ] **Step 4: Update `LivingEntityMixin` damage/effect gates to use normalized evidence**

Preserve the conservative rule: without active or persistent magic evidence, do nothing.

- [ ] **Step 5: Keep `DamageSourcesMixin` as the preferred Iron's-native damage gate**

Use persistent attribution only as fallback where native `SpellDamageSource` metadata is unavailable.

- [ ] **Step 6: Run policy/enforcement/context tests**

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/gabri/magicteam src/test/java/com/gabri/magicteam
git commit -m "refactor: centralize global magic enforcement"
```

---

### Task 5: Audit and Collapse Addon Adapters by Mechanism

**Files:**
- Modify/Delete: `src/main/java/com/gabri/magicteam/mixin/compat/traveloptics/*`
- Modify/Delete: `src/main/java/com/gabri/magicteam/mixin/compat/familiars/*`
- Modify/Delete: `src/main/java/com/gabri/magicteam/mixin/compat/geomancyplus/*`
- Modify/Delete: `src/main/java/com/gabri/magicteam/mixin/compat/cataclysm/*` if present
- Modify: `src/main/resources/magic_team.traveloptics.mixins.json`
- Modify: `src/main/resources/magic_team.familiars.mixins.json`
- Modify: `src/main/resources/magic_team.geomancyplus.mixins.json`
- Modify: `src/main/resources/magic_team.cataclysm.mixins.json`
- Create: `docs/audits/2026-09-26-global-first-adapter-migration.md`
- Modify: structural adapter contracts under `src/test/java/com/gabri/magicteam/mixin/`

**Interfaces:**
- Consumes: global attribution/enforcement from Tasks 2-4.
- Produces: a much smaller optional compatibility layer grouped by reusable mechanism rather than spell identity.

- [ ] **Step 1: Inventory every current adapter into A/B/C/D from the spec**

For each adapter record:
- behavior protected;
- shared path that now covers it, if any;
- mechanism-level replacement, if any;
- reason it must remain, if irreducible.

- [ ] **Step 2: Write/adjust structural tests for the first removal batch**

Start with fragile target/alliance redirects such as Orbital Void and repeated `Entity.isAlliedTo(...)` adapters where final global damage/effect enforcement already protects the result.

Expected before deletion: tests should fail while redundant registrations/sources remain.

- [ ] **Step 3: Delete redundant adapters and registrations**

Remove source and JSON registration together. Do not leave dormant mixins.

- [ ] **Step 4: Replace repeated addon patterns with the smallest shared mechanism hook where necessary**

Examples include shared addon projectile bases, AOE bases, target helpers or damage helpers. One mechanism hook should replace multiple spell adapters.

- [ ] **Step 5: Mark any surviving spell-specific adapter with `GLOBAL_FIRST_EXCEPTION:`**

The comment must state which global/mechanism path is unavailable and why the adapter remains necessary.

- [ ] **Step 6: Run all dependency-free structural contracts locally**

Expected: PASS.

- [ ] **Step 7: Commit migration in mechanism-sized batches**

Examples:

```bash
git commit -m "refactor: remove redundant targeting adapters"
git commit -m "refactor: collapse addon projectile compatibility"
git commit -m "refactor: collapse delayed effect compatibility"
```

---

### Task 6: Remove One-Off Attribution Infrastructure Superseded by the Generic Layer

**Files:**
- Delete or simplify: `src/main/java/com/gabri/magicteam/util/FlareVacuumAttribution.java`
- Delete or simplify: `src/main/java/com/gabri/magicteam/util/ExpiringAttributionIndex.java`
- Modify/Delete: `src/main/java/com/gabri/magicteam/mixin/compat/traveloptics/FlareVacuumAttributionMixin.java`
- Modify/Delete: related tests under `src/test/java/com/gabri/magicteam/util/`

**Interfaces:**
- Consumes: `MagicAttributionIndex` from Task 2.
- Produces: one attribution implementation instead of spell-specific tracking utilities.

- [ ] **Step 1: Write regression tests proving generic attribution covers the Flare Vacuum-style delayed-owner case**

- [ ] **Step 2: Run tests and verify the new generic path passes before deleting old infrastructure**

- [ ] **Step 3: Remove or reduce the one-off attribution implementation**

Keep only behavior that is genuinely unique and cannot be represented by the generic attribution domain.

- [ ] **Step 4: Run attribution and adapter contracts**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: replace spell-specific attribution with global attribution"
```

---

### Task 7: Local Build and Runtime Handoff

**Files:**
- Modify only if local verification exposes a real defect.
- Update: `README.md` only if user-facing compatibility behavior changed materially.

**Interfaces:**
- Consumes: completed global-first refactor.
- Produces: locally buildable JAR and a focused runtime test matrix.

- [ ] **Step 1: Run all dependency-free contracts locally**

- [ ] **Step 2: Run the full local build**

```powershell
.\gradlew.bat clean test build --no-daemon --max-workers=1
```

Expected: `BUILD SUCCESSFUL` and a JAR under `build\libs`.

- [ ] **Step 3: Verify mixin configs/refmap/JAR contents locally**

Confirm removed adapters are absent and all registered mixins have corresponding classes.

- [ ] **Step 4: Runtime-test representative mechanisms in the real Forge/Arclight pack**

Test:
- normal hostile direct spell;
- support spell;
- Iron's projectile;
- Iron's AOE;
- addon custom projectile;
- delayed hostile entity/effect;
- summon/root-owner case;
- one unusual direct side effect;
- historically problematic Rend-like effect;
- Magic Team disabled mode.

- [ ] **Step 5: Fix only reproduced mechanism-level defects**

Do not respond to one broken spell by immediately restoring a spell-specific adapter; first identify which shared mechanism failed.

- [ ] **Step 6: Final local verification and commit any runtime fixes**

```bash
git add -A
git commit -m "fix: close global magic runtime gaps"
```
