package com.example.server

object WebRemoteHtmlBuilder {
    fun buildHtml(): String {
        return """<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Church Presentation - Web Remote Console</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
        body {
            background-color: #0B0F19;
            color: #E2E8F0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            user-select: none;
            -webkit-user-select: none;
            padding-bottom: 76px; /* Ruang untuk fixed bottom control bar */
        }

        /* Top Header */
        header {
            background-color: #131B2E;
            padding: 8px 16px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px solid #1E293B;
            position: sticky;
            top: 0;
            z-index: 100;
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .brand h1 {
            font-size: 14px;
            font-weight: 800;
            color: #FFFFFF;
            letter-spacing: 0.5px;
        }

        .brand-sub {
            font-size: 9px;
            color: #38BDF8;
            font-weight: 800;
            background: rgba(56, 189, 248, 0.15);
            padding: 2px 6px;
            border-radius: 4px;
            border: 1px solid rgba(56, 189, 248, 0.3);
        }

        .status-pill {
            display: flex;
            align-items: center;
            gap: 6px;
            background: #1E293B;
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: 600;
        }

        .dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #EF4444;
            transition: all 0.3s;
        }
        .dot.connected {
            background-color: #10B981;
            box-shadow: 0 0 8px #10B981;
        }

        .view-screen-btn {
            background: #3B82F6;
            color: #FFFFFF;
            text-decoration: none;
            font-size: 11px;
            font-weight: 700;
            padding: 5px 10px;
            border-radius: 6px;
            border: 1px solid #2563EB;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }

        /* Main Container */
        main {
            flex: 1;
            padding: 12px;
            max-width: 1000px;
            width: 100%;
            margin: 0 auto;
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        /* TOP DECK: 2-COLUMN SPLIT (SCREEN LIVE KIRI, DAFTAR SLIDE KANAN) */
        .top-deck-grid {
            display: grid;
            grid-template-columns: 1.15fr 0.85fr;
            gap: 12px;
        }

        @media (max-width: 680px) {
            .top-deck-grid {
                grid-template-columns: 1fr;
                gap: 10px;
            }
        }

        /* Live Program Monitor Box (KIRI) */
        .live-card {
            background: #131B2E;
            border: 1px solid #1E293B;
            border-radius: 12px;
            padding: 12px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .badge-group {
            display: flex;
            gap: 6px;
            align-items: center;
            flex-wrap: wrap;
        }

        .badge {
            font-size: 10px;
            font-weight: 800;
            padding: 3px 7px;
            border-radius: 4px;
            letter-spacing: 0.5px;
        }
        .badge-live { background: #DC2626; color: #FFFFFF; animation: pulse 2s infinite; }
        .badge-black { background: #475569; color: #F8FAFC; }
        .badge-type { background: #334155; color: #94A3B8; }
        .badge-bg { background: #065F46; color: #34D399; }

        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.7; }
        }

        .song-title {
            font-size: 15px;
            font-weight: 800;
            color: #FFFFFF;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .slide-indicator {
            font-size: 11px;
            color: #94A3B8;
            font-weight: 600;
        }

        /* Visual 16:9 Screen Simulation */
        .stage-preview-screen {
            position: relative;
            width: 100%;
            aspect-ratio: 16 / 9;
            background: #000000;
            border-radius: 8px;
            border: 1px solid #334155;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
        }

        .stage-bg-layer {
            position: absolute;
            inset: 0;
            background-size: cover;
            background-position: center;
            background-repeat: no-repeat;
            opacity: 0.85;
            z-index: 1;
            transition: all 0.3s ease;
        }

        .stage-text-layer {
            position: relative;
            z-index: 2;
            width: 100%;
            height: 100%;
            display: flex;
            flex-direction: column;
            padding: 10px;
            box-sizing: border-box;
            transition: all 0.2s ease;
        }

        .stage-text-box {
            background: rgba(0, 0, 0, 0.45);
            color: #FFFFFF;
            padding: 6px 10px;
            border-radius: 6px;
            font-size: 13px;
            line-height: 1.35;
            white-space: pre-wrap;
            word-break: break-word;
            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8);
            font-weight: 700;
        }

        /* Position classes for live preview */
        .pos-center { justify-content: center; align-items: center; text-align: center; }
        .pos-lower-third { justify-content: flex-end; align-items: center; text-align: center; }
        .pos-top { justify-content: flex-start; align-items: center; text-align: center; }
        .pos-bottom { justify-content: flex-end; align-items: flex-start; text-align: left; }

        /* Confidence Next Preview */
        .next-preview-box {
            background: #0F172A;
            border-left: 3px solid #10B981;
            padding: 8px 10px;
            border-radius: 6px;
            font-size: 11px;
            color: #CBD5E1;
        }
        .next-preview-label {
            font-size: 9px;
            font-weight: 800;
            color: #10B981;
            margin-bottom: 2px;
        }

        /* Slide Matrix Card (KANAN) */
        .slide-matrix-card {
            background: #131B2E;
            border: 1px solid #1E293B;
            border-radius: 12px;
            padding: 12px;
            display: flex;
            flex-direction: column;
            gap: 8px;
            min-height: 240px;
        }

        .matrix-header {
            font-size: 12px;
            font-weight: 800;
            color: #38BDF8;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .matrix-count-badge {
            background: #1E293B;
            color: #94A3B8;
            font-size: 10px;
            font-weight: 700;
            padding: 2px 6px;
            border-radius: 4px;
        }

        .slide-list {
            display: flex;
            flex-direction: column;
            gap: 6px;
            max-height: 240px;
            overflow-y: auto;
            padding-right: 2px;
        }
        .slide-list::-webkit-scrollbar { width: 4px; }
        .slide-list::-webkit-scrollbar-thumb { background: #334155; border-radius: 4px; }

        .slide-item {
            background: #1E293B;
            border: 1px solid #334155;
            border-radius: 8px;
            padding: 8px 10px;
            cursor: pointer;
            display: flex;
            gap: 8px;
            align-items: flex-start;
            transition: all 0.15s;
        }
        .slide-item:active { transform: scale(0.98); }
        .slide-item.active {
            background: #1E3A8A;
            border-color: #60A5FA;
            box-shadow: 0 0 10px rgba(96, 165, 250, 0.3);
        }

        .slide-num {
            background: #0F172A;
            color: #FFFFFF;
            font-size: 10px;
            font-weight: 800;
            padding: 2px 6px;
            border-radius: 4px;
            flex-shrink: 0;
        }
        .slide-item.active .slide-num {
            background: #60A5FA;
            color: #0F172A;
        }

        .slide-text {
            font-size: 12px;
            color: #CBD5E1;
            white-space: pre-wrap;
            line-height: 1.35;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }
        .slide-item.active .slide-text {
            color: #FFFFFF;
            font-weight: 700;
        }

        /* BOTTOM DECK: UNIFIED CONTROL HUB (LAGU / ALKITAB / BACKGROUND / STYLE DALAM 1 LAYAR) */
        .control-hub-card {
            background: #131B2E;
            border: 1px solid #1E293B;
            border-radius: 12px;
            padding: 12px;
            display: flex;
            flex-direction: column;
            gap: 12px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
        }

        /* Segmented Selector for Bottom Hub */
        .hub-nav {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            background: #0F172A;
            border-radius: 8px;
            padding: 3px;
            border: 1px solid #1E293B;
            gap: 3px;
        }

        .hub-nav-btn {
            background: transparent;
            border: none;
            color: #94A3B8;
            font-size: 11px;
            font-weight: 700;
            padding: 8px 4px;
            border-radius: 6px;
            cursor: pointer;
            text-align: center;
            transition: all 0.15s;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
            white-space: nowrap;
        }
        .hub-nav-btn.active {
            background: #3B82F6;
            color: #FFFFFF;
            box-shadow: 0 2px 6px rgba(59, 130, 246, 0.4);
        }

        .hub-panel {
            display: none;
            flex-direction: column;
            gap: 10px;
        }
        .hub-panel.active {
            display: flex;
        }

        /* Form Inputs & List Items */
        .search-input {
            width: 100%;
            background: #0F172A;
            border: 1px solid #334155;
            color: #FFFFFF;
            padding: 9px 12px;
            border-radius: 8px;
            font-size: 12px;
            outline: none;
        }
        .search-input:focus { border-color: #38BDF8; }

        .item-card {
            background: #1E293B;
            border: 1px solid #334155;
            border-radius: 8px;
            padding: 10px 12px;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .item-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .item-title {
            font-size: 13px;
            font-weight: 700;
            color: #FFFFFF;
        }

        .btn-action-row {
            display: flex;
            gap: 6px;
            flex-wrap: wrap;
        }

        .btn-chip {
            padding: 6px 10px;
            border-radius: 6px;
            font-size: 11px;
            font-weight: 700;
            cursor: pointer;
            border: none;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            transition: all 0.12s;
        }
        .btn-chip:active { transform: scale(0.96); }

        .btn-chip-live { background: #10B981; color: #FFFFFF; }
        .btn-chip-bg { background: #8B5CF6; color: #FFFFFF; }
        .btn-chip-sec { background: #334155; color: #E2E8F0; }

        .chips-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
        }

        /* Toggle buttons and sliders */
        .toggle-btn {
            background: #1E293B;
            border: 1px solid #334155;
            color: #CBD5E1;
            padding: 9px 6px;
            border-radius: 8px;
            font-size: 11px;
            font-weight: 700;
            cursor: pointer;
            text-align: center;
            transition: all 0.15s;
        }
        .toggle-btn.active {
            background: #38BDF8;
            color: #0F172A;
            border-color: #0284C7;
            box-shadow: 0 0 8px rgba(56, 189, 248, 0.3);
        }

        .slider-group {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }
        .slider-row {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .slider-row input[type=range] {
            flex: 1;
            accent-color: #38BDF8;
        }

        /* FIXED BOTTOM QUICK CONTROL BAR */
        .fixed-bar {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            background: #0F172A;
            border-top: 1px solid #1E293B;
            padding: 8px 12px;
            display: flex;
            gap: 8px;
            z-index: 100;
            box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.5);
            max-width: 1000px;
            margin: 0 auto;
        }

        .fixed-btn {
            flex: 1;
            padding: 12px 6px;
            border-radius: 8px;
            border: none;
            font-size: 13px;
            font-weight: 800;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 5px;
            transition: all 0.12s;
        }
        .fixed-btn:active { transform: scale(0.96); }

        .fixed-btn-prev { background: #1E293B; color: #FFFFFF; border: 1px solid #334155; }
        .fixed-btn-next { background: #10B981; color: #FFFFFF; box-shadow: 0 0 10px rgba(16, 185, 129, 0.3); }
        .fixed-btn-black { background: #334155; color: #F1F5F9; }
        .fixed-btn-clear { background: #334155; color: #F1F5F9; }

        .fixed-btn-black.active { background: #EF4444; color: #FFFFFF; box-shadow: 0 0 10px rgba(239, 68, 68, 0.4); }
        .fixed-btn-clear.active { background: #F59E0B; color: #FFFFFF; box-shadow: 0 0 10px rgba(245, 158, 11, 0.4); }
    </style>
</head>
<body>
    <!-- TOP APP HEADER -->
    <header>
        <div class="brand">
            <span style="font-size: 17px;">📱</span>
            <div>
                <h1>REMOTE CONSOLE</h1>
            </div>
            <span class="brand-sub">CONTROL</span>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
            <div class="status-pill">
                <div id="conn-dot" class="dot"></div>
                <span id="conn-text">Connecting...</span>
            </div>
            <a href="/" target="_blank" class="view-screen-btn">📺 Layar</a>
        </div>
    </header>

    <main>
        <!-- TOP DECK: SCREEN LIVE (KIRI) & DAFTAR SLIDE (KANAN) -->
        <div class="top-deck-grid">
            <!-- LIVE STAGE PREVIEW (KIRI) -->
            <div class="live-card">
                <div class="card-header">
                    <div class="badge-group">
                        <span id="badge-status" class="badge badge-live">LIVE ON AIR</span>
                        <span id="badge-type" class="badge badge-type">LYRICS</span>
                    </div>
                    <span id="badge-bg" class="badge badge-bg">BG: NONE</span>
                </div>

                <div style="display: flex; justify-content: space-between; align-items: baseline;">
                    <h2 id="song-title" class="song-title">Memuat Tayangan...</h2>
                    <div id="slide-indicator" class="slide-indicator">Slide 0/0</div>
                </div>

                <!-- SIMULATED 16:9 LIVE MONITOR SCREEN -->
                <div class="stage-preview-screen" id="preview-screen-box">
                    <div class="stage-bg-layer" id="preview-bg-layer"></div>
                    <div class="stage-text-layer pos-center" id="preview-text-layer">
                        <div class="stage-text-box" id="preview-text-box">Siap Menampilkan</div>
                    </div>
                </div>

                <!-- CONFIDENCE MONITOR -->
                <div id="next-box" class="next-preview-box" style="display: none;">
                    <div class="next-preview-label">⏭ BERIKUTNYA:</div>
                    <div id="next-text">Lirik slide selanjutnya...</div>
                </div>
            </div>

            <!-- INTERACTIVE SLIDE MATRIX (KANAN) -->
            <div class="slide-matrix-card">
                <div class="matrix-header">
                    <span>DAFTAR SLIDE AKTIF</span>
                    <span id="matrix-count" class="matrix-count-badge">0 slides</span>
                </div>
                <div id="slide-list" class="slide-list">
                    <div style="color: #64748B; font-size: 11px; text-align: center; padding: 20px;">Belum ada slide aktif</div>
                </div>
            </div>
        </div>

        <!-- BOTTOM DECK: UNIFIED CONTROL HUB (LAGU / ALKITAB / BACKGROUND / FORMAT DALAM 1 LAYAR) -->
        <div class="control-hub-card">
            <!-- SEGMENTED TABS -->
            <div class="hub-nav">
                <button class="hub-nav-btn active" onclick="switchHubPanel('songs')">🎵 Lagu</button>
                <button class="hub-nav-btn" onclick="switchHubPanel('bible')">📖 Alkitab</button>
                <button class="hub-nav-btn" onclick="switchHubPanel('background')">🖼 Background</button>
                <button class="hub-nav-btn" onclick="switchHubPanel('style')">🎨 Format</button>
            </div>

            <!-- PANEL 1: LAGU & LIRIK -->
            <div id="hub-songs" class="hub-panel active">
                <div style="display: flex; gap: 8px; align-items: center;">
                    <input type="text" id="song-search-input" class="search-input" placeholder="🔍 Cari judul lagu / lirik..." oninput="filterSongs()">
                    <button class="btn-chip btn-chip-live" style="white-space: nowrap; padding: 9px 12px;" onclick="toggleAddSongForm()">+ Lagu</button>
                </div>

                <!-- QUICK ADD SONG FORM -->
                <div id="add-song-form" style="display: none; background: #0F172A; padding: 10px; border-radius: 8px; border: 1px solid #334155;">
                    <div style="font-size: 11px; font-weight: 800; color: #38BDF8; margin-bottom: 6px;">TAMBAH LAGU BARU</div>
                    <input type="text" id="new-song-title" class="search-input" placeholder="Judul Lagu..." style="margin-bottom: 6px;">
                    <textarea id="new-song-lyrics" class="search-input" rows="3" placeholder="Ketik lirik (pisahkan bait dengan 1 baris kosong)..." style="margin-bottom: 6px;"></textarea>
                    <div style="display: flex; gap: 6px;">
                        <button class="btn-chip btn-chip-live" onclick="submitNewSong()">💾 Simpan & Tayangkan</button>
                        <button class="btn-chip btn-chip-sec" onclick="toggleAddSongForm()">Batal</button>
                    </div>
                </div>

                <div id="songs-list-container" style="display: flex; flex-direction: column; gap: 6px; max-height: 220px; overflow-y: auto;">
                    <!-- Dynamic songs rendered here -->
                </div>
            </div>

            <!-- PANEL 2: ALKITAB & SCRIPTURE -->
            <div id="hub-bible" class="hub-panel">
                <div style="font-size: 11px; font-weight: 800; color: #94A3B8;">AYAT POPULER (1-KLIK GO LIVE)</div>
                <div id="bible-quick-chips" class="chips-grid">
                    <!-- Dynamic Bible chips rendered here -->
                </div>

                <div style="font-size: 11px; font-weight: 800; color: #94A3B8; margin-top: 4px;">KUTIPAN AYAT / PENGUMUMAN CUSTOM</div>
                <input type="text" id="bible-custom-title" class="search-input" placeholder="Kitab & Pasal (misal: Yohanes 3:16)...">
                <textarea id="bible-custom-text" class="search-input" rows="2" placeholder="Teks Ayat Alkitab..."></textarea>
                <button class="btn-chip btn-chip-live" style="padding: 9px; font-size: 12px; justify-content: center;" onclick="submitCustomBible()">🚀 TAYANGKAN AYAT KE LAYAR</button>
            </div>

            <!-- PANEL 3: BACKGROUND & MEDIA -->
            <div id="hub-background" class="hub-panel">
                <!-- ACTIVE BACKGROUND STATUS -->
                <div style="background: #18152E; border: 1px solid #8B5CF6; padding: 8px 10px; border-radius: 8px; display: flex; justify-content: space-between; align-items: center;">
                    <div>
                        <div style="font-size: 9px; font-weight: 800; color: #C4B5FD;">BACKGROUND AKTIF:</div>
                        <div id="bg-active-desc" style="font-size: 12px; font-weight: 700; color: #FFFFFF;">⚪ Standar Gelap</div>
                    </div>
                    <button class="btn-chip" style="background: #EF4444; color: #FFFFFF; font-size: 10px;" onclick="clearBackground()">❌ Hapus BG</button>
                </div>

                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span style="font-size: 11px; font-weight: 800; color: #94A3B8;">SUMBER BACKGROUND</span>
                    <button class="btn-chip btn-chip-bg" style="font-size: 10px; padding: 4px 8px;" onclick="toggleAddDroidCamForm()">+ DroidCam IP</button>
                </div>

                <!-- ADD DROIDCAM FORM -->
                <div id="add-droidcam-form" style="display: none; background: #0F172A; padding: 10px; border-radius: 8px; border: 1px solid #8B5CF6;">
                    <div style="font-size: 11px; font-weight: 800; color: #C4B5FD; margin-bottom: 6px;">DROIDCAM / IP CAMERA</div>
                    <input type="text" id="droid-name" class="search-input" placeholder="Nama Kamera (misal: HP Depan)..." style="margin-bottom: 6px;">
                    <input type="text" id="droid-ip" class="search-input" placeholder="IP HP (contoh: 192.168.1.50)..." style="margin-bottom: 6px;">
                    <input type="text" id="droid-port" class="search-input" placeholder="Port (default: 4747)..." value="4747" style="margin-bottom: 6px;">
                    <div style="display: flex; gap: 6px;">
                        <button class="btn-chip btn-chip-bg" onclick="submitDroidCam()">💾 Simpan & Set BG</button>
                        <button class="btn-chip btn-chip-sec" onclick="toggleAddDroidCamForm()">Batal</button>
                    </div>
                </div>

                <div style="display: flex; flex-direction: column; gap: 6px; max-height: 200px; overflow-y: auto;">
                    <!-- DEFAULT -->
                    <div class="item-card" style="padding: 8px 10px;">
                        <div class="item-header">
                            <div class="item-title">⚪ Standar Gelap (Clean)</div>
                            <button class="btn-chip btn-chip-sec" onclick="clearBackground()">Set</button>
                        </div>
                    </div>

                    <!-- LOCAL CAMERA -->
                    <div class="item-card" style="padding: 8px 10px;">
                        <div class="item-header">
                            <div class="item-title">📷 Kamera HP Utama (Local Cam)</div>
                            <button class="btn-chip btn-chip-bg" onclick="sendAction('set_bg', { bgType: 'CAMERA' })">🖼 Set BG</button>
                        </div>
                    </div>

                    <!-- MEDIA LIBRARY ITEMS -->
                    <div id="media-library-items" style="display: flex; flex-direction: column; gap: 6px;">
                        <!-- Dynamic media items -->
                    </div>
                </div>
            </div>

            <!-- PANEL 4: FORMAT & STYLE -->
            <div id="hub-style" class="hub-panel">
                <div style="font-size: 11px; font-weight: 800; color: #94A3B8;">1-KLIK PRESET TEMA</div>
                <div class="chips-grid">
                    <button class="toggle-btn" onclick="sendAction('set_preset', { preset: 'WORSHIP' })">🌟 Worship (Center 32sp)</button>
                    <button class="toggle-btn" onclick="sendAction('set_preset', { preset: 'PRAISE' })">⚡ Praise (Center 36sp Bold)</button>
                    <button class="toggle-btn" onclick="sendAction('set_preset', { preset: 'SERMON' })">📖 Kotbah (Lower Third 24sp)</button>
                    <button class="toggle-btn" onclick="sendAction('set_preset', { preset: 'MINIMALIST' })">✨ Minimalist (Lower Third)</button>
                </div>

                <!-- FONT SIZE -->
                <div class="slider-group" style="margin-top: 4px;">
                    <div style="display: flex; justify-content: space-between; font-size: 11px; font-weight: 700;">
                        <span>Ukuran Font:</span>
                        <span id="font-size-val" style="color: #38BDF8;">32 sp</span>
                    </div>
                    <div class="slider-row">
                        <button class="btn-chip btn-chip-sec" onclick="adjustFontSize(-2)">-</button>
                        <input type="range" id="font-size-slider" min="16" max="64" value="32" oninput="onFontSizeChange(this.value)">
                        <button class="btn-chip btn-chip-sec" onclick="adjustFontSize(2)">+</button>
                    </div>
                </div>

                <!-- POSITION -->
                <div style="margin-top: 4px;">
                    <div style="font-size: 11px; font-weight: 700; margin-bottom: 4px;">Posisi Teks:</div>
                    <div class="chips-grid">
                        <button id="pos-btn-CENTER" class="toggle-btn active" onclick="updatePosition('CENTER')">Tengah (Center)</button>
                        <button id="pos-btn-LOWER_THIRD" class="toggle-btn" onclick="updatePosition('LOWER_THIRD')">Lower Third</button>
                        <button id="pos-btn-TOP" class="toggle-btn" onclick="updatePosition('TOP')">Atas (Top Banner)</button>
                        <button id="pos-btn-BOTTOM" class="toggle-btn" onclick="updatePosition('BOTTOM')">Bawah Kiri</button>
                    </div>
                </div>

                <!-- ALIGNMENT & TOGGLES -->
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 4px;">
                    <div>
                        <div style="font-size: 11px; font-weight: 700; margin-bottom: 4px;">Perataan Teks:</div>
                        <div style="display: flex; gap: 4px;">
                            <button id="align-btn-left" class="toggle-btn" style="flex:1" onclick="updateAlignment('LEFT')">Kiri</button>
                            <button id="align-btn-center" class="toggle-btn active" style="flex:1" onclick="updateAlignment('CENTER')">Tengah</button>
                            <button id="align-btn-right" class="toggle-btn" style="flex:1" onclick="updateAlignment('RIGHT')">Kanan</button>
                        </div>
                    </div>
                    <div>
                        <div style="font-size: 11px; font-weight: 700; margin-bottom: 4px;">Efek:</div>
                        <div style="display: flex; gap: 4px;">
                            <button id="btn-toggle-bold" class="toggle-btn active" style="flex:1" onclick="toggleBold()">Tebal</button>
                            <button id="btn-toggle-shadow" class="toggle-btn active" style="flex:1" onclick="toggleShadow()">Bayangan</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <!-- FIXED BOTTOM QUICK CONTROL BAR (TETAP SATU, TIDAK DOUBLE) -->
    <div class="fixed-bar">
        <button class="fixed-btn fixed-btn-prev" onclick="sendAction('prev')">⏮ PREV</button>
        <button class="fixed-btn fixed-btn-next" onclick="sendAction('next')">NEXT ⏭</button>
        <button id="fixed-black" class="fixed-btn fixed-btn-black" onclick="sendAction('black')">⚫ BLACK</button>
        <button id="fixed-clear" class="fixed-btn fixed-btn-clear" onclick="sendAction('clear')">🔲 CLEAR</button>
    </div>

    <script>
        // State & DOM Elements
        let ws = null;
        let isConnected = false;
        let pollTimer = null;
        let lastState = null;
        let libraryData = { songs: [], bible: [], media: [] };

        const connDot = document.getElementById('conn-dot');
        const connText = document.getElementById('conn-text');
        const badgeStatus = document.getElementById('badge-status');
        const badgeType = document.getElementById('badge-type');
        const badgeBg = document.getElementById('badge-bg');
        const songTitle = document.getElementById('song-title');
        const slideIndicator = document.getElementById('slide-indicator');
        const previewBgLayer = document.getElementById('preview-bg-layer');
        const previewTextLayer = document.getElementById('preview-text-layer');
        const previewTextBox = document.getElementById('preview-text-box');
        const nextBox = document.getElementById('next-box');
        const nextText = document.getElementById('next-text');
        const fixedBlack = document.getElementById('fixed-black');
        const fixedClear = document.getElementById('fixed-clear');
        const slideListContainer = document.getElementById('slide-list');
        const matrixCount = document.getElementById('matrix-count');
        const bgActiveDesc = document.getElementById('bg-active-desc');

        // Hub Switching on main single-screen
        function switchHubPanel(panelId) {
            document.querySelectorAll('.hub-nav-btn').forEach(btn => btn.classList.remove('active'));
            document.querySelectorAll('.hub-panel').forEach(p => p.classList.remove('active'));

            const targetBtn = Array.from(document.querySelectorAll('.hub-nav-btn')).find(b => b.getAttribute('onclick').includes(panelId));
            if (targetBtn) targetBtn.classList.add('active');

            const targetPanel = document.getElementById('hub-' + panelId);
            if (targetPanel) targetPanel.classList.add('active');
        }

        // WebSocket Connection
        function connectWebSocket() {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = protocol + '//' + window.location.host + '/ws';
            
            try {
                ws = new WebSocket(wsUrl);
            } catch (e) {
                startPolling();
                return;
            }

            ws.onopen = function() {
                isConnected = true;
                connDot.className = 'dot connected';
                connText.innerText = 'Live WS';
                stopPolling();
                loadLibrary();
            };

            ws.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    renderRemote(data);
                } catch (err) {}
            };

            ws.onclose = function() {
                isConnected = false;
                connDot.className = 'dot';
                connText.innerText = 'Reconnecting...';
                startPolling();
                setTimeout(connectWebSocket, 2000);
            };

            ws.onerror = function() {
                isConnected = false;
                try { ws.close(); } catch(e) {}
            };
        }

        function fetchStateHttp() {
            fetch('/api/state')
                .then(res => res.json())
                .then(data => {
                    renderRemote(data);
                    if (!isConnected) {
                        connDot.className = 'dot connected';
                        connText.innerText = 'Polling Sync';
                    }
                })
                .catch(() => {
                    connDot.className = 'dot';
                    connText.innerText = 'Offline';
                });
        }

        function startPolling() {
            if (!pollTimer) {
                fetchStateHttp();
                pollTimer = setInterval(fetchStateHttp, 1200);
            }
        }

        function stopPolling() {
            if (pollTimer) {
                clearInterval(pollTimer);
                pollTimer = null;
            }
        }

        function sendAction(action, extraData = {}) {
            if (navigator.vibrate) navigator.vibrate(35);
            const payload = Object.assign({ action: action }, extraData);

            if (ws && ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify(payload));
            } else {
                let url = '/api/remote?action=' + encodeURIComponent(action);
                for (let k in extraData) {
                    url += '&' + encodeURIComponent(k) + '=' + encodeURIComponent(extraData[k]);
                }
                fetch(url).then(res => res.json()).then(renderRemote).catch(() => {});
            }
        }

        // Library Loader
        function loadLibrary() {
            fetch('/api/library')
                .then(res => res.json())
                .then(data => {
                    libraryData = data;
                    renderSongs();
                    renderBible();
                    renderMedia();
                })
                .catch(() => {});
        }

        function renderRemote(data) {
            if (!data) return;
            lastState = data;

            // Status Badges & Controls
            const status = data.status || 'IDLE';
            if (status === 'BLACK') {
                badgeStatus.className = 'badge badge-black';
                badgeStatus.innerText = 'BLACKOUT ON';
                fixedBlack.classList.add('active');
            } else {
                badgeStatus.className = 'badge badge-live';
                badgeStatus.innerText = 'LIVE ON AIR';
                fixedBlack.classList.remove('active');
            }

            if (status === 'CLEAR') {
                fixedClear.classList.add('active');
            } else {
                fixedClear.classList.remove('active');
            }

            badgeType.innerText = data.contentType || 'IDLE';

            // Background Indicator & Preview
            const bgType = data.backgroundType || 'NONE';
            badgeBg.innerText = 'BG: ' + bgType;
            
            if (bgType === 'NONE') {
                bgActiveDesc.innerText = '⚪ Tanpa Background (Gelap Bersih)';
                previewBgLayer.style.backgroundImage = 'none';
                previewBgLayer.style.backgroundColor = '#000000';
            } else if (bgType === 'CAMERA') {
                bgActiveDesc.innerText = '📷 Kamera HP Utama (Local Cam)';
                previewBgLayer.style.backgroundImage = 'linear-gradient(45deg, #1E1B4B, #312E81)';
            } else if (bgType === 'IP_CAMERA') {
                bgActiveDesc.innerText = '📱 DroidCam (' + (data.backgroundMediaUrl || '') + ')';
                previewBgLayer.style.backgroundImage = 'linear-gradient(45deg, #3730A3, #1E1B4B)';
            } else if (bgType === 'IMAGE') {
                bgActiveDesc.innerText = '🖼 Gambar Background';
                previewBgLayer.style.backgroundImage = data.backgroundMediaUrl ? 'url(' + data.backgroundMediaUrl + ')' : 'none';
            } else if (bgType === 'VIDEO') {
                bgActiveDesc.innerText = '🎥 Video Background';
                previewBgLayer.style.backgroundImage = 'linear-gradient(45deg, #064E3B, #065F46)';
            }

            // Title & Slide Indices
            songTitle.innerText = data.title || 'Church Presentation System';
            const total = data.totalSlides || 0;
            const currentIdx = data.slideIndex || 0;
            slideIndicator.innerText = total > 0 ? ('Slide ' + (currentIdx + 1) + '/' + total) : 'Standby';

            // Text Screen Simulation
            if (status === 'BLACK') {
                previewTextBox.innerText = '⚫ LAYAR HITAM (BLACKOUT)';
                previewTextBox.style.color = '#EF4444';
            } else if (status === 'CLEAR') {
                previewTextBox.innerText = '🔲 TEKS DISEMBUNYIKAN (CLEAR)';
                previewTextBox.style.color = '#F59E0B';
            } else {
                previewTextBox.innerText = data.text || 'Siap Menampilkan';
                previewTextBox.style.color = data.textColor || '#FFFFFF';
            }

            // Position & Alignment Preview
            const pos = (data.position || 'CENTER').toLowerCase().replace('_', '-');
            previewTextLayer.className = 'stage-text-layer pos-' + pos;
            previewTextBox.style.textAlign = data.textAlign || 'center';

            // Next Slide Preview
            if (data.nextText && data.nextText.trim() !== '') {
                nextBox.style.display = 'block';
                nextText.innerText = data.nextText;
            } else {
                nextBox.style.display = 'none';
            }

            // Slides Matrix List
            const slides = data.slides || [];
            matrixCount.innerText = slides.length + ' slides';
            slideListContainer.innerHTML = '';

            if (slides.length === 0) {
                slideListContainer.innerHTML = '<div style="color: #64748B; font-size: 11px; text-align: center; padding: 20px;">Belum ada slide aktif</div>';
            } else {
                slides.forEach((slideContent, idx) => {
                    const itemDiv = document.createElement('div');
                    itemDiv.className = 'slide-item' + (idx === currentIdx ? ' active' : '');
                    itemDiv.onclick = () => sendAction('jump', { slide: idx });

                    const numSpan = document.createElement('span');
                    numSpan.className = 'slide-num';
                    numSpan.innerText = '#' + (idx + 1);

                    const textSpan = document.createElement('span');
                    textSpan.className = 'slide-text';
                    textSpan.innerText = slideContent;

                    itemDiv.appendChild(numSpan);
                    itemDiv.appendChild(textSpan);
                    slideListContainer.appendChild(itemDiv);
                });
            }
        }

        // Render Songs List in Hub Panel
        function renderSongs() {
            const container = document.getElementById('songs-list-container');
            container.innerHTML = '';
            const query = (document.getElementById('song-search-input').value || '').toLowerCase();
            const songs = (libraryData.songs || []).filter(s => s.title.toLowerCase().includes(query) || s.slides.some(v => v.toLowerCase().includes(query)));

            if (songs.length === 0) {
                container.innerHTML = '<div style="color: #64748B; font-size: 11px; text-align: center; padding: 12px;">Tidak ada lagu ditemukan</div>';
                return;
            }

            songs.forEach(song => {
                const card = document.createElement('div');
                card.className = 'item-card';

                const header = document.createElement('div');
                header.className = 'item-header';
                header.innerHTML = '<div class="item-title">🎵 ' + song.title + '</div>';

                const btnRow = document.createElement('div');
                btnRow.className = 'btn-action-row';

                const goLiveBtn = document.createElement('button');
                goLiveBtn.className = 'btn-chip btn-chip-live';
                goLiveBtn.innerHTML = '▶ TAYANGKAN';
                goLiveBtn.onclick = () => {
                    sendAction('go_song', { songId: song.id, slide: 0 });
                };
                btnRow.appendChild(goLiveBtn);

                // Slides buttons
                song.slides.forEach((sl, idx) => {
                    const slideBtn = document.createElement('button');
                    slideBtn.className = 'btn-chip btn-chip-sec';
                    slideBtn.innerText = 'Bait ' + (idx + 1);
                    slideBtn.onclick = () => {
                        sendAction('go_song', { songId: song.id, slide: idx });
                    };
                    btnRow.appendChild(slideBtn);
                });

                card.appendChild(header);
                card.appendChild(btnRow);
                container.appendChild(card);
            });
        }

        function filterSongs() {
            renderSongs();
        }

        function toggleAddSongForm() {
            const form = document.getElementById('add-song-form');
            form.style.display = form.style.display === 'none' ? 'block' : 'none';
        }

        function submitNewSong() {
            const title = document.getElementById('new-song-title').value.trim();
            const lyrics = document.getElementById('new-song-lyrics').value.trim();
            if (!title || !lyrics) return;

            sendAction('add_song', { title: title, text: lyrics });
            sendAction('go_custom', { title: title, text: lyrics, type: 'LYRICS' });
            toggleAddSongForm();
            setTimeout(loadLibrary, 500);
        }

        // Render Bible Chips in Hub Panel
        function renderBible() {
            const container = document.getElementById('bible-quick-chips');
            container.innerHTML = '';
            const passages = libraryData.bible || [];

            passages.forEach(p => {
                const btn = document.createElement('button');
                btn.className = 'toggle-btn';
                btn.innerText = '📖 ' + p.title;
                btn.onclick = () => {
                    sendAction('go_bible', { bibleId: p.id, verse: 0 });
                };
                container.appendChild(btn);
            });
        }

        function submitCustomBible() {
            const title = document.getElementById('bible-custom-title').value.trim();
            const text = document.getElementById('bible-custom-text').value.trim();
            if (!text) return;
            sendAction('go_custom', { title: title || 'Alkitab', text: text, type: 'BIBLE' });
        }

        // Render Media Items in Hub Panel
        function renderMedia() {
            const container = document.getElementById('media-library-items');
            container.innerHTML = '';
            const mediaList = libraryData.media || [];

            mediaList.forEach(m => {
                const card = document.createElement('div');
                card.className = 'item-card';
                card.style.padding = '8px 10px';

                const header = document.createElement('div');
                header.className = 'item-header';
                const icon = m.type === 'IP_CAMERA' ? '📱' : (m.type === 'IMAGE' ? '🖼' : '🎥');
                header.innerHTML = '<div class="item-title">' + icon + ' ' + m.title + '</div>';

                const btnRow = document.createElement('div');
                btnRow.className = 'btn-action-row';

                const setBgBtn = document.createElement('button');
                setBgBtn.className = 'btn-chip btn-chip-bg';
                setBgBtn.innerText = '🖼 Set BG';
                setBgBtn.onclick = () => sendAction('set_bg_media', { mediaId: m.id });

                const goLiveBtn = document.createElement('button');
                goLiveBtn.className = 'btn-chip btn-chip-live';
                goLiveBtn.innerText = '▶ Go Live';
                goLiveBtn.onclick = () => {
                    sendAction('go_media', { mediaId: m.id });
                };

                btnRow.appendChild(setBgBtn);
                btnRow.appendChild(goLiveBtn);
                card.appendChild(header);
                card.appendChild(btnRow);
                container.appendChild(card);
            });
        }

        function clearBackground() {
            sendAction('set_bg', { bgType: 'NONE' });
        }

        function toggleAddDroidCamForm() {
            const form = document.getElementById('add-droidcam-form');
            form.style.display = form.style.display === 'none' ? 'block' : 'none';
        }

        function submitDroidCam() {
            const name = document.getElementById('droid-name').value.trim();
            const ip = document.getElementById('droid-ip').value.trim();
            const port = document.getElementById('droid-port').value.trim() || '4747';
            if (!ip) return;

            sendAction('add_droidcam', { title: name || ('DroidCam ' + ip), ip: ip, port: port });
            sendAction('set_bg', { bgType: 'IP_CAMERA', url: 'http://' + ip + ':' + port + '/video' });
            toggleAddDroidCamForm();
            setTimeout(loadLibrary, 500);
        }

        // Style Settings Handlers
        function onFontSizeChange(val) {
            document.getElementById('font-size-val').innerText = val + ' sp';
            sendAction('update_style', { fontSize: parseInt(val) });
        }

        function adjustFontSize(delta) {
            const slider = document.getElementById('font-size-slider');
            let current = parseInt(slider.value) + delta;
            if (current < 16) current = 16;
            if (current > 64) current = 64;
            slider.value = current;
            onFontSizeChange(current);
        }

        function updatePosition(pos) {
            ['CENTER', 'LOWER_THIRD', 'TOP', 'BOTTOM'].forEach(p => {
                const b = document.getElementById('pos-btn-' + p);
                if (b) b.className = p === pos ? 'toggle-btn active' : 'toggle-btn';
            });
            sendAction('update_style', { position: pos });
        }

        function updateAlignment(align) {
            ['left', 'center', 'right'].forEach(a => {
                const b = document.getElementById('align-btn-' + a);
                if (b) b.className = a === align.toLowerCase() ? 'toggle-btn active' : 'toggle-btn';
            });
            sendAction('update_style', { align: align });
        }

        let isBoldActive = true;
        function toggleBold() {
            isBoldActive = !isBoldActive;
            document.getElementById('btn-toggle-bold').className = isBoldActive ? 'toggle-btn active' : 'toggle-btn';
            sendAction('update_style', { isBold: isBoldActive });
        }

        let isShadowActive = true;
        function toggleShadow() {
            isShadowActive = !isShadowActive;
            document.getElementById('btn-toggle-shadow').className = isShadowActive ? 'toggle-btn active' : 'toggle-btn';
            sendAction('update_style', { isShadow: isShadowActive });
        }

        // Keyboard Shortcuts
        window.addEventListener('keydown', function(e) {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
            if (e.key === 'ArrowRight' || e.key === 'ArrowDown' || e.key === 'PageDown' || e.key === ' ') {
                e.preventDefault();
                sendAction('next');
            } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp' || e.key === 'PageUp') {
                e.preventDefault();
                sendAction('prev');
            } else if (e.key === 'b' || e.key === 'B') {
                e.preventDefault();
                sendAction('black');
            } else if (e.key === 'c' || e.key === 'C') {
                e.preventDefault();
                sendAction('clear');
            }
        });

        // Init
        fetchStateHttp();
        connectWebSocket();
        loadLibrary();
    </script>
</body>
</html>"""
    }
}
