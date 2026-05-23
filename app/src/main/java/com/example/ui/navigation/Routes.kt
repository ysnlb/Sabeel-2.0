package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object AdhanTimes : Route

    @Serializable
    data object Adhkar : Route

    @Serializable
    data object Settings : Route
}
