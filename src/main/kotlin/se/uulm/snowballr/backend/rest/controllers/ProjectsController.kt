package se.uulm.snowballr.backend.rest.controllers

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.auth.DummyUser
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/projects")
class ProjectsController {
    private val projects = mutableListOf(
        Project(
            id = UUID.randomUUID(),
            name = "Demo Project",
            status = ProjectStatus.ACTIVE,
            currentStage = 0,
            maxStage = 0,
            similarityThreshold = 0.85F,
            snowballingType = SnowballingType.BOTH,
            reviewMaybeAllowed = false,
            reviewDecisionMatrix = ReviewDecisionMatrix(1, emptyList()),
            fetchers = emptyMap(),
            currentStageStartedAt = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
            createdBy = DummyUser.id,
            modifiedAt = null,
            modifiedBy = null,
            deletedAt = null,
            deletedBy = null,
            archivedAt = null,
            archivedBy = null,
        ),
    )

    @GetMapping
    fun getAllProjects(): List<Project> = projects

    @GetMapping("/{id}")
    fun getProject(@PathVariable id: UUID): Project = projects.first { it.id == id }

    @PostMapping
    fun createProject(@RequestBody request: CreateProjectRequest): Project {
        val newProject = Project(
            id = UUID.randomUUID(),
            name = request.name,
            status = ProjectStatus.ACTIVE,
            currentStage = 0,
            maxStage = 0,
            similarityThreshold = 0.85F,
            snowballingType = SnowballingType.BOTH,
            reviewMaybeAllowed = false,
            reviewDecisionMatrix = ReviewDecisionMatrix(1, emptyList()),
            fetchers = emptyMap(),
            currentStageStartedAt = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
            createdBy = DummyUser.id,
            modifiedAt = null,
            modifiedBy = null,
            deletedAt = null,
            deletedBy = null,
            archivedAt = null,
            archivedBy = null,
        )

        projects.add(newProject)

        return newProject
    }

    @DeleteMapping("/{id}")
    fun deleteProject(@PathVariable id: UUID) {
        projects.removeIf { it.id == id }
    }
}
