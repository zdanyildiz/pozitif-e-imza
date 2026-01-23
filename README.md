# 📦 Pozitif E-İmza Launcher

**Global Pozitif Teknolojiler** tarafından geliştirilen, muhasebe ve finans profesyonellerinin GİB (Gelir İdaresi Başkanlığı) e-Belge uygulamalarına erişimini modernize eden, güvenli masaüstü başlatıcı (launcher) projesidir.

## 🚀 Projenin Amacı

Mevcut durumda `EFaturaWebSocket.jnlp` dosyasını çalıştırmak için kullanıcıların bilgisayarlarında eski Java sürümlerini tutmaları, güvenlik ayarlarını düşürmeleri ve sürekli tarayıcı/sertifika hatalarıyla boğuşmaları gerekmektedir.

**Pozitif E-İmza Launcher**, bu süreci şu şekilde devrimselleştirir:

1. **Gömülü JVM:** Kullanıcının bilgisayarında Java kurulu olmasına gerek yoktur; uygulama kendi izole ve optimize edilmiş Java Runtime Environment (JRE) ile gelir.
2. **Otomatik JNLP Yönetimi:** `.jnlp` dosyalarını analiz eder, gerekli kütüphaneleri indirir ve doğru parametrelerle başlatır.
3. **Sorunsuz Çalışma:** Tarayıcı kısıtlamalarına takılmadan, GİB e-imza uygulamasının ihtiyaç duyduğu ortamı sağlar.

---

## 🏗️ Mimari ve Teknoloji Yığını

Bu proje **KISS (Keep It Simple, Stupid)** prensibine sadık kalarak, `OpenWebStart` mimarisinin "lite" ve GİB özel versiyonu olarak tasarlanmıştır. Karmaşık veritabanı veya ağır framework'ler içermez.

| Bileşen | Teknoloji | Açıklama |
| --- | --- | --- |
| **Çekirdek Dil** | Java 17 LTS | Uzun süreli destek ve yüksek performans için. |
| **UI Framework** | JavaFX | Modern ve responsive yükleme ekranları için. |
| **Ağ Katmanı** | Apache HttpClient | Güvenli JAR indirme işlemleri için. |
| **XML Parser** | Jackson | JNLP yapısını parse etmek için. |
| **Launcher** | ProcessBuilder | İzole process yönetimi için. |

---

## ⚙️ Çalışma Mantığı

Uygulama başlatıldığında aşağıdaki akış (flow) çalışır:

1. **Parse:** Gömülü veya uzaktan çekilen `EFaturaWebSocket.jnlp` dosyası okunur.
    * *Codebase:* `https://ebelge.gib.gov.tr/EFaturaWebSocket/` adresi temel alınır.
    * *Kaynaklar:* Gerekli kütüphaneler (örn: `bcprov-jdk15to18-*.jar`, `jetty-all.jar`) listelenir.

2. **Download & Cache:**
    * Yerel `cache` klasörü (`user.home/.giblauncher/cache`) kontrol edilir.
    * Dosya mevcut değilse veya bozuksa GİB sunucularından indirilir.
    * *Basit Cache:* İndirilen dosyalar tekrar tekrar indirilmez, hız kazandırır.

3. **Execution (ProcessBuilder):**
    * JNLP içinde belirtilen `main-class` (tr.com.cs.imz.websocket.ImzWebSocketMain) hazırlanır.
    * Classpath (`-cp`) yerel cache klasöründeki JAR dosyalarına göre oluşturulur.
    * Bellek ayarları (`-Xmx2048m`) ve JVM argümanları (`-XX:+UseG1GC`) parametre olarak eklenir.
    * Uygulama, kullanıcının sisteminden bağımsız, izole bir Java süreci olarak başlatılır.

---

## 🛡️ Güvenlik

* **Whitelist Koruması:** Launcher sadece `ebelge.gib.gov.tr` domaininden gelen kaynakları indirir ve çalıştırır.
* **İzole Ortam:** Launcher ve İmzacı uygulaması ayrı süreçlerde çalışır.

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
│   │   ├── ui/         # JavaFX arayüzleri
│   │   └── Main.java   # Entry Point
│   └── resources/
```

---

## 📝 Yol Haritası (Roadmap)

* [x] **v0.1 (MVP):** JNLP Parse etme ve JAR'ları indirme.
* [x] **v0.5:** ProcessBuilder ile uygulamayı ayağa kaldırma.
* [x] **v0.6:** Kullanıcı Giriş Ekranı (Login) ve API Entegrasyonu.
* [ ] **v1.0:** `jpackage` ile .exe üretimi ve Release.

---

**Pozitif Architect Notu:** *Bu projede "Legacy Code" (Miras Kod) barındırmak yasaktır. PSR standartlarına (Java karşılığı Google Java Style) uyulmalı ve kod sadeliği korunmalıdır.*

