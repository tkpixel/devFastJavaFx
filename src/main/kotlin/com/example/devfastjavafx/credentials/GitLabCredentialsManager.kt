package com.example.devfastjavafx.credentials

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object GitLabCredentialsManager {
    private const val SUBSYSTEM = "devFastJavaFx"
    private const val GITLAB_TOKEN_KEY = "GitLabPersonalAccessToken"

    private fun createCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(generateServiceName(SUBSYSTEM, GITLAB_TOKEN_KEY))
    }

    fun saveToken(token: String) {
        val attributes = createCredentialAttributes()
        val credentials = Credentials(null, token)
        PasswordSafe.instance.set(attributes, credentials)
    }

    fun getToken(): String? {
        val attributes = createCredentialAttributes()
        return PasswordSafe.instance.getPassword(attributes)
    }

    fun removeToken() {
        val attributes = createCredentialAttributes()
        PasswordSafe.instance.set(attributes, null)
    }
}
