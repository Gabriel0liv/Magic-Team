# Magic Team — Magic-Only Friendly-Fire Policy Design

Date: 2026-08-28
Status: Approved in chat, pending written-spec review
Branch: `agent/friendly-fire-compat-2.3.3`

## Problem

The current Magic Team policy incorrectly uses vanilla scoreboard-team `friendlyFire` as part of the decision for whether hostile magic may affect an ally. That is not the intended behavior.

Vanilla `/team` friendly fire must remain responsible only for vanilla/non-magic combat behavior. Magic Team must make its own magic-only protection decision whenever Magic Team is enabled.

## Intended Behavior

### Vanilla team behavior

Vanilla `/team` remains untouched.

Example:

```mcfunction
/team modify deuses friendlyFire false
```

With this setting, Minecraft continues to block vanilla teammate combat according to vanilla rules. Magic Team must not modify, bypass, or replace that behavior for punches, melee weapons, vanilla arrows, or other non-magic interactions.

### Magic Team enabled

When:

```text
/magicteam enabled true
```

Magic Team applies magic-only ally protection independently of the vanilla `friendlyFire` flag.

Rules:

| Relation / interaction | Result |
| --- | --- |
| Ally + HOSTILE magic | blocked |
| Ally + SUPPORT magic | allowed |
| Non-ally + HOSTILE magic | allowed |
| Non-ally + SUPPORT magic | existing support-targeting semantics apply |
| Self-target helper/support effect | allowed according to existing spell semantics |
| Vanilla/non-magic combat | Magic Team does not intervene |

A future change to:

```mcfunction
/team modify deuses friendlyFire true
```

may alter vanilla combat between teammates, but it must not permit hostile magic through Magic Team while `/magicteam enabled true`.

### Magic Team disabled

When:

```text
/magicteam enabled false
```

Magic Team must stop applying its magic-only protection and defer to the original Iron's Spellbooks/addon behavior. It must not override vanilla `/team` behavior either.

## Relationship Source

Babel Core remains the source of relationship identity through `BabelEntityRelations.areAllies(...)` and root-owner resolution.

The concepts remain separate:

- **Alliance / ownership identity:** Babel Core and Minecraft team relationships.
- **Vanilla friendly fire:** vanilla `/team` behavior for vanilla/non-magic combat.
- **Magic protection:** Magic Team policy, active only when Magic Team is enabled.

`TeamUtils.areAllies(...)` must remain relationship-only and must never depend on vanilla `Team.isAllowFriendlyFire()`.

## Policy Change

`TeamUtils.shouldBlockFriendlyFire(...)` currently inspects the attacker/target scoreboard teams and `Team.isAllowFriendlyFire()`. That dependency must be removed from the magic-protection decision.

The new effective policy for Magic Team hostile interactions is:

```text
if Magic Team disabled:
    do not block

resolve root attacker and root target

if same resolved entity:
    do not block as teammate hostile damage

if Babel says they are allies:
    block hostile magic

otherwise:
    allow hostile magic
```

The implementation may rename the pure policy helper if that produces clearer terminology, but existing call sites should continue to express one concept: whether Magic Team must block a hostile magical interaction between the resolved entities.

## Spell Classification

No spell-classification semantics change in this task.

- `SUPPORT` remains ally-allowed/support behavior.
- `HOSTILE` remains offensive behavior.
- Explicit per-spell overrides remain authoritative.
- Unknown/unlisted spells continue to default to `HOSTILE`.

## Scope of Magic-Only Enforcement

The existing Magic Team interception points remain the enforcement boundary:

- Iron's spell targeting
- Iron's spell damage
- harmful spell effects
- magic projectiles/AOE
- supported addon-specific hostile paths

This task does not introduce generic hooks into vanilla `LivingEntity.hurt`, player melee, vanilla arrows, or other non-magic damage paths.

## Existing Mixins

`DamageSourcesMixin` and other magic/addon adapters must call the corrected Magic Team magic-protection policy without consulting vanilla scoreboard friendly-fire permission.

`EntityMixin` remains relationship compatibility only. It must not be expanded into a global vanilla damage override.

## Tests

Use TDD.

First change the dependency-free contracts so they fail against the current implementation. The contracts must require:

1. `TeamUtils.shouldBlockFriendlyFire(...)` does not call `Team.isAllowFriendlyFire()` or otherwise inspect vanilla scoreboard friendly-fire permission.
2. Magic Team enabled + allied hostile interaction blocks regardless of the vanilla team friendly-fire state.
3. Magic Team disabled remains transparent.
4. Non-allies are not blocked by Magic Team.
5. Self interactions are not treated as teammate hostile damage.
6. `TeamUtils.areAllies(...)` remains relationship-only.
7. No generic vanilla damage hook is added for punches, melee weapons, vanilla arrows, or other non-magic damage.
8. Existing support/hostile override behavior remains covered by the command/config contracts.

After RED is observed, update the policy and affected assertions, then run Structural Contracts and Forge Compile until both are GREEN.

## Runtime Validation

Static/compile checks do not replace runtime testing.

Minimum in-game matrix:

| Vanilla team FF | Magic Team | Interaction | Expected |
| --- | --- | --- | --- |
| false | true | hostile magic vs ally | blocked by Magic Team |
| true | true | hostile magic vs ally | still blocked by Magic Team |
| false | true | support magic vs ally | allowed |
| true | true | support magic vs ally | allowed |
| false | false | Iron's/addon magic | original behavior |
| true | false | Iron's/addon magic | original behavior |
| false | true/false | vanilla melee between teammates | vanilla handles it |
| true | true/false | vanilla melee between teammates | vanilla handles it |

## Out of Scope

- Overriding vanilla `/team` friendly-fire behavior.
- Allowing vanilla melee through a team with `friendlyFire=false`.
- Blocking vanilla melee through a team with `friendlyFire=true`.
- Changing spell IDs or classification defaults.
- Reworking addon-optional Mixin loading.
- Changing healing, summon ownership, or ally identity semantics beyond removing the vanilla friendly-fire dependency from hostile magic permission.
