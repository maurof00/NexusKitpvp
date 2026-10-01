# Build verification note

The source was statically checked for balanced Java braces, duplicate imports, and valid YAML configuration.

A full Maven compile could not be executed in this environment because Maven is not installed and outbound dependency downloads are unavailable here. The project therefore remains source-first and should be compiled in IntelliJ IDEA/Maven on the developer machine, where the configured Spigot repository can supply `spigot-api:1.8.8-R0.1-SNAPSHOT`.
