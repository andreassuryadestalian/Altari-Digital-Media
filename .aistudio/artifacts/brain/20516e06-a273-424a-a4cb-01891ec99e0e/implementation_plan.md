# Rencana Implementasi: Perombakan Tata Letak UI Adaptif untuk Smartphone & Tablet

Desain ulang sistem tata letak (responsive layout system) pada seluruh antarmuka aplikasi Altari Digital agar tampil proporsional, rapi, dan mudah dioperasikan di semua ukuran layar—mulai dari smartphone layar kecil (360–412dp) hingga tablet layar besar (600–1200dp), baik dalam orientasi portrait maupun landscape.

## User Review & Critical Decisions

> [!IMPORTANT]
> Berdasarkan jawaban klarifikasi yang telah dikonfirmasi oleh pengguna:
> 1. **Orientasi Layar**: Aplikasi mendukung **Adaptif Otomatis Portrait dan Landscape** pada smartphone dan tablet.
> 2. **Navigasi Utama**: Menggunakan **Tab bar atas yang dapat digeser secara horizontal (ScrollableTabRow)** sehingga semua modul (Dashboard, Stage, Lyrics, Bible, PPT, Media, Playlist, Settings) dapat diakses dengan mulus tanpa terpotong.
> 3. **Tata Letak Dasbor Smartphone**: Mode vertikal responsif dengan preview monitor di bagian atas, tombol kontrol navigasi di tengah, dan workspace materi/lirik di bawah dengan scrolling terpadu.

- **Keputusan Terkonfirmasi 1**: Tab navigasi utama tetap berada di bilah atas namun dijadikan `ScrollableTabRow` dengan *touch target* 48dp dan label yang tidak berdesakan.
- **Keputusan Terkonfirmasi 2**: Bilah atas (*TopBar*) dipisah menjadi dua tingkat responsif pada layar sempit: baris pertama untuk identitas & quick dropdown playlist, dan baris kedua (dapat digulir horizontal) untuk pill status (*Ticker, Timer, Split Screen, Remote URL*).
- **Keputusan Terkonfirmasi 3**: Pada *LandscapeOperatorConsole*, lebar panel tidak lagi di-*hardcode* menjadi `280dp` dan `300dp`, melainkan menggunakan bobot dinamis (`weight`) atau *drawer toggle* adaptif berdasarkan lebar layar yang tersedia (`BoxWithConstraints`).

---

## 1. Overview & Core Concept

* **Tujuan**: Memastikan 100% fitur, tombol aksi (seperti *Go Live, Next/Prev, Black, Clear, Tambah Lagu, Pilih Ayat*), dan preview video dapat diakses dengan mudah oleh operator di smartphone tanpa terpotong batas layar atau saling tumpang tindih.
* **Target Pengguna**: Operator multimedia gereja yang menggunakan smartphone Android pribadi (portrait saat mobilitas santai atau landscape saat standby di meja operator) serta kru yang menggunakan tablet besar di mimbar/meja multimedia.
* **Nilai Utama**: Fleksibilitas perangkat sejati—pengalaman kendali ibadah tetap profesional dan nyaman di layar 5.5 inci hingga tablet 12 inci.

---

## 2. User Experience & Visual Design

### Responsivitas Berdasarkan Ukuran Layar (Window Size Classes)

1. **Compact Screen (Smartphone Portrait, lebar < 600dp)**:
   * **TopBar**: Baris atas ringkas (Logo Altari + Pemilih Playlist aktif + Tombol Web Remote cepat), baris bawah *horizontal scrollable pill badges* (Ticker ON/OFF, Sermon Timer, Split Screen).
   * **Top Navigation**: `ScrollableTabRow` dengan indikator aktif yang jelas dan padding sentuh yang nyaman.
   * **Operator Console**: 
     * Monitor Preview & Program ditata berdampingan dengan rasio aspek 16:9 yang proporsional (~100–110dp tinggi) agar tidak memakan seluruh ruang vertikal.
     * Tombol aksi utama (*GO LIVE*, *PREV*, *NEXT*, *BLACK*, *CLEAR*) menggunakan grid responsif 2 baris dengan label teks kontras tinggi.
     * Bagian bawah (*Workspace Hub*: Order Ibadah, Database Lagu, Media) dilengkapi *LazyColumn* mandiri sehingga operator dapat leluasa menggulir dan mencari lirik.
   * **Layar Manajemen (Alkitab & Lirik)**:
     * *Lirik*: Tombol aksi kartu lagu (*+Playlist, Edit, Hapus, Preview, GO LIVE*) disusun di bawah judul lagu dalam bentuk baris responsif/ikonik agar judul lagu tidak tertekan habis.
     * *Alkitab*: Pada layar HP sempit, daftar pasal Alkitab dan daftar ayat disusun secara tumpuk vertikal (*collapsible/tab selector*) atau penyesuaian tinggi seimbang, bukan dipaksa 2 kolom horizontal yang sempit.

2. **Expanded / Medium Screen (Tablet atau Smartphone Landscape, lebar >= 600dp)**:
   * **Landscape 3-Panel Console**: Tiga panel proporsional dengan pembagian lebar dinamis:
     * Panel Kiri (Order Ibadah / Pemilih Media): 30% lebar layar
     * Panel Tengah (Slide Grid & Slide Selector): 42% lebar layar
     * Panel Kanan (Dual Monitor Preview/Program + Master Desk): 28% lebar layar
   * Menghilangkan *hardcoded width* `280dp` dan `300dp` yang sebelumnya merusak konsol landscape pada smartphone beresolusi standar.

