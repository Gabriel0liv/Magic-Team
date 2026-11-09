# Magic Team 2.3.3 Friendly-Fire Compatibility Design

Date: 2026-08-26
Status: Approved architecture, pending implementation plan

## Problem

Magic Team 2.3.2 correctly separates alliance semantics from scoreboard friendly-fire semantics in `TeamUtils`, and it correctly fixes paths that reach Iron's `DamageSources`. However, Iron's and several addons pre-filter offensive targets with direct `Entity.isAlliedTo(...)` checks before damage or harmful effects are attempted.

Those pre-filters bypass `TeamUtils.shouldBlockFriendlyFire(...)`. As a result, two entities on the same scoreboard team can remain immune to some hostile magic even when the team's `friendlyFire` flag is enabled. Conversely, some addon effects can apply harmful effects without passing through the existing Magic Team context.

The goal of 2.3.3 is to make offensive magic obey the same rule everywhere without redefining what "allied" means for support, healing, AI, ownership, or summon behavior.

## Core invariants

The implementation must preserve all of these invariants:

1. `friendlyFire=true` does not mean `areAllies=false`.
2. `TeamUtils.areAllies(a, b)` remains the social/team/ownership relationship query.
3. `TeamUtils.shouldBlockFriendlyFire(attacker, target)` is the authoritative hostile-interaction gate.
4. Support and healing continue to use alliance semantics, not hostile-interaction semantics.
5. Summon ownership and ally AI continue to use alliance semantics unless a specific offensive action is being evaluated.
6. Vanilla potion behavior remains unaffected by Magic Team's magic context.
7. Optional-addon compatibility must not make Magic Team fail to load when an addon is absent.
8. Every adapter must be scoped to a concrete offensive call site; no global redefinition of `Entity.isAlliedTo` is allowed for friendly-fire behavior.

## Chosen architecture

Use call-site-specific compatibility adapters.

The core relationship layer remains:

```text
BabelEntityRelations
        |
        v
TeamUtils
|- areAllies(A, B)
|    `- relationship/support semantics
|
|- shouldBlockFriendlyFire(A, B)
|    `- relationship + scoreboard Team.isAllowFriendlyFire()
|
|- canHarm(A, B)
|    `- !shouldBlockFriendlyFire(A, B)
|
`- canSupport(A, B)
     `- areAllies(A, B)
```

`canHarm` and `canSupport` may be explicit helper methods if they improve readability, but the canonical logic must still delegate to the existing `shouldBlockFriendlyFire` and `areAllies` behavior rather than duplicating policy.

## Compatibility package layout

New compatibility mixins should be grouped by integration rather than accumulated in the root mixin package:

```text
com.gabri.magicteam.mixin.compat
|- irons
|  `- AoeEntityFriendlyFireMixin
|- traveloptics
|  |- SyncedAoeEntityFriendlyFireMixin
|  `- AcidRainAoeContextMixin
|- geomancyplus
|  `- SolarStormFriendlyFireMixin
`- familiars
   `- HikenFriendlyFireMixin
```

Existing mixins may remain where they are for 2.3.3. This change should not include unrelated package migration.

## Iron's AoeEntity

### Existing behavior

`io.redspace.ironsspellbooks.entity.spells.AoeEntity#canHitEntity` rejects a target when `getOwner().isAlliedTo(target)` is true.

That check occurs before `applyEffect`, so an allied player on a team with `friendlyFire=true` never reaches Magic Team's later damage/effect gates.

### Required behavior

Only the alliance check inside this offensive target filter should be replaced semantically with:

```java
TeamUtils.shouldBlockFriendlyFire(owner, target)
```

The surrounding vanilla/Iron's checks, including owner exclusion and superclass `canHitEntity`, must remain intact.

### Version verification

The exact method descriptor and bytecode pattern must be verified against the Iron's 1.20.1 3.15.3 class present in MineDev before the mixin is finalized. The readable older source is useful for semantics, but it is not sufficient evidence for the final injection descriptor.

## Travel Optics SyncedAoeEntity

### Existing behavior

`com.gametechbc.traveloptics.api.entity.SyncedAoeEntity` implements its own AOE hierarchy and does not inherit from Iron's `AoeEntity`. Its target filter also rejects `owner.isAlliedTo(target)`.

### Required behavior

Add an optional compatibility mixin for this class and replace only the hostile target-filter relationship check with `TeamUtils.shouldBlockFriendlyFire(owner, target)` semantics.

