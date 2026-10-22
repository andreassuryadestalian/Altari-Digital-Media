package com.example.data

import com.example.model.LyricsContent

data class PresetSong(
    val id: String,
    val title: String,
    val category: String, // "Kidung Jemaat", "NKB / PKJ", "Praise & Worship"
    val slides: List<String>
) {
    fun toLyricsContent(): LyricsContent {
        return LyricsContent(
            id = "song_${System.currentTimeMillis()}_${id}",
            title = title,
            slides = slides
        )
    }
}

object PresetSongbook {

    val categories = listOf("Semua", "Kidung Jemaat", "NKB / PKJ", "Praise & Worship")

    val songs: List<PresetSong> = listOf(
        // --- KIDUNG JEMAAT ---
        PresetSong(
            id = "kj_400",
            title = "KJ 400 - Kudaki Jalan Mulia",
            category = "Kidung Jemaat",
            slides = listOf(
                "Kudaki jalan mulia;\ntetap doaku serta:\n\"Tuhan, tempatkan langkahku\ndi tempat tinggi dan teguh.\"",
                "Reff:\nYa Tuhan, angkat jiwaku\nlebih dekat kepada-Mu;\nmakin tinggi 'ku merindukan:\nTuhan, tempatkan langkahku.",
                "Hatiku tidak mau tinggal\ndi tempat ragu dan bimbang;\nwalau di sana banyak orang,\n'ku mau tinggal di puncak t'rang.\n\n(Kembali ke Reff)",
                "'Ku mau hidup di atas dunia,\nmeski dicela oleh setan;\n'kan kudengar senandung surga\nyang dinyanyikan orang kudus.\n\n(Kembali ke Reff)"
            )
        ),
        PresetSong(
            id = "kj_003",
            title = "KJ 003 - Kami Puji Dengan Riang",
            category = "Kidung Jemaat",
            slides = listOf(
                "Kami puji dengan riang\nDikau, Allah yang besar;\nBagai bunga t'rima siang,\nhati kami pun mekar.",
                "Kabut dosa dan derita,\nkebimbangan t'lah lenyap.\nSumber suka yang abadi,\nb'ri sinar-Mu menyerap.",
                "Semesta alam memuji\nkemuliaan nama-Mu;\nBintang, surga, bumi raya\nmengagungkan karya-Mu.",
                "Padang, hutan, bukit, lembah,\nburung bernyanyi merdu;\nSumber air berbisik riang,\nmembesarkan nama-Mu."
            )
        ),
        PresetSong(
            id = "kj_040",
            title = "KJ 040 - Ajaib Benar Anugerah (Amazing Grace)",
            category = "Kidung Jemaat",
            slides = listOf(
                "Ajaib benar anugerah\npembebas jiwaku!\n'Ku t'lah hilang, kini dapatlah;\n'ku buta, kini celik.",
                "Anugerah yang menggetarkan\nhatiku beriman;\nbetapa indahnya saat\n'ku percaya t'rang-Mu.",
                "Di s'panjang jalan bahaya,\njerat dan cobaan;\nanugerah-Nya t'lah membimbingku,\nmembawaku pulang.",
                "Dan bila nanti di surga\nberibu tahunlah,\nkekal kita bersyukur pada-Nya,\ntiada berkesudahan."
            )
        ),
        PresetSong(
            id = "kj_064",
            title = "KJ 064 - Bila Kulihat Bintang Gemerlapan",
            category = "Kidung Jemaat",
            slides = listOf(
                "Bila kulihat bintang gemerlapan\ndan kudengar derunya guntur Hu,\ntercengang aku tampak kebesaran\nyang Kau bentangkan di jagat semesta.",
                "Reff:\nMaka jiwaku pun memuji-Mu:\n\"Sungguh besar Kau, Allahku!\"\nMaka jiwaku pun memuji-Mu:\n\"Sungguh besar Kau, Allahku!\"",
                "Ya Tuhanku, 'pabila kurenungkan\npemberian-Mu dalam Penebus,\n'ku tertegun: bagiaku sekalian\nYesus disalib mati tertebus.\n\n(Kembali ke Reff)",
                "Bila kelak 'ku masuk surga mulia\ndan bersujud menyembah Tuhanku,\ndengan penuh sukacita di surga\n'ku nyanyikan: \"Besar Kau, Allahku!\"\n\n(Kembali ke Reff)"
            )
        ),
        PresetSong(
            id = "kj_392",
            title = "KJ 392 - 'Ku Berbahagia (Blessed Assurance)",
            category = "Kidung Jemaat",
            slides = listOf(
                "'Ku berbahagia, yakin teguh:\nYesus abadi kepunyaanku!\nAku waris-Nya, 'ku ditebus,\nciptaan baru Roh Kudus.",
                "Reff:\nAku bernyanyi bahagia\nmemuji Yesus selamanya.\nAku bernyanyi bahagia\nmemuji Yesus selamanya.",
                "Pasrah sempurna, nikmat penuh;\nsuka sorgawi melimpahiku.\nBala sorgawi menyanyikan\nkasih dan rahmat kebaikan.\n\n(Kembali ke Reff)",
                "Pasrah sempurna, damai tenang;\ndalam Tuhanku hatiku senang.\nSambil memandang kemuliaan,\n'ku 'kan bersorak bagi Tuhan.\n\n(Kembali ke Reff)"
            )
        ),
        PresetSong(
            id = "kj_450",
            title = "KJ 450 - Hidup Kita Yang Benar",
            category = "Kidung Jemaat",
            slides = listOf(
                "Hidup kita yang benar\nharuslah mengucap syukur.\nDalam Kristus bergemar;\nsenang susah beryukur.",
                "Reff:\nHaleluya, puji Tuhan,\npuji Dia s'lamanya!\nHaleluya, puji Tuhan,\npuji Dia s'lamanya!",
                "Tuhan Yesus, Tolonglah\nkami setia bersyukur;\nsuka duka di dunia,\n'ku serahkan pada-Mu.\n\n(Kembali ke Reff)"
            )
        ),
        PresetSong(
            id = "kj_457",
            title = "KJ 457 - Ya Tuhan, Tiap Jam",
            category = "Kidung Jemaat",
            slides = listOf(
                "Ya Tuhan, tiap jam\n'ku memerlukan-Mu,\nEngkaulah yang memb'ri\nsejahtera penuh.",
                "Reff:\nSetiap jam, ya Tuhan,\nDikau kuperlukan;\ndatanglah dan berkati,\nya Jurus'lamat!",
                "Ya Tuhan, tiap jam,\ndi suka-dukaku,\njikalau Tuhan jauh,\npercuma hidupku.\n\n(Kembali ke Reff)",
                "Ya Tuhan, tiap jam,\najarkan maksud-Mu;\nb'ri janji-Mu genap\ndi dalam hidupku.\n\n(Kembali ke Reff)"
            )
        ),

        // --- NKB & PKJ ---
        PresetSong(
            id = "nkb_133",
            title = "NKB 133 - Syukur Pada-Mu, Ya Allah",
            category = "NKB / PKJ",
            slides = listOf(
                "Syukur pada-Mu, ya Allah,\natas segala rahmat-Mu;\nSyukur atas kecukupan\ndari kasih-Mu yang penuh.",
                "Syukur atas air sejuk,\nsurya pagi yang cerah;\nSyukur atas bunga mekar,\nharum semerbak mewangi.",
                "Syukur atas doa yang dijawab\ndan yang tidak diijinkan;\nSyukur atas air mata\ndan suka duka hidupku.",
                "Syukur atas pengampunan\ndan damai sejahtera;\nSyukur atas hari ini\ndan surga yang abadi."
            )
        ),
        PresetSong(
            id = "nkb_197",
            title = "NKB 197 - Besarlah Setia-Mu (Great Is Thy Faithfulness)",
            category = "NKB / PKJ",
            slides = listOf(
                "Besarlah setia-Mu, Allah Bapaku,\nbesarlah kasih-Mu berlimpahan;\ntiada kurang dan tiada batas,\nEngkau setia selamanya.",
                "Reff:\nBesarlah setia-Mu, besarlah setia-Mu,\nsetiap pagi bertambah berkat;\nyang kuperlukan t'lah Kau sediakan,\nbesarlah setia-Mu kepadaku.",
                "Musim menuai, musim kemarau,\nsurya dan bintang serta rembulan;\nsaksi setia karya rahmat-Mu,\nmenyatakan setia-Mu yang baka.\n\n(Kembali ke Reff)",
                "Ampunan dosa dan damai abadi,\npimpinan-Mu menghibur hatiku;\npengharapan masa depan cerah,\nberkat melimpah kurasakan p'nuh.\n\n(Kembali ke Reff)"
            )
        ),
        PresetSong(
            id = "pkj_002",
            title = "PKJ 002 - Mulia, Mulia Nama-Nya (Majesty)",
            category = "NKB / PKJ",
            slides = listOf(
                "Mulia, mulia nama-Nya,\nbagi Yesus kemuliaan, puji, sembah!\nMulia, kekuasaan-Nya\nmemb'ri berkat bagi jemaat, bersyukurlah!",
                "Pujilah, tinggikanlah Rajamu Yesus;\nDialah selamanya Sang Penebus.\nMulia, mulia nama-Nya,\nbagi Yesus kemuliaan, puji, sembah!"
            )
        ),
        PresetSong(
            id = "pkj_242",
            title = "PKJ 242 - Seindah Siang Disinari Terang",
            category = "NKB / PKJ",
            slides = listOf(
                "Seindah siang disinari terang,\ncara Tuhan mengasihiku;\nSeindah petang dengan angin sejuk,\ncara Tuhan mengasihiku.",
                "Reff:\nTuhanku lembut dan penyayang,\nDia mengasihiku selalu;\nWalau 'ku jatuh dan tersesat,\nkasih-Nya tak pernah berubah.",
                "Sedalam lautan, seluas angkasa,\ncara Tuhan mengasihiku;\nSeharum kembang yang mekar di padang,\ncara Tuhan mengasihiku.\n\n(Kembali ke Reff)"
            )
        ),

        // --- PRAISE & WORSHIP ---
        PresetSong(
            id = "pw_bapa_surgawi",
            title = "Bapa Surgawi",
            category = "Praise & Worship",
            slides = listOf(
                "Bapa Surgawi, ajarku mengenal\nbetapa dalamnya kasih-Mu.\nBapa Surgawi, buatku mengerti\nbetapa mulia rencana-Mu.",
                "Reff:\n'Ku sembah Sujud di hadapan-Mu,\n'ku serahkan seluruh hidupku.\nPakailah aku sesuai kehendak-Mu,\nmenyatakan kemuliaan-Mu.",
                "Bapa yang setia, tak pernah Kau ingkar,\njanji-Mu abadi selamanya.\nDalam setiap musim, dalam setiap langkah,\ntangan-Mu menopang hidupku.\n\n(Kembali ke Reff)"
            )
        ),
        PresetSong(
            id = "pw_sentuh_hatiku",
            title = "Sentuh Hatiku",
            category = "Praise & Worship",
            slides = listOf(
                "Betapa kumencintai\nsegala yang t'lah terjadi,\ntak pernah sendiri jalani hidup ini,\nselalu menyertai.",
                "Betapa kumenyadari\ndi dalam hidupku ini,\nKau s'lalu memberi rancangan terbaik\noleh karena kasih.",
                "Reff:\nBapa, sentuh hatiku,\nubah hidupku menjadi yang baru.\nBagai emas yang murni,\nkau membentuk bejana hatiku.",
                "Bapa, ajarku mengerti\nsebuah kasih yang selalu memberi.\nBagai air mengalir,\nyang tiada pernah berhenti."
            )
        ),
        PresetSong(
            id = "pw_kusyapkan_hatiku",
            title = "Kusiapkan Hatiku Tuhan",
            category = "Praise & Worship",
            slides = listOf(
                "Kusiapkan hatiku, Tuhan,\n'tuk dengar firman-Mu saat ini.\n'Ku sujud menyembah-Mu, Tuhan,\nmasuk hadirat-Mu saat ini.",
                "Curahkan urapan-Mu, Tuhan,\nbagi jemaat-Mu saat ini.\nKusiapkan hatiku, Tuhan,\n'tuk dengar firman-Mu.",
                "Reff:\nFirman-Mu, Tuhan, tiada berubah,\ndahulu, sekarang, selama-lamanya,\ntiada berkesudahan.",
                "Firman-Mu, Tuhan, penolong hidupku,\nkusiapkan hatiku, Tuhan,\n'tuk dengar firman-Mu."
            )
        ),
        PresetSong(
            id = "pw_sampai_memutih_rambutku",
            title = "Sampai Memutih Rambutku",
            category = "Praise & Worship",
            slides = listOf(
                "Kau s'lalu ada buat diriku,\ndan tak pernah tinggalkanku.\nSaat nafasku berakhir nanti,\nKau tetap Tuhanku.",
                "Reff:\nSampai memutih rambutku,\nKau setia menopang hidupku.\nTak pernah Kau biarkanku\nbergumul seorang diri.",
                "Sampai habis umurku,\n'ku 'kan tetap setia pada-Mu.\nMenyembah-Mu, meninggikan-Mu,\nYesus, Tuhanku."
            )
        ),
        PresetSong(
            id = "pw_seperti_rusa",
            title = "S'perti Rusa Rindu Sungai-Mu",
            category = "Praise & Worship",
            slides = listOf(
                "S'perti rusa rindu sungai-Mu,\njiwaku rindu Engkau.\nKaulah Tuhan hasrat hatiku,\n'ku rindu menyembah-Mu.",
                "Reff:\nKaulah kekuatan dan perisaiku,\nkepada-Mu rohku berserah.\nKaulah Tuhan hasrat hatiku,\n'ku rindu menyembah-Mu.",
                "Yesus, Yesus, Kau segalanya bagiku;\nYesus, Yesus, tiada lain s'perti-Mu.\nKaulah Tuhan hasrat hatiku,\n'ku rindu menyembah-Mu."
            )
        ),
        PresetSong(
            id = "pw_allah_peduli",
            title = "Allah Peduli",
            category = "Praise & Worship",
            slides = listOf(
                "Banyak perkara yang tak dapat ku mengerti,\nmengapakah harus terjadi\ndi dalam kehidupan ini.",
                "Satu perkara yang kusimpan dalam hati:\ntiada satupun 'kan terjadi\ntanpa Allah peduli.",
                "Reff:\nAllah mengerti, Allah peduli,\nsegala persoalan yang kita hadapi.\nTak akan pernah dibiarkan-Nya\n'ku bergumul sendiri, s'bab Allah peduli."
            )
        ),
        PresetSong(
            id = "pw_janji_mu_fajar",
            title = "Janji-Mu S'perti Fajar",
            category = "Praise & Worship",
            slides = listOf(
                "Ketika kuhadapi kehidupan ini,\njalan mana yang harus kupilih.\n'Ku tahu, 'ku tak mampu,\n'ku tahu, 'ku tak sanggup,\nhanya Kau, Tuhan, tempat jawabanku.",
                "Aku pun tahu 'ku tak pernah sendiri,\ns'bab Engkau, Allah, yang menggendongku.\nTangan-Mu membelaiku, cinta-Mu memuaskanku,\nKau mengangkatku ke tempat yang tinggi.",
                "Reff:\nJanji-Mu s'perti fajar pagi hari,\ndan tiada pernah terlambat bersinar.\nCinta-Mu s'perti sungai yang mengalir,\ndan kutahu betapa dalam kasih-Mu."
            )
        ),
        PresetSong(
            id = "pw_hatiku_percaya",
            title = "Hatiku Percaya",
            category = "Praise & Worship",
            slides = listOf(
                "Saat ku tak melihat jalan-Mu,\nsaat ku tak mengerti rencana-Mu,\nnamun satu hal yang kutahu pasti:\nkasih-Mu tak pernah berkesudahan.",
                "Reff:\nTuhanlah perlindunganku,\nhatiku percaya pada-Mu.\nDi dalam setiap musim hidupku,\nKau Allah yang setia.",
                "Yesus, pada-Mu 'ku berserah,\nYesus, pada-Mu 'ku percaya.\nSegala perkara dapat kutanggung\ndi dalam Kristus yang memb'ri kekuatan."
            )
        ),
        PresetSong(
            id = "pw_ruang_maha_kudus",
            title = "Ku Masuk Ruang Maha Kudus",
            category = "Praise & Worship",
            slides = listOf(
                "'Ku masuk ruang Maha Kudus,\ndengan darah Anak Domba.\n'Ku masuk dengan hati tulus,\nmenyembah Yang Maha Kuasa.",
                "Reff:\n'Ku menyembah-Mu, 'ku menyembah-Mu,\n'ku menyembah-Mu, 'ku menyembah-Mu.\nS'bab nama-Mu kudus, kudus, Tuhan;\ns'bab nama-Mu kudus, kudus, Tuhan."
            )
        )
    )
}
