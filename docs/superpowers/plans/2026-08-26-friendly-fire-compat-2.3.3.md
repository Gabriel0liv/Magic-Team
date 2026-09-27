# Magic Team 2.3.3 Friendly-Fire Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every confirmed hostile Iron's/Travel Optics/Geomancy Plus/Familiars path obey scoreboard `friendlyFire` without changing alliance, healing, ownership, or summon-AI semantics.

**Architecture:** `TeamUtils.areAllies` stays the relationship query; `TeamUtils.shouldBlockFriendlyFire` stays the hostile-action gate. Add explicit interaction intent to `MagicTeamEffectContext`, then patch only hostile pre-filters and side effects that bypass Iron's central `DamageSources` path. Optional addons use the project's existing `@Pseudo` + string-target pattern.

**Tech Stack:** Minecraft 1.20.1, Forge 47.4.10, Java 17, Sponge Mixin, Iron's Spells 3.15.3, Babel Core 2.0.0-SNAPSHOT.

**Spec:** `docs/superpowers/specs/2026-08-26-friendly-fire-compat-2.3.3-design.md`

## Global Constraints

- `friendlyFire=true` never means `areAllies=false`.
- Support/healing/summon ownership and AI keep alliance semantics.
- Vanilla potion behavior stays unchanged.
- Optional addon classes must not become hard Java dependencies.
- Root owners continue through Babel Core 2.
- Target version is `2.3.3`.
- Source refinement: Acid Rain has three hostile bypasses: Corroded, beneficial-effect cleanse, and `increaseFireTicksForEntities()`.
- Source refinement: audited Travel Optics has only `FloodPoolEntity` and `BrinePoolEntity` extending `SyncedAoeEntity`; both apply harmful `WET`, and Brine can also damage, so the base filter is hostile in this version.

## File Map

Modify:
- `src/main/java/com/gabri/magicteam/util/MagicTeamEffectContext.java`
- `src/main/java/com/gabri/magicteam/util/TeamUtils.java`
- `src/main/java/com/gabri/magicteam/mixin/LivingEntityMixin.java`
- `src/test/java/com/gabri/magicteam/util/MagicTeamEffectContextContractTest.java`
- `src/main/resources/magic_team.mixins.json`
- `src/main/java/com/gabri/magicteam/mixin/EntityMixin.java` (documentation only unless a real conflict is proven)
- `gradle.properties`

Create:
- `src/main/java/com/gabri/magicteam/mixin/compat/irons/AoeEntityFriendlyFireMixin.java`
- `src/main/java/com/gabri/magicteam/mixin/compat/traveloptics/SyncedAoeEntityFriendlyFireMixin.java`
- `src/main/java/com/gabri/magicteam/mixin/compat/traveloptics/AcidRainAoeContextMixin.java`
- `src/main/java/com/gabri/magicteam/mixin/compat/geomancyplus/SolarStormFriendlyFireMixin.java`
- `src/main/java/com/gabri/magicteam/mixin/compat/familiars/HikenFriendlyFireMixin.java`

---

### Task 1: InteractionType and context contract

**Produces:** `InteractionType { GENERIC, HARMFUL, BENEFICIAL }`, `push(Entity, InteractionType)`, `push(Entity, AbstractSpell, CastSource, InteractionType)`, `getInteractionType()`, `isHarmfulInteraction()`.

- [ ] Add failing contract assertions for generic default, explicit harmful/beneficial, nested pop restoration, vanilla potion bypass, and clear.
- [ ] Run `./gradlew compileTestJava`; expected red is the missing new context API, not a missing Babel jar.
- [ ] Add `interactionType` to the context record. Existing push overloads delegate with `GENERIC`; vanilla potion remains `Origin.VANILLA_POTION` + `GENERIC`.
- [ ] Make `shouldFilterDamage()` return false for vanilla potion and explicitly `BENEFICIAL` context while preserving current `GENERIC` behavior.
- [ ] Add a five-argument `TeamUtils.shouldAllowEffect(..., InteractionType)` overload with this exact policy: `HARMFUL -> !shouldBlockFriendlyFire(source,target)`; `BENEFICIAL -> areAllies(source,target)`; `GENERIC -> preserve the existing effect-category behavior`.
- [ ] Pass `MagicTeamEffectContext.getInteractionType()` from both `LivingEntityMixin#addEffect` injections.
- [ ] Include `interaction=` in context diagnostics.
- [ ] Run `./gradlew build` and commit `refactor: classify magic interaction intent`.

