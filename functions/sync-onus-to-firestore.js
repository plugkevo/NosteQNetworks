import {onSchedule} from "firebase-functions/v2/scheduler";
import {onRequest} from "firebase-functions/v2/https";
import {initializeApp} from "firebase-admin/app";
import {getFirestore, FieldValue} from "firebase-admin/firestore";
import fetch from "node-fetch";

initializeApp();
const db = getFirestore();

// Sync function extracted to be reused by both scheduled and manual triggers
/**
 * Performs ONU synchronization from SmartOLT API to Firestore.
 * Fetches all ONUs and stores them in batches.
 * @return {Promise<Object>} Sync result with success status and duration
 */
async function performOnuSync() {
  const apiUrl = "https://nosteq.smartolt.com";
  const apiToken = "c9767e1d55694735a99b793c8e973b6d";

  if (!apiUrl || !apiToken) {
    throw new Error("Missing API credentials");
  }

  console.log("[v0] Starting ONU sync for 4300+ ONUs...");
  const syncStartTime = Date.now();

  const response = await fetch(`${apiUrl}/api/onu/get_all_onus_details`, {
    headers: {
      "X-Token": apiToken,
    },
  });

  if (!response.ok) {
    throw new Error(`API error: ${response.status} ${response.statusText}`);
  }

  const data = await response.json();
  const onus = data.onus || [];

  console.log(`[v0] Fetched ${onus.length} ONUs from API`);

  // Step 1: Get all existing ONUs from Firestore cache
  console.log("[v0] Fetching existing ONUs from Firestore...");
  const existingSnapshot = await db.collection("onus_cache").get();
  const existingSnIndices = new Set();
  const docsToDelete = [];

  existingSnapshot.docs.forEach((doc) => {
    // Skip metadata document
    if (doc.id === "_metadata") return;
    existingSnIndices.add(doc.id);
  });

  // Step 2: Find ONUs that exist in Firestore but NOT in the API response
  const apiSnSet = new Set(onus.map((onu) => onu.sn));

  existingSnIndices.forEach((sn) => {
    if (!apiSnSet.has(sn)) {
      docsToDelete.push(sn);
    }
  });

  // Step 3: Delete old ONUs that are no longer in the API
  if (docsToDelete.length > 0) {
    console.log(`[v0] Deleting ${docsToDelete.length} ONUs that no longer exist in API`);
    const batchSize = 100;
    const deleteBatches = [];

    for (let i = 0; i < docsToDelete.length; i += batchSize) {
      const batch = db.batch();
      const chunk = docsToDelete.slice(i, i + batchSize);

      chunk.forEach((sn) => {
        const ref = db.collection("onus_cache").doc(sn);
        batch.delete(ref);
      });

      deleteBatches.push(batch.commit());
    }

    await Promise.all(deleteBatches);
    console.log(`[v0] Deleted ${docsToDelete.length} ONUs`);
  }

  // Step 4: Add/Update ONUs from the API
  const batchSize = 100;
  const commits = [];

  for (let i = 0; i < onus.length; i += batchSize) {
    const batch = db.batch();
    const chunk = onus.slice(i, i + batchSize);

    chunk.forEach((onu) => {
      const ref = db.collection("onus_cache").doc(onu.sn);

      batch.set(ref, {
        ...onu,
        updatedAt: FieldValue.serverTimestamp(),
      });
    });

    commits.push(batch.commit());

    const processed = Math.min(i + batchSize, onus.length);
    if ((i + batchSize) % 500 === 0 || i + batchSize >= onus.length) {
      console.log(`[v0] Processed ${processed}/${onus.length} ONUs`);
    }
  }

  await Promise.all(commits);

  const syncEndTime = Date.now();
  await db
      .collection("onus_cache")
      .doc("_metadata")
      .set({
        lastSyncTime: FieldValue.serverTimestamp(),
        totalCount: onus.length,
        deletedCount: docsToDelete.length,
        syncStatus: "success",
        syncDurationMs: syncEndTime - syncStartTime,
      });

  const totalTime = Date.now() - syncStartTime;
  console.log(
      `[v0] ONU sync completed successfully. Total ONUs: ${onus.length}, ` +
      `Deleted: ${docsToDelete.length}, Time: ${totalTime}ms`,
  );

  return {
    success: true,
    message: `Synced ${onus.length} ONUs, deleted ${docsToDelete.length} old ONUs in ${totalTime}ms`,
    totalOnu: onus.length,
    deletedCount: docsToDelete.length,
    syncDuration: totalTime,
  };
}

  console.log("[v0] Starting ONU sync for 4300+ ONUs...");
  const syncStartTime = Date.now();

  const response = await fetch(`${apiUrl}/api/onu/get_all_onus_details`, {
    headers: {
      "X-Token": apiToken,
    },
  });

  if (!response.ok) {
    throw new Error(`API error: ${response.status} ${response.statusText}`);
  }

  const data = await response.json();
  const onus = data.onus || [];

  console.log(`[v0] Fetched ${onus.length} ONUs from API`);

  const batchSize = 100;
  const commits = [];

  for (let i = 0; i < onus.length; i += batchSize) {
    const batch = db.batch();
    const chunk = onus.slice(i, i + batchSize);

    chunk.forEach((onu) => {
      const ref = db.collection("onus_cache").doc(onu.sn);

      batch.set(ref, {
        ...onu,
        updatedAt: FieldValue.serverTimestamp(),
      });
    });

    commits.push(batch.commit());

    const processed = Math.min(i + batchSize, onus.length);
    if ((i + batchSize) % 500 === 0 || i + batchSize >= onus.length) {
      console.log(`[v0] Processed ${processed}/${onus.length} ONUs`);
    }
  }

  await Promise.all(commits);

  const syncEndTime = Date.now();
  await db
      .collection("onus_cache")
      .doc("_metadata")
      .set({
        lastSyncTime: FieldValue.serverTimestamp(),
        totalCount: onus.length,
        syncStatus: "success",
        syncDurationMs: syncEndTime - syncStartTime,
      });

  const totalTime = Date.now() - syncStartTime;
  console.log(
      `[v0] ONU sync completed successfully. Total ONUs: ${onus.length}, ` +
      `Time: ${totalTime}ms`,
  );

  return {
    success: true,
    message: `Synced ${onus.length} ONUs in ${totalTime}ms`,
    totalOnus: onus.length,
    syncDuration: totalTime,
  };
}

