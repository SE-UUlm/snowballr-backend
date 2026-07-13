package se.uulm.snowballr.backend.repository.association

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.PaperNavigationDirection
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaper
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectPaperNotFoundException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.doesEntityExist
import se.uulm.snowballr.backend.repository.getEntities
import se.uulm.snowballr.backend.repository.getEntityByIdOrNull
import se.uulm.snowballr.backend.repository.getEntityByKeyAsResult
import se.uulm.snowballr.backend.repository.getEntityOrNull
import se.uulm.snowballr.backend.repository.insertAndGet
import se.uulm.snowballr.backend.repository.wrapAsResult
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.association.toProjectPaper
import se.uulm.snowballr.backend.table.association.toProjectPaperWithPaper
import java.sql.Connection
import java.util.UUID
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

/**
 * Defines an interface for repository operations related to the [ProjectPaperTable].
 *
 * This interface provides abstraction for handling persistence and retrieval
 * operations for project papers. By using this interface, the functionality for creating
 * project papers can remain decoupled from the specifics of the database layer.
 */
@Suppress("ComplexInterface")
interface IProjectPaperTableRepo {
    /**
     * Returns a [Result] containing the project paper by its ID or a [NotFoundException] if the project paper with the
     * passed [id] doesn't exist.
     */
    suspend fun getProjectPaperById(id: UUID): Result<ProjectPaper>

    /**
     * Returns a [Result] containing the project paper by its relative ID in a project or a
     * [ProjectPaperNotFoundException] if the project paper with the passed [relativeId] doesn't exist.
     *
     * Therefore, this method searches for a project paper in the project with the [projectId] with
     * the [relativeId] (`localPaperId`).
     *
     * @param projectId The unique identifier of the project to which the project paper belongs.
     * @param relativeId The local paper ID of the project paper in the project.
     * @return The [ProjectPaper] matching the given project ID and relative ID.
     */
    suspend fun getProjectPaperByRelativeId(projectId: UUID, relativeId: Int): Result<ProjectPaper>

    /**
     * Checks if a project paper exists in a project, based on the provided paper ID.
     *
     * @param projectId The unique identifier of the project.
     * @param paperId The unique identifier of the paper to check for existence.
     * @return `true` if the project paper exists, otherwise `false`.
     */
    suspend fun doesProjectPaperExist(projectId: UUID, paperId: UUID): Boolean

    /**
     * Retrieves all project papers for the specified project.
     *
     * @param projectId The unique identifier of the project for which the project papers should be retrieved.
     * @return A list of [ProjectPaper] instances associated with the given project.
     */
    suspend fun getAllProjectPapersForProject(projectId: UUID): List<ProjectPaper>

    /**
     * Retrieves the adjacent local paper ID for the project paper with the given local paper ID.
     *
     * @param projectId The unique identifier of the project for which the adjacent local paper ID is requested.
     * @param localPaperId The current local paper ID used as a reference to compute the adjacent ID.
     * @param direction The navigation direction indicating whether to retrieve the next or previous paper.
     * Must be one of the values from [PaperNavigationDirection].
     * @return A [Result] containing the adjacent available local paper ID as a [Long] or a
     * [FailedPreconditionException] if it cannot be determined.
     */
    suspend fun getAdjacentPaper(
        projectId: UUID,
        localPaperId: Int,
        direction: PaperNavigationDirection,
    ): Result<ProjectPaper>

    /**
     * Retrieves a list of project papers along with their associated papers for the specified project.
     *
     * This method fetches data that combines information from a `ProjectPaper` and its related `Paper`
     * for a given project, represented by its unique identifier.
     *
     * @param projectId The unique identifier of the project for which the project papers and
     *                  their associated papers should be retrieved.
     * @return A list of [ProjectPaperWithPaper] instances, each representing a relationship
     *         between a project paper and its associated paper.
     */
    suspend fun getAllProjectPapersWithPapers(projectId: UUID): List<ProjectPaperWithPaper>

