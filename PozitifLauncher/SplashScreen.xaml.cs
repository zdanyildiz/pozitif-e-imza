using System.Windows;

namespace PozitifLauncher
{
    /// <summary>
    /// Interaction logic for SplashScreen.xaml
    /// </summary>
    public partial class SplashScreen : Window
    {
        public SplashScreen()
        {
            InitializeComponent();
        }

        public void AddLog(string message)
        {
            try
            {
                // Log dosyasına yaz
                string logFile = System.IO.Path.Combine(System.AppDomain.CurrentDomain.BaseDirectory, "PozitifLauncher.log");
                System.IO.File.AppendAllText(logFile, $"[{System.DateTime.Now:HH:mm:ss}] {message}{System.Environment.NewLine}");
            }
            catch { /* Log yazma hatası UI'ı bozmasın */ }

            Dispatcher.Invoke(() =>
            {
                LogText.AppendText($"\n[{System.DateTime.Now:HH:mm:ss}] {message}");
                LogText.ScrollToEnd();
            });
        }

        public void ShowCloseButton()
        {
            Dispatcher.Invoke(() =>
            {
                ActionButtons.Visibility = Visibility.Visible;
                Progress.IsIndeterminate = false;
                Progress.Value = 100;
                StatusText.Text = "İşlem Tamamlandı";
            });
        }

        private void CloseBtn_Click(object sender, RoutedEventArgs e)
        {
            Application.Current.Shutdown();
        }

        private void CopyBtn_Click(object sender, RoutedEventArgs e)
        {
            try 
            {
                Clipboard.SetText(LogText.Text);
                
                // Ayrıca masaüstüne kaydet
                string desktopPath = System.Environment.GetFolderPath(System.Environment.SpecialFolder.Desktop);
                string logFile = System.IO.Path.Combine(desktopPath, "PozitifLauncher_Log.txt");
                System.IO.File.WriteAllText(logFile, LogText.Text);
                
                MessageBox.Show($"Loglar kopyalandı ve Masaüstüne kaydedildi:\n{logFile}", "Bilgi", MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (System.Exception ex)
            {
                MessageBox.Show("Kopyalama hatası: " + ex.Message);
            }
        }
    }
}
