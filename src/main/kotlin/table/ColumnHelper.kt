package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import java.time.OffsetDateTime

/** Common column definition for a user reference */
fun Table.userReference(name: String, onDelete: ReferenceOption, onUpdate: ReferenceOption,) =
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
 * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by a entity
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
