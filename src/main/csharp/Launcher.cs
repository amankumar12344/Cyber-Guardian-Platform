using System;
using System.IO;
using System.Diagnostics;
using System.Reflection;
using System.IO.Compression;

namespace CyberGuardian
{
    class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            try
            {
                // 1. Define temporary extraction folder
                string tempRoot = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Temp");
                string tempFolder = Path.Combine(tempRoot, "Kockroch_Runtime");

                // 2. Get current executable details
                string exePath = Assembly.GetExecutingAssembly().Location;
                if (string.IsNullOrEmpty(exePath))
                {
                    exePath = Process.GetCurrentProcess().MainModule.FileName;
                }
                DateTime exeTime = File.GetLastWriteTime(exePath);

                // 3. Determine if extraction is needed
                string versionFile = Path.Combine(tempFolder, ".version");
                bool needExtract = true;

                if (Directory.Exists(tempFolder) && File.Exists(versionFile) && File.Exists(Path.Combine(tempFolder, "app.jar")))
                {
                    try
                    {
                        string verText = File.ReadAllText(versionFile).Trim();
                        long extractedTicks;
                        if (long.TryParse(verText, out extractedTicks))
                        {
                            DateTime extractedTime = new DateTime(extractedTicks);
                            // If the extracted files are newer or same age as our exe, skip extraction!
                            if (extractedTime >= exeTime)
                            {
                                needExtract = false;
                            }
                        }
                    }
                    catch
                    {
                        needExtract = true;
                    }
                }

                // 4. Perform extraction if required
                if (needExtract)
                {
                    if (Directory.Exists(tempFolder))
                    {
                        try { Directory.Delete(tempFolder, true); } catch { }
                    }
                    Directory.CreateDirectory(tempFolder);

                    // Extract payload zip embedded as a resource
                    Assembly assembly = Assembly.GetExecutingAssembly();
                    using (Stream resourceStream = assembly.GetManifestResourceStream("payload.zip"))
                    {
                        if (resourceStream == null)
                        {
                            throw new Exception("Payload resource not found inside the executable.");
                        }

                        string zipTempPath = Path.Combine(tempFolder, "payload.zip");
                        using (FileStream fileStream = new FileStream(zipTempPath, FileMode.Create, FileAccess.Write))
                        {
                            resourceStream.CopyTo(fileStream);
                        }

                        // Extract using .NET standard zip extraction
                        ZipFile.ExtractToDirectory(zipTempPath, tempFolder);
                        
                        try { File.Delete(zipTempPath); } catch { }
                    }

                    // Write version timestamp
                    File.WriteAllText(versionFile, exeTime.Ticks.ToString());
                }

                // 5. Check and apply external config.json override
                string localConfig = Path.Combine(Path.GetDirectoryName(exePath), "config.json");
                string targetConfig = Path.Combine(tempFolder, "config.json");
                if (File.Exists(localConfig))
                {
                    try
                    {
                        File.Copy(localConfig, targetConfig, true);
                    }
                    catch { }
                }

                // 6. Launch the Java App in the background silently
                string javaBinPath = Path.Combine(tempFolder, "jre", "bin", "javaw.exe");
                string jarPath = Path.Combine(tempFolder, "app.jar");

                if (!File.Exists(javaBinPath) || !File.Exists(jarPath))
                {
                    throw new FileNotFoundException("Required executable files (app.jar or jre/bin/javaw.exe) were not found inside the extracted package.");
                }

                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = javaBinPath;
                psi.Arguments = "-jar \"" + jarPath + "\" --agent";
                psi.WorkingDirectory = tempFolder;
                psi.WindowStyle = ProcessWindowStyle.Hidden;
                psi.CreateNoWindow = true;
                psi.UseShellExecute = false;

                Process.Start(psi);
            }
            catch (Exception ex)
            {
                // Write standard crash log if something critical fails
                try
                {
                    string crashLog = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "launcher_error.txt");
                    File.WriteAllText(crashLog, "Timestamp: " + DateTime.Now.ToString() + "\r\nError: " + ex.ToString());
                }
                catch { }
            }
        }
    }
}
