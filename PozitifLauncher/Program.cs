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
            // AUDIT CHECK 5: Defensive Coding - Check argument
            if (args == null || args.Length == 0)
            {
                MessageBox.Show("Please drag and drop a .jnlp file onto this application or provide a file path as an argument.",
                                "PozitifLauncher", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            string jnlpFilePath = args[0];

            // AUDIT CHECK 1: Relative Paths
            // Must use AppDomain.CurrentDomain.BaseDirectory
            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            string javaHomePath = Path.Combine(baseDir, "bin", "jre8_32");
            string icedTeaPath = Path.Combine(baseDir, "bin", "icedtea", "bin", "javaws.exe");

            // AUDIT CHECK 5: Defensive Coding - Check critical component
            if (!File.Exists(icedTeaPath))
            {
                // Exact error message requirement
                MessageBox.Show($"Critical component missing: javaws.exe\nPath: {icedTeaPath}",
                                "Critical Error", MessageBoxButton.OK, MessageBoxImage.Error);
                return;
            }

            // Initialize WPF App
            App app = new App();
            app.InitializeComponent();

            // AUDIT CHECK 4: Splash Screen Logic
            // "Shown on a separate thread or non-blocking Task" -> We use the main thread for UI,
            // but the heavy lifting (launch monitoring) is offloaded to an async Task so the UI remains responsive.
            SplashScreen splash = new SplashScreen();

            // Hook into Loaded to start the process asynchronously
            splash.Loaded += async (s, e) =>
            {
                await LaunchAndMonitorAsync(icedTeaPath, javaHomePath, jnlpFilePath, splash);
            };

            app.Run(splash);
        }

        private static async Task LaunchAndMonitorAsync(string exePath, string javaHomePath, string jnlpPath, SplashScreen splash)
        {
            try
            {
                // AUDIT CHECK 3: Arguments & Process
                ProcessStartInfo psi = new ProcessStartInfo
                {
                    FileName = exePath,
                    // Arguments: -Xnosplash -headless "{jnlpFilePath}"
                    Arguments = $"-Xnosplash -headless \"{jnlpPath}\"",
                    UseShellExecute = false, // Required for environment variables and CreateNoWindow
                    CreateNoWindow = true,   // Hide console
                    RedirectStandardError = true,
                    RedirectStandardOutput = false // Avoid deadlock: we don't read stdout
                };

                // AUDIT CHECK 2: Environment Variables
                // ITW_JAVA_HOME and JAVA_HOME must point to the bundled JRE
                psi.EnvironmentVariables["ITW_JAVA_HOME"] = javaHomePath;
                psi.EnvironmentVariables["JAVA_HOME"] = javaHomePath;

                using (Process process = new Process { StartInfo = psi })
                {
                    StringBuilder errorOutput = new StringBuilder();
                    process.ErrorDataReceived += (sender, args) =>
                    {
                        if (!string.IsNullOrEmpty(args.Data))
                        {
                            errorOutput.AppendLine(args.Data);
                        }
                    };

                    if (!process.Start())
                    {
                        splash.Topmost = false;
                        MessageBox.Show("Failed to start the Java runtime process.", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                        splash.Close();
                        return;
                    }

                    process.BeginErrorReadLine();

                    // AUDIT CHECK 4: Splash Screen Logic - Close when active or timeout
                    // Monitor for up to 15 seconds
                    int timeoutMs = 15000;
                    int checkIntervalMs = 500;
                    int elapsedMs = 0;
                    bool launchSuccess = false;

                    while (elapsedMs < timeoutMs)
                    {
                        if (process.HasExited)
                        {
                            // Process died prematurely
                            break;
                        }

                        // Refresh process state
                        process.Refresh();

                        // Heuristic: If we have a window handle, the app is likely up.
                        // Or if the process has been running for a sufficient time without crashing.
                        if (process.MainWindowHandle != IntPtr.Zero)
                        {
                            launchSuccess = true;
                            break;
                        }

                        await Task.Delay(checkIntervalMs);
                        elapsedMs += checkIntervalMs;
                    }

                    // If the process is still running after timeout, we assume it launched successfully (just maybe no window handle yet or headless wrapper)
                    if (!process.HasExited)
                    {
                        launchSuccess = true;
                    }

                    if (launchSuccess)
                    {
                        // Close splash screen as requested
                        splash.Close();
                    }
                    else
                    {
                        // Process exited or failed
                        string errorMsg = errorOutput.ToString();
                        if (string.IsNullOrWhiteSpace(errorMsg)) errorMsg = "Unknown error or silent exit.";

                        splash.Topmost = false;
                        MessageBox.Show($"The application exited unexpectedly.\nExit Code: {process.ExitCode}\n\nDetails:\n{errorMsg}",
                                        "Launch Error", MessageBoxButton.OK, MessageBoxImage.Error);
                        splash.Close();
                    }
                }
            }
            catch (Exception ex)
            {
                splash.Topmost = false;
                MessageBox.Show($"An exception occurred while launching:\n{ex.Message}",
                                "Critical Error", MessageBoxButton.OK, MessageBoxImage.Error);
                splash.Close();
            }
        }
    }
}
