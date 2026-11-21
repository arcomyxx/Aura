"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.onGroupInvite = exports.onFriendRequestAccepted = exports.onFriendRequest = exports.onSupporterAwarded = exports.onBattleEnded = exports.onBattleStarted = exports.onAuraEvent = void 0;
const admin = __importStar(require("firebase-admin"));
const functions = __importStar(require("firebase-functions/v2"));
admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();
// ── Helpers ────────────────────────────────────────────────────────────────────
async function fcmToken(uid) {
    const doc = await db.collection("users").doc(uid).get();
    return doc.exists ? (doc.data()?.fcmToken ?? null) : null;
}
async function send(token, title, body, data) {
    try {
        await messaging.send({
            token,
            notification: { title, body },
            android: { notification: { channelId: "aura_default", sound: "default" } },
            apns: { payload: { aps: { sound: "default" } } },
            ...(data ? { data } : {}),
        });
    }
    catch (err) {
        functions.logger.warn("FCM send failed", { token, err });
    }
}
/** Envoie à une liste de tokens (dédupliqués, max 500 par appel FCM). */
async function sendMulti(uids, title, body, data) {
    if (uids.length === 0)
        return;
    const tokens = (await Promise.all(uids.map((uid) => fcmToken(uid)))).filter((t) => t !== null);
    if (tokens.length === 0)
        return;
    const unique = [...new Set(tokens)];
    // sendEachForMulticast accepte max 500 tokens
    for (let i = 0; i < unique.length; i += 500) {
        const batch = unique.slice(i, i + 500);
        const res = await messaging.sendEachForMulticast({
            tokens: batch,
            notification: { title, body },
            android: { notification: { channelId: "aura_default", sound: "default" } },
            apns: { payload: { aps: { sound: "default" } } },
            ...(data ? { data } : {}),
        });
        res.responses.forEach((r, idx) => {
            if (!r.success)
                functions.logger.warn("FCM multicast partial failure", {
                    token: batch[idx],
                    error: r.error,
                });
        });
    }
}
// ── 1. Événement d'aura ───────────────────────────────────────────────────────
// Déclenché quand un ami signale un événement sur `users/{uid}/events/{eventId}`.
// Notifie le destinataire avec le détail.
exports.onAuraEvent = functions.firestore.onDocumentCreated("users/{uid}/events/{eventId}", async (event) => {
    const uid = event.params.uid;
    const data = event.data?.data();
    if (!data)
        return;
    const { authorName, delta, description, label } = data;
    const token = await fcmToken(uid);
    if (!token)
        return;
    const sign = delta >= 0 ? "+" : "";
    const detail = description || label || "Consulte ton profil.";
    await send(token, `${sign}${delta} aura · ${authorName || "Anonyme"}`, detail, { screen: "profile" });
});
// ── 2. Battle lancée ──────────────────────────────────────────────────────────
// Déclenché à la création d'une battle dans `battles/{battleId}`.
// Notifie tous les amis de l'hôte.
exports.onBattleStarted = functions.firestore.onDocumentCreated("battles/{battleId}", async (event) => {
    const battleId = event.params.battleId;
    const data = event.data?.data();
    if (!data)
        return;
    const { hostUid, hostName } = data;
    if (!hostUid)
        return;
    const friendsSnap = await db
        .collection("users")
        .doc(hostUid)
        .collection("friends")
        .get();
    if (friendsSnap.empty)
        return;
    const friendUids = friendsSnap.docs.map((d) => d.id);
    await sendMulti(friendUids, `⚔️ ${hostName || "Un ami"} est en battle !`, "Tape pour faire exploser son score 🔥", { screen: "battle", battleId });
});
// ── 3. Battle terminée ────────────────────────────────────────────────────────
// Déclenché quand le statut d'une battle passe de "live" à "ended".
// Notifie l'hôte de son gain d'aura.
exports.onBattleEnded = functions.firestore.onDocumentUpdated("battles/{battleId}", async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after)
        return;
    if (before.status !== "live" || after.status !== "ended")
        return;
    const { hostUid, hostName, auraAwarded, score } = after;
    if (!hostUid)
        return;
    const token = await fcmToken(hostUid);
    if (!token)
        return;
    const title = auraAwarded > 0
        ? `+${auraAwarded} aura 🏆 Belle battle, ${hostName || "champion"} !`
        : `Battle terminée — score final : ${score} pts`;
    const body = auraAwarded > 0
        ? `Tu as encaissé ${auraAwarded} points d'aura grâce à ta battle.`
        : "Pas d'aura cette fois, mais tu peux relancer une battle !";
    await send(token, title, body, { screen: "profile" });
});
// ── 4. Récompense supporter ───────────────────────────────────────────────────
// Déclenché quand `auraEarned` apparaît sur un document supporter.
// Notifie le supporter de son gain d'aura.
exports.onSupporterAwarded = functions.firestore.onDocumentUpdated("battles/{battleId}/supporters/{uid}", async (event) => {
    const uid = event.params.uid;
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after)
        return;
    // Déclencher uniquement quand auraEarned vient d'être crédité
    if ((before.auraEarned ?? 0) > 0 || (after.auraEarned ?? 0) <= 0)
        return;
    const aura = after.auraEarned;
    const token = await fcmToken(uid);
    if (!token)
        return;
    await send(token, `+${aura} aura 🔥 Merci pour le soutien !`, "Ta hype a été récompensée. Continue comme ça !", { screen: "profile" });
});
// ── 5. Demande d'ami reçue ────────────────────────────────────────────────────
// Déclenché à la création d'un document dans `friendRequests/{requestId}`.
// Notifie le destinataire.
exports.onFriendRequest = functions.firestore.onDocumentCreated("friendRequests/{requestId}", async (event) => {
    const data = event.data?.data();
    if (!data)
        return;
    const { fromName, toUid } = data;
    if (!toUid)
        return;
    const token = await fcmToken(toUid);
    if (!token)
        return;
    await send(token, `${fromName || "Quelqu'un"} veut t'ajouter`, "Ouvre Aura pour accepter ou refuser la demande.", { screen: "friends" });
});
// ── 6. Demande d'ami acceptée ─────────────────────────────────────────────────
// Déclenché quand le statut d'une demande passe à "accepted".
// Notifie l'envoyeur d'origine.
exports.onFriendRequestAccepted = functions.firestore.onDocumentUpdated("friendRequests/{requestId}", async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after)
        return;
    if (before.status === "accepted" || after.status !== "accepted")
        return;
    const { fromUid, toName } = after;
    if (!fromUid)
        return;
    const token = await fcmToken(fromUid);
    if (!token)
        return;
    await send(token, `${toName || "Quelqu'un"} a accepté ta demande ✦`, "Vous êtes maintenant amis sur Aura !", { screen: "friends" });
});
// ── 7. Invitation de groupe ───────────────────────────────────────────────────
// Déclenché à la création d'un document dans `groupInvites/{inviteId}`.
// Notifie le destinataire.
exports.onGroupInvite = functions.firestore.onDocumentCreated("groupInvites/{inviteId}", async (event) => {
    const data = event.data?.data();
    if (!data)
        return;
    const { fromName, toUid, groupName } = data;
    if (!toUid)
        return;
    const token = await fcmToken(toUid);
    if (!token)
        return;
    const group = groupName ? `« ${groupName} »` : "un groupe";
    await send(token, `${fromName || "Un ami"} t'invite dans ${group} ⬡`, "Rejoins le groupe pour comparer vos auras !", { screen: "groups" });
});
//# sourceMappingURL=index.js.map