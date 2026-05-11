package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.v1.core.BasicBinaryColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.postgresql.util.HStoreConverter
import se.uulm.snowballr.backend.table.columntypes.HStoreColumnType
import se.uulm.snowballr.backend.table.columntypes.ObfuscatedTextColumnType
import se.uulm.snowballr.backend.table.columntypes.RedactedBinaryColumnType
import java.time.OffsetDateTime

/** Common column definition for a user reference */
fun Table.userReference(name: String, onDelete: ReferenceOption, onUpdate: ReferenceOption) =
    reference(name, UserTable, onDelete, onUpdate)

/** Common column definition for the "created at" timestamp. */
fun Table.createdAt() = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }

/**
 * Reference to the user who created the entity.
 *
 * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by the entity
 * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
 */
fun Table.createdBy() = userReference("created_by", ReferenceOption.RESTRICT, ReferenceOption.CASCADE)

/** Common column definition for the "modified at" timestamp. */
fun Table.modifiedAt() = timestampWithTimeZone("modified_at").nullable()

/**
 * Nullable reference to the user who modified the entity.
 *
 * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by an entity
 * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
 */
fun Table.modifiedBy() = userReference("modified_by", ReferenceOption.RESTRICT, ReferenceOption.CASCADE).nullable()

/** Common column definition for the "deleted at" timestamp. */
fun Table.deletedAt() = timestampWithTimeZone("deleted_at").nullable()

/**
 * Nullable reference to the user who deleted the project.
 *
 * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by a project
 * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
 */
fun Table.deletedBy() = userReference("deleted_by", ReferenceOption.RESTRICT, ReferenceOption.CASCADE).nullable()

/**
 * Same as [Table.text], but with the [ObfuscatedTextColumnType] instead of the [TextColumnType].
 */
fun Table.obfuscatedText(name: String, collate: String? = null, eagerLoading: Boolean = false) =
    registerColumn(name, ObfuscatedTextColumnType(collate, eagerLoading))

/**
 * Same as [Table.binary], but with the [RedactedBinaryColumnType] instead of the [BasicBinaryColumnType].
 */
fun Table.redactedBinary(name: String) = registerColumn(name, RedactedBinaryColumnType())

/**
 * Stores a Map<String, String> inside an [HStoreColumnType]. Unallowed characters are automatically escaped.
 *
 * The order of the key-value pairs is not guaranteed.
 */
fun Table.stringMap(name: String): Column<Map<String, String>> = registerColumn(name, HStoreColumnType())
    .transform(
        wrap = {
            // HStore -> Map
            it.trim('{', '}')
                .split(", ")
                .filter(String::isNotEmpty)
                .associate { pair ->
                    val (left, right) = pair.split('=')
                    left to right
                }
        },
        unwrap = {
            // Map -> HStore
            HStoreConverter.toString(it)
        },
    )
