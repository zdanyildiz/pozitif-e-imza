using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading.Tasks;
using System.Windows;

namespace PozitifLauncher
{
    public static class Program
    {
        [STAThread]
        public static void Main(string[] args)
        {
            // 1. Argument Validation
            if (args.Length == 0)
            {
                MessageBox.Show("Please drag and drop a .jnlp file onto this application or provide a file path as an argument.",
                                "PozitifLauncher", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            string jnlpFilePath = args[0];

            // 2. Path Detection
            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            string javaPath = Path.Combine(baseDir, "bin", "jre8_32");
            string icedTeaPath = Path.Combine(baseDir, "bin", "icedtea", "bin", "javaws.exe");

            if (!File.Exists(icedTeaPath))
            {
                MessageBox.Show($"Required component not found:\n{icedTeaPath}\n\nPlease reinstall the application.",
                                "Configuration Error", MessageBoxButton.OK, MessageBoxImage.Error);
                return;
            }

            // 3. Set Environment Variables
            try
            {
                Environment.SetEnvironmentVariable("ITW_JAVA_HOME", javaPath);
                Environment.SetEnvironmentVariable("JAVA_HOME", javaPath);
            }
            catch (Exception ex)
            {
                 MessageBox.Show($"Failed to set environment variables: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                 return;
            }

            // 4. Initialize WPF Application
            App app = new App();
            app.InitializeComponent();

            // 5. Setup Splash Screen
            SplashScreen splash = new SplashScreen();

            // 6. Launch Logic
            splash.Loaded += async (s, e) =>
            {
                await LaunchAndMonitorAsync(icedTeaPath, jnlpFilePath, splash);
            };

            app.Run(splash);
        }

        private static async Task LaunchAndMonitorAsync(string exePath, string jnlpPath, SplashScreen splash)
        {
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo
                {
                    FileName = exePath,
                    Arguments = $"-Xnosplash -headless \"{jnlpPath}\"",
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    RedirectStandardError = true,
                    RedirectStandardOutput = true
                };

                using (Process process = new Process { StartInfo = psi })
                {
                    StringBuilder errorOutput = new StringBuilder();
                    process.ErrorDataReceived += (sender, args) =>
                    {
                        if (args.Data != null)
                        {
                            errorOutput.AppendLine(args.Data);
                        }
                    };

                    if (!process.Start())
                    {
                        splash.Topmost = false;
                        MessageBox.Show("Failed to start the Java runtime process.", "Global Pozitif Support", MessageBoxButton.OK, MessageBoxImage.Error);
                        splash.Close();
                        return;
                    }

                    // Begin asynchronous read of the error stream
                    process.BeginErrorReadLine();

                    // Monitor strategy:
                    int timeoutMs = 15000;
                    int checkIntervalMs = 500;
                    int elapsedMs = 0;

                    bool processExited = false;

                    while (elapsedMs < timeoutMs)
                    {
                        if (process.HasExited)
                        {
                            processExited = true;
                            break;
                        }

                        process.Refresh();
                        if (process.MainWindowHandle != IntPtr.Zero)
                        {
                            break;
                        }

                        await Task.Delay(checkIntervalMs);
                        elapsedMs += checkIntervalMs;
                    }

                    if (processExited)
                    {
                        if (process.ExitCode != 0)
                        {
                            string errorMsg = errorOutput.ToString();
                            // Limit error message length
                            if (errorMsg.Length > 500) errorMsg = errorMsg.Substring(0, 500) + "...";

                            splash.Topmost = false;
                            MessageBox.Show($"The application exited with an error (Code: {process.ExitCode}).\n\nDetails:\n{errorMsg}\n\nPlease contact Global Pozitif Support.",
                                            "Launch Error", MessageBoxButton.OK, MessageBoxImage.Error);
                        }
                    }

                    splash.Close();
                }
            }
            catch (Exception ex)
            {
                splash.Topmost = false;
                MessageBox.Show($"An critical error occurred:\n{ex.Message}\n\nPlease contact Global Pozitif Support.",
                                "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                splash.Close();
            }
        }
    }
}
