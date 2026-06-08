# SmartPress 💡📶

**SmartPress** adalah solusi monitoring dan kontrol lampu pintar berbasis IoT (Internet of Things) yang dirancang khusus untuk meningkatkan efisiensi energi listrik pada Mushola. Proyek ini merupakan bagian dari **Project Based Learning (PBL) - Kelompok 3 (Tim 3)**.

Sistem ini terdiri dari dua bagian utama:
1. **IoT Firmware (ESP32)**: Mengendalikan lampu fisik, membaca sensor intensitas cahaya (LDR), serta mengukur parameter kelistrikan menggunakan sensor PZEM-004T.
2. **Android Application**: Aplikasi mobile berbasis native Java untuk memantau konsumsi energi, mengatur jadwal, mengubah mode kontrol, dan menerima notifikasi real-time melalui Firebase.

---

## 🚀 Fitur Utama

### 📱 1. Aplikasi Android (Mobile App)
* **Real-time Monitoring & Control**: Memantau status lampu (ON/OFF) serta mengontrol lampu secara manual dari jarak jauh.
* **Dual Mode (Manual & Otomatis)**: 
  * **Manual**: Kontrol ON/OFF langsung oleh pengguna.
  * **Otomatis**: Lampu dikendalikan oleh sensor cahaya (LDR) berdasarkan tingkat kegelapan lingkungan.
* **Scheduling (Jadwal Lampu)**: Mengatur waktu otomatis kapan lampu menyala dan mati secara terjadwal.
* **Statistik Konsumsi Listrik**:
  * Menampilkan data real-time untuk **Tegangan (V)**, **Arus (A)**, dan **Daya (Watt)**.
  * Menghitung **Akumulasi Energi (kWh)** harian, mingguan, dan bulanan.
  * Menyajikan **Estimasi Biaya** listrik berdasarkan tarif PLN per kWh.
* **Laporan Aktivitas & PDF**: Menyimpan riwayat aktivitas kontrol lampu dan mengunduh laporan bulanan dalam format **PDF**.
* **Keamanan & Manajemen Pengguna**:
  * Registrasi & Login (termasuk integrasi **Facebook Login SDK**).
  * Manajemen User oleh **Admin** (tambah, edit, dan hapus user pendukung).
  * Notifikasi peringatan jika lampu menyala terlalu lama (*overtime*) atau terjadi beban berlebih (*overload*).

### 🛠️ 2. IoT Firmware (ESP32)
* **Konektivitas Firebase**: Sinkronisasi data real-time secara dua arah antara hardware dan aplikasi.
* **Proteksi Kelistrikan**: Mematikan lampu secara otomatis ketika terdeteksi beban berlebih (*overload*).
* **Heartbeat Mechanism**: Menjaga status koneksi (*Online/Offline*) antara ESP32 dengan Cloud agar aplikasi dapat mengetahui kondisi perangkat.

---

## 🔌 Detail Hardware & Wiring (ESP32)

Berikut adalah komponen utama yang digunakan serta pin mapping pada ESP32:

| Komponen | Pin ESP32 | Deskripsi |
| :--- | :--- | :--- |
| **Relay Module** | `GPIO 26` | Mengontrol saklar lampu (Active Low) |
| **Sensor LDR** | `GPIO 34` | Mendeteksi intensitas cahaya (Analog ADC) |
| **PZEM-004T (RX)** | `GPIO 16` | Komunikasi Serial (RX2) dengan sensor daya |
| **PZEM-004T (TX)** | `GPIO 17` | Komunikasi Serial (TX2) dengan sensor daya |

---

## 📂 Struktur Repositori

```text
PBL-SmartPress/
├── app/                      # Source code aplikasi Android (Java)
│   ├── src/main/java/...     # Logic & Activity (MainActivity, DetailActivity, dll.)
│   └── src/main/res/         # UI Layouts, Icons, dan Resources (strings.xml)
├── esp32_smart_lamp.ino       # Firmware Arduino/ESP32
├── build.gradle.kts          # Konfigurasi Gradle Project
└── README.md                 # Dokumentasi proyek
```

---

## 🛠️ Panduan Instalasi & Konfigurasi

### 1. Konfigurasi Hardware (ESP32)
1. Buka file [esp32_smart_lamp.ino](file:///c:/Users/USER/PBL-SmartPress/esp32_smart_lamp.ino) di Arduino IDE.
2. Install library yang dibutuhkan melalui **Library Manager**:
   * `Firebase ESP32 Client`
   * `PZEM-004T v3.0`
3. Sesuaikan konfigurasi Wi-Fi dan Firebase pada kode program:
   ```cpp
   #define WIFI_SSID "NAMA_WIFI"
   #define WIFI_PASSWORD "PASSWORD_WIFI"
   #define FIREBASE_HOST "https://smartpress-ea81d-default-rtdb.asia-southeast1.firebasedatabase.app/"
   #define FIREBASE_AUTH "DATABASE_SECRET_ATAU_TOKEN"
   ```
4. Upload program ke papan ESP32 Anda.

### 2. Konfigurasi Aplikasi Android
1. Buka folder `/app` menggunakan **Android Studio**.
2. Pastikan file `google-services.json` dari Firebase Console sudah diletakkan di dalam direktori `app/` untuk menghubungkan aplikasi dengan database Firebase.
3. Lakukan sinkronisasi Gradle (**Sync Project with Gradle Files**).
4. Run/Build aplikasi ke perangkat Android Anda.

---

## 📊 Skema Database Firebase Realtime
Data disinkronisasikan di bawah path root `monitoring/perangkat_utama` dengan struktur berikut:

```json
{
  "monitoring": {
    "perangkat_utama": {
      "lamp_status": false,
      "auto_mode": false,
      "sensor_lux": 1500,
      "ldr_threshold": 2500,
      "sensor_voltage": 220.5,
      "sensor_current": 0.25,
      "sensor_power": 55.0,
      "sensor_energy": 0.32,
      "timer_enabled": false,
      "timer_on": "18:00",
      "timer_off": "06:00",
      "is_connected": true,
      "is_connected_tick": 1717834500000
    }
  }
}
```

---

## 👥 Tim Pengembang
* **Kelompok PBL - Tim 3**
* Aplikasi dikembangkan oleh **Sofiani** & Tim.
* Versi Aplikasi: `v1.1.0`

---
*SmartPress - Solusi Cerdas untuk Efisiensi Energi Rumah Ibadah.* 🌟
