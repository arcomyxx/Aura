import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v2";
import * as functionsV1 from "firebase-functions/v1";

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

const MAX_POSITIVES_FREE = 3;
const MAX_POSITIVES_PREMIUM = 5;
const MAX_NEGATIVES_FREE = 1;
const MAX_NEGATIVES_PREMIUM = 2;
const MAX_FREE_GROUPS = 3;
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

  // Lire le statut premium de l'appelant (hors transaction pour éviter les lectures async dedans)
  const callerDoc = await db.collection("users").doc(myUid).get();
  const isPremium = callerDoc.data()?.isPremium ?? false;
  const maxPositives = isPremium ? MAX_POSITIVES_PREMIUM : MAX_POSITIVES_FREE;
  const maxNegatives = isPremium ? MAX_NEGATIVES_PREMIUM : MAX_NEGATIVES_FREE;

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

    if (isPositive && positiveSent >= maxPositives)
      throw new functions.https.HttpsError("resource-exhausted",
        `Limite de ${maxPositives} signalements positifs atteinte`);
    if (!isPositive && negativeSent >= maxNegatives)
      throw new functions.https.HttpsError("resource-exhausted",
        `Limite de ${maxNegatives} signalement(s) négatif(s) atteinte`);

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

// ── Webhook RevenueCat ────────────────────────────────────────────────────────
// Met à jour isPremium dans Firestore à chaque événement d'abonnement.
// Configurer l'URL dans le dashboard RevenueCat → Integrations → Webhooks.
// La clé secrète doit être stockée dans Firebase Secret Manager sous la clé
// "REVENUECAT_WEBHOOK_SECRET" et passée via runWith({ secrets: [...] }).

export const onRevenueCatEvent = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") { res.status(405).send("Method Not Allowed"); return; }

  const secret = process.env.REVENUECAT_WEBHOOK_SECRET;
  if (secret) {
    const authHeader = req.headers.authorization ?? "";
    if (authHeader !== secret) {
      functions.logger.warn("RevenueCat webhook: signature invalide");
      res.status(401).send("Unauthorized");
      return;
    }
  }

  const body = req.body as {
    event?: {
      type?: string;
      app_user_id?: string;
      expiration_at_ms?: number | null;
      entitlement_ids?: string[];
    };
  };

  const event = body.event;
  if (!event?.app_user_id) { res.status(400).send("Bad Request"); return; }

  const uid = event.app_user_id;
  const eventType = event.type ?? "";

  const ACTIVATE_EVENTS = ["INITIAL_PURCHASE", "RENEWAL", "UNCANCELLATION", "SUBSCRIPTION_PAUSED"];
  const DEACTIVATE_EVENTS = ["EXPIRATION", "CANCELLATION", "SUBSCRIBER_ALIAS"];

  if (ACTIVATE_EVENTS.includes(eventType)) {
    await db.collection("users").doc(uid).update({
      isPremium: true,
      premiumExpiresAt: event.expiration_at_ms ?? null,
      premiumEntitlementId: (event.entitlement_ids ?? ["premium"])[0],
    });
    functions.logger.info("Premium activé", { uid, eventType });
  } else if (DEACTIVATE_EVENTS.includes(eventType)) {
    await db.collection("users").doc(uid).update({
      isPremium: false,
      premiumExpiresAt: null,
      premiumEntitlementId: "",
    });
    functions.logger.info("Premium désactivé", { uid, eventType });
  }

  res.status(200).send("OK");
});

// ── Création de groupe (callable) ─────────────────────────────────────────────
// Vérifie la limite de groupes côté serveur (Free: 3, Premium: illimité).

