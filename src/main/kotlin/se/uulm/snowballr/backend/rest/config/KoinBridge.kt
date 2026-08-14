package se.uulm.snowballr.backend.rest.config

import org.koin.java.KoinJavaComponent.getKoin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import se.uulm.snowballr.backend.auth.IAuthenticationManager
import se.uulm.snowballr.backend.auth.ICookieManager
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.service.IAuthenticationService
import se.uulm.snowballr.backend.service.ICriterionService
import se.uulm.snowballr.backend.service.IExportService
import se.uulm.snowballr.backend.service.IInvitationService
import se.uulm.snowballr.backend.service.IProjectMemberService
import se.uulm.snowballr.backend.service.IProjectPaperService
import se.uulm.snowballr.backend.service.IProjectService
import se.uulm.snowballr.backend.service.IReadingListService
import se.uulm.snowballr.backend.service.IReviewService
import se.uulm.snowballr.backend.service.IUserService

/**
 * Conversion layer from Koin to Beans.
 */
@Configuration
class KoinBridge {
    @Bean
    fun projectService(): IProjectService = getKoin().get()

    @Bean
    fun authService(): IAuthenticationService = getKoin().get()

    @Bean
    fun authenticationManager(): IAuthenticationManager = getKoin().get()

    @Bean
    fun cookieManager(): ICookieManager = getKoin().get()

    @Bean
    fun envReader(): EnvReader = getKoin().get()

    @Bean
    fun userService(): IUserService = getKoin().get()

    @Bean
    fun invitationService(): IInvitationService = getKoin().get()

    @Bean
    fun exportService(): IExportService = getKoin().get()

    @Bean
    fun projectMemberService(): IProjectMemberService = getKoin().get()

    @Bean
    fun readingListService(): IReadingListService = getKoin().get()

    @Bean
    fun projectPaperService(): IProjectPaperService = getKoin().get()

    @Bean
    fun reviewService(): IReviewService = getKoin().get()

    @Bean
    fun criterionService(): ICriterionService = getKoin().get()
}
