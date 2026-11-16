package com.tumba.ai.data.security

interface SecretStore {
    fun put(alias: String, value: String)

    fun get(alias: String): String?

    fun remove(alias: String)

    fun aliases(): Set<String>

    fun clear()
}
