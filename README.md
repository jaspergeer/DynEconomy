# DynEconomy
This is a Spigot plugin I built for a Towny/Economy server that I thought I'd share. It adds a level of depth to the economic simulation by putting a soft cap on the amount of money players can create and making Towny into a dedicated money sink.
### Versions
* For 1.16.5 - [download .jar](https://www.mediafire.com/file/c9hb33hy82j4620/DynEconomy-1.1.jar/file)

## Getting Started
### Prerequisites
* Java
### Installation
Step 1) Navigate to the root directory and run
```
gradlew jar
```
Step 2) This should generate a jarfile in build/plugin-jar. Move this jarfile to to your server plugins directory.

## Built With
* [Java](https://www.java.com/en/) - Language the application is built in
* [Intellij](https://www.jetbrains.com/idea/) - IDE used for development
* [Gradle](https://gradle.org/features/) - Build automation tool
### Dependencies
* [Spigot](https://www.spigotmc.org) - Minecraft server API
* [TheNewEconomy](https://www.spigotmc.org/resources/the-new-economy.7805/) - Economy plugin
* [Towny](https://www.spigotmc.org/resources/towny-advanced.72694/) - For claiming land, creating towns and nations
* [Vault](https://www.spigotmc.org/resources/vault.34315/)
