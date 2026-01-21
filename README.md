# Pozitif E-İmza JNLP Launcher

**Global Pozitif Teknolojiler A.Ş.** tarafından geliştirilen, muhasebe ve finans profesyonelleri için "Tek Tıkla" JNLP (Java Network Launch Protocol) başlatıcı.

## 🎯 Amaç
Son kullanıcıyı Java sürüm uyumsuzluklarından, güvenlik uyarılarından ve karmaşık yapılandırmalardan kurtarmak. E-Defter, E-Fatura ve Kurumsal Java uygulamalarını (Uyap vb.) sorunsuz çalıştıran, taşınabilir (portable) bir motor sunar.

## 🏗 Mimari
Bu proje bir **"Wrapper" (Kabuk)** uygulamasıdır.
- **Dil:** C# (.NET 6.0 - Windows Desktop)
- **Arayüz:** WPF (Modern Splash Screen için)
- **Motor:** IcedTea-Web (Open Source JNLP Implementation)
- **Yakıt:** OpenJDK 8 (32-Bit) - (Mali Mühür/AKİS uyumluluğu için özel seçim)

## 🚀 Çalışma Mantığı
1. Uygulama `.jnlp` dosyası ile ilişkilendirilir.
2. Kullanıcı dosyaya çift tıkladığında `PozitifLauncher.exe` devreye girer.
3. Launcher, kendi içinde gömülü/paketlenmiş olan Java ve IcedTea yollarını bulur.
4. Sistemdeki Java'ya dokunmadan, tamamen izole bir ortamda JNLP dosyasını çalıştırır.
5. Kullanıcıya "Pozitif" markalı bir yükleme ekranı gösterir ve arkaplandaki karmaşayı gizler.

## 🛠 Geliştirme Ortamı
- Visual Studio 2022 veya JetBrains Rider
- .NET 6.0 SDK
- Inno Setup 6 (Dağıtım paketi oluşturmak için)

## ⚠️ Önemli Notlar
- `/assets` klasörü lisans ve boyut nedeniyle Git reposuna dahil edilmemiştir.
- Derleme öncesi `assets/jre8_32` ve `assets/icedtea` klasörlerinin proje çıktısına kopyalandığından emin olun.

---
*© 2026 Global Pozitif Teknolojiler A.Ş. - Tüm Hakları Saklıdır.*
