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

async function send(
  token: string,
  title: string,
  body: string,
  data?: Record<string, string>,
): Promise<void> {
  try {
    await messaging.send({
      token,
      notification: { title, body },
      android: { notification: { channelId: "aura_default", sound: "default" } },
      apns: { payload: { aps: { sound: "default" } } },
      ...(data ? { data } : {}),
    });
  } catch (err) {
    functions.logger.warn("FCM send failed", { token, err });
  }
}

/** Envoie à une liste de tokens (dédupliqués, max 500 par appel FCM). */
async function sendMulti(
  uids: string[],
  title: string,
  body: string,
  data?: Record<string, string>,
): Promise<void> {
  if (uids.length === 0) return;
  const tokens = (
    await Promise.all(uids.map((uid) => fcmToken(uid)))
  ).filter((t): t is string => t !== null);
  if (tokens.length === 0) return;

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
    const detail = description || label || "Consulte ton profil.";
    await send(
      token,
      `${sign}${delta} aura · ${authorName || "Anonyme"}`,
      detail,
      { screen: "profile" },
    );
  },
);

// ── 2. Battle lancée ──────────────────────────────────────────────────────────
// Déclenché à la création d'une battle dans `battles/{battleId}`.
// Notifie tous les amis de l'hôte.

export const onBattleStarted = functions.firestore.onDocumentCreated(
  "battles/{battleId}",
  async (event) => {
    const battleId = event.params.battleId;
    const data = event.data?.data();
    if (!data) return;

    const { hostUid, hostName } = data as { hostUid: string; hostName: string };
    if (!hostUid) return;

    const friendsSnap = await db
      .collection("users")
      .doc(hostUid)
      .collection("friends")
      .get();
    if (friendsSnap.empty) return;

    const friendUids = friendsSnap.docs.map((d) => d.id);
    await sendMulti(
      friendUids,
      `⚔️ ${hostName || "Un ami"} est en battle !`,
      "Tape pour faire exploser son score 🔥",
      { screen: "battle", battleId },
    );
  },
);

// ── 3. Battle terminée ────────────────────────────────────────────────────────
// Déclenché quand le statut d'une battle passe de "live" à "ended".
// Notifie l'hôte de son gain d'aura.

export const onBattleEnded = functions.firestore.onDocumentUpdated(
  "battles/{battleId}",
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;
    if (before.status !== "live" || after.status !== "ended") return;

    const { hostUid, hostName, auraAwarded, score } = after as {
      hostUid: string;
      hostName: string;
      auraAwarded: number;
      score: number;
    };
    if (!hostUid) return;

    const token = await fcmToken(hostUid);
    if (!token) return;

    const title =
      auraAwarded > 0
        ? `+${auraAwarded} aura 🏆 Belle battle, ${hostName || "champion"} !`
        : `Battle terminée — score final : ${score} pts`;
    const body =
      auraAwarded > 0
        ? `Tu as encaissé ${auraAwarded} points d'aura grâce à ta battle.`
        : "Pas d'aura cette fois, mais tu peux relancer une battle !";

    await send(token, title, body, { screen: "profile" });
  },
);

// ── 4. Récompense supporter ───────────────────────────────────────────────────
// Déclenché quand `auraEarned` apparaît sur un document supporter.
// Notifie le supporter de son gain d'aura.

export const onSupporterAwarded = functions.firestore.onDocumentUpdated(
  "battles/{battleId}/supporters/{uid}",
  async (event) => {
    const uid = event.params.uid;
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;
    // Déclencher uniquement quand auraEarned vient d'être crédité
    if ((before.auraEarned ?? 0) > 0 || (after.auraEarned ?? 0) <= 0) return;

    const aura: number = after.auraEarned;
    const token = await fcmToken(uid);
    if (!token) return;

    await send(
      token,
      `+${aura} aura 🔥 Merci pour le soutien !`,
      "Ta hype a été récompensée. Continue comme ça !",
      { screen: "profile" },
    );
  },
);

// ── 5. Demande d'ami reçue ────────────────────────────────────────────────────
// Déclenché à la création d'un document dans `friendRequests/{requestId}`.
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
      { screen: "friends" },
    );
  },
);

// ── 6. Demande d'ami acceptée ─────────────────────────────────────────────────
// Déclenché quand le statut d'une demande passe à "accepted".
// Notifie l'envoyeur d'origine.

export const onFriendRequestAccepted = functions.firestore.onDocumentUpdated(
  "friendRequests/{requestId}",
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;
    if (before.status === "accepted" || after.status !== "accepted") return;

    const { fromUid, toName } = after as { fromUid: string; toName: string };
    if (!fromUid) return;

    const token = await fcmToken(fromUid);
    if (!token) return;

    await send(
      token,
      `${toName || "Quelqu'un"} a accepté ta demande ✦`,
      "Vous êtes maintenant amis sur Aura !",
      { screen: "friends" },
    );
  },
);

// ── Signalement d'événement (callable) ───────────────────────────────────────
// Remplace la logique côté client : valide les limites anti-abus côté serveur,
// écrit l'événement et incrémente les scores de manière atomique.

const MAX_POSITIVES_PER_DAY = 3;
const MAX_NEGATIVES_PER_DAY = 1;
const MAX_REWARD_PER_DAY = 10;
const MAX_LOSS_PER_DAY = 50;
const REPORTER_REWARD = 2;

const RARITY_COOLDOWN_MS: Record<string, number> = {
  COMMON: 12 * 3_600_000,
  RARE: 24 * 3_600_000,
  LEGENDARY: 168 * 3_600_000,
};