This single adapter should cover offensive subclasses that rely on the base `SyncedAoeEntity` target filtering, including currently observed classes such as FloodPool/BrinePool and other derived AOEs.

### Beneficial subclasses

Do not assume every `SyncedAoeEntity` subclass is hostile. Before redirecting the base call globally, classify subclasses that use the base target filter. If a beneficial/support subclass relies on the same filter, the adapter must distinguish hostile versus support behavior rather than applying `canHarm` indiscriminately.

Preferred distinction order:

1. explicit known harmful class/category mapping where required;
2. harmful effect/damage behavior available from the current entity/spell context;
3. narrow subclass-specific adapters if base-level discrimination cannot be made safely.

Do not infer hostility from namespace alone.

## Travel Optics AcidRainAoe

### Existing behavior

`AcidRainAoe` applies the harmful `Corroded` effect directly from its tick path, outside the normal `AoeEntity.checkHits()` scope. Therefore the existing AOE context wrapper does not necessarily classify the application as hostile magic.

Its beneficial-effect cleanse is also an offensive utility action. The current implementation protects scoreboard allies with its own `isAlly(owner, target)` predicate, so teammates remain protected from the cleanse even when scoreboard `friendlyFire=true`.

### Required behavior

Wrap only the harmful application paths in a Magic Team context and mark them as harmful interactions. The common effect/interaction gates should then decide whether each target is allowed.

Both Acid Rain hostile behaviors must obey the same policy:

- `Corroded` application;
- removal of beneficial effects from a target.

For same-team targets, both are blocked when `friendlyFire=false` and allowed when `friendlyFire=true`. Do not directly hard-code a separate team policy inside Acid Rain if the common Magic Team gate can express it.

## GTBC Geomancy Plus SolarStormEffect

### Existing behavior

`SolarStormEffect#isValidTarget` excludes targets using direct `target.isAlliedTo(caster)` logic.

### Required behavior

Replace only that offensive relationship predicate with `TeamUtils.shouldBlockFriendlyFire(caster, target)` semantics, preserving self exclusion and line-of-sight checks.

The adapter must be optional because Geomancy Plus is not a hard dependency of Magic Team.

## Alshanex Familiars HikenEntity

### Existing behavior

Hiken's impact loops skip targets when the projectile owner reports `isAlliedTo(target)`, before `LivingEntity#hurt` is called.

### Required behavior

Replace only those hostile impact-filter checks with `TeamUtils.shouldBlockFriendlyFire(owner, target)` semantics.

Do not change general familiar ally behavior, summon AI, `isAlliedHelper`, follow-owner behavior, or support targeting.

## Existing EntityMixin

The current broad `EntityMixin` changes `isAlliedTo` behavior only when an Iron's namespace entity participates. It may still be useful for owner/projectile/summon relationship propagation, but it must not be the mechanism used to implement scoreboard friendly fire.

For 2.3.3:

- keep it unless testing proves it unnecessary or harmful;
- document it as relationship compatibility, not hostile-interaction policy;
- audit interactions with the new adapters to ensure it does not cause recursive relationship checks.

Removal or redesign of this mixin is explicitly out of scope unless required to make the new adapters correct.

## Interaction context model

The current `MagicTeamEffectContext.Origin` describes where an interaction came from. Origin and hostility are separate concerns and should not be conflated.

Add a second dimension:

```text
InteractionType
|- HARMFUL
|- BENEFICIAL
`- GENERIC
```

Expected examples:

```text
Acid Rain Corroded application:
origin = ENTITY_SCOPE
interaction = HARMFUL

