package se.uulm.snowballr.backend.repository.abstractions

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.repository.SingleColumnQuery
import se.uulm.snowballr.backend.model.repository.SingleEntityId
import java.util.UUID

open class SingleIdRepository<EntityT : Any, TableT : IdTable<UUID>, EntityIdT : SingleEntityId>(
    table: TableT,
    entityMapper: IEntityMapper<EntityT>,
    entityType: EntityType,
) : Repository<TableT, UUID, EntityT, EntityIdT>(
    table,
    entityMapper,
    entityType,
    { entityId -> SingleColumnQuery(table.id, EntityID(entityId.id, table)) },
)
