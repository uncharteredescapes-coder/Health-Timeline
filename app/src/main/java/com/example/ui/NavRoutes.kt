package com.example.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute {
    @Serializable
    data object Onboarding : NavRoute
    @Serializable
    data object Home : NavRoute
    @Serializable
    data object Capture : NavRoute
    @Serializable
    data class Extraction(val documentId: String) : NavRoute
    @Serializable
    data class ConditionDetail(val conditionId: String) : NavRoute
    @Serializable
    data object Medications : NavRoute
    @Serializable
    data object Timeline : NavRoute
    @Serializable
    data object Summary : NavRoute
    @Serializable
    data object Profile : NavRoute
    @Serializable
    data class ReportViewer(val documentId: String) : NavRoute
}
