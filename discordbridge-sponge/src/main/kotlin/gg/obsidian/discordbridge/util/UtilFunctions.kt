package gg.obsidian.discordbridge.util

import java.util.*

fun <T> Optional<T>.unwrap(): T? = orElse(null)
