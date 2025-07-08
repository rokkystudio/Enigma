package fuck.system.enigma

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Blowfish — простой класс симметричного шифрования с поддержкой режимов CBC и ECB,
 * кодированием Base64 или HEX, а также опциональной вставкой IV в начало шифртекста.
 */
class Blowfish
{
    private var mode: String = "CBC"
    private var useBase64: Boolean = true
    private var embedIv: Boolean = true
    private var iv: ByteArray? = null

    companion object {
        private const val BLOCK_SIZE = 8
    }

    /**
     * Устанавливает режим шифрования (например, "CBC" или "ECB").
     *
     * @param value Название режима. Поддерживается "CBC" и "ECB".
     */
    fun setMode(value: String) {
        mode = value.uppercase()
    }

    /**
     * Включает кодирование выходного шифртекста в Base64.
     *
     * @param value true для Base64, false — для HEX.
     */
    fun setUseBase64(value: Boolean) {
        useBase64 = value
    }

    /**
     * Включает автоматическое добавление IV в начало шифртекста (только для режима CBC).
     *
     * @param value true для включения, false — для отключения.
     */
    fun setEmbedIv(value: Boolean) {
        embedIv = value
    }

    /**
     * Устанавливает вектор инициализации (IV) вручную.
     *
     * @param bytes Массив байтов длиной 8 байт, либо null для автоматической генерации.
     */
    fun setIv(bytes: ByteArray?) {
        iv = bytes
    }

    /**
     * Шифрует входной текст Blowfish с текущими настройками.
     *
     * @param text Открытый текст для шифрования.
     * @param key Ключ шифрования. Будет приведён к байтам в UTF-8.
     * @return Строка зашифрованного текста в Base64 или HEX, в зависимости от настроек.
     */
    fun encode(text: String, key: String): String {
        val realIv = iv ?: if (mode == "CBC") SecureRandom().generateSeed(BLOCK_SIZE) else null
        val cipher = createCipher(Cipher.ENCRYPT_MODE, key, realIv)
        val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

        val full = if (mode == "CBC" && embedIv && realIv != null) {
            realIv + encrypted
        } else encrypted

        return if (useBase64)
            Base64.encodeToString(full, Base64.NO_WRAP)
        else
            toHexString(full)
    }

    /**
     * Расшифровывает зашифрованный текст Blowfish с текущими настройками.
     *
     * @param text Зашифрованная строка в Base64 или HEX.
     * @param key Ключ шифрования. Должен совпадать с тем, что использовался при шифровании.
     * @return Расшифрованный текст.
     *
     * @throws IllegalStateException если режим CBC и IV не задан и не встроен.
     */
    fun decode(text: String, key: String): String {
        val data = if (useBase64)
            Base64.decode(text, Base64.NO_WRAP)
        else
            hexToByteArray(text)

        val (actualIv, payload) = if (mode == "CBC") {
            when {
                iv != null -> iv to data
                embedIv -> {
                    require(data.size >= BLOCK_SIZE) { "Data too short for IV" }
                    val ivPart = data.copyOfRange(0, BLOCK_SIZE)
                    val encryptedPart = data.copyOfRange(BLOCK_SIZE, data.size)
                    ivPart to encryptedPart
                }
                else -> throw IllegalStateException("IV required for CBC mode")
            }
        } else {
            null to data
        }

        val cipher = createCipher(Cipher.DECRYPT_MODE, key, actualIv)
        val decrypted = cipher.doFinal(payload)
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * Создаёт и инициализирует экземпляр {@link Cipher} для Blowfish-шифрования или дешифрования
     * с учётом текущего режима работы (ECB или CBC) и переданного ключа.
     *
     * <p>В режиме CBC используется переданный IV (если задан), либо внутреннее значение {@code iv},
     * либо сгенерированный случайный IV (если оба отсутствуют).</p>
     *
     * @param modeCode Режим инициализации {@link Cipher}: {@link Cipher#ENCRYPT_MODE} или {@link Cipher#DECRYPT_MODE}.
     * @param key Строка-ключ, преобразуемая в байтовый массив UTF-8 без нормализации.
     * @param ivOverride Вектор инициализации, который перекрывает внутреннее поле {@code iv}. Используется только в CBC.
     * @return Инициализированный объект {@link Cipher}, готовый к {@code doFinal(...)}.
     *
     * @throws IllegalArgumentException если указан неподдерживаемый режим (не "ECB" и не "CBC").
     */
    private fun createCipher(modeCode: Int, key: String, ivBytes: ByteArray? = null): Cipher {
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "Blowfish")
        val transformation = when (mode) {
            "CBC" -> "Blowfish/CBC/PKCS5Padding"
            "ECB" -> "Blowfish/ECB/PKCS5Padding"
            else -> throw IllegalArgumentException("Unsupported mode: $mode")
        }

        val cipher = Cipher.getInstance(transformation)
        if (mode == "CBC") {
            requireNotNull(ivBytes) { "IV required for CBC mode" }
            cipher.init(modeCode, secretKey, IvParameterSpec(ivBytes))
        } else {
            cipher.init(modeCode, secretKey)
        }
        return cipher
    }

    /**
     * Преобразует массив байтов в строку шестнадцатеричного представления без пробелов и префиксов.
     *
     * <p>Каждый байт преобразуется в 2-символьный нижний hex (например, {@code 0A} → {@code "0a"}).</p>
     *
     * @param data Входной массив байтов.
     * @return Строка в HEX-формате (например, {@code "4f2c9a..."}).
     */
    private fun toHexString(data: ByteArray): String {
        return data.joinToString("") { "%02x".format(it) }
    }

    /**
     * Преобразует строку HEX-формата в массив байтов.
     *
     * <p>Допускаются только символы 0–9, a–f (регистр игнорируется). Все прочие символы удаляются.
     * Строка должна содержать чётное количество допустимых символов.</p>
     *
     * @param hex Строка в шестнадцатеричном представлении.
     * @return Массив байтов, соответствующий HEX-строке.
     *
     * @throws IllegalArgumentException если длина очищенной строки нечётная.
     */
    private fun hexToByteArray(hex: String): ByteArray {
        val cleanHex = hex.lowercase().replace(Regex("[^0-9a-f]"), "")
        require(cleanHex.length % 2 == 0) { "Hex must have even length" }

        return ByteArray(cleanHex.length / 2) {
            cleanHex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }
}
