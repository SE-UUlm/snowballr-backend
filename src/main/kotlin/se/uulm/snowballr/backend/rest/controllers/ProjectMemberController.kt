package se.uulm.snowballr.backend.rest.controllers

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.dto.projectmember.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.incoming.projectmember.UpdateMemberRoleRequest
import se.uulm.snowballr.backend.model.incoming.projectmember.UpdateProjectMemberRoleRequest
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IProjectMemberService
import java.util.UUID

@RestController
@RequestMapping(Routes.PROJECT_MEMBERS_ROUTE)
class ProjectMemberController(private val projectMemberService: IProjectMemberService) {
    @GetMapping
    fun getProjectMembers(@PathVariable projectId: UUID): List<ProjectMemberWithUser> = onRequest {
        projectMemberService.getProjectMembers(projectId)
    }

    @PutMapping("/{userId}/role")
    fun updateProjectMemberRole(
        @PathVariable projectId: UUID,
        @PathVariable userId: UUID,
        @RequestBody request: UpdateMemberRoleRequest,
    ) {
        onRequest {
            projectMemberService.updateProjectMemberRole(
                UpdateProjectMemberRoleRequest(projectId = projectId, userId = userId, newRole = request.newRole),
            )
        }
    }

    @DeleteMapping("/{email}")
    fun removeProjectMember(@PathVariable projectId: UUID, @PathVariable email: String) {
        onRequest { projectMemberService.removeProjectMember(projectId, email) }
    }
}