### Task 2: Iron's AoeEntity

**Files:** new `compat/irons/AoeEntityFriendlyFireMixin.java`, mixin JSON.

- [ ] Target exact Iron's 3.15.3. MineDev binary SHA is `62a778fb0dbcdb3a7142188fc291311032bf24d4`; runtime constants confirm `m_5603_(Entity)Z` and `m_7307_(Entity)Z`, while readable source identifies the method as `canHitEntity(Entity)`.
- [ ] Redirect only the `Entity.isAlliedTo` invocation inside `AoeEntity#canHitEntity` to `TeamUtils.shouldBlockFriendlyFire(owner,target)`. Keep the original surrounding negation, owner exclusion, and superclass filter intact.
- [ ] Register `compat.irons.AoeEntityFriendlyFireMixin`.
- [ ] Run `./gradlew clean build` and verify the generated refmap contains the inherited vanilla method/call mapping; missing mapping is a hard failure, not a reason to broaden `EntityMixin`.
- [ ] Runtime: same team FF false -> hostile AOE rejected; FF true -> teammate eligible.
- [ ] Commit `fix: honor friendly fire in Iron's AOE targeting`.

### Task 3: Travel Optics SyncedAoeEntity

**Files:** new `compat/traveloptics/SyncedAoeEntityFriendlyFireMixin.java`, mixin JSON.

- [ ] Use `@Pseudo` + target `com.gametechbc.traveloptics.api.entity.SyncedAoeEntity`, `remap=false`.
- [ ] Redirect runtime `Entity.m_7307_` inside runtime `m_5603_(Entity)Z` to `TeamUtils.shouldBlockFriendlyFire(owner,target)`.
- [ ] Register the mixin without importing Travel Optics classes.
- [ ] Build with Travel Optics absent.
- [ ] Runtime Flood Pool and Brine Pool: FF false blocks WET/damage; FF true permits them.
- [ ] Commit `fix: honor friendly fire in Travel Optics synced AOE`.

### Task 4: AcidRainAoe — all hostile side effects

**Files:** new `compat/traveloptics/AcidRainAoeContextMixin.java`, mixin JSON.

- [ ] Use `@Pseudo` + target `com.gametechbc.traveloptics.entity.projectiles.AcidRainAoe`.
- [ ] Wrap `applyCorrodedEffectToEntities()V` HEAD/RETURN with `MagicTeamEffectContext.push((Entity)(Object)this, InteractionType.HARMFUL)` / `pop()`. Existing `LivingEntityMixin` then gates Corroded.
- [ ] Inject cancellably at HEAD of private `isAlly(LivingEntity owner, LivingEntity target)` and return `TeamUtils.shouldBlockFriendlyFire(owner,target)`. This makes the hostile cleanse respect FF while preserving the separate tamed exclusion.
- [ ] Cancel `increaseFireTicksForEntities()V` and reproduce its original range loop, but call `setSecondsOnFire(currentTicks / 20 + 1)` only when owner is null or `!TeamUtils.shouldBlockFriendlyFire(owner,target)`. This path cannot be solved by effect context because it does not call hurt/addEffect.
- [ ] Do not change particles, duration, effect blacklist, or tamed behavior.
- [ ] Runtime FF false: teammate gets no Corroded, cleanse, or extra fire time. FF true: all three behave as on another hostile target. Tamed cleanse exclusion remains unchanged.
- [ ] Commit `fix: cover Acid Rain hostile side effects`.

### Task 5: GTBC Geomancy Plus SolarStorm

**Files:** new `compat/geomancyplus/SolarStormFriendlyFireMixin.java`, mixin JSON.

