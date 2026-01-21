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
            // Uygulamayı manuel başlatıyoruz
            Application app = new Application();
            // Splash ekranını oluştur (Senin SplashScreen.xaml dosyanı kullanır)
            SplashScreen splash = new SplashScreen();
            
            app.Startup += (s, e) =>
            {
                splash.Show(); // Arayüzü göster
                
                // Ağır işi arka planda yap
                Thread backgroundThread = new Thread(() =>
                {
                    try
                    {
                        LaunchJnlp(args);
                    }
                    catch (Exception ex)
                    {
                        MessageBox.Show($"Başlatma Hatası: {ex.Message}", "Global Pozitif Launcher", MessageBoxButton.OK, MessageBoxImage.Error);
                    }
                    finally
                    {
                        // İş bitince uygulamayı kapat
                        app.Dispatcher.Invoke(() => app.Shutdown());
                    }
                });
                backgroundThread.IsBackground = true;
                backgroundThread.Start();
            };

            app.Run();
        }

        private static void LaunchJnlp(string[] args)
        {
            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            
            // Yollar: Çıktı klasöründeki (bin/Debug/...) assets klasörünü gösterir
            string javaHome = Path.Combine(baseDir, "assets", "jre8_32");
            string icedTeaBin = Path.Combine(baseDir, "assets", "icedtea", "bin", "javaws.exe");

            // Basit Kontroller
            if (!File.Exists(icedTeaBin))
            {
                throw new FileNotFoundException($"Motor dosyası bulunamadı!\nAranan: {icedTeaBin}");
            }

            string jnlpFile = "";
            if (args.Length > 0)
            {
                jnlpFile = args[0];
            }
            else
            {
                MessageBox.Show("Lütfen bir .jnlp dosyasına çift tıklayarak açın.", "Dosya Bekleniyor", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            // İşlemi Hazırla
            ProcessStartInfo psi = new ProcessStartInfo();
            psi.FileName = icedTeaBin;
            
            // IcedTea için kritik ortam değişkenleri (Sistemi yoksay, bunu kullan)
            psi.EnvironmentVariables["JAVA_HOME"] = javaHome;
            psi.EnvironmentVariables["ITW_JAVA_HOME"] = javaHome; 
            
            // Parametreler: Logoyu gizle, sessiz çalış
            psi.Arguments = $"-Xnosplash \"{jnlpFile}\""; 
            
            psi.UseShellExecute = false; 
            psi.CreateNoWindow = true;   

            Process proc = Process.Start(psi);

            if (proc != null)
            {
                // Java açılana kadar splash ekranını biraz beklet (4 saniye)
                Thread.Sleep(4000); 
            }
        }
    }
}