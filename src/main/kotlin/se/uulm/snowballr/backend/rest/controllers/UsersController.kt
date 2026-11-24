package se.uulm.snowballr.backend.rest.controllers

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.incoming.user.RegisterRequest
import se.uulm.snowballr.backend.rest.REST_API_ENABLED_PROPERTY
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IUserService

@RestController
@ConditionalOnBooleanProperty(REST_API_ENABLED_PROPERTY)
@RequestMapping(Routes.USERS_ROUTE)
class UsersController(private val userService: IUserService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest) = onRequest { userService.register(request) }
}
