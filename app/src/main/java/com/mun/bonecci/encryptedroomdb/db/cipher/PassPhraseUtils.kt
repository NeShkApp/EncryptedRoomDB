package com.mun.bonecci.encryptedroomdb.db.cipher

/**
 * Utility class for managing the passphrase used for database encryption.
 */
import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.security.auth.x500.X500Principal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

object PassPhraseUtils {
    private var cachedPassphrase: String? = null
    private const val PASSPHRASE_PREFS = "PassphrasePrefs"
    private const val PASSPHRASE_KEY = "SHARED_PREF_PASSPHRASE_KEY"
    private const val KEY_ALIAS = "ENCRYPTED_ROOM_DB_KEY_ALIAS"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    // Для безпечного паралельного доступу
    private val atomicPassphrase = AtomicReference<String?>(null)

    /**
     * Повертає пасфрейз для шифрування/дешифрування бази даних.
     * Якщо він уже кешований або зчитаний з Keystore+SharedPreferences, повертається негайно.
     * Інакше генерується новий, зберігається (засекречується) і повертається.
     */
    fun getPassphrase(context: Context): String {
        // 1) Перевіряємо кешоване значення
        cachedPassphrase?.let {
            Log.d("MAIN TEST PassPhraseUtils", "Using cached passphrase: $it")
            return it
        }
        atomicPassphrase.get()?.let {
            Log.d("MAIN TEST PassPhraseUtils", "Using atomic passphrase: $it")
            return it
        }

        // 2) Якщо ні, синхронізовано генеруємо/читаємо
        return synchronized(this) {
            // Перевіряємо ще раз під час блокування
            cachedPassphrase?.let {
                Log.d("MAIN TEST PassPhraseUtils", "Using cached passphrase (inside lock): $it")
                return it
            }
            atomicPassphrase.get()?.let {
                Log.d("MAIN TEST PassPhraseUtils", "Using atomic passphrase (inside lock): $it")
                return it
            }

            // Зчитуємо з «зашифрованих» SharedPreferences
            val passphrase = readOrCreateEncryptedPassphrase(context)
            cachedPassphrase = passphrase
            atomicPassphrase.set(passphrase)

            Log.d("MAIN TEST PassPhraseUtils", "Generated or retrieved passphrase: $passphrase")
            passphrase
        }
    }

    /**
     * Зчитує існуючий зашифрований пасфрейз з SharedPreferences, якщо він є.
     * Якщо його немає — генерує новий (UUID), зашифровує через Keystore, записує в SharedPreferences.
     */
    private fun readOrCreateEncryptedPassphrase(context: Context): String {
        val prefs = context.getSharedPreferences(PASSPHRASE_PREFS, Context.MODE_PRIVATE)

        // Якщо в SharedPreferences є запис (Base64(iv):Base64(ciphertext)), дешифруємо
        val encryptedStored = prefs.getString(PASSPHRASE_KEY, null)
        if (!encryptedStored.isNullOrBlank()) {
            try {
                return decryptPassphrase(encryptedStored)
            } catch (e: Exception) {
                Log.e("PassPhraseUtils", "Не вдалося дешифрувати збережений пасфрейз: ${e.message}")
                // У випадку помилки можемо вирішити: згенерувати новий або кидати виняток
                // Тут згенеруємо новий.
            }
        }

        // Якщо немає чи десилкація не вдалася — генеруємо новий пасфрейз
        val newPassphrase = UUID.randomUUID().toString()
        Log.d("MAIN TEST PassPhraseUtils", "Creating new passphrase: $newPassphrase")
        // Зашифровуємо та зберігаємо в SharedPreferences
        val encryptedForStore = encryptPassphrase(newPassphrase)
        prefs.edit().putString(PASSPHRASE_KEY, encryptedForStore).apply()
        Log.d("MAIN TEST PassPhraseUtils", "Stored encrypted passphrase: $encryptedForStore")

        return newPassphrase
    }

    /**
     * Шифрує переданий текст (пасфрейз) через Keystore-ключ, повертаючи строку
     * у форматі Base64(IV):Base64(ciphertext).
     */
    private fun encryptPassphrase(plaintext: String): String {
        // 1) Переконуємось, що ключ існує в Keystore
        generateKeyIfNeeded()

        // 2) Отримуємо SecretKey з Keystore
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        // 3) Ініціалізуємо Cipher для шифрування (AES/GCM/NoPadding)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        // 4) Отримуємо IV і шифротекст
        val iv = cipher.iv                                  // 12 байтів IV
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // 5) Кодуємо обидва у Base64 (без переривань рядка)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

        // 6) Повертаємо комбінований рядок "iv:ciphertext"
        return "$ivBase64:$cipherBase64"
    }

    /**
     * Дешифрує рядок (у форматі Base64(IV):Base64(ciphertext)) через Keystore-ключ,
     * повертаючи оригінальний текст.
     */
    private fun decryptPassphrase(encryptedDataWithIv: String): String {
        // 1) Сплітимо на IV та шифротекст
        val parts = encryptedDataWithIv.split(":")
        require(parts.size == 2) { "Невірний формат зашифрованих даних" }

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

        // 2) Отримуємо SecretKey з Keystore
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        // 3) Ініціалізуємо Cipher для дешифрування
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)  // тег 128 біт
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        // 4) Отримуємо оригінальний байт-масив, конвертуємо в рядок
        val plaintextBytes = cipher.doFinal(cipherBytes)
        return String(plaintextBytes, Charsets.UTF_8)
    }

    /**
     * Створює AES-ключ у Android Keystore, якщо він ще не існує.
     * Використовується для шифрування/дешифрування пасфрейзу.
     */
    private fun generateKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keySpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // Додаткові обмеження (за бажанням):
                // .setUserAuthenticationRequired(true)
                // .setUserAuthenticationValidityDurationSeconds(300)
                .build()
            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
            Log.d("MAIN TEST PassPhraseUtils", "Keystore AES key generated under alias $KEY_ALIAS")
        }
    }
}