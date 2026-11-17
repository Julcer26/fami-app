const { onValueCreated } = require("firebase-functions/v2/database");
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getDatabase } = require("firebase-admin/database");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const geminiApiKeySecret = defineSecret("GEMINI_API_KEY");
const ALLOWED_GEMINI_MODELS = new Set([
  "gemini-3.5-flash-lite",
  "gemini-flash-lite-latest",
  "gemini-3-flash-preview",
  "gemini-2.5-flash",
]);

function normalizeAlias(raw) {
  if (!raw || typeof raw !== "string") return "";
  return raw
    .trim()
    .split("@")[0]
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}

function isAssignedToAll(assignedTo) {
  const norm = normalizeAlias(assignedTo);
  return norm === "todos" || norm === "all" || norm === "familia";
}

exports.geminiGenerate = onRequest(
  { secrets: [geminiApiKeySecret], timeoutSeconds: 30, cors: false },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed" });
      return;
    }

    const authHeader = (req.headers.authorization || "").toString();
    if (!authHeader.startsWith("Bearer ")) {
      res.status(401).json({ error: "Missing Firebase ID token" });
      return;
    }

    const idToken = authHeader.substring("Bearer ".length).trim();
    try {
      await getAuth().verifyIdToken(idToken, true);
    } catch (_err) {
      res.status(401).json({ error: "Invalid or expired Firebase ID token" });
      return;
    }

    let apiKey = (geminiApiKeySecret.value() || process.env.GEMINI_API_KEY || "").trim();
    if (!apiKey) {
      try {
        const snap = await getDatabase().ref("/secure_config/gemini_api_key").get();
        if (snap.exists()) {
          apiKey = (snap.val() || "").toString().trim();
        }
      } catch (_e) {}
    }

    if (!apiKey) {
      res.status(503).json({ error: "Server Gemini API key not configured" });
      return;
    }

    const requestedModel = ((req.body && req.body.model) || "gemini-2.5-flash").toString().trim();
    const model = ALLOWED_GEMINI_MODELS.has(requestedModel) ? requestedModel : "gemini-2.5-flash";
    const payload = req.body && req.body.payload;
    if (!payload || typeof payload !== "object") {
      res.status(400).json({ error: "Invalid Gemini payload" });
      return;
    }

    try {
      const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(apiKey)}`;
      const upstream = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const text = await upstream.text();
      res.status(upstream.status).type("application/json").send(text);
    } catch (_err) {
      res.status(502).json({ error: "Upstream Gemini request failed" });
    }
  }
);

exports.notifyAssignedChore = onValueCreated(
  "/families/{familyId}/chores/{choreId}",
  async (event) => {
    const chore = event.data.val();
    if (!chore || chore.isPrivate === true || chore.isCompleted === true) {
      return null;
    }

    const familyId = event.params.familyId;
    const choreId = event.params.choreId;
    const title = (chore.title || "Nueva tarea").toString().trim();
    const assignedTo = (chore.assignedTo || "Todos").toString().trim();
    const createdBy = (chore.createdBy || "").toString().trim();
    const priority = (chore.priority || "Medio").toString().trim();

    const tokensSnap = await getDatabase()
      .ref(`/families/${familyId}/fcm_tokens`)
      .get();

    if (!tokensSnap.exists()) {
      return null;
    }

    const creatorNorm = normalizeAlias(createdBy);
    const assignedNorm = normalizeAlias(assignedTo);
    const forAll = isAssignedToAll(assignedTo);

    const targetTokens = [];
    const uidByToken = new Map();

    tokensSnap.forEach((child) => {
      const entry = child.val();
      if (!entry || !entry.token) return;
      const memberNorm = normalizeAlias(entry.normalizedName || entry.userName || "");

      // No auto-notificar al dispositivo de quien acaba de crear la tarea
      if (creatorNorm && memberNorm && memberNorm === creatorNorm) {
        return;
      }

      if (forAll || (assignedNorm && memberNorm === assignedNorm)) {
        targetTokens.push(entry.token);
        uidByToken.set(entry.token, child.key);
      }
    });

    if (targetTokens.length === 0) {
      return null;
    }

    const creatorLabel = createdBy.split("@")[0].trim() || "Un familiar";
    const notifTitle = forAll
      ? "🏡 Nueva tarea para Todos"
      : "📌 Nueva tarea asignada a ti";
    const notifBody = `${creatorLabel} agregó: "${title}" (Prioridad: ${priority})`;

    const response = await getMessaging().sendEachForMulticast({
      tokens: targetTokens,
      notification: {
        title: notifTitle,
        body: notifBody,
      },
      data: {
        choreId,
        title,
        assignedTo,
        createdBy,
        priority,
        tab: "tareas",
        open_tab: "tareas",
      },
      android: {
        priority: "high",
        notification: {
          channelId: "fami_tasks_channel",
          color: "#FF6B57",
        },
      },
    });

    // Limpiar tokens expirados automáticamente
    const cleanup = [];
    response.responses.forEach((res, idx) => {
      if (!res.success && res.error) {
        const code = res.error.code || "";
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token"
        ) {
          const uid = uidByToken.get(targetTokens[idx]);
          if (uid) {
            cleanup.push(
              getDatabase().ref(`/families/${familyId}/fcm_tokens/${uid}`).remove()
            );
          }
        }
      }
    });
    if (cleanup.length > 0) {
      await Promise.all(cleanup);
    }

    return null;
  }
);
