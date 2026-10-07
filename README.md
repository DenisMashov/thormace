# ThorMace

Purpur / Paper 1.21.11 - custom Thor-style mace.

- `/thormace` (aliases: `/thor`, `/mjolnir`) - gives you the hammer
- `/thormace <player>` - gives it to another player
- `/thormace reload` - reloads the config

Hitting a player or mob with the hammer strikes it with lightning 5 times,
blinds it for 5 seconds (players also see a red THOR'S CURSE title) and
blasts the blocks within a radius of 5 into the air. Real blocks are never
changed - the animation is purely visual. Everything is configurable in `config.yml`.

## Build
Push to GitHub -> Actions -> Build -> download the `ThorMace` artifact (jar).
Locally: `mvn package`

## Permissions
- thormace.give (op) / thormace.use (everyone) / thormace.admin (op)
