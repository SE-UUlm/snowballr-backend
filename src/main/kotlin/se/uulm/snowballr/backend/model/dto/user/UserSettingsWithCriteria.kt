package se.uulm.snowballr.backend.model.dto.user

import se.uulm.snowballr.backend.model.dto.criterion.Criterion

data class UserSettingsWithCriteria(
    val settings: UserSettings,
    val criteria: List<Criterion>,
)
