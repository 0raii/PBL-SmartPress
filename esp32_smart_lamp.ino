#include <WiFi.h>
#include <FirebaseESP32.h>
#include <PZEM004Tv30.h>

// --- KONFIGURASI WIFI ---
#define WIFI_SSID "NAMA_WIFI"
#define WIFI_PASSWORD "PASSWORD_WIFI"

// --- KONFIGURASI FIREBASE ---
#define FIREBASE_HOST "https://smartpress-ea81d-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define FIREBASE_AUTH "DATABASE_SECRET_ATAU_TOKEN"

// --- PIN HARDWARE ---
#define RELAY_PIN 26
#define LDR_PIN 34
#define PZEM_RX_PIN 16
#define PZEM_TX_PIN 17

// --- OBJEK ---
PZEM004Tv30 pzem(Serial2, PZEM_RX_PIN, PZEM_TX_PIN);
FirebaseData firebaseData;
FirebaseAuth auth;
FirebaseConfig config;

// --- VARIABEL GLOBAL ---
int ldrThreshold = 2500;
bool autoMode = false;
bool timerEnabled = false;
bool lampStatus = false;
String timerOn = "18:00";
String timerOff = "06:00";
unsigned long lastHeartbeat = 0;

void setup() {
  Serial.begin(115200);
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, HIGH); // Off (Active Low Relay)

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi Connected!");

  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Initial Sync
  syncSettings();
}

void loop() {
  if (Firebase.ready()) {
    // 1. Baca Sensor PZEM
    float voltage = pzem.voltage();
    float current = pzem.current();
    float power = pzem.power();
    float energy = pzem.energy();

    if (!isnan(voltage)) {
      Firebase.setDouble(firebaseData, "/sensor_voltage", voltage);
      Firebase.setDouble(firebaseData, "/sensor_current", current);
      Firebase.setDouble(firebaseData, "/sensor_power", power);
      Firebase.setDouble(firebaseData, "/sensor_energy", energy);
    }

    // 2. Baca Sensor LDR
    int ldrValue = analogRead(LDR_PIN);
    Firebase.setInt(firebaseData, "/sensor_lux", ldrValue);

    // 3. Heartbeat (Setiap 5 detik)
    if (millis() - lastHeartbeat > 5000) {
      Firebase.setTimestamp(firebaseData, "/is_connected_tick");
      Firebase.setBool(firebaseData, "/is_connected", true);
      lastHeartbeat = millis();
      syncSettings(); // Update settings dari Firebase
    }

    // 4. Logika Kontrol
    if (autoMode) {
      // Prioritas LDR (Gelap nyala, Terang mati)
      if (ldrValue > ldrThreshold) {
        setLamp(true);
      } else {
        setLamp(false);
      }
    } else {
      // Manual Mode: Ambil dari Firebase
      if (Firebase.getBool(firebaseData, "/lamp_status")) {
        setLamp(firebaseData.boolData());
      }
    }
  }
}

void setLamp(bool on) {
  lampStatus = on;
  digitalWrite(RELAY_PIN, on ? LOW : HIGH); // Active Low
  Firebase.setBool(firebaseData, "/lamp_status", on);
}

void syncSettings() {
  if (Firebase.getInt(firebaseData, "/ldr_threshold")) {
    ldrThreshold = firebaseData.intData();
  }
  if (Firebase.getBool(firebaseData, "/auto_mode")) {
    autoMode = firebaseData.boolData();
  }
  if (Firebase.getString(firebaseData, "/timer_on")) {
    timerOn = firebaseData.stringData();
  }
  if (Firebase.getBool(firebaseData, "/timer_enabled")) {
    timerEnabled = firebaseData.boolData();
  }
}
