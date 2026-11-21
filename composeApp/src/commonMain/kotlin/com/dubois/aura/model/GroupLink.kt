package com.dubois.aura.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupLink(
    val token: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
)