- [ ] Use `@Pseudo` + target `com.gametechbc.gtbcs_geomancy_plus.effects.SolarStormEffect`.
- [ ] Redirect the single runtime `Entity.m_7307_` call inside private `isValidTarget(LivingEntity target, LivingEntity caster)` to `TeamUtils.shouldBlockFriendlyFire(caster,target)`.
- [ ] Preserve self exclusion and LOS.
- [ ] Runtime FF false -> teammate not selected; FF true -> teammate can be selected under normal range/LOS.
- [ ] Commit `fix: honor friendly fire in Solar Storm targeting`.

### Task 6: Alshanex Familiars Hiken

**Files:** new `compat/familiars/HikenFriendlyFireMixin.java`, mixin JSON.

- [ ] Use `@Pseudo` + target `net.alshanex.alshanex_familiars.entity.misc.HikenEntity`.
- [ ] Redirect both matching runtime `Entity.m_7307_` calls inside `m_6532_(HitResult)V` to `TeamUtils.shouldBlockFriendlyFire(owner,target)`.
- [ ] Preserve owner exclusion, alive check, damage, knockback, fire, particles, and griefing.
- [ ] Test block-impact and entity-impact branches with FF false/true. Exercise spell-griefing explosion while the existing `AbstractMagicProjectileMixin` context is active.
- [ ] Commit `fix: honor friendly fire in Hiken impacts`.

### Task 7: EntityMixin recursion/semantic audit

- [ ] Search Magic Team for `isAlliedTo|m_7307_`; new adapter bodies must call `shouldBlockFriendlyFire` directly, not recursively call entity alliance.
- [ ] Verify `TeamUtils.shouldBlockFriendlyFire` uses Babel relations plus `Team.isAlliedTo`, never `Entity.isAlliedTo`.
- [ ] Keep `EntityMixin` behavior unchanged and add a comment identifying it as relationship compatibility only.
- [ ] Build and commit `docs: clarify EntityMixin relationship semantics`.

### Task 8: Final MineDev bypass audit

- [ ] Search Iron's/Travel Optics/Geomancy Plus/Familiars for `isAlliedTo`, `m_7307_`, team `isAlliedTo/m_83536_`, `DamageSources.isFriendlyFireBetween`, and `shouldHealEntity`.
- [ ] Classify every hit: hostile pre-filter; support/ownership/AI; unrelated. Every hostile pre-filter must either reach corrected `DamageSources` or an explicit adapter.
- [ ] Search range-loop side effects for `addEffect/m_7292_`, `hurt/m_6469_`, `removeEffect/m_21195_`, `setSecondsOnFire/m_20254_`, and direct knockback/movement. Every hostile side effect must have corrected target selection or its own adapter.
- [ ] Do not release with any unclassified hostile path; add a dedicated compatibility task if one is found.

### Task 9: Full Forge/Arclight regression

- [ ] Build Babel Core 2 so `../../shared/Babel-Core/build/libs/babel_core-2.0.0-SNAPSHOT.jar` exists, then run `./gradlew clean build`.
- [ ] Boot once with only Iron's + Babel + Magic Team: optional addon mixins must not crash when addons are absent.
- [ ] Boot with all exact audited addons: no injection failures.
- [ ] Same team FF false: vanilla melee blocked; Iron's damage/AOE blocked; Flood/Brine blocked; all three Acid Rain hostile side effects blocked; Solar Storm skips teammate; Hiken protects teammate; healing/support works; summon relation/AI unchanged.
- [ ] Same team FF true: all corresponding hostile magic is allowed while healing/support still works and summon relation/AI remains allied.
- [ ] Repeated mixed casts without restarting must produce no leaked-context warning and no stale-context filtering of unrelated damage.

### Task 10: Version and release gate

- [ ] Only after Tasks 1-9 pass, change `mod_version=2.3.2` to `mod_version=2.3.3` in `gradle.properties`.
- [ ] Run `./gradlew clean build` and verify reobfuscated 2.3.3 jar.
- [ ] Final diff must not make `areAllies` depend on friendlyFire, change summon ally AI, alter vanilla potions, or import optional addon classes directly.
- [ ] Commit `chore: bump Magic Team to 2.3.3`.
- [ ] Request code review focused on hostile/support separation, optional mixins, Acid Rain's three bypasses, and runtime mapping names before merge.
