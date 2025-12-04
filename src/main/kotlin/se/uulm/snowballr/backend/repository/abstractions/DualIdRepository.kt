package se.uulm.snowballr.backend.repository.abstractions

import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.repository.DualEntityId
import se.uulm.snowballr.backend.model.repository.DualIdColumnQuery
import java.util.UUID

open class DualIdRepository<EntityT : Any, TableT : IdTable<CompositeID>, EntityIdT : DualEntityId>(
    table: TableT,
    mapper: IEntityMapper<EntityT>,
    entityType: EntityType,
    private val idColumn1: Column<EntityID<UUID>>,
    private val idColumn2: Column<EntityID<UUID>>,
) : Repository<TableT, CompositeID, EntityT, EntityIdT>(
    table,
    mapper,
    entityType,
    { entityId -> DualIdColumnQuery(idColumn1, entityId.id1, idColumn2, entityId.id2) },
)

open class DualIdRepositoryNoEntity<TableT : IdTable<CompositeID>, EntityIdT : DualEntityId>(
    table: TableT,
    idColumn1: Column<EntityID<UUID>>,
    idColumn2: Column<EntityID<UUID>>,
) : DualIdRepository<Unit, TableT, EntityIdT>(table, { }, EntityType.NONE, idColumn1, idColumn2)
