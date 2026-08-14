package se.uulm.snowballr.backend.rest.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserSettingsWithCriteria
import se.uulm.snowballr.backend.model.incoming.user.RegisterRequest
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserRequest
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IInvitationService
import se.uulm.snowballr.backend.service.IUserService
import java.util.UUID

@RestController
@RequestMapping(Routes.USERS_ROUTE)
class UsersController(
    private val userService: IUserService,
    private val invitationService: IInvitationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest) = onRequest { userService.register(request) }

    @GetMapping
    fun getUsers(@RequestParam(required = false) email: String?): List<User> = onRequest {
        if (email != null) listOf(userService.getUserByEmail(email)) else userService.getAllUsers()
    }

    @GetMapping("/invite-candidates")
    fun getInviteCandidates(
        @RequestParam(required = false) projectId: UUID?,
        @RequestParam query: String,
    ): List<User> = onRequest { invitationService.getInviteCandidates(projectId, query) }

    @GetMapping("/me")
    fun getCurrentUser(): User = onRequest { userService.getCurrentUser() }

    @GetMapping("/me/settings")
    fun getUserSettings(): UserSettingsWithCriteria = onRequest { userService.getUserSettings() }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): User = onRequest { userService.getUserById(id) }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: UUID, @RequestBody request: UpdateUserRequest): User = onRequest {
        userService.updateUser(request.copy(userId = id), FULL_UPDATE_PATHS)
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: UUID) {
        onRequest { userService.softDeleteUser(id) }
    }

    private companion object {
        // Every field of UpdateUserRequest, so a REST PUT always behaves as a full replace
        // instead of the partial field-mask updates the underlying service also supports.
        val FULL_UPDATE_PATHS = listOf("user.email", "user.first_name", "user.last_name", "user.role", "user.status")
    }
}
