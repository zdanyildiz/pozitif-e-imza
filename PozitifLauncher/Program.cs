using System;
using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Windows;

namespace PozitifLauncher
{
    public static class Program
    {
        [STAThread]
        public static void Main(string[] args)
        {
            Application app = new Application();
            SplashScreen splash = new SplashScreen();
            
            app.Startup += (s, e) =>
            {
                splash.Show();
                
                Thread backgroundThread = new Thread(() =>
                {
                    bool success = false;
                    try
                    {
                        LaunchJnlp(args, splash);
                        success = true;
                    }
                    catch (Exception ex)
                    {
                        splash.AddLog("--------------------------------------------------");
                        splash.AddLog("KRİTİK HATA OLUŞTU:");
                        splash.AddLog(ex.Message);
                        splash.AddLog("--------------------------------------------------");
                    }
                    finally
                    {
                        // Her durumda kapatma butonunu göster, asla otomatik kapatma!
                        // Böylece kullanıcı ne olduğunu görür.
                        splash.ShowCloseButton();
                    }
                });
                backgroundThread.IsBackground = true;
                backgroundThread.Start();
            };

            app.Run();
        }

        private static void LaunchJnlp(string[] args, SplashScreen splash)
        {
            splash.AddLog("Başlatma rutini tetiklendi.");
            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            
            // --- YOL TANIMLARI ---
            // Release/Debug klasör yapısına göre:
            string assetsDir = Path.Combine(baseDir, "assets");
            splash.AddLog($"Varlık dizini kontrol ediliyor: {assetsDir}");
            
            string javaHome = Path.Combine(assetsDir, "jre8_32");
            string javaExe = Path.Combine(javaHome, "bin", "java.exe");
            string icedTeaBin = Path.Combine(assetsDir, "icedtea", "bin", "javaws.exe"); // Referans için
            
            string jnlpFile = "";
            if (args.Length > 0) jnlpFile = args[0];
            else
            {
                splash.AddLog("UYARI: JNLP dosyası parametrelerden gelmedi.");
                MessageBox.Show("Test modundasınız. Bir .jnlp dosyası seçilmedi.", "Bilgi", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }
            splash.AddLog($"Hedef JNLP: {Path.GetFileName(jnlpFile)}");

            // --- KONTROLLER ---
            if (!Directory.Exists(javaHome))
            {
                splash.AddLog("HATA: JRE klasörü bulunamadı.");
                throw new DirectoryNotFoundException($"Java klasörü (JRE) bulunamadı!\n{javaHome}");
            }

            if (!File.Exists(javaExe))
            {
                 // Mevcut 'java' dosyasını kontrol et (uzantısız)
                string javaNoExe = Path.Combine(javaHome, "bin", "java");
                if (File.Exists(javaNoExe))
                     throw new FileNotFoundException($"HATA: Linux JRE tespit edildi! Windows 32-bit kullanmalısınız.");
                
                throw new FileNotFoundException($"Java çalıştırıcısı bulunamadı!\n{javaExe}");
            }
            splash.AddLog("Java ortamı doğrulandı.");

            // --- JAR YOL TANIMLARI ---
            string shareDir = Path.Combine(assetsDir, "icedtea", "share", "icedtea-web");
            string javawsJar = Path.Combine(shareDir, "javaws.jar");
            string pluginJar = Path.Combine(shareDir, "plugin.jar");
            splash.AddLog($"IcedTea Kütüphaneleri: {shareDir}");
            
            // Classpath oluştur 
            string classPath = $"{javawsJar};{pluginJar}";

            // --- YAPILANDIRMA DOSYASI (CONFIG FILE) ---
            // StackOverflow hatasını önlemek için custom policy yerine
            // IcedTea'nin kendi yapılandırma dosyasını (deployment.properties) düzenliyoruz.
            string userProfile = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            string itwConfigDir = Path.Combine(userProfile, ".config", "icedtea-web");
            
            if (!Directory.Exists(itwConfigDir))
                Directory.CreateDirectory(itwConfigDir);
            
            string propsFile = Path.Combine(itwConfigDir, "deployment.properties");
            string javaHomeEscaped = javaHome.Replace("\\", "/");
            
            try
            {
                // Mevcut içeriği oku veya yeni oluştur
                string content = "";
                if (File.Exists(propsFile))
                    content = File.ReadAllText(propsFile);
                
                // Ayarları güncelle veya ekle
                // Eğer dosya zaten varsa ve ayar varsa değiştirmek daha güvenli ama şimdilik append yapalım, 
                // IcedTea son satırı baz alabilir veya dosya boşsa yazarız.
                // Temiz kurulum için direkt yazmak en garantisi.
                string newConfig = $"deployment.jre.dir={javaHomeEscaped}\n" +
                                   $"deployment.security.level=ALLOW_UNSIGNED\n" +
                                   $"deployment.security.askgrantdialog.notinca=false\n" +
                                   $"deployment.security.jsse.hostmismatch.warning=false\n";
                
                File.WriteAllText(propsFile, newConfig);
                splash.AddLog($"Yapılandırma dosyası güncellendi: {propsFile}");
            }
            catch (Exception ex)
            {
                splash.AddLog($"UYARI: Yapılandırma dosyası yazılamadı: {ex.Message}");
            }

            // --- PROCESS BAŞLATMA (JAVA DOĞRUDAN ÇAĞRILACAK) ---
            ProcessStartInfo psi = new ProcessStartInfo();
            psi.FileName = javaExe;
            psi.WorkingDirectory = baseDir; 
            
            // KRİTİK DÜZELTME: IcedTea JAR'larını Boot Classpath'e ekle
            // Bu sayede "System Jar" olarak algılanacaklar ve SecurityManager döngüsüne girmeyecekler.
            psi.ArgumentList.Add($"-Xbootclasspath/a:{classPath}");
            
            // IcedTea ve JRE için gerekli sistem özellikleri
            psi.ArgumentList.Add($"-Dicedtea-web.bin.name=javaws");
            psi.ArgumentList.Add($"-Dicedtea-web.bin.location={icedTeaBin}");
            
            // Ana Sınıf (Boot Classpath'te olduğu için cp vermeye gerek yok, direkt sınıf adı)
            // DİKKAT: Ana sınıftan önceki argümanlar JVM içindir (örn: -Xbootclasspath, -D...)
            // Ana sınıftan SONRAKİ argümanlar ise uygulamanın kendisine (IcedTea) iletilir.
            psi.ArgumentList.Add("net.sourceforge.jnlp.runtime.Boot");

            // IcedTea Parametreleri (Artık JVM hatası vermez, uygulamaya gider)
            psi.ArgumentList.Add("-Xtrustall");
            psi.ArgumentList.Add("-verbose");
            // -Xnosplash IcedTea tarafından desteklenmiyor olabilir, kaldırıldı.
            
            // Konfigüratif parametreler (Varsa args'dan gelenler)

            // En son JNLP dosyası
            psi.ArgumentList.Add(jnlpFile);
            
            psi.UseShellExecute = false; 
            psi.CreateNoWindow = true;   
            psi.RedirectStandardOutput = true;
            psi.RedirectStandardError = true;

            string debugCmd = $"\"{psi.FileName}\" {string.Join(" ", psi.ArgumentList)}";
            splash.AddLog("Komut hazırlandı. Java işlemi başlatılıyor...");

            Process proc = new Process();
            proc.StartInfo = psi;

            // Logları yakala
            proc.OutputDataReceived += (s, e) => { if (!string.IsNullOrEmpty(e.Data)) splash.AddLog($"[STDOUT] {e.Data}"); };
            proc.ErrorDataReceived += (s, e) => { if (!string.IsNullOrEmpty(e.Data)) splash.AddLog($"[STDERR] {e.Data}"); };

            if (proc.Start())
            {
                splash.AddLog($"PID: {proc.Id} ile Java başlatıldı. Çıktılar bekleniyor...");
                proc.BeginOutputReadLine();
                proc.BeginErrorReadLine();
                
                proc.WaitForExit();
                splash.AddLog($"İşlem sonlandı. Çıkış Kodu: {proc.ExitCode}");

                if (proc.ExitCode != 0)
                {
                    throw new Exception($"Uygulama {proc.ExitCode} kodu ile kapandı. Logları inceleyiniz.");
                }
            }
            else
            {
                throw new Exception("Process başlatılamadı (proc.Start false döndü).");
            }
        }
    }
}