    /**
     * Adds a paper to a project and returns the newly created project paper instance.
     *
     * This function associates a paper with a specific project by creating a new [ProjectPaper]
     * entry, linking the provided paper and project. The local paper ID of the new [ProjectPaper] gets increased by 1,
     * depending on the maximum local paper ID of the current papers in the project.
     *
     * @param request The request object containing the information necessary to add the paper
     *                to the project, such as the paper and project IDs.
     * @param userId The unique identifier of the user performing the operation.
     * @return The created [ProjectPaper] object representing the association between the paper
     *         and the project.
     */
    suspend fun addPaperToProject(request: GrpcProjectPaper.Add, userId: UUID): ProjectPaper

    /**
     * Calculates and returns the progress of a project as a float value between 0.0 and 1.0.
     *
     * The progress is determined by evaluating the ratio of fully reviewed papers to all papers
     * in a project.
     *
     * @param projectId The unique identifier of the project.
     * @return A float between 0.0 and 1.0 representing the project progress.
     */
    suspend fun getProjectProgress(projectId: UUID): Float

    /**
     * Retrieves a list of subsequent project papers from a given starting point defined by `localPaperId` and `stage`.
     *
     * Subsequent project papers are defined as project papers within the same project that are either:
     * - in the same `stage` but have a greater `localPaperId`, or
     * - in a subsequent `stage` regardless of their `localPaperId`.
     *
     * @param projectId The unique identifier of the project to which the papers belong.
     * @param localPaperId The local ID of the starting paper in the project from which the later papers are retrieved.
     * @param stage The stage of the papers that should be considered when retrieving later project papers.
     * @return A list of subsequent [ProjectPaper] instances meeting the specified criteria.
     */
    suspend fun getSubsequentProjectPapers(projectId: UUID, localPaperId: Int, stage: Int): List<ProjectPaper>

    /**
     * Updates the decision of a project paper.
     *
     * @param projectPaperId The unique identifier of the project paper to be updated.
     * @param decision The new [PaperDecision] to be set.
     */
    suspend fun updateProjectPaperDecision(projectPaperId: UUID, decision: PaperDecision)
}

