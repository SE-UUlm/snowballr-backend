package se.uulm.snowballr.backend.repository.abstractions

interface IRepository<EntityT, EntityIdT, CreateRequestT, UpdateRequestT> :
    ICreatable<EntityT, CreateRequestT>,
    IReadable<EntityT, EntityIdT>,
    IUpdatable<EntityT, UpdateRequestT>,
    IDeletable<EntityIdT>

fun interface ICreatable<EntityT, CreateRequestT> {
    suspend fun createEntity(createRequest: CreateRequestT): EntityT
}

interface IReadable<EntityT, EntityIdT> {
    suspend fun getEntityById(id: EntityIdT): Result<EntityT>
    suspend fun getAllEntities(): List<EntityT>
}

fun interface IUpdatable<EntityT, UpdateRequestT> {
    suspend fun updateEntity(updateRequest: UpdateRequestT): EntityT
}

fun interface IDeletable<EntityIdT> {
    suspend fun deleteEntityById(id: EntityIdT)
}
