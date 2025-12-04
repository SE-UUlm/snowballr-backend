package se.uulm.snowballr.backend.repository.abstractions

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow

fun interface IEntityMapper<EntityT> {
    fun toEntity(row: ResultRow): EntityT
}

class SingleValueMapper<T : Any>(val column: Column<EntityID<T>>) : IEntityMapper<T> {
    override fun toEntity(row: ResultRow): T = row[column].value
}
