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
            padding-bottom: 74px; /* Space for fixed bottom bar */
        }

        /* Top Header */
        header {
            background-color: #131B2E;
            padding: 10px 16px;
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
            font-size: 15px;
            font-weight: 800;
            color: #FFFFFF;
            letter-spacing: 0.5px;
        }

        .brand-sub {
            font-size: 10px;
            color: #94A3B8;
            font-weight: 600;
            background: #1E293B;
            padding: 2px 6px;
            border-radius: 4px;
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

        /* Navigation Tab Bar */
        .tab-bar {
            background: #0F172A;
            display: flex;
            overflow-x: auto;
            border-bottom: 1px solid #1E293B;
            scrollbar-width: none;
            position: sticky;
            top: 48px;
            z-index: 90;
        }
        .tab-bar::-webkit-scrollbar { display: none; }

        .tab-btn {
            flex: 1;
            min-width: 78px;
            padding: 10px 8px;
            text-align: center;
            background: transparent;
            border: none;
            color: #94A3B8;
            font-size: 12px;
            font-weight: 700;
            cursor: pointer;
            border-bottom: 3px solid transparent;
            transition: all 0.15s;
            white-space: nowrap;
        }
        .tab-btn.active {
            color: #38BDF8;
            border-bottom-color: #38BDF8;
            background: rgba(56, 189, 248, 0.08);
        }

        /* Main Container */
        main {
            flex: 1;
            padding: 14px;
            max-width: 720px;
            width: 100%;
            margin: 0 auto;
            display: flex;
            flex-direction: column;
            gap: 14px;
        }

        .tab-panel {
            display: none;
            flex-direction: column;
            gap: 14px;
        }
        .tab-panel.active {
            display: flex;
        }

        /* Live Program Monitor */
        .live-card {
            background: #131B2E;
            border: 1px solid #1E293B;
            border-radius: 12px;
            padding: 14px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
            position: relative;
            overflow: hidden;
        }

        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
        }

        .badge-group {
            display: flex;
            gap: 6px;
            align-items: center;
        }

        .badge {
            font-size: 10px;
            font-weight: 800;
            padding: 3px 8px;
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
            font-size: 17px;
            font-weight: 800;
            color: #FFFFFF;
            margin-bottom: 2px;
        }

        .slide-indicator {
            font-size: 11px;
            color: #94A3B8;
            font-weight: 600;
            margin-bottom: 10px;
        }

        /* Visual Mini Stage Screen Simulation */
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
            margin-bottom: 10px;
        }

        .stage-bg-layer {
            position: absolute;
            inset: 0;
            background-size: cover;
            background-position: center;
            background-repeat: no-repeat;
            opacity: 0.85;
            z-index: 1;
        }

        .stage-text-layer {
            position: relative;
            z-index: 2;
            width: 100%;
            height: 100%;
            display: flex;
            flex-direction: column;
            padding: 12px;
            box-sizing: border-box;
            transition: all 0.2s ease;
        }

        .stage-text-box {
            background: rgba(0, 0, 0, 0.4);
            color: #FFFFFF;
            padding: 8px 12px;
            border-radius: 6px;
            font-size: 15px;
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
            border-left: 4px solid #10B981;
            padding: 10px 12px;
            border-radius: 6px;
            font-size: 12px;
            color: #CBD5E1;
        }
        .next-preview-label {
            font-size: 10px;
            font-weight: 800;
            color: #10B981;
            margin-bottom: 2px;
        }

        /* Primary Nav Controls */
        .nav-controls {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
        }

        .btn-nav {
            padding: 16px 12px;
            border-radius: 10px;
            border: none;
            font-size: 16px;
            font-weight: 800;
            cursor: pointer;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 4px;
            transition: all 0.12s;
            box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
        }
        .btn-nav:active {
            transform: scale(0.96);
        }

        .btn-prev {
            background: #1E293B;
            color: #F1F5F9;
            border: 1px solid #334155;
        }
        .btn-next {
            background: #10B981;
            color: #FFFFFF;
        }
        .btn-sub {
            font-size: 10px;
            font-weight: 500;
            opacity: 0.85;
        }

        /* Stage Toggle Buttons */
        .stage-controls {
            display: grid;
            grid-template-columns: 1fr 1fr 1fr;
            gap: 8px;
        }

        .btn-stage {
            padding: 10px 6px;
            border-radius: 8px;
            border: 1px solid #334155;
            font-size: 11px;
            font-weight: 800;
            cursor: pointer;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 3px;
            background: #1E293B;
            color: #F1F5F9;
            transition: all 0.15s;
        }
        .btn-stage:active {
            transform: scale(0.96);
        }

        .btn-black.active {
            background: #EF4444;
            color: #FFFFFF;
            border-color: #DC2626;
            box-shadow: 0 0 12px rgba(239, 68, 68, 0.4);
        }
        .btn-clear.active {
            background: #F59E0B;
            color: #FFFFFF;
            border-color: #D97706;
            box-shadow: 0 0 12px rgba(245, 158, 11, 0.4);
        }

        /* Slide Matrix Card */
        .section-card {
            background: #131B2E;
            border: 1px solid #1E293B;
            border-radius: 12px;
            padding: 14px;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .section-title {
            font-size: 12px;
            font-weight: 800;
            color: #94A3B8;
            letter-spacing: 0.5px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .slide-list {
            display: flex;
            flex-direction: column;
            gap: 6px;
            max-height: 280px;
            overflow-y: auto;
        }

        .slide-item {
            background: #1E293B;
            border: 1px solid #334155;
            border-radius: 8px;
            padding: 10px 12px;
            cursor: pointer;
            display: flex;
            gap: 10px;
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
            font-size: 11px;
            font-weight: 800;
            padding: 2px 7px;
            border-radius: 4px;
            flex-shrink: 0;
        }
        .slide-item.active .slide-num {
            background: #60A5FA;
            color: #0F172A;
        }

        .slide-text {
            font-size: 13px;
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

        /* Form Inputs & Lists */
        .search-input {
            width: 100%;
            background: #0F172A;
            border: 1px solid #334155;
            color: #FFFFFF;
            padding: 10px 12px;
            border-radius: 8px;
            font-size: 13px;
            outline: none;
        }
        .search-input:focus { border-color: #38BDF8; }

        .item-card {
            background: #1E293B;
            border: 1px solid #334155;
            border-radius: 8px;
            padding: 12px;
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
            font-size: 14px;
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

        /* Chips Grid */
        .chips-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
        }

        /* Range Slider and Styling Controls */
        .slider-group {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }
        .slider-row {
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .slider-row input[type=range] {
            flex: 1;
            accent-color: #38BDF8;
        }

        .toggle-group {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
        }

        .toggle-btn {
            background: #1E293B;
            border: 1px solid #334155;
            color: #CBD5E1;
            padding: 10px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 700;
            cursor: pointer;
            text-align: center;
        }
        .toggle-btn.active {
            background: #38BDF8;
            color: #0F172A;
            border-color: #0284C7;
        }

        /* Fixed Bottom Quick Bar */
        .fixed-bar {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            background: #0F172A;
            border-top: 1px solid #1E293B;
            padding: 8px 12px;
            display: flex;
            gap: 6px;
            z-index: 100;
            box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.5);
            max-width: 720px;
            margin: 0 auto;
        }

        .fixed-btn {
            flex: 1;
            padding: 10px 4px;
            border-radius: 8px;
            border: none;
            font-size: 12px;
            font-weight: 800;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
            transition: all 0.12s;
        }
        .fixed-btn:active { transform: scale(0.96); }

        .fixed-btn-prev { background: #1E293B; color: #FFFFFF; border: 1px solid #334155; }
        .fixed-btn-next { background: #10B981; color: #FFFFFF; }
        .fixed-btn-black { background: #334155; color: #F1F5F9; }
        .fixed-btn-clear { background: #334155; color: #F1F5F9; }

        .fixed-btn-black.active { background: #EF4444; color: #FFFFFF; }
        .fixed-btn-clear.active { background: #F59E0B; color: #FFFFFF; }
    </style>
</head>
<body>
    <!-- HEADER -->
    <header>
        <div class="brand">
            <span style="font-size: 18px;">📱</span>
            <div>
                <h1>REMOTE CONSOLE</h1>
            </div>
            <span class="brand-sub">PRO</span>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
            <div class="status-pill">
                <div id="conn-dot" class="dot"></div>
                <span id="conn-text">Connecting...</span>
            </div>
            <a href="/" target="_blank" class="view-screen-btn">📺 Layar</a>
        </div>
    </header>

    <!-- NAVIGATION TABS -->
    <nav class="tab-bar">
        <button class="tab-btn active" onclick="switchTab('console')">🎛 Console</button>
        <button class="tab-btn" onclick="switchTab('songs')">🎵 Lagu</button>
        <button class="tab-btn" onclick="switchTab('bible')">📖 Alkitab</button>
        <button class="tab-btn" onclick="switchTab('background')">🖼 Background</button>
        <button class="tab-btn" onclick="switchTab('style')">🎨 Format</button>
    </nav>

    <!-- MAIN PANELS -->
    <main>
        <!-- TAB 1: CONSOLE (LIVE MONITOR & SLIDE CONTROLS) -->
        <div id="tab-console" class="tab-panel active">
            <!-- LIVE STAGE PREVIEW SCREEN -->
            <div class="live-card">
                <div class="card-header">
                    <div class="badge-group">
                        <span id="badge-status" class="badge badge-live">LIVE ON AIR</span>
                        <span id="badge-type" class="badge badge-type">LYRICS</span>
                    </div>
                    <span id="badge-bg" class="badge badge-bg">BG: NONE</span>
                </div>

                <h2 id="song-title" class="song-title">Memuat Tayangan...</h2>
                <div id="slide-indicator" class="slide-indicator">Slide 0 dari 0</div>

                <!-- SIMULATED 16:9 SCREEN PREVIEW -->
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

            <!-- PRIMARY NAVIGATION CONTROLS -->
            <div class="nav-controls">
                <button class="btn-nav btn-prev" onclick="sendAction('prev')">
                    <span>⏮ PREV</span>
                    <span class="btn-sub">Slide Sebelumnya</span>
                </button>
                <button class="btn-nav btn-next" onclick="sendAction('next')">
                    <span>NEXT ⏭</span>
                    <span class="btn-sub">Slide Selanjutnya</span>
                </button>
            </div>

            <!-- STAGE TOGGLES -->
            <div class="stage-controls">
                <button id="btn-black" class="btn-stage btn-black" onclick="sendAction('black')">
                    <span>⚫</span>
                    <span id="label-black">BLACKOUT</span>
                </button>
                <button id="btn-clear" class="btn-stage btn-clear" onclick="sendAction('clear')">
                    <span>🔲</span>
                    <span id="label-clear">CLEAR TEXT</span>
                </button>
                <button id="btn-vid-toggle" class="btn-stage" onclick="sendAction('toggle_video')">
                    <span>⏯</span>
                    <span>PAUSE BG</span>
                </button>
            </div>

            <!-- INTERACTIVE SLIDE MATRIX -->
            <div class="section-card">
                <div class="section-title">
                    <span>DAFTAR SLIDE (TAP UNTUK PINDAH)</span>
                    <span id="matrix-count" style="font-size: 11px; color: #94A3B8;">0 slides</span>
                </div>
                <div id="slide-list" class="slide-list">
                    <!-- Dynamic Slide Items will be rendered here -->
                </div>
            </div>
        </div>

        <!-- TAB 2: SONGS & LYRICS -->
        <div id="tab-songs" class="tab-panel">
            <div class="section-card">
                <div class="section-title">
                    <span>CARI & TAYANGKAN LAGU</span>
                    <button class="btn-chip btn-chip-live" onclick="toggleAddSongForm()">+ Tambah Lagu</button>
                </div>
                <input type="text" id="song-search-input" class="search-input" placeholder="🔍 Cari judul lagu / lirik..." oninput="filterSongs()">

                <!-- QUICK ADD SONG FORM -->
                <div id="add-song-form" style="display: none; background: #0F172A; padding: 12px; border-radius: 8px; border: 1px solid #334155; margin-top: 8px;">
                    <div style="font-size: 12px; font-weight: 800; color: #38BDF8; margin-bottom: 8px;">TAMBAH LAGU BARU</div>
                    <input type="text" id="new-song-title" class="search-input" placeholder="Judul Lagu..." style="margin-bottom: 8px;">
                    <textarea id="new-song-lyrics" class="search-input" rows="4" placeholder="Ketik lirik (pisahkan bait dengan 1 baris kosong)..." style="margin-bottom: 8px;"></textarea>
                    <div style="display: flex; gap: 8px;">
                        <button class="btn-chip btn-chip-live" onclick="submitNewSong()">💾 Simpan & Tayangkan</button>
                        <button class="btn-chip btn-chip-sec" onclick="toggleAddSongForm()">Batal</button>
                    </div>
                </div>

                <div id="songs-list-container" style="display: flex; flex-direction: column; gap: 8px; margin-top: 6px;">
                    <!-- Dynamic songs rendered here -->
                </div>
            </div>
        </div>

        <!-- TAB 3: BIBLE & SCRIPTURES -->
        <div id="tab-bible" class="tab-panel">
            <div class="section-card">
                <div class="section-title">
                    <span>AYAT POPULER (1-KLIK GO LIVE)</span>
                </div>
                <div id="bible-quick-chips" class="chips-grid">
                    <!-- Dynamic Bible chips rendered here -->
                </div>
            </div>

            <!-- QUICK SCRIPTURE COMPOSER -->
            <div class="section-card">
                <div class="section-title">
                    <span>TAYANGKAN AYAT CUSTOM</span>
                </div>
                <input type="text" id="bible-custom-title" class="search-input" placeholder="Nama Kitab & Pasal (contoh: Yohanes 3:16)..." style="margin-bottom: 8px;">
                <textarea id="bible-custom-text" class="search-input" rows="3" placeholder="Teks Ayat Alkitab..." style="margin-bottom: 8px;"></textarea>
                <button class="btn-chip btn-chip-live" style="padding: 10px; font-size: 13px;" onclick="submitCustomBible()">🚀 TAYANGKAN AYAT KE LAYAR</button>
            </div>
        </div>

        <!-- TAB 4: MEDIA & BACKGROUND SETTING -->
        <div id="tab-background" class="tab-panel">
            <!-- ACTIVE BACKGROUND MONITOR -->
            <div class="section-card" style="border: 1px solid #8B5CF6; background: #18152E;">
                <div class="section-title">
                    <span style="color: #C4B5FD;">BACKGROUND AKTIF DI LAYAR</span>
                    <button class="btn-chip" style="background: #EF4444; color: #FFFFFF;" onclick="clearBackground()">❌ Hapus BG</button>
                </div>
                <div id="bg-active-desc" style="font-size: 14px; font-weight: 800; color: #FFFFFF;">⚪ Tanpa Background (Gelap Bersih)</div>
            </div>

            <!-- CHOOSE BACKGROUND PRESETS -->
            <div class="section-card">
                <div class="section-title">
                    <span>PILIH BACKGROUND / MEDIA</span>
                    <button class="btn-chip btn-chip-bg" onclick="toggleAddDroidCamForm()">+ DroidCam IP</button>
                </div>

                <!-- ADD DROIDCAM FORM -->
                <div id="add-droidcam-form" style="display: none; background: #0F172A; padding: 12px; border-radius: 8px; border: 1px solid #8B5CF6; margin-bottom: 8px;">
                    <div style="font-size: 12px; font-weight: 800; color: #C4B5FD; margin-bottom: 8px;">HUBUNGKAN DROIDCAM / IP CAMERA</div>
                    <input type="text" id="droid-name" class="search-input" placeholder="Nama Kamera (misal: HP Depan)..." style="margin-bottom: 8px;">
                    <input type="text" id="droid-ip" class="search-input" placeholder="IP HP (contoh: 192.168.1.50)..." style="margin-bottom: 8px;">
                    <input type="text" id="droid-port" class="search-input" placeholder="Port (default: 4747)..." value="4747" style="margin-bottom: 8px;">
                    <div style="display: flex; gap: 8px;">
                        <button class="btn-chip btn-chip-bg" onclick="submitDroidCam()">💾 Simpan & Set BG</button>
                        <button class="btn-chip btn-chip-sec" onclick="toggleAddDroidCamForm()">Batal</button>
                    </div>
                </div>

                <!-- DEFAULT BACKGROUND OPTIONS -->
                <div style="display: flex; flex-direction: column; gap: 8px;">
                    <!-- NONE -->
                    <div class="item-card">
                        <div class="item-header">
                            <div class="item-title">⚪ Standar Gelap (Dark Gradient)</div>
                            <button class="btn-chip btn-chip-sec" onclick="clearBackground()">Set Default</button>
                        </div>
                    </div>

                    <!-- LOCAL CAMERA -->
                    <div class="item-card">
                        <div class="item-header">
                            <div class="item-title">📷 Kamera HP Utama (Local Cam)</div>
                        </div>
                        <div class="btn-action-row">
                            <button class="btn-chip btn-chip-bg" onclick="sendAction('set_bg', { bgType: 'CAMERA' })">🖼 Set Background</button>
                            <button class="btn-chip btn-chip-live" onclick="sendAction('go_custom', { title: 'Live Camera', text: 'Live Video Feed', type: 'CAMERA' })">▶ Go Live</button>
                        </div>
                    </div>

                    <!-- MEDIA LIBRARY ITEMS -->
                    <div id="media-library-items" style="display: flex; flex-direction: column; gap: 8px;">
                        <!-- Dynamic media items rendered here -->
                    </div>
                </div>
            </div>
        </div>

        <!-- TAB 5: FORMAT & STYLE -->
        <div id="tab-style" class="tab-panel">
            <!-- QUICK PRESETS -->
            <div class="section-card">
                <div class="section-title">
                    <span>1-KLIK PRESET TEMA TAMPILAN</span>
                </div>
                <div class="chips-grid">
                    <button class="toggle-btn" onclick="sendAction('set_preset', { preset: 'WORSHIP' })">🌟 Worship (Center 32sp)</button>
                    <button class="toggle-btn" onclick="sendAction('set_preset', { preset: 'PRAISE' })">⚡ Praise (Center 36sp Bold)</button>
                    <button class="toggle-btn" onclick="sendAction('set_preset', { preset: 'SERMON' })">📖 Kotbah (Lower Third 24sp)</button>
                    <button class="toggle-btn" onclick="sendAction('set_preset', { preset: 'MINIMALIST' })">✨ Minimalist (Lower Third)</button>
                </div>
            </div>

            <!-- CUSTOM ADJUSTMENTS -->
            <div class="section-card">
                <div class="section-title">
                    <span>KUSTOMISASI UKURAN & POSISI TEKS</span>
                </div>

                <!-- FONT SIZE -->
                <div class="slider-group">
                    <div style="display: flex; justify-content: space-between; font-size: 12px; font-weight: 700;">
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
                <div style="margin-top: 8px;">
                    <div style="font-size: 12px; font-weight: 700; margin-bottom: 6px;">Posisi Teks di Layar:</div>
                    <div class="chips-grid">
                        <button id="pos-btn-CENTER" class="toggle-btn active" onclick="updatePosition('CENTER')">Tengah (Center)</button>
                        <button id="pos-btn-LOWER_THIRD" class="toggle-btn" onclick="updatePosition('LOWER_THIRD')">Lower Third (Bawah 1/3)</button>
                        <button id="pos-btn-TOP" class="toggle-btn" onclick="updatePosition('TOP')">Atas (Top Banner)</button>
                        <button id="pos-btn-BOTTOM" class="toggle-btn" onclick="updatePosition('BOTTOM')">Bawah Rata Kiri</button>
                    </div>
                </div>

                <!-- ALIGNMENT -->
                <div style="margin-top: 8px;">
                    <div style="font-size: 12px; font-weight: 700; margin-bottom: 6px;">Perataan Teks:</div>
                    <div class="toggle-group" style="grid-template-columns: 1fr 1fr 1fr;">
                        <button id="align-btn-left" class="toggle-btn" onclick="updateAlignment('LEFT')">Kiri</button>
                        <button id="align-btn-center" class="toggle-btn active" onclick="updateAlignment('CENTER')">Tengah</button>
                        <button id="align-btn-right" class="toggle-btn" onclick="updateAlignment('RIGHT')">Kanan</button>
                    </div>
                </div>

                <!-- TOGGLES -->
                <div style="margin-top: 8px;">
                    <div class="chips-grid">
                        <button id="btn-toggle-bold" class="toggle-btn active" onclick="toggleBold()">Tebal (Bold)</button>
                        <button id="btn-toggle-shadow" class="toggle-btn active" onclick="toggleShadow()">Bayangan Teks</button>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <!-- FIXED BOTTOM QUICK CONTROL BAR (FOR QUICK THUMB ACCESS) -->
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
        const btnBlack = document.getElementById('btn-black');
        const labelBlack = document.getElementById('label-black');
        const btnClear = document.getElementById('btn-clear');
        const labelClear = document.getElementById('label-clear');
        const fixedBlack = document.getElementById('fixed-black');
        const fixedClear = document.getElementById('fixed-clear');
        const slideListContainer = document.getElementById('slide-list');
        const matrixCount = document.getElementById('matrix-count');
        const bgActiveDesc = document.getElementById('bg-active-desc');

        // Tab Switching
        function switchTab(tabId) {
            document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
            document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));

            const targetBtn = Array.from(document.querySelectorAll('.tab-btn')).find(b => b.getAttribute('onclick').includes(tabId));
            if (targetBtn) targetBtn.classList.add('active');

            const targetPanel = document.getElementById('tab-' + tabId);
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
                btnBlack.classList.add('active');
                fixedBlack.classList.add('active');
                labelBlack.innerText = 'UN-BLACK';
            } else {
                badgeStatus.className = 'badge badge-live';
                badgeStatus.innerText = 'LIVE ON AIR';
                btnBlack.classList.remove('active');
                fixedBlack.classList.remove('active');
                labelBlack.innerText = 'BLACKOUT';
            }

            if (status === 'CLEAR') {
                btnClear.classList.add('active');
                fixedClear.classList.add('active');
                labelClear.innerText = 'SHOW TEXT';
            } else {
                btnClear.classList.remove('active');
                fixedClear.classList.remove('active');
                labelClear.innerText = 'CLEAR TEXT';
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
                bgActiveDesc.innerText = '📱 DroidCam Stream (' + (data.backgroundMediaUrl || '') + ')';
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
            slideIndicator.innerText = total > 0 ? ('Slide ' + (currentIdx + 1) + ' dari ' + total) : 'Standby Mode';

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
                slideListContainer.innerHTML = '<div style="color: #64748B; font-size: 12px; text-align: center; padding: 12px;">Belum ada slide aktif</div>';
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

        // Render Songs List in Tab 2
        function renderSongs() {
            const container = document.getElementById('songs-list-container');
            container.innerHTML = '';
            const query = (document.getElementById('song-search-input').value || '').toLowerCase();
            const songs = (libraryData.songs || []).filter(s => s.title.toLowerCase().includes(query) || s.slides.some(v => v.toLowerCase().includes(query)));

            if (songs.length === 0) {
                container.innerHTML = '<div style="color: #64748B; font-size: 12px; text-align: center; padding: 16px;">Tidak ada lagu ditemukan</div>';
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
                    switchTab('console');
                };
                btnRow.appendChild(goLiveBtn);

                // Slides buttons
                song.slides.forEach((sl, idx) => {
                    const slideBtn = document.createElement('button');
                    slideBtn.className = 'btn-chip btn-chip-sec';
                    slideBtn.innerText = 'Bait ' + (idx + 1);
                    slideBtn.onclick = () => {
                        sendAction('go_song', { songId: song.id, slide: idx });
                        switchTab('console');
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
            switchTab('console');
        }

        // Render Bible Chips in Tab 3
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
                    switchTab('console');
                };
                container.appendChild(btn);
            });
        }

        function submitCustomBible() {
            const title = document.getElementById('bible-custom-title').value.trim();
            const text = document.getElementById('bible-custom-text').value.trim();
            if (!text) return;
            sendAction('go_custom', { title: title || 'Alkitab', text: text, type: 'BIBLE' });
            switchTab('console');
        }

        // Render Media Items in Tab 4
        function renderMedia() {
            const container = document.getElementById('media-library-items');
            container.innerHTML = '';
            const mediaList = libraryData.media || [];

            mediaList.forEach(m => {
                const card = document.createElement('div');
                card.className = 'item-card';

                const header = document.createElement('div');
                header.className = 'item-header';
                const icon = m.type === 'IP_CAMERA' ? '📱' : (m.type === 'IMAGE' ? '🖼' : '🎥');
                header.innerHTML = '<div class="item-title">' + icon + ' ' + m.title + '</div>';

                const btnRow = document.createElement('div');
                btnRow.className = 'btn-action-row';

                const setBgBtn = document.createElement('button');
                setBgBtn.className = 'btn-chip btn-chip-bg';
                setBgBtn.innerText = '🖼 Set Background';
                setBgBtn.onclick = () => sendAction('set_bg_media', { mediaId: m.id });

                const goLiveBtn = document.createElement('button');
                goLiveBtn.className = 'btn-chip btn-chip-live';
                goLiveBtn.innerText = '▶ Go Live Full';
                goLiveBtn.onclick = () => {
                    sendAction('go_media', { mediaId: m.id });
                    switchTab('console');
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
            switchTab('console');
        }

        // Style Settings Handlers in Tab 5
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

        // Keyboard Controls
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
