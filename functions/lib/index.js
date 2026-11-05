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
exports.onFriendRequest = exports.onBattleStarted = exports.onAuraEvent = void 0;
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
async function send(token, title, body) {
    try {
        await messaging.send({ token, notification: { title, body } });
    }
    catch (err) {
        functions.logger.warn("FCM send failed", { token, err });
    }
}
// ── 1. Événement d'aura ───────────────────────────────────────────────────────
// Déclenché quand un ami signale un event sur `users/{uid}/events/{eventId}`.
// Notifie le destinataire avec le détail de l'event.
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
    const detail = description || label;
    await send(token, `${sign}${delta} aura de ${authorName || "Anonyme"}`, detail || "Consulte ton profil pour voir l'event.");
});
// ── 2. Battle lancée ──────────────────────────────────────────────────────────
// Déclenché quand une battle est créée dans `battles/{battleId}`.
// Notifie tous les amis de l'hôte.
exports.onBattleStarted = functions.firestore.onDocumentCreated("battles/{battleId}", async (event) => {
    const data = event.data?.data();
    if (!data)
        return;
    const { hostUid, hostName } = data;
    if (!hostUid)
        return;
    // Récupère les amis de l'hôte
    const friendsSnap = await db
        .collection("users")
        .doc(hostUid)
        .collection("friends")
        .get();
    const title = `⚔️ ${hostName || "Un ami"} est en battle !`;
    const body = "Rejoins la hype et tape pour faire exploser son score 🔥";
    await Promise.all(friendsSnap.docs.map(async (doc) => {
        const token = await fcmToken(doc.id);
        if (token)
            await send(token, title, body);
    }));
});
// ── 3. Demande d'ami ──────────────────────────────────────────────────────────
// Déclenché quand une demande est créée dans `friendRequests/{requestId}`.
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
    await send(token, `${fromName || "Quelqu'un"} veut t'ajouter`, "Ouvre Aura pour accepter ou refuser la demande.");
});
//# sourceMappingURL=index.js.map