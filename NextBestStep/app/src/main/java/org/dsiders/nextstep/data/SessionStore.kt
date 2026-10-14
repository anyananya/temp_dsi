package org.dsiders.nextstep.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Remembers who is signed in on this phone. */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val _userId = MutableStateFlow(prefs.getLong(KEY_USER, -1L).takeIf { it > 0 })
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    fun signIn(id: Long) {
        prefs.edit().putLong(KEY_USER, id).apply()
        _userId.value = id
    }

    fun signOut() {
        prefs.edit().remove(KEY_USER).apply()
        _userId.value = null
    }

    private companion object {
        const val KEY_USER = "user_id"
    }
}

/** PBKDF2 password hashing; passwords never touch the database in plain text. */
object Passwords {
    private const val ITERATIONS = 20_000
    private const val KEY_BITS = 256

    fun newSalt(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return bytes.toHex()
    }

    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded.toHex()
    }

    fun matches(password: String, user: UserEntity): Boolean = hash(password, user.salt) == user.passwordHash

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
