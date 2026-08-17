package se.uulm.snowballr.backend.rest.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.incoming.invitation.InviteUserRequest
import se.uulm.snowballr.backend.model.outgoing.invitation.InvitationResponse
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IInvitationService
import java.util.UUID

/**
 * No single class-level base route: [InviteUserToProject][IInvitationService.inviteUserToProject] and
 * [GetPendingInvitationsForProject][IInvitationService.getPendingInvitationsForProject] nest under
 * [Routes.PROJECTS_ROUTE], while [AcceptProjectInvitation][IInvitationService.acceptProjectInvitation] is keyed only
 * by an opaque token and can't nest under a project at all - see GRPC_TO_REST_MAPPING.md.
 */
@RestController
class InvitationController(private val invitationService: IInvitationService) {
    @PostMapping("${Routes.PROJECTS_ROUTE}/{projectId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    fun inviteUserToProject(@PathVariable projectId: UUID, @RequestBody request: InviteUserRequest) {
        onRequest { invitationService.inviteUserToProject(projectId, request.userEmail) }
    }

    @GetMapping("${Routes.PROJECTS_ROUTE}/{projectId}/invitations")
    fun getPendingInvitationsForProject(@PathVariable projectId: UUID): List<InvitationResponse> = onRequest {
        invitationService.getPendingInvitationsForProject(projectId)
    }

    @PostMapping("${Routes.INVITATIONS_ROUTE}/{token}/accept")
    fun acceptProjectInvitation(@PathVariable token: String) {
        onRequest { invitationService.acceptProjectInvitation(token) }
    }
}
