package se.uulm.snowballr.backend.rest.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.model.auth.AuthenticationStatus
import se.uulm.snowballr.backend.model.incoming.authentication.ChangePasswordRequest
import se.uulm.snowballr.backend.model.incoming.authentication.LoginRequest
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IAuthenticationService

@RestController
@RequestMapping(Routes.AUTH_ROUTE)
class AuthController(private val authService: IAuthenticationService) {
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest) = onRequest { authService.login(request) }

    @PostMapping("/logout")
    fun logout() = onRequest { authService.logout() }

    @GetMapping("/status")
    fun getAuthStatus(): AuthenticationStatus = RequestContext.current().authStatus

    @PostMapping("/verify-email")
    fun verifyEmail(token: String) = onRequest { authService.verifyEmail(token) }

    @PostMapping("/change-password")
    fun changePassword(@RequestBody request: ChangePasswordRequest) = onRequest { authService.changePassword(request) }
}
