# Magic-Team

Magic-Team is a professional administrative mod for Minecraft 1.20.1 (Forge) designed to enhance team-based gameplay within the Iron's Spells 'n Spellbooks ecosystem. It provides server administrators with granular, real-time control over spell targeting, damage, and effects, ensuring that friendly-fire is prevented while support mechanisms remain functional.

## Features

### Administrative Control System
The mod implements a robust command-driven interface (`/magicteam`) exclusively available to server operators (Level 2+). All configuration changes are applied in real-time and persist through server restarts via a dedicated server configuration file.

### Dynamic Spell Filtering
Magic-Team allows for the categorization of spells without requiring source code modifications.
*   **Beneficial Spells**: A whitelist of spell IDs or keywords that are permitted on allies.
*   **Explicit Harmful Spells**: A blacklist of spells that are strictly blocked from targeting allies, even if they belong to traditionally supportive magic schools (e.g., Holy).
*   **Tab-Completion**: Full integration with the Minecraft command engine provides suggestions for all registered spell IDs from Iron's Spells and any installed addons.

### Multi-Language Support (i18n)
Full internationalization is provided via translation keys. The mod currently supports:
*   English (en_us)
*   Portuguese (pt_br)
Messages and command feedback adapt automatically to the client's language selection.

### Staff Bypass Mode
Administrators can toggle a personal or target-based bypass mode using `/magicteam bypass [player]`. When active, the designated player ignores all protection rules, allowing for technical testing or event management without restriction.

### Stealth and Transparency
Command feedback for configuration changes is broadcast only to online operators to ensure transparency among the staff team, while remaining completely invisible to regular players. Informational commands (e.g., status, list, view) are strictly private to the executor.

## Commands

### Configuration Toggles
*   `/magicteam targeting <true|false>`: Toggles the blocking of spell targeting against allies.
*   `/magicteam damage <true|false>`: Toggles the prevention of spell damage against allies.
*   `/magicteam effects <true|false>`: Toggles the filtering of status effects applied by spells to allies.
*   `/magicteam alliance <true|false>`: Toggles a global "areAllies" override for team compatibility.

### Filter Management
*   `/magicteam filter add beneficial <spell_id>`: Adds a spell to the support whitelist.
*   `/magicteam filter remove beneficial <spell_id>`: Removes a spell from the support whitelist.
*   `/magicteam filter add harmful <spell_id>`: Adds a spell to the offensive blacklist.
*   `/magicteam filter remove harmful <spell_id>`: Removes a spell from the offensive blacklist.
*   `/magicteam filter view <beneficial|harmful>`: Lists the currently active keywords in the specified filter.

### Information and Status
*   `/magicteam status`: Displays the current state of all protection modules and filter counts.
*   `/magicteam list` (Alias for status): Displays the protection summary.

### Administrative Utility
*   `/magicteam bypass [player]`: Toggles bypass mode for the executor or a specified player.

## Installation and Requirements

*   **Minecraft Version**: 1.20.1
*   **Mod Loader**: Forge 47.4.x+
*   **Required Dependency**: Iron's Spells 'n Spellbooks (3.15.3+)

Install the JAR file into the `mods` folder of your Forge server. The server configuration will be generated at `config/magic_team-server.toml` upon the first launch.

## License
All rights reserved. Professional use only.
