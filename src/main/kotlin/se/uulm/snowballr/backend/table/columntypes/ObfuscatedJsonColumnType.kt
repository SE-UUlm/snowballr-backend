package se.uulm.snowballr.backend.table.columntypes

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.JsonColumnType
import org.jetbrains.exposed.v1.json.json

/**
 * [JsonColumnType], but values for sensitive keys are obfuscated in SQL logs.
 *
 * Use [obfuscatedJson] to register a column using this column type.
 */
class ObfuscatedJsonColumnType<T : Any>(
    serialize: (T) -> String,
    deserialize: (String) -> T,
    private val serializeForLog: (T) -> String,
) : JsonColumnType<T>(serialize, deserialize) {
    override fun nonNullValueToString(value: T): String = "'${serializeForLog(value)}'"
}

/**
 * Words that might represent sensitive data.
 */
val SENSITIVE_WORDS: Set<String> = setOf("key", "token", "password", "pwd", "passwd", "secret", "access", "auth")

/**
 * Same as [json], but values for sensitive keys are obfuscated in SQL logs across all nested JSON objects.
 */
inline fun <reified T : Any> Table.obfuscatedJson(
    name: String,
    jsonConfig: Json,
    kSerializer: KSerializer<T> = serializer<T>(),
): Column<T> = registerColumn(
    name,
    ObfuscatedJsonColumnType(
        serialize = { jsonConfig.encodeToString(kSerializer, it) },
        deserialize = { jsonConfig.decodeFromString(kSerializer, it) },
        serializeForLog = {
            jsonConfig.encodeToString(
                obfuscateJsonElement(jsonConfig.encodeToJsonElement(kSerializer, it), SENSITIVE_WORDS),
            )
        },
    ),
)

/**
 * Obfuscates all values of keys that are considered sensitive by [sensitiveWords].
 */
fun obfuscateJsonElement(element: JsonElement, sensitiveWords: Set<String>): JsonElement = when (element) {
    is JsonObject -> JsonObject(
        element.entries.associate { (key, value) ->
            val isSensitive = sensitiveWords.any { it.lowercase() in key.lowercase() }
            key to if (isSensitive) JsonPrimitive("[OBFUSCATED]") else obfuscateJsonElement(value, sensitiveWords)
        },
    )

    is JsonArray -> JsonArray(element.map { obfuscateJsonElement(it, sensitiveWords) })
    is JsonPrimitive -> element
}
