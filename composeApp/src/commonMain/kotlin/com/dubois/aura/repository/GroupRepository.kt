package com.dubois.aura.repository

import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.EventType
import com.dubois.aura.model.Group
import com.dubois.aura.model.GroupInvite
import com.dubois.aura.model.GroupLink
import com.dubois.aura.model.GroupMember
import com.dubois.aura.model.UserProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import kotlin.time.Clock

class GroupRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private fun myUid(): String? = auth.currentUser?.uid

    private fun membersCol(groupId: String) =
        firestore.collection("groups").document(groupId).collection("members")

    private fun groupEventsCol(groupId: String) =
        firestore.collection("groups").document(groupId).collection("events")

    /** Crée un nouveau groupe dont l'utilisateur courant est le seul membre initial. */
    suspend fun createGroup(name: String): Result<Group> {
        return try {
            val uid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            val me = firestore.collection("users").document(uid)
                .get().data(UserProfile.serializer())
            val docRef = firestore.collection("groups").document
            val now = Clock.System.now().toEpochMilliseconds()
            val group = Group(
                id = docRef.id,
                name = name.trim(),
                createdBy = uid,
                createdAt = now,
                memberUids = listOf(uid),
            )
            docRef.set(Group.serializer(), group)
            // Initialise le score de groupe du créateur à 0.
            membersCol(group.id).document(uid).set(
                GroupMember.serializer(),
                GroupMember(uid = uid, displayName = me.displayName, photoUrl = me.photoUrl),
            )
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Groupes dont l'utilisateur courant est membre. */
    suspend fun myGroups(): Result<List<Group>> {
        return try {
            val uid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            val groups = firestore.collection("groups")
                .where { "memberUids" contains uid }
                .get().documents
                .map { it.data(Group.serializer()) }
                .sortedBy { it.name }
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Invite [target] dans le groupe [group]. */
    suspend fun inviteToGroup(group: Group, target: UserProfile): Result<Unit> {
        return try {
            val uid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            if (target.uid in group.memberUids) {
                return Result.failure(IllegalStateException("${target.displayName} est déjà membre"))
            }
            val me = firestore.collection("users").document(uid)
                .get().data(UserProfile.serializer())
            val docRef = firestore.collection("groupInvites").document
            val now = Clock.System.now().toEpochMilliseconds()
            val invite = GroupInvite(
                id = docRef.id,
                groupId = group.id,
                groupName = group.name,
                fromUid = uid,
                fromName = me.displayName.ifBlank { me.email },
                toUid = target.uid,
                status = "pending",
                createdAt = now,
            )
            docRef.set(GroupInvite.serializer(), invite)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Invitations de groupe reçues et en attente. */
    suspend fun incomingInvites(): Result<List<GroupInvite>> {
        return try {
            val uid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            val invites = firestore.collection("groupInvites")
                .where { "toUid" equalTo uid }
                .get().documents
                .map { it.data(GroupInvite.serializer()) }
                .filter { it.status == "pending" }
                .sortedByDescending { it.createdAt }
            Result.success(invites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Accepte une invitation : ajoute l'utilisateur au groupe et initialise son score de groupe. */
    suspend fun acceptInvite(invite: GroupInvite): Result<Unit> {
        return try {
            val uid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            val me = firestore.collection("users").document(uid)
                .get().data(UserProfile.serializer())

            firestore.collection("groupInvites").document(invite.id)
                .update(mapOf("status" to "accepted"))

            val groupRef = firestore.collection("groups").document(invite.groupId)
            val group = groupRef.get().data(Group.serializer())
            val updated = (group.memberUids + uid).distinct()
            groupRef.update(mapOf("memberUids" to updated))

            // Crée le doc membre avec score de groupe à 0.
            membersCol(invite.groupId).document(uid).set(
                GroupMember.serializer(),
                GroupMember(uid = uid, displayName = me.displayName, photoUrl = me.photoUrl),
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Refuse une invitation. */
    suspend fun declineInvite(invite: GroupInvite): Result<Unit> {
        return try {
            firestore.collection("groupInvites").document(invite.id)
                .update(mapOf("status" to "declined"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Quitte le groupe [group] (le créateur ne peut pas quitter). */
    suspend fun leaveGroup(group: Group): Result<Unit> {
        return try {
            val uid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            if (group.createdBy == uid) {
                return Result.failure(IllegalStateException("Le créateur ne peut pas quitter le groupe"))
            }
            val updated = group.memberUids.filter { it != uid }
            firestore.collection("groups").document(group.id)
                .update(mapOf("memberUids" to updated))
            membersCol(group.id).document(uid).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Supprime le groupe (créateur uniquement). */
    suspend fun deleteGroup(group: Group): Result<Unit> {
        return try {
            val uid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            if (group.createdBy != uid) {
                return Result.failure(IllegalStateException("Seul le créateur peut supprimer le groupe"))
            }
            firestore.collection("groups").document(group.id).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Migration : crée les docs membres manquants pour un groupe existant.
     * Appelé la première fois qu'on ouvre un groupe créé avant la feature.
     */
    suspend fun initMissingMembers(group: Group) {
        val existing = try {
            membersCol(group.id).get().documents.map { it.id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
        group.memberUids.filter { it !in existing }.forEach { uid ->
            try {
                val profile = firestore.collection("users").document(uid)
                    .get().data(UserProfile.serializer())
                membersCol(group.id).document(uid).set(
                    GroupMember.serializer(),
                    GroupMember(uid = uid, displayName = profile.displayName, photoUrl = profile.photoUrl),
                )
            } catch (_: Exception) {}
        }
    }

    /** Classement des membres triés par score de groupe décroissant. */
    suspend fun groupLeaderboard(groupId: String): Result<List<GroupMember>> {
        return try {
            val members = membersCol(groupId)
                .get().documents
                .map { it.data(GroupMember.serializer()) }
                .sortedByDescending { it.groupAuraScore }
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signale un événement dans le contexte d'un groupe.
     * Crée l'événement dans `groups/{groupId}/events` et incrémente
     * le score de groupe de la cible (sans toucher au score global).
     */
    suspend fun reportGroupEvent(
        groupId: String,
        targetUid: String,
        type: EventType,
        description: String,
    ): Result<Unit> {
        return try {
            val uid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            if (targetUid == uid) return Result.failure(IllegalStateException("Impossible de se signaler soi-même"))

            val me = firestore.collection("users").document(uid)
                .get().data(UserProfile.serializer())

            val now = Clock.System.now().toEpochMilliseconds()
            val docRef = groupEventsCol(groupId).document
            val event = AuraEvent(
                id = docRef.id,
                targetUid = targetUid,
                authorUid = uid,
                authorName = me.displayName,
                typeId = type.id,
                emoji = type.emoji,
                label = type.label,
                delta = type.delta,
                description = description.trim(),
                createdAt = now,
            )
            docRef.set(AuraEvent.serializer(), event)

            // Incrémente uniquement le score de groupe de la cible.
            membersCol(groupId).document(targetUid)
                .update(mapOf("groupAuraScore" to FieldValue.increment(type.delta)))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Applique [delta] au score de groupe de [targetUid] dans tous les groupes
     * dont les deux utilisateurs sont membres. Appelé après un signalement global.
     */
    suspend fun applyDeltaInCommonGroups(myUid: String, targetUid: String, delta: Int) {
        try {
            val groups = firestore.collection("groups")
                .where { "memberUids" contains myUid }
                .get().documents
                .map { it.data(Group.serializer()) }
                .filter { targetUid in it.memberUids }
            groups.forEach { group ->
                try {
                    membersCol(group.id).document(targetUid)
                        .update(mapOf("groupAuraScore" to FieldValue.increment(delta)))
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    /** Génère un token d'invitation partageable pour [group] et le stocke dans Firestore. */
    suspend fun generateInviteLink(group: Group): Result<String> {
        return try {
            val uid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            val bytes = ByteArray(15)
            java.security.SecureRandom().nextBytes(bytes)
            val token = bytes.joinToString("") { "%02x".format(it) }
            val now = Clock.System.now().toEpochMilliseconds()
            val link = GroupLink(token = token, groupId = group.id, groupName = group.name, createdBy = uid, createdAt = now)
            firestore.collection("groupLinks").document(token).set(GroupLink.serializer(), link)
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Rejoint le groupe associé au [token] d'invitation. */
    suspend fun joinByToken(token: String): Result<Group> {
        return try {
            val uid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            val linkDoc = firestore.collection("groupLinks").document(token).get()
            if (!linkDoc.exists) return Result.failure(IllegalArgumentException("Lien invalide ou expiré"))
            val link = linkDoc.data(GroupLink.serializer())

            val me = firestore.collection("users").document(uid).get().data(UserProfile.serializer())
            val groupRef = firestore.collection("groups").document(link.groupId)
            val group = groupRef.get().data(Group.serializer())

            if (uid !in group.memberUids) {
                groupRef.update(mapOf("memberUids" to (group.memberUids + uid).distinct()))
                membersCol(link.groupId).document(uid).set(
                    GroupMember.serializer(),
                    GroupMember(uid = uid, displayName = me.displayName, photoUrl = me.photoUrl),
                )
            }
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Événements récents du groupe (les plus récents d'abord). */
    suspend fun groupEvents(groupId: String, targetUid: String, limit: Int = 20): List<AuraEvent> {
        return try {
            groupEventsCol(groupId)
                .where { "targetUid" equalTo targetUid }
                .get().documents
                .map { it.data(AuraEvent.serializer()) }
                .sortedByDescending { it.createdAt }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
