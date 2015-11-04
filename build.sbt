name := "DiscordBridge"

version := "1.0.0"

scalaVersion := "2.11.7"

resolvers += "BungeeCord" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "spigot-repo" at "https://hub.spigotmc.org/nexus/content/groups/public/"
resolvers += "xyz.gghost" at "http://gghost.xyz/maven/"

libraryDependencies += "org.spigotmc" % "spigot-api" % "1.8.8-R0.1-SNAPSHOT"
libraryDependencies += "xyz.gghost" % "jdiscord" % "1.2"

sources in EditSource <++= baseDirectory.map(d => (d / "src/main/resources" ** "*.yml").get)
targetDirectory in EditSource <<= baseDirectory(_ / "target")
variables in EditSource += ("version", version.toString)

assemblySettings
