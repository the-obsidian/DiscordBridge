package gg.obsidian.discordbridge.wrappers

// https://github.com/CraftIRC/4/blob/master/src/main/java/org/kitteh/craftirc/util/Logger.java
interface IDbLogger {
    fun info(message: String)
    fun warning(message: String)
    fun warning(message: String, throwable: Throwable)
    fun severe(message: String)
    fun severe(message: String, throwable: Throwable)
}