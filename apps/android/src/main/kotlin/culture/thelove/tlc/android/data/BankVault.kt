package culture.thelove.tlc.android.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import culture.thelove.tlc.domain.Bank
import culture.thelove.tlc.domain.BankAccount
import culture.thelove.tlc.domain.BankDirectory
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

/**
 * The encrypted vault for transfer details.
 *
 * A full account number is the one secret this app holds, so it never touches the state file,
 * the bundle, the ledger, a log or a notification. It lives only here: AES-256/GCM, with a key
 * generated inside the Android Keystore and never exported. Losing the key (uninstall, restore to
 * another device) loses the numbers, which is the correct outcome: the saved reference can still
 * be read, and the person can add the details again deliberately.
 */
public class BankVault(filesDir: File) {

    private val file: File = File(filesDir, "bank-vault.bin")
    private val alias: String = "tlc-bank-vault-v1"

    public var lastError: String? = null
        private set

    public fun load(): List<BankAccount> {
        lastError = null
        if (!file.exists()) return emptyList()
        return try {
            val bytes = file.readBytes()
            if (bytes.size <= IV_BYTES) return emptyList()
            val iv = bytes.copyOfRange(0, IV_BYTES)
            val body = bytes.copyOfRange(IV_BYTES, bytes.size)
            val plain = decrypt(iv, body)
            parse(String(plain, Charsets.UTF_8))
        } catch (e: Exception) {
            lastError = "Saved transfer details could not be opened on this device."
            emptyList()
        }
    }

    /** Replaces the whole vault. The write is atomic so a crash cannot leave half a file. */
    public fun save(accounts: List<BankAccount>) {
        val plain = serialize(accounts).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val body = cipher.doFinal(plain)
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.outputStream().use { out ->
            out.write(iv)
            out.write(body)
            out.flush()
            out.fd.sync()
        }
        if (!temp.renameTo(file)) {
            temp.delete()
            error("The vault could not be replaced.")
        }
    }

    public fun clear() {
        file.delete()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    private fun decrypt(iv: ByteArray, body: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(body)
    }

    private fun serialize(accounts: List<BankAccount>): String {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject()
                    .put("bank_code", account.bank.code)
                    .put("account_number", account.accountNumber)
                    .put("holder_name", account.holderName),
            )
        }
        return JSONObject().put("format", FORMAT).put("accounts", array).toString()
    }

    private fun parse(text: String): List<BankAccount> {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT) error("Unknown vault format.")
        val array = root.optJSONArray("accounts") ?: JSONArray()
        val result = mutableListOf<BankAccount>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val bank: Bank = BankDirectory.byCode(item.getString("bank_code")) ?: continue
            result.add(
                BankAccount(
                    bank = bank,
                    accountNumber = item.getString("account_number"),
                    holderName = item.getString("holder_name"),
                ),
            )
        }
        return result
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val FORMAT = "tlc-bank-vault-v1"
    }
}
