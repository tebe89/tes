# Kalkulator Android

Aplikasi kalkulator sederhana untuk Android, ditulis dengan Kotlin tanpa library eksternal.

Project ini ditempatkan di subfolder `kalkulator/` dan di-build oleh workflow `.github/workflows/build-kalkulator.yml`.

## Cara mengunduh & memasang APK

1. Buka tab **Releases** repo ini.
2. Unduh file `Kalkulator-debug.apk` pada bagian **Assets**.
3. Buka file tersebut di HP -> izinkan *instal dari sumber tidak dikenal* -> selesai.

## Fitur kalkulator

- Operasi dasar: tambah (`+`), kurang (`-`), kali (`x`), bagi (`div`)
- Persen (`%`), angka desimal, tanda negatif
- Tombol hapus satu karakter (backspace) dan reset (C)
- Penanganan pembagian nol dan input tidak valid

## Informasi teknis

- Bahasa: Kotlin, UI dibangun secara programatik (tanpa XML layout)
- minSdk 26 (Android 8.0), targetSdk 34 (Android 14)
- Build: Gradle 8.9, Android Gradle Plugin 8.5.2, JDK 17
