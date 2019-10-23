package fr.rischmann.apero

import okhttp3.OkHttpClient

class Provision(private val client: OkHttpClient, private val endpoint: String) {

    fun exchange(code: String): Credentials {
        TODO()
    }
}