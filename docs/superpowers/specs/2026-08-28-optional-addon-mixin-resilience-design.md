# Magic Team — Optional Addon Fail-Soft Adapter Design

Date: 2026-08-28
Revised: 2026-09-26
Status: Approved design, pending revised implementation plan
Branch: `agent/friendly-fire-compat-2.3.3`

## Problem

Magic Team has optional adapters for Travel Optics, GTBC Geomancy Plus, Alshanex Familiars, Cataclysm and similar Iron's addons. These adapters exist only to close gaps that the generic Magic Team protection cannot cover, such as custom target selection, custom entities, delayed effects, teleports and addon-owned damage paths.

The previous resilience design was incomplete. It split optional adapters into `required: false` mixin configs but left `injectors.defaultRequire: 1`. Real Arclight/Forge smoke tests proved that this still crashes startup when an installed addon changes an injection point: Aqua Missiles, Solar Storm, Tidal Grasp and Orbital Void all produced critical `0/1` or `1/2` injection failures despite living in optional configs.

Therefore `required: false` alone is not sufficient. Optional adapter incompatibility must degrade only that adapter, not the server.

## Required behavior

1. Forge, Minecraft, Iron's Spellbooks and Babel Core remain mandatory dependencies.
2. Core Magic Team hooks remain strict. A broken hook required for the generic Magic Team policy may still fail startup rather than silently disabling the core protection layer.
3. Optional addon adapters are fail-soft. An adapter that no longer matches its addon must not crash Minecraft, Forge, Arclight, the addon, or Magic Team.
4. A failed optional adapter falls back to the behavior that would exist without that adapter:
   - generic Magic Team protection remains active wherever the spell still passes through generic hooks;
   - only the adapter-specific customization is lost;
   - if the spell bypasses every generic hook and depended exclusively on the adapter, that path reverts to the addon's native behavior until compatibility is restored.
5. Adapter degradation must be observable through a WARN identifying the integration/adapter that could not be applied. Startup success must never be presented as proof that every optional adapter is active.
6. One failed adapter must not disable other adapters in the same addon family or in other addon families.
7. Friendly-fire classification, SUPPORT/HOSTILE overrides, ownership rules, healing behavior and summon semantics are unchanged by this resilience work.

## Architecture

### Core mixin config

`magic_team.mixins.json` remains strict:

- `required: true`
- `injectors.defaultRequire: 1`
- only core Minecraft/Iron's/Magic Team hooks belong here

The core layer is the generic protection baseline. It must not be weakened merely to make optional compatibility more tolerant.

### Optional addon configs

The existing family configs remain independent:

- `magic_team.traveloptics.mixins.json`
- `magic_team.geomancyplus.mixins.json`
- `magic_team.familiars.mixins.json`
- `magic_team.cataclysm.mixins.json`

Each optional config must use:

```json
{
  "required": false,
  "injectors": {
    "defaultRequire": 0
  }
}
```

Optional injectors must not override this with a positive `require` unless the adapter is deliberately reclassified as core. A missing `@Inject`, `@Redirect` or similar target therefore becomes a no-op instead of an `InjectionError`.

This is the primary fallback mechanism: when the injection point is absent, the target addon's original method remains in control, while unrelated generic Magic Team hooks continue to operate normally.

### Diagnostic layer

Fail-soft must not become silent failure. Magic Team must add an optional-adapter diagnostic layer that reports compatibility degradation as WARN.

The diagnostic layer has two responsibilities:

1. Detect known optional adapter compatibility failures that can be determined safely at mixin/application time or startup without loading optional addon classes as hard dependencies.
2. Emit a concise warning containing at least the addon family and adapter/mixin name, with wording that the adapter-specific protection is unavailable and generic/native fallback remains in effect.

The diagnostic mechanism may use Mixin configuration/plugin/error-handler facilities and bytecode metadata where appropriate, but it must obey two constraints:

- it may downgrade failures only for mixins belonging to the optional addon configs;
- it must never downgrade failures from `magic_team.mixins.json` or otherwise hide a broken core hook.

