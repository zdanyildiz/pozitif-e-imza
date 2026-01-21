# 📦 Pozitif E-İmza Launcher

**Global Pozitif Teknolojiler** tarafından geliştirilen, muhasebe ve finans profesyonellerinin GİB (Gelir İdaresi Başkanlığı) e-Belge uygulamalarına erişimini modernize eden, güvenli masaüstü başlatıcı (launcher) projesidir.

## 🚀 Projenin Amacı

Mevcut durumda `EFaturaWebSocket.jnlp` dosyasını çalıştırmak için kullanıcıların bilgisayarlarında eski Java sürümlerini tutmaları, güvenlik ayarlarını düşürmeleri ve sürekli tarayıcı/sertifika hatalarıyla boğuşmaları gerekmektedir.

**Pozitif E-İmza Launcher**, bu süreci şu şekilde devrimselleştirir:

1. **Gömülü JVM:** Kullanıcının bilgisayarında Java kurulu olmasına gerek yoktur; uygulama kendi izole ve optimize edilmiş Java Runtime Environment (JRE) ile gelir.
2. **Otomatik Güncelleme:** GİB sunucularındaki değişiklikleri anlık algılar.
3. **Kesintisiz Erişim:** `.jnlp` dosya ilişkilendirme sorunlarını ortadan kaldırır.

---

## 🏗️ Mimari ve Teknoloji Yığını

Bu proje **KISS (Keep It Simple, Stupid)** prensibine sadık kalarak, `OpenWebStart` mimarisinin "lite" ve GİB özel versiyonu olarak tasarlanmıştır.

| Bileşen | Teknoloji | Açıklama |
| --- | --- | --- |
| **Çekirdek Dil** | Java 17 LTS | Uzun süreli destek ve yüksek performans için. |
| **UI Framework** | JavaFX | Modern, responsive yükleme ekranları ve log arayüzü için. |
| **Veri Saklama** | SQLite | Versiyon takibi, cache yönetimi ve audit loglar için yerel DB. |
| **Ağ Katmanı** | Apache HttpClient | Güvenli JAR indirme ve SSL Handshake yönetimi için. |
| **XML Parser** | Jackson / JAXB | JNLP yapısını parse etmek için. |
| **Dağıtım** | jpackage | Windows (.exe) ve macOS (.dmg) için native installer üretimi. |

---

## ⚙️ Çalışma Mantığı

Uygulama başlatıldığında aşağıdaki akış (flow) çalışır:

1. **Parse:** Gömülü veya uzaktan çekilen `EFaturaWebSocket.jnlp` dosyası okunur.
* 
*Codebase:* `https://ebelge.gib.gov.tr/EFaturaWebSocket/` adresi doğrulanır.


* 
*Kaynaklar:* Gerekli kütüphaneler (örn: `bcprov-jdk15to18-1.79.jar`, `jetty-all.jar`) listelenir.




2. **Sync & Cache:**
* Yerel `cache` klasörü taranır.
* Sunucudaki dosyaların MD5/SHA hash'leri kontrol edilir. Sadece değişen dosyalar indirilir.


3. **Security Check:**
* İndirilen JAR dosyalarının imzaları (GİB sertifikası) doğrulanır.


4. **Execution (ProcessBuilder):**
* JNLP içinde belirtilen `main-class` olan `tr.com.cs.imz.websocket.ImzWebSocketMain` tetiklenir.


* Bellek ayarları (`-Xms512m -Xmx2048m`) ve GC ayarları (`-XX:+UseG1GC`) parametre olarak eklenir.





---

## 🛡️ Güvenlik Politikası

Finansal veri işlendiği için güvenlik "Feature" değil, "Zorunluluktur".

* **Whitelist Koruması:** Launcher sadece `ebelge.gib.gov.tr` domaininden gelen kaynakları kabul eder.
* 
**Hassas Veri Temizliği:** Uygulama kapandığında, JNLP konfigürasyonunda belirtilen heap dump dosyaları (`user.home/efatura-websocket-heapdump.hprof`) güvenlik gereği kontrol edilir/temizlenir.


* **İzole Ortam:** Launcher ve İmzacı uygulaması ayrı Process ID (PID) altında çalışır. Launcher çökse bile imzalama işlemi yarıda kalmaz.

---

## 💻 Geliştirici Kurulumu (Developer Setup)

Projeyi geliştirmek için aşağıdaki adımları izleyin:

### Gereksinimler

* JDK 17+
* Maven 3.8+
* IntelliJ IDEA (Önerilen)

### Kurulum

```bash
# Repoyu klonla
git clone https://github.com/GlobalPozitif/PozitifEImza.git

# Bağımlılıkları indir
mvn clean install

# Uygulamayı Dev modunda başlat
mvn javafx:run

```

---

## 📂 Klasör Yapısı (Project Structure)

```text
src/
├── main/
│   ├── java/com/globalpozitif/giblauncher/
│   │   ├── core/       # JNLP Parser ve Downloader mantığı
│   │   ├── security/   # İmza doğrulama ve Hash kontrolü
│   │   ├── ui/         # JavaFX arayüzleri
│   │   └── Main.java   # Entry Point
│   └── resources/
│       ├── config.xml  # Varsayılan ayarlar
│       └── db/         # SQLite migrasyon dosyaları

```

---

## 📝 Yol Haritası (Roadmap)

* [ ] **v0.1 (MVP):** JNLP Parse etme ve JAR'ları indirme.
* [ ] **v0.5:** ProcessBuilder ile uygulamayı ayağa kaldırma.
* [ ] **v0.8:** SQLite entegrasyonu ve Cache mekanizması.
* [ ] **v1.0:** `jpackage` ile .exe üretimi ve Release.

---

**Pozitif Architect Notu:** *Bu projede "Legacy Code" (Miras Kod) barındırmak yasaktır. PSR standartlarına (Java karşılığı Google Java Style) uyulmalı ve her commit öncesi Unit Testler geçilmelidir.*