### Identitas Visual & Estetika
* **Tema**: *Pro Video Switcher Dark Aesthetic* (Latar belakang gelap `#0A0E1A` dan `#131B2E`, aksen Emerald `#10B981`, Cyan `#06B6D4`, dan Purple `#7C3AED`).
* **Tipografi**: Skala font adaptif dengan unit `sp` (11–13sp untuk teks tombol di smartphone agar tidak meluap).
* **Touch Target**: Seluruh tombol interaktif memiliki tinggi minimal 40–48dp dengan efek ripple feedback yang instan saat disentuh.

---

## 3. Key Product Decisions & Trade-Offs

* **Keputusan 1: Pemisahan Baris Status Header pada Smartphone**
  * *Pilihan*: Mengubah baris header atas menjadi 2 baris fleksibel pada layar compact (`< 600dp`).
  * *Alasan*: Menaruh judul, IP address, dan 5 tombol pill dalam 1 baris horizontal selalu meluap di HP berlayar 360–400dp. Dengan 2 baris, semua tombol status tetap terlihat dan dapat ditekan.
* **Keputusan 2: Penataan Kartu Aksi Lirik Responsif**
  * *Pilihan*: Menata tombol aksi (*+Playlist, Edit, Preview, Go Live*) di bawah judul lagu pada kartu lagu.
  * *Alasan*: Meletakkan 5 tombol di samping judul pada layar smartphone menyisakan hanya 40dp untuk judul lagu. Penataan vertikal memberikan ruang penuh untuk judul lagu dan tombol yang nyaman ditekan jari.
* **Keputusan 3: Pembagian Proporsi Dinamis Menggunakan BoxWithConstraints**
  * *Pilihan*: Menggunakan `BoxWithConstraints` untuk mengukur lebar aktual layar saat merender `LandscapeOperatorConsole` dan `BibleManagementScreen`.
  * *Alasan*: Menghindari *hardcoded pixel/dp* yang kaku dan memastikan tampilan otomatis menyesuaikan berbagai resolusi layar HP dan tablet.

---

## 4. Technical Architecture & Component Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                       MainActivity                          │
└──────────────────────────────┬──────────────────────────────┘
                               │
                ┌──────────────▼──────────────┐
                │       DashboardScreen       │
                │    (BoxWithConstraints)     │
                └──────────────┬──────────────┘
                               │
       ┌───────────────────────┴───────────────────────┐
       ▼                                               ▼
┌──────────────────────────────┐       ┌──────────────────────────────┐
│           TopBar             │       │      Active Tab Content      │
│ ├─ Adaptive Status Header    │       │  (DASHBOARD, LYRICS, BIBLE,  │
│ ├─ Scrollable Quick Pills    │       │   PPT, MEDIA, PLAYLIST, etc) │
│ └─ ScrollableTabRow (Tabs)   │       └──────────────┬───────────────┘
└──────────────────────────────┘                      │
                                                      │
                       ┌──────────────────────────────┴──────────────────────────────┐
                       ▼                                                             ▼
        [isLandscape && screenWidth >= 600dp]                         [isPortrait || screenWidth < 600dp]
        ┌───────────────────────────────────┐                         ┌───────────────────────────────────┐
        │     LandscapeOperatorConsole      │                         │      PortraitOperatorConsole      │
        │ ├─ Panel Kiri: Order (weight 3)   │                         │ ├─ Dual Monitor Bar (16:9 ratio)  │
        │ ├─ Panel Tengah: Slides (weight 4)│                         │ ├─ Action Desk Grid (Go Live)     │
        │ └─ Panel Kanan: Master (weight 3) │                         │ └─ Workspace Hub (Tabs & List)    │
        └───────────────────────────────────┘                         └───────────────────────────────────┘
```

### Rincian Komponen yang Dimodifikasi:
1. **`TopBar` (`DashboardScreen.kt`)**:
   * Deteksi lebar layar: jika `< 600dp`, ubah tata letak header menjadi bertingkat dengan *scrollable horizontal row* untuk pill actions.
   * `ScrollableTabRow` dioptimalkan dengan padding dan ukuran font yang proporsional untuk smartphone.
2. **`PortraitOperatorConsole` (`DashboardScreen.kt`)**:
   * Reduksi tinggi monitor preview/program menjadi rasio aspek 16:9 adaptif (100–115dp).
   * Tata letak tombol Master Action (*Go Live, Prev, Next, Black, Clear*) dibuat proporsional tanpa *clipping*.
3. **`LandscapeOperatorConsole` (`DashboardScreen.kt`)**:
   * Ganti `.width(280.dp)` dan `.width(300.dp)` dengan bobot dinamis `weight(0.30f)`, `weight(0.42f)`, `weight(0.28f)` pada `Row` utama.
4. **`BibleManagementScreen.kt`**:
   * Deteksi layar sempit: saat di smartphone portrait, pisahkan pemilihan pasal dan tampilan ayat menjadi tata letak bertumpuk yang seimbang atau tab cepat, sehingga teks ayat tidak terhimpit.
5. **`LyricsManagementScreen.kt`**:
   * Header aksi (*Import TXT, Smart Paste, +Add Song*) dibuat *wrap/scrollable*.
   * Kartu lagu (*Song Card*): Tombol aksi disusun rapi di bawah judul lagu pada layar kecil agar seluruh tombol (*Preview, Go Live, Edit, +Playlist*) dapat ditekan dengan presisi.
6. **`MediaManagementScreen.kt` & `PlaylistManagementScreen.kt`**:
   * Penyesuaian grid dan padding kartu agar tidak meluap ke luar layar smartphone.