function todayUtc(): string {
  const d = new Date();
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, "0");
  const day = String(d.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export const reportEvent = functions.https.onCall(async (request) => {
  const auth = request.auth;
  if (!auth) throw new functions.https.HttpsError("unauthenticated", "Non authentifié");

  const { targetUid, typeId, emoji, label, delta, description, rarity } = request.data as {
    targetUid: string;
    typeId: string;
    emoji: string;
    label: string;
    delta: number;
    description: string;
    rarity: string;
  };

  const myUid = auth.uid;
  if (!targetUid || targetUid === myUid)
    throw new functions.https.HttpsError("invalid-argument", "Cible invalide");

  const isPositive = delta >= 0;
  const now = Date.now();
  const todayStr = todayUtc();

  // ── Lecture atomique des compteurs (transaction) ──────────────────────────
  const myLimitsRef = db.collection("users").doc(myUid).collection("limits").doc("daily");
  const targetLimitsRef = db.collection("users").doc(targetUid).collection("limits").doc("daily");
  const rarityRef = db.collection("users").doc(myUid).collection("limits")
    .doc(`rarity_${targetUid}_${typeId}`);

  let effectiveDelta: number;
  let reward: number;

  await db.runTransaction(async (tx) => {
    const [mySnap, targetSnap, raritySnap] = await Promise.all([
      tx.get(myLimitsRef),
      tx.get(targetLimitsRef),
      tx.get(rarityRef),
    ]);

    const myLimits = mySnap.exists
      ? (mySnap.data() as { date?: string; positivesSent?: number; negativesSent?: number; rewardEarned?: number })
      : {};
    const myDate = myLimits.date ?? "";
    const positiveSent = myDate === todayStr ? (myLimits.positivesSent ?? 0) : 0;
    const negativeSent = myDate === todayStr ? (myLimits.negativesSent ?? 0) : 0;
    const rewardEarned = myDate === todayStr ? (myLimits.rewardEarned ?? 0) : 0;

    if (isPositive && positiveSent >= MAX_POSITIVES_PER_DAY)
      throw new functions.https.HttpsError("resource-exhausted",
        `Limite de ${MAX_POSITIVES_PER_DAY} signalements positifs atteinte`);
    if (!isPositive && negativeSent >= MAX_NEGATIVES_PER_DAY)
      throw new functions.https.HttpsError("resource-exhausted",
        "Limite de 1 signalement négatif atteinte");

    const cooldown = RARITY_COOLDOWN_MS[rarity] ?? 0;
    const lastMs = raritySnap.exists ? (raritySnap.data()?.lastMs ?? 0) : 0;
    if (now - lastMs < cooldown)
      throw new functions.https.HttpsError("resource-exhausted", "Cooldown de rareté non écoulé");

    const targetData = targetSnap.exists ? (targetSnap.data() as { date?: string; receivedLoss?: number }) : {};
    const targetDate = targetData.date ?? "";
    const receivedLoss = targetDate === todayStr ? (targetData.receivedLoss ?? 0) : 0;

    effectiveDelta = isPositive
      ? delta
      : Math.max(delta, -(MAX_LOSS_PER_DAY - receivedLoss));

    reward = isPositive
      ? Math.min(REPORTER_REWARD, Math.max(0, MAX_REWARD_PER_DAY - rewardEarned))
      : 0;

    // Écriture de l'événement
    const eventRef = db.collection("users").doc(targetUid).collection("events").doc();
    tx.set(eventRef, {
      id: eventRef.id,
      targetUid,
      authorUid: myUid,
      authorName: (await db.collection("users").doc(myUid).get()).data()?.displayName ?? "",
      typeId,
      emoji,
      label,
      delta: effectiveDelta,
      description: (description ?? "").trim(),
      createdAt: now,
    });

    // Incréments de score
    if (effectiveDelta !== 0)
      tx.update(db.collection("users").doc(targetUid), {
        auraScore: admin.firestore.FieldValue.increment(effectiveDelta),
      });
    if (reward > 0)
      tx.update(db.collection("users").doc(myUid), {
        auraScore: admin.firestore.FieldValue.increment(reward),
      });

    // Mise à jour des compteurs
    tx.set(myLimitsRef, {
      date: todayStr,
      positivesSent: positiveSent + (isPositive ? 1 : 0),
      negativesSent: negativeSent + (!isPositive ? 1 : 0),
      rewardEarned: rewardEarned + reward,
    });
    if (!isPositive && effectiveDelta !== 0)
      tx.set(targetLimitsRef, {
        date: todayStr,
        receivedLoss: receivedLoss + (-effectiveDelta),
      }, { merge: true });
    tx.set(rarityRef, { lastMs: now });
  });

  return { effectiveDelta: effectiveDelta!, reward: reward! };
});

// ── 7. Invitation de groupe ───────────────────────────────────────────────────
// Déclenché à la création d'un document dans `groupInvites/{inviteId}`.
// Notifie le destinataire.

export const onGroupInvite = functions.firestore.onDocumentCreated(
  "groupInvites/{inviteId}",
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const { fromName, toUid, groupName } = data as {
      fromName: string;
      toUid: string;
      groupName: string;
    };
    if (!toUid) return;

    const token = await fcmToken(toUid);
    if (!token) return;

    const group = groupName ? `« ${groupName} »` : "un groupe";
    await send(
      token,
      `${fromName || "Un ami"} t'invite dans ${group} ⬡`,
      "Rejoins le groupe pour comparer vos auras !",
      { screen: "groups" },
    );
  },
);