If Mixin reports an optional adapter application error that is broader than a zero-match injector (for example a changed shadow, descriptor or target member), the optional integration layer must prefer WARN + skip/fallback rather than escalating that optional adapter into a server-wide startup failure, provided Mixin can safely continue transformation. Cases that cannot be safely recovered must be explicitly documented and covered by a regression test rather than silently claimed as fail-soft.

## Fallback semantics

A failed adapter does not disable Magic Team for the spell globally.

Conceptually:

```text
spell action
  -> generic Magic Team hook still applies?
       yes -> generic ally protection/classification still applies
       no  -> addon native behavior
  -> adapter-specific hook available?
       yes -> apply the extra compatibility behavior
       no  -> skip only that customization and WARN
```

Examples:

- A custom projectile whose eventual damage reaches a generic protected damage path can remain ally-safe even if a targeting adapter fails.
- A teleport spell that requires an adapter solely to stop teleporting an ally may revert to native addon behavior if that adapter is unavailable.
- A failed Travel Optics adapter must not disable working Travel Optics adapters for other spells.

## Optional adapter class rules

Optional adapters must continue to avoid hard addon dependencies:

- use `@Pseudo` for optional target classes;
- prefer string targets, e.g. `@Mixin(targets = "fully.qualified.Target", remap = false)`;
- do not import optional addon classes into signatures or fields;
- keep optional configs separate by actual runtime target dependency;
- do not move an adapter into core merely to make its injection failure strict.

## Runtime evidence driving the revision

The fail-soft behavior is required because real server boots exposed sequential optional failures that the previous design incorrectly allowed to become fatal:

- Travel Optics Aqua Missiles: injector count mismatch;
- Geomancy Plus Solar Storm: missing internal alliance call;
- Iron's dispatch hook: compiled lambda layout mismatch (core issue, therefore fixed strictly rather than made optional);
- Travel Optics Tidal Grasp: missing teleport redirect target;
- Travel Optics Orbital Void: missing friendly-fire redirect target.

Orbital Void confirms that continuing to repair optional adapters one crash at a time is not sufficient; the failure policy itself must be corrected.

## Tests

Use TDD. The revised contracts must first fail against the current repository and then pass after implementation.

Structural requirements:

1. Core `magic_team.mixins.json` remains `required: true` with `defaultRequire: 1`.
2. Every optional addon config remains `required: false` and uses `defaultRequire: 0`.
3. No optional adapter declares a positive injector `require` that can reintroduce a fatal zero-match failure.
4. Optional adapters remain separated from core and avoid direct optional-addon imports.
5. All optional configs remain registered in the built JAR.
6. A regression fixture representing a missing optional injection point must complete without a fatal requirement.
7. Diagnostic behavior must be tested so a degraded optional adapter produces a WARN-level compatibility signal rather than being silently treated as healthy.
8. Core mixin failure semantics must remain strict in the contracts.
9. Existing friendly-fire policy, command/config, Familiars, architecture and runtime-crash regression contracts must continue to pass.
10. Forge Compile must pass and the generated refmap must remain present.

Real runtime verification remains required after automated checks: boot the same Arclight/Forge modpack that exposed the failures and confirm that incompatible optional adapters no longer stop startup.

## Success criteria

The work is complete when:

- an installed optional addon can change an adapter injection point without producing a server-stopping `InjectionError` from Magic Team;
- Magic Team logs a WARN for the degraded adapter/integration;
- generic Magic Team protection continues wherever independently applicable;
- the affected spell uses addon-native behavior only for the compatibility path that could not be adapted;
- unrelated adapters continue working;
- core Iron's/Minecraft failures remain strict;
- automated contracts and Forge Compile pass;
- the real Arclight smoke test reaches normal server startup past the previously failing optional adapters.

## Out of scope

- Automatically reverse-engineering arbitrary future addon implementations.
- Guaranteeing ally protection for a spell whose only protection path was the failed optional adapter.
- Turning core Iron's hooks into optional hooks.
- Changing spell classification semantics.
- Treating a clean startup as proof that every optional adapter is compatible.