/**
 * Repository implementation for managing the [ProjectPaperTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations
 * for project paper data by leveraging the database abstraction defined in [IDatabase]. It
 * facilitates CRUD operations on project paper records associated with a given project and
 * ensures database transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class ProjectPaperTableRepo(
    private val db: IDatabase,
) : IProjectPaperTableRepo {
    private fun getProjectPaperByIdOrNull(id: UUID): ProjectPaper? =
        ProjectPaperTable.getEntityByIdOrNull(id, ResultRow::toProjectPaper)

    /**
     * Generates the next available local paper ID for the specified project by checking the existent maximum local
     * paper ID within the project and incrementing it by one. If no local paper IDs exist for the given project, it
     * returns 0.
     *
     * @param projectId The unique identifier of the project for which the next local paper ID is to be generated.
     * @return The next available local paper ID as a [Int].
     */
    private fun getNextLocalIdForProject(projectId: UUID): Int = ProjectPaperTable
        .selectAll()
        .where { ProjectPaperTable.projectId eq projectId }
        .maxOfOrNull { it[ProjectPaperTable.localPaperId] }
        ?.plus(1)
        ?: 0

    override suspend fun getAdjacentPaper(
        projectId: UUID,
        localPaperId: Int,
        direction: PaperNavigationDirection,
    ): Result<ProjectPaper> = db.query {
        val isNext = direction == PaperNavigationDirection.NEXT
        val paperId = ProjectPaperTable
            .selectAll()
            .where {
                (ProjectPaperTable.projectId eq projectId) and
                    if (isNext) {
                        ProjectPaperTable.localPaperId greater localPaperId
                    } else {
                        ProjectPaperTable.localPaperId less localPaperId
                    }
            }
            .orderBy(
                ProjectPaperTable.localPaperId to if (isNext) SortOrder.ASC else SortOrder.DESC,
            )
            .map { it.toProjectPaper() }
            .firstOrNull()

        wrapAsResult(paperId, FailedPreconditionException("There is no $direction project paper in the project."))
    }

    override suspend fun getProjectPaperById(id: UUID): Result<ProjectPaper> = db.query {
        getEntityByKeyAsResult(::getProjectPaperByIdOrNull, EntityType.PROJECT_PAPER, id)
    }

    override suspend fun getProjectPaperByRelativeId(projectId: UUID, relativeId: Int): Result<ProjectPaper> =
        db.query {
            val projectPaper = ProjectPaperTable.getEntityOrNull(ResultRow::toProjectPaper) {
                (ProjectPaperTable.projectId eq projectId) and (ProjectPaperTable.localPaperId eq relativeId)
            }

            wrapAsResult(projectPaper, ProjectPaperNotFoundException(relativeId, projectId))
        }

    override suspend fun doesProjectPaperExist(projectId: UUID, paperId: UUID): Boolean = db.query {
        ProjectPaperTable.doesEntityExist {
            (ProjectPaperTable.paperId eq paperId) and (ProjectPaperTable.projectId eq projectId)
        }
    }

    override suspend fun getAllProjectPapersForProject(projectId: UUID): List<ProjectPaper> = db.query {
        ProjectPaperTable.getEntities(ResultRow::toProjectPaper) { ProjectPaperTable.projectId eq projectId }
    }

    override suspend fun getAllProjectPapersWithPapers(projectId: UUID): List<ProjectPaperWithPaper> = db.query {
        ProjectPaperTable
            .join(
                PaperTable,
                JoinType.INNER,
                onColumn = ProjectPaperTable.paperId,
                otherColumn = PaperTable.id,
            )
            .selectAll()
            .where { ProjectPaperTable.projectId eq projectId }
            .map { it.toProjectPaperWithPaper() }
    }

    /**
     * Uses [Connection.TRANSACTION_READ_COMMITTED] so that transactions don't get aborted after next paper is added
     * with a higher local paper ID. The second transaction unblocks after lock is removed and then sees fresh snapshot
     * from first transaction.
     */
    override suspend fun addPaperToProject(request: GrpcProjectPaper.Add, userId: UUID): ProjectPaper =
        db.query(transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            val paperId = parseUUID(request.paperId, EntityType.PAPER)
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)

            // Lock the project row so that concurrent calls for the same project block here and then re-read MAX with a
            // fresh snapshot after the previous transaction commits.
            ProjectTable.selectAll()
                .where { ProjectTable.id eq projectId }
                .forUpdate(ForUpdateOption.ForUpdate)
                .single()

            val localPaperId = getNextLocalIdForProject(projectId)

            ProjectPaperTable
                .insertAndGet(ResultRow::toProjectPaper) {
                    it[ProjectPaperTable.paperId] = paperId
                    it[ProjectPaperTable.projectId] = projectId
                    it[ProjectPaperTable.localPaperId] = localPaperId
                    it[stage] = request.stage.toInt()
                    it[decision] = PaperDecision.UNREVIEWED
                    it[createdBy] = userId
                }
        }

    override suspend fun getProjectProgress(projectId: UUID): Float = db.query {
        val allPapersCount =
            ProjectPaperTable.selectAll().where { ProjectPaperTable.projectId eq projectId }.count()
        if (allPapersCount == 0L) {
            0.0f
        } else {
            val fullyReviewedPapersCount = ProjectPaperTable.selectAll().where {
                val paperReviewedOp =
                    (ProjectPaperTable.decision eq PaperDecision.ACCEPTED) or
                        (ProjectPaperTable.decision eq PaperDecision.DECLINED)

                paperReviewedOp and (ProjectPaperTable.projectId eq projectId)
            }.count()
            val progress = fullyReviewedPapersCount.toFloat() / allPapersCount
            progress
        }
    }

    override suspend fun updateProjectPaperDecision(projectPaperId: UUID, decision: PaperDecision): Unit = db.query {
        ProjectPaperTable.update({ ProjectPaperTable.id eq projectPaperId }) {
            it[ProjectPaperTable.decision] = decision
        }
    }

    override suspend fun getSubsequentProjectPapers(
        projectId: UUID,
        localPaperId: Int,
        stage: Int,
    ): List<ProjectPaper> = db.query {
        val sameStageButGreaterLocalIdOp =
            (ProjectPaperTable.stage eq stage) and (ProjectPaperTable.localPaperId greater localPaperId)
        val greaterStageOp = ProjectPaperTable.stage greater stage
        ProjectPaperTable
            .selectAll()
            .where {
                (ProjectPaperTable.projectId eq projectId) and (sameStageButGreaterLocalIdOp or greaterStageOp)
            }
            .map { it.toProjectPaper() }
    }
}
