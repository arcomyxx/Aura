package com.dubois.aura.model

import kotlinx.serialization.Serializable

/**
 * Groupe de classement. Stocké dans la collection `groups/{groupId}`.
 * Les membres sont identifiés par [memberUids] ; le créateur est toujours membre.
 */
@Serializable
data class Group(
    val id: String = "",
    val name: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val memberUids: List<String> = emptyList(),
)