// Scheduled sync at midnight
/**
 * Scheduled Cloud Function that syncs ONUs every day at midnight.
 * Runs in Africa/Nairobi timezone.
 */
export const syncOnusToFirestore = onSchedule(
    {
      schedule: "*/30 * * * *",
      timeZone: "Africa/Nairobi",
      timeoutSeconds: 540,
      memory: "1GB",
    },
    async () => {
      try {
        await performOnuSync();
      } catch (error) {
        console.error("[v0] ONU sync failed", error);
        await db.collection("onus_cache").doc("_metadata").set({
          lastSyncTime: FieldValue.serverTimestamp(),
          syncStatus: "failed",
          syncError: error.message,
        });
        throw error;
      }
    },
);

// Manual sync endpoint
/**
 * HTTP endpoint to manually trigger ONU synchronization.
 * Can be called anytime to force a sync without waiting for midnight.
 * @param {Object} req - HTTP request object
 * @param {Object} res - HTTP response object
 */
export const manualSyncOnus = onRequest(
    {
      timeoutSeconds: 540,
      memory: "1GB",
    },
    async (req, res) => {
      try {
        const result = await performOnuSync();
        res.status(200).json(result);
      } catch (error) {
        console.error("[v0] Manual ONU sync failed", error);
        res.status(500).json({
          success: false,
          error: error.message,
        });
      }
    },
);

// npx eslint index.js --fix
// npm run lint
// firebase deploy --only functions
