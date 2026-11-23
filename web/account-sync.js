const SUPABASE_CONFIG_URL = "/api/supabase-config";
const DECEASED_TABLE = "deceased_prayers";
const DELETED_IDS_KEY = "yt_arwah_pending_deletes_v1";

let client;
let currentSession;
let accountCardStatus = "";

async function getClient() {
  if (client) return client;
  const response = await fetch(SUPABASE_CONFIG_URL, { headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error("Konfigurasi sinkronisasi belum tersedia.");
  const config = await response.json();
  if (!config.url || !config.publishableKey) throw new Error("Konfigurasi Supabase belum lengkap.");
  const { createClient } = await import("https://esm.sh/@supabase/supabase-js@2");
  client = createClient(config.url, config.publishableKey, {
    auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true }
  });
  const { data } = await client.auth.getSession();
  currentSession = data.session;
  client.auth.onAuthStateChange((_event, session) => {
    currentSession = session;
    renderAccountCard();
    if (session) syncLocalList().catch(showSyncError);
  });
  return client;
}

function readLocalList() {
  try {
    const data = JSON.parse(localStorage.getItem("yt_arwah_list_v1") || "[]");
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

function writeLocalList(entries) {
  localStorage.setItem("yt_arwah_list_v1", JSON.stringify(entries));
  window.dispatchEvent(new CustomEvent("arwah-cloud-updated"));
}

function toCloudRow(entry, userId) {
  const now = new Date().toISOString();
  const createdAt = Number.isFinite(Number(entry.createdAtMillis)) && entry.createdAtMillis > 0
    ? new Date(entry.createdAtMillis).toISOString()
    : now;
  const updatedAt = Number.isFinite(Number(entry.updatedAtMillis)) && entry.updatedAtMillis > 0
    ? new Date(entry.updatedAtMillis).toISOString()
    : createdAt;
  return {
    id: entry.id,
    user_id: userId,
    honorific: entry.honorific === "Almarhumah" ? "Almarhumah" : "Almarhum",
    full_name: String(entry.fullName || "").trim().slice(0, 80),
    parent_prefix: entry.parentPrefix === "binti" ? "binti" : "bin",
    parent_name: String(entry.parentName || "").trim().slice(0, 80),
    relation_note: String(entry.relationNote || "").trim().slice(0, 100) || null,
    included_in_prayer: entry.includedInPrayer !== false,
    created_at: createdAt,
    updated_at: updatedAt
  };
}

function fromCloudRow(row) {
  return {
    id: row.id,
    honorific: row.honorific,
    fullName: row.full_name,
    parentPrefix: row.parent_prefix,
    parentName: row.parent_name || "",
    relationNote: row.relation_note,
    includedInPrayer: row.included_in_prayer,
    createdAtMillis: Date.parse(row.created_at) || Date.now(),
    updatedAtMillis: Date.parse(row.updated_at) || Date.now()
  };
}

function isUuid(value) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value || "");
}

function getPendingDeletes() {
  try {
    const ids = JSON.parse(localStorage.getItem(DELETED_IDS_KEY) || "[]");
    return Array.isArray(ids) ? ids : [];
  } catch {
    return [];
  }
}

function showSyncError(error) {
  accountCardStatus = error?.message || "Sinkronisasi gagal. Coba lagi saat online.";
  renderAccountCard();
}

async function syncLocalList() {
  const supabase = await getClient();
  const { data: { user }, error: userError } = await supabase.auth.getUser();
  if (userError || !user) throw userError || new Error("Masuk untuk menyinkronkan daftar.");

  const pendingDeletes = getPendingDeletes();
  if (pendingDeletes.length) {
    const validIds = pendingDeletes.filter(isUuid);
    if (validIds.length) {
      const { error } = await supabase.from(DECEASED_TABLE).delete().in("id", validIds);
      if (error) throw error;
    }
    localStorage.setItem(DELETED_IDS_KEY, "[]");
  }

  const { data: remoteRows, error: pullError } = await supabase
    .from(DECEASED_TABLE)
    .select("id,honorific,full_name,parent_prefix,parent_name,relation_note,included_in_prayer,created_at,updated_at")
    .order("updated_at", { ascending: false });
  if (pullError) throw pullError;

  let local = readLocalList().map((entry) => ({
    ...entry,
    id: isUuid(entry.id) ? entry.id : crypto.randomUUID(),
    createdAtMillis: entry.createdAtMillis || Date.now(),
    updatedAtMillis: entry.updatedAtMillis || entry.createdAtMillis || Date.now()
  }));
  const byId = new Map();
  for (const row of remoteRows || []) byId.set(row.id, fromCloudRow(row));
  for (const entry of local) {
    const remote = byId.get(entry.id);
    if (!remote || entry.updatedAtMillis >= remote.updatedAtMillis) byId.set(entry.id, entry);
  }
  const merged = [...byId.values()].sort((a, b) => b.createdAtMillis - a.createdAtMillis);
  const payload = merged
    .filter((entry) => String(entry.fullName || "").trim())
    .map((entry) => toCloudRow(entry, user.id));
  if (payload.length) {
    const { error } = await supabase.from(DECEASED_TABLE).upsert(payload, { onConflict: "id" });
    if (error) throw error;
  }
  writeLocalList(merged);
  accountCardStatus = "Daftar nama arwah tersinkron.";
  renderAccountCard();
}

async function deleteEntry(id) {
  const pending = new Set(getPendingDeletes());
  pending.add(id);
  localStorage.setItem(DELETED_IDS_KEY, JSON.stringify([...pending]));
  if (!currentSession) return;
  try {
    await syncLocalList();
  } catch (error) {
    showSyncError(error);
  }
}

function renderAccountCard() {
  const card = document.getElementById("cloud-account-card");
  if (!card) return;
  const account = currentSession?.user?.email;
  card.innerHTML = account
    ? `<div class="cloud-account-copy"><strong>Sinkronisasi antarperangkat</strong><span>Terhubung sebagai ${escapeAccountText(account)}</span>${accountCardStatus ? `<span class="muted" role="status">${escapeAccountText(accountCardStatus)}</span>` : ""}</div><div class="btn-group"><button type="button" class="btn-outline" id="btn-cloud-sync">Sinkronkan</button><button type="button" class="btn-text" id="btn-cloud-signout">Keluar</button></div>`
    : `<div class="cloud-account-copy"><strong>Sinkronisasi antarperangkat</strong><span>Masuk dengan Google untuk memakai daftar nama yang sama di web dan Android.</span>${accountCardStatus ? `<span class="muted" role="status">${escapeAccountText(accountCardStatus)}</span>` : ""}</div><button type="button" class="btn-primary" id="btn-cloud-signin">Masuk dengan Google</button>`;
}

function escapeAccountText(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}

document.addEventListener("click", async (event) => {
  const button = event.target.closest("#btn-cloud-signin, #btn-cloud-sync, #btn-cloud-signout");
  if (!button) return;
  button.disabled = true;
  try {
    const supabase = await getClient();
    if (button.id === "btn-cloud-signin") {
      const { error } = await supabase.auth.signInWithOAuth({
        provider: "google",
        options: { redirectTo: window.location.origin + window.location.pathname }
      });
      if (error) throw error;
      accountCardStatus = "Membuka login Google...";
    } else if (button.id === "btn-cloud-signout") {
      const { error } = await supabase.auth.signOut();
      if (error) throw error;
      currentSession = null;
      accountCardStatus = "Akun dilepas. Daftar lokal tetap tersimpan di perangkat ini.";
      renderAccountCard();
    } else {
      accountCardStatus = "Menyinkronkan daftar...";
      renderAccountCard();
      await syncLocalList();
    }
  } catch (error) {
    showSyncError(error);
  } finally {
    button.disabled = false;
  }
});

window.YasinCloud = {
  syncLocalList: () => syncLocalList().catch(showSyncError),
  deleteEntry
};

window.addEventListener("arwah-cloud-updated", renderAccountCard);
getClient().then(renderAccountCard).catch(showSyncError);
