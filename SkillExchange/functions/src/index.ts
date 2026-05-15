import * as admin from "firebase-admin";
import type { DocumentSnapshot, Query } from "firebase-admin/firestore";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { VertexAI } from "@google-cloud/vertexai";

admin.initializeApp();
const db = admin.firestore();
const vertex = new VertexAI({ project: process.env.GCLOUD_PROJECT, location: "us-central1" });
const model = vertex.getGenerativeModel({ model: "gemini-1.5-flash" });

export const recommendMatches = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in required.");

  const skill = String(request.data?.skill ?? "");
  const village = String(request.data?.village ?? "");
  let query: Query = db.collection("needPosts").where("status", "==", "OPEN").limit(20);
  if (skill) query = query.where("requiredSkill", "==", skill);
  const posts = (await query.get()).docs.map((doc) => ({ id: doc.id, ...doc.data() }));
  const prompt = [
    "You recommend rural barter service matches. Return compact JSON only.",
    `User: ${uid}. Village preference: ${village || "any"}. Skill filter: ${skill || "any"}.`,
    `Posts: ${JSON.stringify(posts)}.`,
    "Schema: [{\"postId\":\"id\",\"title\":\"title\",\"score\":0.0,\"reason\":\"short reason\"}]"
  ].join("\n");

  const ai = await model.generateContent(prompt);
  const text = ai.response.candidates?.[0]?.content.parts?.map((p) => p.text ?? "").join("") ?? "[]";
  const jsonText = text.replace(/```json|```/g, "").trim();
  return JSON.parse(jsonText);
});

export const detectFraud = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in required.");

  const text = String(request.data?.text ?? "");
  const prompt = [
    "Classify whether this rural barter post is likely fraud, spam, coercive, or unsafe.",
    "Return JSON only: {\"flagged\":true|false,\"severity\":\"low|medium|high\",\"reason\":\"short\"}.",
    `Post text: ${text}`
  ].join("\n");
  const ai = await model.generateContent(prompt);
  const raw = ai.response.candidates?.[0]?.content.parts?.map((p) => p.text ?? "").join("") ?? "{}";
  const parsed = JSON.parse(raw.replace(/```json|```/g, "").trim());

  if (parsed.flagged) {
    await db.collection("fraudSignals").add({
      userId: uid,
      severity: parsed.severity ?? "medium",
      reason: parsed.reason ?? "AI fraud signal",
      createdAt: Date.now()
    });
  }
  return { flagged: Boolean(parsed.flagged) };
});

export const updateTrustAfterBothConfirm = onDocumentUpdated("swapOffers/{offerId}", async (event) => {
  const before = event.data?.before.data();
  const after = event.data?.after.data();
  if (!before || !after) return;
  if (before.status === "COMPLETED" || after.status !== "COMPLETED") return;
  if (after.fromConfirmedComplete !== true || after.toConfirmedComplete !== true) return;

  const fromRef = db.collection("users").doc(after.fromUserId);
  const toRef = db.collection("users").doc(after.toUserId);

  await db.runTransaction(async (tx) => {
    const [fromSnap, toSnap] = await Promise.all([tx.get(fromRef), tx.get(toRef)]);
    const bump = (snap: DocumentSnapshot) => {
      const trustScore = Number(snap.get("trustScore") ?? 50);
      const completedSwaps = Number(snap.get("completedSwaps") ?? 0);
      return {
        trustScore: Math.min(100, trustScore + 2),
        completedSwaps: completedSwaps + 1
      };
    };
    tx.update(fromRef, bump(fromSnap));
    tx.update(toRef, bump(toSnap));
  });
});
