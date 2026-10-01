# XeonKitPvP Core

Core KitPvP per Spigot/Bukkit, con impostazione da network competitivo e base Java 8.

## Funzioni

- Kit configurabili in `kits.yml`: inventario, armatura, effetti, prezzo, permission, cooldown e ability.
- GUI `/kit` per selezione e acquisto.
- Coin economy interna persistente.
- XP e livelli configurabili fino a 100.
- Kills, deaths, K/D, killstreak e best streak persistenti.
- Ricompense killstreak con coin e comandi console.
- Bounty/taglie.
- Combat tag con penalità al quit durante il combattimento.
- Spawn protection e protezione del void.
- Configurazione di build/break/hunger.
- Soup healing.
- Scoreboard live.
- Classifiche `/top kills|coins|streak`.
- Sistema gang/clan con leader, inviti, join, leave e friendly-fire configurabile.
- `/kitpvp debug` con informazioni del core e dell'ambiente.
- Multi-world tramite lista degli arena worlds.
- Messaggi configurabili in `lang.yml`.

## Compatibilità

Il progetto viene compilato contro Spigot API 1.8.8 e usa Java 8 bytecode. Non usa NMS e riduce l'uso di enum/version-specifici tramite parsing dei materiali e fallback.

Questo approccio è pensato per coprire 1.8.x fino alle versioni moderne. Le nuove release di Minecraft possono comunque cambiare il comportamento del server o deprecare API, quindi la release esatta va testata sul server reale prima della produzione.

## IntelliJ IDEA

Apri la cartella come progetto Maven e usa:

`mvn clean package`

Il JAR sarà in:

`target/XeonKitPvP-1.0.0.jar`

## Primo setup

1. Avvia il server una volta.
2. Crea o usa il mondo KitPvP.
3. Esegui `/kitpvp setspawn` dal punto di spawn desiderato.
4. Esegui `/kitpvp arena add <mondo>` se il mondo non è `kitpvp`.
5. Personalizza `config.yml`, `lang.yml` e `kits.yml`.

## Comandi giocatori

`/kit`
`/kit <nome>`
`/spawn`
`/stats [player]`
`/top [kills|coins|streak]`
`/bounty <player> [amount]`
`/gang create|invite|join|leave|info|disband`

## Comandi admin

`/kitpvp help`
`/kitpvp setspawn`
`/kitpvp arena add|remove|list [world]`
`/kitpvp reload`
`/kitpvp debug`
`/kitpvp top ...`
`/kitpvp kit <nome>`

Permission principale: `kitpvp.admin`

Permission bypass: `kitpvp.bypass`

Accesso a un singolo kit: `kitpvp.kit.<nome>`

## Build verification in this workspace

The Java sources and YAML files were statically validated. A full Maven build was not available in this workspace because Maven/dependency downloads are unavailable here, so the final JAR must be produced in IntelliJ/Maven on your machine.
