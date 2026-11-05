import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v2";

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

// ── Helpers ────────────────────────────────────────────────────────────────────

async function fcmToken(uid: string): Promise<string | null> {
  const doc = await db.collection("users").doc(uid).get();
  return doc.exists ? (doc.data()?.fcmToken ?? null) : null;
}

async function send(token: string, title: string, body: string): Promise<void> {
  try {
    await messaging.send({ token, notification: { title, body } });
  } catch (err) {
    functions.logger.warn("FCM send failed", { token, err });
  }
}

// ── 1. Événement d'aura ───────────────────────────────────────────────────────
// Déclenché quand un ami signale un event sur `users/{uid}/events/{eventId}`.
// Notifie le destinataire avec le détail de l'event.

export const onAuraEvent = functions.firestore.onDocumentCreated(
  "users/{uid}/events/{eventId}",
  async (event) => {
    const uid = event.params.uid;
    const data = event.data?.data();
    if (!data) return;

    const { authorName, delta, description, label } = data as {
      authorName: string;
      delta: number;
      description: string;
      label: string;
    };

    const token = await fcmToken(uid);
    if (!token) return;

    const sign = delta >= 0 ? "+" : "";
    const detail = description || label;
    await send(
      token,
      `${sign}${delta} aura de ${authorName || "Anonyme"}`,
      detail || "Consulte ton profil pour voir l'event.",
    );
  },
);

// ── 2. Battle lancée ──────────────────────────────────────────────────────────
// Déclenché quand une battle est créée dans `battles/{battleId}`.
// Notifie tous les amis de l'hôte.

export const onBattleStarted = functions.firestore.onDocumentCreated(
  "battles/{battleId}",
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const { hostUid, hostName } = data as { hostUid: string; hostName: string };
    if (!hostUid) return;

    // Récupère les amis de l'hôte
    const friendsSnap = await db
      .collection("users")
      .doc(hostUid)
      .collection("friends")
      .get();

    const title = `⚔️ ${hostName || "Un ami"} est en battle !`;
    const body = "Rejoins la hype et tape pour faire exploser son score 🔥";

    await Promise.all(
      friendsSnap.docs.map(async (doc) => {
        const token = await fcmToken(doc.id);
        if (token) await send(token, title, body);
      }),
    );
  },
);

// ── 3. Demande d'ami ──────────────────────────────────────────────────────────
// Déclenché quand une demande est créée dans `friendRequests/{requestId}`.
// Notifie le destinataire.

export const onFriendRequest = functions.firestore.onDocumentCreated(
  "friendRequests/{requestId}",
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const { fromName, toUid } = data as { fromName: string; toUid: string };
    if (!toUid) return;

    const token = await fcmToken(toUid);
    if (!token) return;

    await send(
      token,
      `${fromName || "Quelqu'un"} veut t'ajouter`,
      "Ouvre Aura pour accepter ou refuser la demande.",
    );
  },
);
