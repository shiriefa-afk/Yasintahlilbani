(function () {
  const DATA = window.YASIN_DATA;
  const STORAGE_KEYS = {
    settings: "yt_reader_settings_v1",
    reading: "yt_reading_state_v1",
    tasbih: "yt_tasbih_state_v1",
    arwah: "yt_arwah_list_v1"
  };

  const defaultSettings = {
    arabicFontSize: 28,
    showLatin: true,
    showTranslation: true,
    nightMode: false,
    hapticEnabled: true
  };

  const defaultReading = {
    lastYasinVerse: 1,
    lastTahlilStep: 0,
    bookmarkedVerses: []
  };

  const defaultTasbih = {
    selectedDhikrId: "subhanallah",
    currentCount: 0,
    targetCount: 33,
    lifetimeCount: 0
  };

  function loadJson(key, fallback) {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return structuredClone(fallback);
      return { ...structuredClone(fallback), ...JSON.parse(raw) };
    } catch {
      return structuredClone(fallback);
    }
  }

  function loadArray(key) {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return [];
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  function saveJson(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      // Ignore storage quota errors
    }
  }

  const state = {
    activeTab: "home",
    yasinFilterBookmarked: false,
    selectedShortSurahId: DATA.shortSurahs[0].id,
    tahlilMode: "step", // "step" | "all"
    tahlilStepCounts: {},
    arwahSearch: "",
    editingArwahId: null,
    settings: loadJson(STORAGE_KEYS.settings, defaultSettings),
    reading: loadJson(STORAGE_KEYS.reading, defaultReading),
    tasbih: loadJson(STORAGE_KEYS.tasbih, defaultTasbih),
    arwah: loadArray(STORAGE_KEYS.arwah)
  };

  const el = {
    body: document.body,
    appMain: document.getElementById("app-main"),
    toast: document.getElementById("toast"),
    nightToggleBtn: document.getElementById("btn-night-toggle"),
    settingsModal: document.getElementById("settings-modal"),
    arwahModal: document.getElementById("arwah-modal"),
    arwahForm: document.getElementById("arwah-form"),
    tocModal: document.getElementById("toc-modal")
  };

  let toastTimer = null;
  function showToast(msg) {
    el.toast.textContent = msg;
    el.toast.classList.add("visible");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.toast.classList.remove("visible"), 2400);
  }

  function triggerHaptic() {
    if (state.settings.hapticEnabled && navigator.vibrate) {
      navigator.vibrate(15);
    }
  }

  function escapeHtml(str) {
    return String(str ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function formatArwahName(item) {
    const base = `${item.honorific} ${item.fullName}`.trim();
    return item.parentName && item.parentName.trim()
      ? `${base} ${item.parentPrefix} ${item.parentName.trim()}`
      : base;
  }

  function applyTheme() {
    el.body.classList.toggle("dark-theme", Boolean(state.settings.nightMode));
    document.documentElement.style.setProperty("--arabic-size", `${state.settings.arabicFontSize}px`);
    el.nightToggleBtn.textContent = state.settings.nightMode ? "☀️ Mode Terang" : "🌙 Mode Malam";
  }

  function setTab(tabName) {
    state.activeTab = tabName;
    document.querySelectorAll("[data-tab]").forEach((btn) => {
      btn.classList.toggle("active", btn.dataset.tab === tabName);
    });
    renderActiveTab();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function renderVerseCard(verse, { allowBookmark = false, allowTapLastRead = false } = {}) {
    const isBookmarked = state.reading.bookmarkedVerses.includes(verse.number);
    const isLastRead = allowTapLastRead && state.reading.lastYasinVerse === verse.number;

    return `
      <article class="card verse-card ${isLastRead ? "last-read" : ""} ${isBookmarked ? "bookmarked" : ""}" data-verse="${verse.number}">
        <div class="verse-header">
          <div class="verse-num-wrap">
            <span class="medallion">${verse.number}</span>
            ${isLastRead ? '<span class="badge badge-primary">Terakhir Dibaca</span>' : ""}
          </div>
          <div class="verse-actions">
            ${allowTapLastRead ? `<button type="button" class="btn-chip ${isLastRead ? "active" : ""}" data-action="mark-last-read" data-verse="${verse.number}">${isLastRead ? "Batas Aktif" : "Tandai Batas"}</button>` : ""}
            ${allowBookmark ? `<button type="button" class="btn-icon ${isBookmarked ? "bookmarked" : ""}" data-action="toggle-bookmark" data-verse="${verse.number}" title="${isBookmarked ? "Hapus penanda" : "Tandai ayat"}">${isBookmarked ? "★" : "☆"}</button>` : ""}
          </div>
        </div>
        <p class="arabic-text" dir="rtl" lang="ar">${escapeHtml(verse.arabic)}</p>
        ${state.settings.showLatin ? `<p class="latin-text">${escapeHtml(verse.latin)}</p>` : ""}
        ${state.settings.showTranslation ? `<p class="translation-text">${escapeHtml(verse.translation)}</p>` : ""}
      </article>
    `;
  }

  function renderHome() {
    const activeArwah = state.arwah.filter((a) => a.includedInPrayer);
    const lastVerse = state.reading.lastYasinVerse || 1;
    const bookmarkCount = state.reading.bookmarkedVerses.length;

    el.appMain.innerHTML = `
      <section class="hero-card">
        <div class="hero-top">
          <div>
            <span class="hero-pill">Surah Yasin • Ayat ${lastVerse} dari 83</span>
            <h2>Lanjutkan Bacaan Yasin</h2>
            <p class="hero-sub">${bookmarkCount} ayat ditandai • Siap dibaca kapan saja</p>
          </div>
          <div class="hero-arabic" dir="rtl" lang="ar">${DATA.surahArabicName}</div>
        </div>
        <div class="hero-actions">
          <button type="button" class="btn-gold" data-jump-yasin="${lastVerse}">
            ${lastVerse > 1 ? `Lanjut Ayat ${lastVerse}` : "Mulai Baca Yasin"} →
          </button>
        </div>
      </section>

      <div class="mushaf-divider"><span>◆</span></div>

      <section>
        <h3 class="section-title">Daftar Bacaan & Ibadah</h3>
        <div class="menu-grid">
          <button type="button" class="card feature-card" data-nav="yasin">
            <span class="feature-icon">📖</span>
            <strong>Surah Yasin</strong>
            <small>83 Ayat Lengkap</small>
          </button>
          <button type="button" class="card feature-card" data-nav="tahlil">
            <span class="feature-icon">🤲</span>
            <strong>Tahlil & Doa Arwah</strong>
            <small>16 Urutan Bacaan</small>
          </button>
          <button type="button" class="card feature-card" data-nav="short">
            <span class="feature-icon">🕌</span>
            <strong>Surah Pendek</strong>
            <small>Fatihah, Kursi & 3 Qul</small>
          </button>
          <button type="button" class="card feature-card" data-nav="tasbih">
            <span class="feature-icon">📿</span>
            <strong>Tasbih Digital</strong>
            <small>Total: ${state.tasbih.lifetimeCount} dzikir</small>
          </button>
        </div>
      </section>

      <section class="card arwah-summary-card">
        <div class="card-head-row">
          <div>
            <h3>Daftar Nama Arwah</h3>
            <p class="muted">${activeArwah.length} nama disertakan dalam Doa Tahlil</p>
          </div>
          <button type="button" class="btn-outline" data-nav="arwah">Kelola</button>
        </div>
        ${
          activeArwah.length === 0
            ? '<p class="muted">Tambahkan nama Almarhum/Almarhumah keluarga agar otomatis tampil saat membaca Tawasul & Doa Arwah.</p>'
            : `<ul class="arwah-preview-list">${activeArwah
                .slice(0, 4)
                .map((p) => `<li>${escapeHtml(formatArwahName(p))}</li>`)
                .join("")}${activeArwah.length > 4 ? `<li class="more-link">+${activeArwah.length - 4} nama lainnya</li>` : ""}</ul>`
        }
      </section>
    `;
  }

  function renderYasin(scrollToVerse = null) {
    const bookmarkedSet = new Set(state.reading.bookmarkedVerses);
    const verses = state.yasinFilterBookmarked
      ? DATA.verses.filter((v) => bookmarkedSet.has(v.number))
      : DATA.verses;

    const jumpMarks = [1, 20, 36, 58, 70, 83];

    el.appMain.innerHTML = `
      <div class="section-header">
        <div>
          <h2>${DATA.surahName}</h2>
          <p class="muted">${DATA.surahSubtitle}</p>
        </div>
        <button type="button" class="btn-outline" id="btn-open-settings-inline">⚙️ Tampilan</button>
      </div>

      <div class="chip-row">
        <button type="button" class="chip ${!state.yasinFilterBookmarked ? "active" : ""}" data-yasin-filter="all">Semua (83)</button>
        <button type="button" class="chip ${state.yasinFilterBookmarked ? "active" : ""}" data-yasin-filter="bookmarked">★ Ditandai (${bookmarkedSet.size})</button>
        ${state.reading.lastYasinVerse > 1 ? `<button type="button" class="chip" data-yasin-jump="${state.reading.lastYasinVerse}">Terakhir: Ayat ${state.reading.lastYasinVerse}</button>` : ""}
        ${jumpMarks.map((n) => `<button type="button" class="chip" data-yasin-jump="${n}">Ayat ${n}</button>`).join("")}
      </div>

      ${
        !state.yasinFilterBookmarked
          ? `<div class="card bismillah-card">
              <div class="surah-title-ar" dir="rtl" lang="ar">${DATA.surahArabicName}</div>
              <div class="mushaf-divider"><span>◆</span></div>
              <p class="arabic-text center" dir="rtl" lang="ar">${DATA.bismillahArabic}</p>
              ${state.settings.showLatin ? `<p class="latin-text center">${DATA.bismillahLatin}</p>` : ""}
              ${state.settings.showTranslation ? `<p class="translation-text center">${DATA.bismillahTranslation}</p>` : ""}
            </div>`
          : ""
      }

      ${
        verses.length === 0
          ? `<div class="card empty-card">
              <p><strong>Belum ada ayat yang ditandai</strong></p>
              <p class="muted">Ketuk ikon ☆ pada ayat mana saja untuk menyimpannya di sini.</p>
            </div>`
          : `<div class="verse-list">${verses.map((v) => renderVerseCard(v, { allowBookmark: true, allowTapLastRead: true })).join("")}</div>`
      }
    `;

    if (scrollToVerse) {
      const targetEl = el.appMain.querySelector(`[data-verse="${scrollToVerse}"]`);
      if (targetEl) {
        setTimeout(() => targetEl.scrollIntoView({ behavior: "smooth", block: "center" }), 60);
      }
    }
  }

  function renderArwahBoxInTahlil() {
    const activeArwah = state.arwah.filter((a) => a.includedInPrayer);
    return `
      <div class="arwah-inline-box">
        <div class="card-head-row">
          <strong>🤲 Khususon Arwah yang Didoakan</strong>
          <button type="button" class="btn-text" data-nav="arwah">${activeArwah.length ? "Kelola" : "Tambah"}</button>
        </div>
        ${
          activeArwah.length === 0
            ? '<p class="muted">Belum ada nama Almarhum/Almarhumah aktif. Tambahkan melalui tab Arwah agar disebut langsung di doa ini.</p>'
            : `<ol class="arwah-prayer-list">${activeArwah.map((p) => `<li>${escapeHtml(formatArwahName(p))}</li>`).join("")}</ol>`
        }
      </div>
    `;
  }

  function renderTahlilStepCard(step, index) {
    const count = state.tahlilStepCounts[step.id] || 0;
    const completed = step.repetitionTarget > 1 && count >= step.repetitionTarget;
    return `
      <article class="card tahlil-card ${completed ? "completed" : ""}">
        <div class="verse-header">
          <div class="verse-num-wrap">
            <span class="medallion">${step.order}</span>
            <div>
              <h3>${escapeHtml(step.title)}</h3>
              <span class="arabic-sub" dir="rtl" lang="ar">${escapeHtml(step.arabicTitle)}</span>
            </div>
          </div>
          ${step.repetitionTarget > 1 ? `<span class="badge ${completed ? "badge-primary" : "badge-gold"}">${step.repetitionTarget}x</span>` : ""}
        </div>
        <div class="mushaf-divider"><span>◆</span></div>
        <p class="arabic-text" dir="rtl" lang="ar">${escapeHtml(step.arabic)}</p>
        ${state.settings.showLatin ? `<p class="latin-text">${escapeHtml(step.latin)}</p>` : ""}
        ${state.settings.showTranslation ? `<p class="translation-text">${escapeHtml(step.translation)}</p>` : ""}
        ${step.guidanceNote ? `<p class="guidance-note">${escapeHtml(step.guidanceNote)}</p>` : ""}
        ${step.supportsDeceasedNames ? renderArwahBoxInTahlil() : ""}
        ${
          step.repetitionTarget > 1
            ? `<div class="step-counter-row">
                <button type="button" class="btn-primary flex-1" data-tahlil-count="${step.id}" data-target="${step.repetitionTarget}" data-step-index="${index}">
                  ${completed ? `✓ Selesai (${count} / ${step.repetitionTarget})` : `+ Hitung Bacaan: ${count} / ${step.repetitionTarget}`}
                </button>
                ${count > 0 ? `<button type="button" class="btn-outline" data-tahlil-reset="${step.id}">Ulangi</button>` : ""}
              </div>`
            : ""
        }
      </article>
    `;
  }

  function renderTahlil() {
    const total = DATA.tahlilSteps.length;
    const stepIdx = Math.min(Math.max(state.reading.lastTahlilStep || 0, 0), total - 1);
    const step = DATA.tahlilSteps[stepIdx];
    const progressPct = Math.round(((stepIdx + 1) / total) * 100);

    el.appMain.innerHTML = `
      <div class="section-header">
        <div>
          <h2>Panduan Tahlil & Doa Arwah</h2>
          <p class="muted">Bacaan ${stepIdx + 1} dari ${total}</p>
        </div>
        <div class="btn-group">
          <button type="button" class="btn-outline" id="btn-tahlil-mode">${state.tahlilMode === "step" ? "Mode Semua" : "Mode Langkah"}</button>
          <button type="button" class="btn-outline" id="btn-tahlil-toc">Daftar Isi</button>
        </div>
      </div>

      <div class="progress-track"><div class="progress-fill" style="width:${progressPct}%"></div></div>

      <div class="chip-row">
        <button type="button" class="chip" id="btn-tahlil-toc-chip">☰ Daftar Isi (${total})</button>
        <button type="button" class="chip" id="btn-jump-doa-arwah">🤲 Langsung ke Doa Arwah</button>
      </div>

      ${
        state.tahlilMode === "step"
          ? `${renderTahlilStepCard(step, stepIdx)}
             <div class="step-nav-bar">
               <button type="button" class="btn-outline flex-1" id="btn-tahlil-prev" ${stepIdx === 0 ? "disabled" : ""}>← Sebelumnya</button>
               ${
                 stepIdx < total - 1
                   ? `<button type="button" class="btn-primary flex-1" id="btn-tahlil-next">Berikutnya →</button>`
                   : `<button type="button" class="btn-gold flex-1" id="btn-tahlil-restart">↻ Ulangi Awal</button>`
               }
             </div>`
          : `<div class="verse-list">${DATA.tahlilSteps.map((s, idx) => renderTahlilStepCard(s, idx)).join("")}</div>`
      }
    `;
  }

  function renderShortSurahs() {
    const selected = DATA.shortSurahs.find((s) => s.id === state.selectedShortSurahId) || DATA.shortSurahs[0];
    el.appMain.innerHTML = `
      <div class="section-header">
        <div>
          <h2>Surah Pendek & Ayat Kursi</h2>
          <p class="muted">Al-Fatihah, Ayat Kursi, Al-Ikhlas, Al-Falaq, An-Nas</p>
        </div>
      </div>

      <div class="chip-row">
        ${DATA.shortSurahs
          .map((s) => `<button type="button" class="chip ${s.id === selected.id ? "active" : ""}" data-short-surah="${s.id}">${escapeHtml(s.titleLatin)}</button>`)
          .join("")}
      </div>

      <div class="card bismillah-card">
        <div class="surah-title-ar" dir="rtl" lang="ar">${escapeHtml(selected.titleArabic)}</div>
        <h3>${escapeHtml(selected.titleLatin)}</h3>
        <p class="muted">${escapeHtml(selected.subtitle)}</p>
      </div>

      <div class="verse-list">
        ${selected.verses.map((v) => renderVerseCard(v)).join("")}
      </div>
    `;
  }

  function renderTasbih() {
    const preset = DATA.dhikrPresets.find((d) => d.id === state.tasbih.selectedDhikrId) || DATA.dhikrPresets[0];
    const { currentCount, targetCount, lifetimeCount } = state.tasbih;
    const isTargetReached = targetCount > 0 && currentCount >= targetCount;
    const progress = targetCount > 0 ? Math.min(currentCount / targetCount, 1) : (currentCount % 33) / 33;
    const circumference = 2 * Math.PI * 98;
    const dashOffset = circumference * (1 - progress);

    el.appMain.innerHTML = `
      <div class="section-header">
        <div>
          <h2>Tasbih Digital</h2>
          <p class="muted">Total sepanjang waktu: ${lifetimeCount} dzikir</p>
        </div>
      </div>

      <div class="chip-row">
        ${DATA.dhikrPresets
          .map((d) => `<button type="button" class="chip ${d.id === preset.id ? "active" : ""}" data-dhikr="${d.id}">${escapeHtml(d.title)}</button>`)
          .join("")}
      </div>

      <div class="card dhikr-card">
        <p class="arabic-text center" dir="rtl" lang="ar">${escapeHtml(preset.arabic)}</p>
        <p class="latin-text center">${escapeHtml(preset.latin)}</p>
        <p class="translation-text center">${escapeHtml(preset.meaning)}</p>
      </div>

      <div class="tasbih-center">
        <button type="button" class="tasbih-ring-btn ${isTargetReached ? "reached" : ""}" id="btn-tasbih-tap" aria-label="Ketuk untuk menghitung dzikir">
          <svg class="tasbih-svg" viewBox="0 0 220 220" aria-hidden="true">
            <circle class="ring-track" cx="110" cy="110" r="98" />
            <circle class="ring-progress" cx="110" cy="110" r="98"
              stroke-dasharray="${circumference}"
              stroke-dashoffset="${dashOffset}" />
          </svg>
          <span class="tasbih-inner">
            <span class="tasbih-count">${currentCount}</span>
            <span class="tasbih-target">${targetCount > 0 ? `dari ${targetCount}` : "Tanpa Batas"}</span>
            <span class="tasbih-hint">Ketuk di sini</span>
          </span>
        </button>
        ${isTargetReached ? `<div class="badge badge-gold">✓ Alhamdulillah, target ${targetCount} tercapai</div>` : ""}
      </div>

      <div class="tasbih-controls">
        <span class="muted">Target Hitungan</span>
        <div class="chip-row center">
          ${[
            [33, "33"],
            [99, "99"],
            [100, "100"],
            [0, "Bebas"]
          ]
            .map(([val, label]) => `<button type="button" class="chip ${targetCount === val ? "active" : ""}" data-tasbih-target="${val}">${label}</button>`)
            .join("")}
        </div>
        <button type="button" class="btn-outline" id="btn-tasbih-reset" ${currentCount === 0 ? "disabled" : ""}>↻ Ulangi Hitungan</button>
      </div>
    `;
  }

  function renderArwah() {
    const q = state.arwahSearch.trim().toLowerCase();
    const filtered = q
      ? state.arwah.filter(
          (a) =>
            a.fullName.toLowerCase().includes(q) ||
            (a.parentName || "").toLowerCase().includes(q) ||
            (a.relationNote || "").toLowerCase().includes(q)
        )
      : state.arwah;

    el.appMain.innerHTML = `
      <div class="section-header">
        <div>
          <h2>Daftar Nama Arwah</h2>
          <p class="muted">Nama yang aktif akan otomatis tampil di bagian Tawasul & Doa Arwah.</p>
        </div>
        <button type="button" class="btn-primary" id="btn-add-arwah">+ Tambah Nama</button>
      </div>

      <div class="search-wrap">
        <input type="search" id="input-arwah-search" class="text-input" value="${escapeHtml(state.arwahSearch)}" placeholder="Cari nama Almarhum / Almarhumah..." />
      </div>

      ${
        filtered.length === 0
          ? `<div class="card empty-card">
              <p><strong>${q ? "Nama tidak ditemukan" : "Belum Ada Nama Arwah Tersimpan"}</strong></p>
              <p class="muted">${q ? "Coba kata kunci lain atau tambahkan nama baru." : "Simpan nama orang tua atau keluarga tercinta yang telah berpulang agar selalu tersertakan saat Yasinan & Tahlilan."}</p>
            </div>`
          : `<div class="verse-list">
              ${filtered
                .map(
                  (p) => `
                  <article class="card arwah-item-card ${p.includedInPrayer ? "active" : ""}">
                    <div class="card-head-row">
                      <div>
                        <h4>${escapeHtml(formatArwahName(p))}</h4>
                        ${p.relationNote ? `<p class="muted">${escapeHtml(p.relationNote)}</p>` : ""}
                      </div>
                      <label class="switch">
                        <input type="checkbox" data-toggle-arwah="${p.id}" ${p.includedInPrayer ? "checked" : ""} />
                        <span class="slider"></span>
                      </label>
                    </div>
                    <div class="card-foot-row">
                      <span class="status-text ${p.includedInPrayer ? "on" : ""}">
                        ${p.includedInPrayer ? "Disertakan di Doa Arwah" : "Disembunyikan sementara"}
                      </span>
                      <div class="btn-group">
                        <button type="button" class="btn-text" data-edit-arwah="${p.id}">Ubah</button>
                        <button type="button" class="btn-text danger" data-delete-arwah="${p.id}">Hapus</button>
                      </div>
                    </div>
                  </article>
                `
                )
                .join("")}
            </div>`
      }
    `;

    const searchEl = document.getElementById("input-arwah-search");
    if (searchEl) {
      searchEl.addEventListener("input", (e) => {
        state.arwahSearch = e.target.value;
        renderArwah();
        const nextSearch = document.getElementById("input-arwah-search");
        if (nextSearch) {
          nextSearch.focus();
          nextSearch.setSelectionRange(nextSearch.value.length, nextSearch.value.length);
        }
      });
    }
  }

  function renderActiveTab(scrollVerse = null) {
    switch (state.activeTab) {
      case "home":
        renderHome();
        break;
      case "yasin":
        renderYasin(scrollVerse);
        break;
      case "tahlil":
        renderTahlil();
        break;
      case "short":
        renderShortSurahs();
        break;
      case "tasbih":
        renderTasbih();
        break;
      case "arwah":
        renderArwah();
        break;
      default:
        renderHome();
    }
  }

  function openSettingsModal() {
    document.getElementById("setting-font-size").value = state.settings.arabicFontSize;
    document.getElementById("setting-font-value").textContent = `${state.settings.arabicFontSize} px`;
    document.getElementById("setting-show-latin").checked = state.settings.showLatin;
    document.getElementById("setting-show-translation").checked = state.settings.showTranslation;
    document.getElementById("setting-night-mode").checked = state.settings.nightMode;
    document.getElementById("setting-haptic").checked = state.settings.hapticEnabled;
    el.settingsModal.showModal();
  }

  function openArwahModal(entry = null) {
    state.editingArwahId = entry ? entry.id : null;
    document.getElementById("arwah-modal-title").textContent = entry ? "Ubah Nama Arwah" : "Tambah Nama Arwah";
    document.getElementById("arwah-honorific").value = entry ? entry.honorific : "Almarhum";
    document.getElementById("arwah-fullname").value = entry ? entry.fullName : "";
    document.getElementById("arwah-parent").value = entry ? entry.parentName : "";
    document.getElementById("arwah-note").value = entry && entry.relationNote ? entry.relationNote : "";
    el.arwahModal.showModal();
  }

  function openTocModal() {
    const listEl = document.getElementById("toc-list");
    listEl.innerHTML = DATA.tahlilSteps
      .map(
        (step, idx) => `
        <button type="button" class="toc-item ${idx === state.reading.lastTahlilStep ? "active" : ""}" data-toc-step="${idx}">
          <span class="medallion small">${step.order}</span>
          <span>${escapeHtml(step.title)}</span>
        </button>
      `
      )
      .join("");
    el.tocModal.showModal();
  }

  // Event delegation for main content
  el.appMain.addEventListener("click", (e) => {
    const target = e.target.closest("button");
    if (!target) return;

    if (target.dataset.nav) {
      setTab(target.dataset.nav);
      return;
    }

    if (target.dataset.jumpYasin) {
      const verse = Number(target.dataset.jumpYasin);
      state.activeTab = "yasin";
      state.yasinFilterBookmarked = false;
      document.querySelectorAll("[data-tab]").forEach((btn) => btn.classList.toggle("active", btn.dataset.tab === "yasin"));
      renderYasin(verse);
      return;
    }

    if (target.id === "btn-open-settings-inline") {
      openSettingsModal();
      return;
    }

    if (target.dataset.yasinFilter) {
      state.yasinFilterBookmarked = target.dataset.yasinFilter === "bookmarked";
      renderYasin();
      return;
    }

    if (target.dataset.yasinJump) {
      const verse = Number(target.dataset.yasinJump);
      state.yasinFilterBookmarked = false;
      renderYasin(verse);
      return;
    }

    if (target.dataset.action === "toggle-bookmark") {
      const verse = Number(target.dataset.verse);
      const list = new Set(state.reading.bookmarkedVerses);
      if (list.has(verse)) {
        list.delete(verse);
        showToast(`Penanda ayat ${verse} dihapus.`);
      } else {
        list.add(verse);
        showToast(`Ayat ${verse} ditandai.`);
      }
      state.reading.bookmarkedVerses = Array.from(list).sort((a, b) => a - b);
      saveJson(STORAGE_KEYS.reading, state.reading);
      renderYasin();
      return;
    }

    if (target.dataset.action === "mark-last-read") {
      const verse = Number(target.dataset.verse);
      state.reading.lastYasinVerse = verse;
      saveJson(STORAGE_KEYS.reading, state.reading);
      showToast(`Batas bacaan Yasin disimpan di ayat ${verse}.`);
      renderYasin();
      return;
    }

    if (target.id === "btn-tahlil-mode") {
      state.tahlilMode = state.tahlilMode === "step" ? "all" : "step";
      renderTahlil();
      return;
    }

    if (target.id === "btn-tahlil-toc" || target.id === "btn-tahlil-toc-chip") {
      openTocModal();
      return;
    }

    if (target.id === "btn-jump-doa-arwah") {
      state.reading.lastTahlilStep = DATA.tahlilSteps.length - 1;
      saveJson(STORAGE_KEYS.reading, state.reading);
      renderTahlil();
      return;
    }

    if (target.id === "btn-tahlil-prev") {
      state.reading.lastTahlilStep = Math.max(0, (state.reading.lastTahlilStep || 0) - 1);
      saveJson(STORAGE_KEYS.reading, state.reading);
      renderTahlil();
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    if (target.id === "btn-tahlil-next") {
      state.reading.lastTahlilStep = Math.min(DATA.tahlilSteps.length - 1, (state.reading.lastTahlilStep || 0) + 1);
      saveJson(STORAGE_KEYS.reading, state.reading);
      renderTahlil();
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    if (target.id === "btn-tahlil-restart") {
      state.reading.lastTahlilStep = 0;
      saveJson(STORAGE_KEYS.reading, state.reading);
      renderTahlil();
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    if (target.dataset.tahlilCount) {
      triggerHaptic();
      const id = target.dataset.tahlilCount;
      const limit = Number(target.dataset.target);
      const stepIndex = Number(target.dataset.stepIndex);
      const current = state.tahlilStepCounts[id] || 0;
      state.tahlilStepCounts[id] = current >= limit ? 0 : current + 1;
      state.reading.lastTahlilStep = stepIndex;
      saveJson(STORAGE_KEYS.reading, state.reading);
      renderTahlil();
      return;
    }

    if (target.dataset.tahlilReset) {
      state.tahlilStepCounts[target.dataset.tahlilReset] = 0;
      renderTahlil();
      return;
    }

    if (target.dataset.shortSurah) {
      state.selectedShortSurahId = target.dataset.shortSurah;
      renderShortSurahs();
      return;
    }

    if (target.dataset.dhikr) {
      const preset = DATA.dhikrPresets.find((d) => d.id === target.dataset.dhikr);
      if (preset) {
        state.tasbih.selectedDhikrId = preset.id;
        state.tasbih.currentCount = 0;
        state.tasbih.targetCount = preset.defaultTarget;
        saveJson(STORAGE_KEYS.tasbih, state.tasbih);
        renderTasbih();
      }
      return;
    }

    if (target.id === "btn-tasbih-tap") {
      triggerHaptic();
      const { currentCount, targetCount } = state.tasbih;
      state.tasbih.currentCount = targetCount > 0 && currentCount >= targetCount ? 1 : currentCount + 1;
      state.tasbih.lifetimeCount += 1;
      saveJson(STORAGE_KEYS.tasbih, state.tasbih);
      renderTasbih();
      return;
    }

    if (target.dataset.tasbihTarget !== undefined) {
      state.tasbih.targetCount = Number(target.dataset.tasbihTarget);
      saveJson(STORAGE_KEYS.tasbih, state.tasbih);
      renderTasbih();
      return;
    }

    if (target.id === "btn-tasbih-reset") {
      state.tasbih.currentCount = 0;
      saveJson(STORAGE_KEYS.tasbih, state.tasbih);
      renderTasbih();
      return;
    }

    if (target.id === "btn-add-arwah") {
      openArwahModal(null);
      return;
    }

    if (target.dataset.editArwah) {
      const entry = state.arwah.find((a) => a.id === target.dataset.editArwah);
      if (entry) openArwahModal(entry);
      return;
    }

    if (target.dataset.deleteArwah) {
      state.arwah = state.arwah.filter((a) => a.id !== target.dataset.deleteArwah);
      saveJson(STORAGE_KEYS.arwah, state.arwah);
      showToast("Nama arwah dihapus.");
      renderArwah();
    }
  });

  el.appMain.addEventListener("change", (e) => {
    const input = e.target;
    if (input.dataset && input.dataset.toggleArwah) {
      const entry = state.arwah.find((a) => a.id === input.dataset.toggleArwah);
      if (entry) {
        entry.includedInPrayer = input.checked;
        saveJson(STORAGE_KEYS.arwah, state.arwah);
        renderArwah();
      }
    }
  });

  // Top & bottom navigation
  document.querySelectorAll("[data-tab]").forEach((btn) => {
    btn.addEventListener("click", () => setTab(btn.dataset.tab));
  });

  el.nightToggleBtn.addEventListener("click", () => {
    state.settings.nightMode = !state.settings.nightMode;
    saveJson(STORAGE_KEYS.settings, state.settings);
    applyTheme();
  });

  document.getElementById("btn-open-settings").addEventListener("click", openSettingsModal);
  document.getElementById("btn-close-settings").addEventListener("click", () => el.settingsModal.close());

  document.getElementById("setting-font-size").addEventListener("input", (e) => {
    state.settings.arabicFontSize = Number(e.target.value);
    document.getElementById("setting-font-value").textContent = `${state.settings.arabicFontSize} px`;
    saveJson(STORAGE_KEYS.settings, state.settings);
    applyTheme();
  });

  document.getElementById("setting-show-latin").addEventListener("change", (e) => {
    state.settings.showLatin = e.target.checked;
    saveJson(STORAGE_KEYS.settings, state.settings);
    renderActiveTab();
  });

  document.getElementById("setting-show-translation").addEventListener("change", (e) => {
    state.settings.showTranslation = e.target.checked;
    saveJson(STORAGE_KEYS.settings, state.settings);
    renderActiveTab();
  });

  document.getElementById("setting-night-mode").addEventListener("change", (e) => {
    state.settings.nightMode = e.target.checked;
    saveJson(STORAGE_KEYS.settings, state.settings);
    applyTheme();
  });

  document.getElementById("setting-haptic").addEventListener("change", (e) => {
    state.settings.hapticEnabled = e.target.checked;
    saveJson(STORAGE_KEYS.settings, state.settings);
  });

  document.getElementById("btn-cancel-arwah").addEventListener("click", () => el.arwahModal.close());

  el.arwahForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const honorific = document.getElementById("arwah-honorific").value;
    const fullName = document.getElementById("arwah-fullname").value.trim();
    const parentName = document.getElementById("arwah-parent").value.trim();
    const relationNote = document.getElementById("arwah-note").value.trim();
    const parentPrefix = honorific === "Almarhumah" ? "binti" : "bin";

    if (!fullName) {
      showToast("Nama lengkap wajib diisi.");
      return;
    }

    if (state.editingArwahId) {
      const idx = state.arwah.findIndex((a) => a.id === state.editingArwahId);
      if (idx !== -1) {
        state.arwah[idx] = { ...state.arwah[idx], honorific, fullName, parentPrefix, parentName, relationNote };
      }
      showToast("Data arwah diperbarui.");
    } else {
      state.arwah.unshift({
        id: `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
        honorific,
        fullName,
        parentPrefix,
        parentName,
        relationNote,
        includedInPrayer: true
      });
      showToast("Nama arwah berhasil disimpan.");
    }

    saveJson(STORAGE_KEYS.arwah, state.arwah);
    el.arwahModal.close();
    renderActiveTab();
  });

  document.getElementById("btn-close-toc").addEventListener("click", () => el.tocModal.close());
  document.getElementById("toc-list").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-toc-step]");
    if (!btn) return;
    state.reading.lastTahlilStep = Number(btn.dataset.tocStep);
    saveJson(STORAGE_KEYS.reading, state.reading);
    el.tocModal.close();
    renderTahlil();
    window.scrollTo({ top: 0, behavior: "smooth" });
  });

  applyTheme();
  renderActiveTab();
})();
