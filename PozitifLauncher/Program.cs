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

            // 2. Path Detection (Relative Paths Rule)
            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            // Expected structure: {BaseDir}\bin\jre8_32
            string javaPath = Path.Combine(baseDir, "bin", "jre8_32");
            // Expected structure: {BaseDir}\bin\icedtea\bin\javaws.exe
            string icedTeaPath = Path.Combine(baseDir, "bin", "icedtea", "bin", "javaws.exe");

            // 3. Defensive Coding: Check Critical Component
            if (!File.Exists(icedTeaPath))
            {
                MessageBox.Show($"Critical component missing:\n{icedTeaPath}\n\nPlease reinstall the application.",
                                "Configuration Error", MessageBoxButton.OK, MessageBoxImage.Error);
                return;
            }

            // 4. Initialize WPF Application
            App app = new App();
            app.InitializeComponent();

            // 5. Setup Splash Screen
            // "Splash Screen must be shown... and stay AlwaysOnTop"
            SplashScreen splash = new SplashScreen();

            // 6. Launch Logic
            splash.Loaded += async (s, e) =>
            {
                // Non-blocking Task execution
                await LaunchAndMonitorAsync(icedTeaPath, javaPath, jnlpFilePath, splash);
            };

            app.Run(splash);
        }

        private static async Task LaunchAndMonitorAsync(string exePath, string javaHomePath, string jnlpPath, SplashScreen splash)
        {
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo
                {
                    FileName = exePath,
                    // Arguments Rule: -Xnosplash -headless "{jnlpFilePath}"
                    Arguments = $"-Xnosplash -headless \"{jnlpPath}\"",
                    UseShellExecute = false, // Required for Environment Variables and CreateNoWindow
                    CreateNoWindow = true,   // Hide Console
                    RedirectStandardError = true,
                    RedirectStandardOutput = true
                };

                // Environment Variables Rule: Strictly set for that process
                // UseShellExecute is false, so we can modify EnvironmentVariables
                if (psi.EnvironmentVariables.ContainsKey("ITW_JAVA_HOME"))
                    psi.EnvironmentVariables["ITW_JAVA_HOME"] = javaHomePath;
                else
                    psi.EnvironmentVariables.Add("ITW_JAVA_HOME", javaHomePath);

                if (psi.EnvironmentVariables.ContainsKey("JAVA_HOME"))
                    psi.EnvironmentVariables["JAVA_HOME"] = javaHomePath;
                else
                    psi.EnvironmentVariables.Add("JAVA_HOME", javaHomePath);

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

                    // Monitor strategy: Wait for launch or timeout
                    // "It must close automatically when the javaws process is successfully started and active (or after a 10-15s timeout as a fallback)."
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
                        // If the process has a window handle, it's likely up and running.
                        // Note: -headless might prevent a Main Window for javaws, but the spawned app might show one.
                        // However, we check strictly for javaws state here.
                        // If javaws stays alive for a bit without exiting, we assume success.
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
                            if (string.IsNullOrWhiteSpace(errorMsg)) errorMsg = "Unknown error.";
                            if (errorMsg.Length > 800) errorMsg = errorMsg.Substring(0, 800) + "...";

                            splash.Topmost = false;
                            MessageBox.Show($"The application exited with an error (Code: {process.ExitCode}).\n\nDetails:\n{errorMsg}\n\nPlease contact Global Pozitif Support.",
                                            "Launch Error", MessageBoxButton.OK, MessageBoxImage.Error);
                        }
                        // If ExitCode is 0, it might be a launcher that exits successfully after spawning the main app.
                    }

                    // Close splash screen on success or timeout
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
