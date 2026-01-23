package com.globalpozitif.giblauncher.core.service;

import com.globalpozitif.giblauncher.core.model.JnlpDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcessManager {
    private static final Logger logger = LoggerFactory.getLogger(ProcessManager.class);

    public ProcessBuilder buildProcess(JnlpDoc jnlp, Path libraryPath) throws IOException {
        List<String> commands = new ArrayList<>();

        // 1. Java Komutu Tespiti (En Garanti Yontem: Su an calisan Java'yi kullan)
        String javaBin = ProcessHandle.current().info().command().orElse(null);

        if (javaBin == null || !new File(javaBin).exists()) {
            logger.warn("ProcessHandle üzerinden Java yolu bulunamadi, java.home denemesi yapiliyor...");
            String javaHome = System.getProperty("java.home");
            javaBin = javaHome + File.separator + "bin" + File.separator + "java.exe";
        }

        if (!new File(javaBin).exists()) {
            // Son care: javaw denemesi
            javaBin = javaBin.replace("java.exe", "javaw.exe");
        }

        if (!new File(javaBin).exists()) {
            logger.error("Kritik Hata: Java calistirilabilir dosyasi hiçbir yerde bulunamadi!");
            throw new IOException(
                    "Programin calismasi icin gereken Java motoru bulunamadi. Lutfen loglari kontrol edin.");
        }

        logger.info("Secilen Java (Garantili): {}", javaBin);
        commands.add(javaBin);

        // 2. VM Args
        if (jnlp.getResources() != null && jnlp.getResources().getJ2se() != null) {
            String vmArgs = jnlp.getResources().getJ2se().getJavaVmArgs();
            if (vmArgs != null && !vmArgs.isEmpty()) {
                // VM argümanlarını boşluklara göre ayırıp ekle
                String[] args = vmArgs.split("\\s+");
                for (String arg : args) {
                    if (!arg.trim().isEmpty()) {
                        commands.add(arg.trim());
                    }
                }
            }
        }

        // 3. Classpath (-cp)
        String classpath = buildClasspath(libraryPath);
        if (!classpath.isEmpty()) {
            commands.add("-cp");
            commands.add(classpath);
        }

        // 4. Main Class
        if (jnlp.getApplicationDesc() != null && jnlp.getApplicationDesc().getMainClass() != null) {
            commands.add(jnlp.getApplicationDesc().getMainClass());
        }

        logger.info("Starting process with command: {}", String.join(" ", commands));

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(libraryPath.toFile()); // Çalışma dizinini kütüphanelerin olduğu yer yapalım
        pb.inheritIO(); // Çıktıları ana sürece aktar (debug için kullanışlı olabilir)

        return pb;
    }

    private String buildClasspath(Path libraryPath) throws IOException {
        if (!Files.exists(libraryPath)) {
            return "";
        }

        String separator = File.pathSeparator;
        try (Stream<Path> walk = Files.walk(libraryPath)) {
            return walk
                    .filter(p -> !Files.isDirectory(p))
                    .map(Path::toString)
                    .filter(f -> f.endsWith(".jar"))
                    .collect(Collectors.joining(separator));
        }
    }
}