A support buff scope:
origin = ENTITY_SCOPE
interaction = BENEFICIAL
```

### Context rules

1. Existing callers without an explicit interaction type default to `GENERIC` unless the current API can safely infer a more precise value.
2. `LivingEntityMixin` may use `InteractionType.HARMFUL` as positive evidence that an effect/damage application is offensive.
3. `BENEFICIAL` must never cause hostile filtering.
4. `VANILLA_POTION` origin continues to bypass Magic Team filtering as in 2.3.2.
5. Context push/pop remains stack-based and must preserve depth/origin diagnostics.
6. End-of-tick leak detection remains active and continues logging before clearing leaked state.

## Optional mixin loading

Addon-specific mixins must use optional loading patterns (`@Pseudo` and string targets where appropriate) so missing addons do not crash Magic Team during class resolution.

The mixin configuration must be updated only after each target class/method has been verified against the exact addon version present in MineDev.

A missing optional target must fail safely; a changed descriptor in an installed addon must be observable during development/testing rather than silently treated as coverage.

## Regression matrix

The following matrix is the minimum acceptance set.

### Same scoreboard team, friendlyFire=false

| Interaction | Expected |
| --- | --- |
| Vanilla melee | blocked by Minecraft |
| Iron's direct spell damage | blocked |
| Iron's AoeEntity hostile effect/damage | blocked |
| Travel Optics hostile SyncedAoeEntity | blocked |
| Travel Optics Acid Rain Corroded | blocked |
| Travel Optics Acid Rain hostile cleanse | blocked |
| GTBC Solar Storm targeting | ally not selected |
| Familiars Hiken impact | blocked/skipped |
| Healing/support buff | allowed |
| Summon follow/protect/owner relation | unchanged |

### Same scoreboard team, friendlyFire=true

| Interaction | Expected |
| --- | --- |
| Vanilla melee | allowed |
| Iron's direct spell damage | allowed |
| Iron's AoeEntity hostile effect/damage | allowed |
| Travel Optics hostile SyncedAoeEntity | allowed |
| Travel Optics Acid Rain Corroded | allowed |
| Travel Optics Acid Rain hostile cleanse | allowed |
| GTBC Solar Storm targeting | teammate may be selected |
| Familiars Hiken impact | teammate may be hit |
| Healing/support buff | still allowed |
| Summon follow/protect/owner relation | unchanged |

### Different teams / non-allies

Hostile interactions remain allowed unless another game rule/mod independently blocks them. Beneficial targeting keeps its existing Magic Team behavior.

### Owner/self/root-owner relationships

Projectiles, summons, and indirect damage must continue resolving through Babel Core's root-owner relation. Historical self/owner protection without a scoreboard-team relationship remains protected where 2.3.2 already defines it.

## Tests

### Pure policy tests

Keep the existing dependency-free `FriendlyFirePolicyContractTest` and extend only if new policy branches are introduced.

### Context tests

Extend `MagicTeamEffectContextContractTest` for `InteractionType`:

- default generic context;
- explicit harmful context;
- explicit beneficial context;
- nested contexts restore previous type after pop;
- vanilla potion remains excluded from damage filtering;
- clear removes all state.

### Adapter contract tests

Where a mixin itself cannot be unit-tested without Minecraft, extract the smallest pure decision function needed by the adapter and test its mapping. Do not duplicate Minecraft's `Team` implementation in tests.

### Runtime integration checklist

Run a Forge/Arclight test server with the exact MineDev versions and exercise at least one concrete ability/entity from each adapter family:

- Iron's hostile AOE;
- Travel Optics hostile SyncedAoeEntity;
- Travel Optics Acid Rain;
- GTBC Solar Storm;
- Familiars Hiken;
- one healing/support spell;
- one summon support/AI case.

For each hostile case test both `friendlyFire=false` and `friendlyFire=true` without restarting between every cast, so context leakage is also exercised.

## Version and release

Target Magic Team version: `2.3.3`.

The release should not be merged until:

1. every target descriptor is verified against MineDev's installed versions;
2. policy/context contract tests pass;
3. full Gradle build passes in the shared workspace with Babel Core 2 built;
4. runtime smoke tests cover all four compatibility families;
5. server logs contain no leaked-context warnings during the smoke suite;
6. support/healing and summon behavior remain unchanged.

## Out of scope

- changing scoreboard team semantics;
- making `friendlyFire=true` cause entities to stop being allies;
- redesigning Iron's or addon AI generally;
- changing vanilla potion PvP rules;
- broad compatibility with addons not present in MineDev without evidence of a concrete bypass;
- refactoring all existing Magic Team mixins into the new package during this release.

## Implementation order

The implementation plan should order work by shared risk and coverage:

1. extend context model and tests;
2. verify exact Iron's 3.15.3 target descriptors;
3. implement Iron's AoeEntity adapter;
4. classify Travel Optics SyncedAoeEntity subclasses and implement the safe adapter strategy;
5. implement Acid Rain context coverage;
6. implement GTBC Solar Storm adapter;
7. implement Familiars Hiken adapter;
8. audit existing EntityMixin interactions;
9. run contract/build/runtime verification;
10. bump/release 2.3.3 only after all acceptance criteria pass.