export const createGroup = functions.https.onCall(async (request) => {
  const auth = request.auth;
  if (!auth) throw new functions.https.HttpsError("unauthenticated", "Non authentifié");

  const { name } = request.data as { name: string };
  if (!name?.trim()) throw new functions.https.HttpsError("invalid-argument", "Nom invalide");

  const uid = auth.uid;
  const userDoc = await db.collection("users").doc(uid).get();
  const isPremium = userDoc.data()?.isPremium ?? false;

  if (!isPremium) {
    const groupsSnap = await db.collection("groups")
      .where("memberUids", "array-contains", uid)
      .count()
      .get();
    if (groupsSnap.data().count >= MAX_FREE_GROUPS)
      throw new functions.https.HttpsError(
        "resource-exhausted",
        `Limite de ${MAX_FREE_GROUPS} groupes atteinte. Passe à Premium pour des groupes illimités.`,
      );
  }

  const docRef = db.collection("groups").doc();
  const now = Date.now();
  const displayName = userDoc.data()?.displayName ?? "";
  const photoUrl = userDoc.data()?.photoUrl ?? "";

  await db.runTransaction(async (tx) => {
    tx.set(docRef, {
      id: docRef.id,
      name: name.trim(),
      createdBy: uid,
      createdAt: now,
      memberUids: [uid],
    });
    tx.set(
      db.collection("groups").doc(docRef.id).collection("members").doc(uid),
      { uid, displayName, photoUrl, groupAuraScore: 0 },
    );
  });

  return { groupId: docRef.id };
});

// ── 8. Suppression de compte ──────────────────────────────────────────────────
// Déclenché automatiquement quand un compte Firebase Auth est supprimé.
// Efface toutes les données personnelles de l'utilisateur (RGPD + exigence Apple).
//
// Données supprimées :
//   - users/{uid} + sous-collections (events, friends, limits)
//   - friendRequests impliquant cet uid
//   - groupInvites impliquant cet uid
//   - groupLinks créés par cet uid
//   - présence dans chaque groupe (membres + tableau memberUids)
//   - groupes dont il était le seul membre (suppression complète)
//   - battles hébergées par cet uid

export const onDeleteUser = functionsV1.auth.user().onDelete(async (user) => {
  const uid = user.uid;
  functions.logger.info("Suppression du compte, nettoyage données", { uid });

  // 1. Profil + sous-collections (events, friends, limits) via recursiveDelete.
  await db.recursiveDelete(db.collection("users").doc(uid));

  // 2. Demandes d'ami envoyées ou reçues.
  await deleteWhere("friendRequests", "fromUid", uid);
  await deleteWhere("friendRequests", "toUid", uid);

  // 3. Invitations de groupe envoyées ou reçues.
  await deleteWhere("groupInvites", "fromUid", uid);
  await deleteWhere("groupInvites", "toUid", uid);

  // 4. Liens d'invitation créés par cet utilisateur.
  await deleteWhere("groupLinks", "createdBy", uid);

  // 5. Battles hébergées.
  await deleteWhere("battles", "hostUid", uid);

  // 6. Groupes : retirer l'uid du tableau memberUids ; supprimer si dernier membre.
  const groupsSnap = await db.collection("groups")
    .where("memberUids", "array-contains", uid)
    .get();

  await Promise.all(groupsSnap.docs.map(async (doc) => {
    const data = doc.data();
    const remaining: string[] = (data.memberUids ?? []).filter((u: string) => u !== uid);

    if (remaining.length === 0) {
      await db.recursiveDelete(doc.ref);
    } else {
      const batch = db.batch();
      batch.update(doc.ref, { memberUids: remaining });
      batch.delete(doc.ref.collection("members").doc(uid));
      await batch.commit();
    }
  }));
});

/** Supprime tous les docs d'une collection dont [field] == [value]. */
async function deleteWhere(
  collection: string,
  field: string,
  value: string,
): Promise<void> {
  const snap = await db.collection(collection).where(field, "==", value).get();
  if (snap.empty) return;
  const batch = db.batch();
  snap.docs.forEach((d) => batch.delete(d.ref));
  await batch.commit();
}

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
