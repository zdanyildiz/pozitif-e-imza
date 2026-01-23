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

        // 1. Java Komutu Tespiti
        // jpackage ile paketlenmiş uygulamalarda java.home genellikle runtime klasörünü gösterir.
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java.exe";
        
        File javaFile = new File(javaBin);
        boolean useSystemJava = false;
        
        // Eğer java.home altında bulunamazsa, çalışma dizinine (user.dir) göre runtime/bin kontrolü yap
        if (!javaFile.exists()) {
             logger.warn("Bundled java.exe bulunamadi: {}. Alternatif yol (runtime/bin) deneniyor...", javaBin);
             String userDir = System.getProperty("user.dir");
             javaBin = userDir + File.separator + "runtime" + File.separator + "bin" + File.separator + "java.exe";
             javaFile = new File(javaBin);
        }

        // Eğer hala bulunamazsa, sistem PATH'indeki 'java' komutunu deneyelim
        if (!javaFile.exists()) {
            logger.warn("Bundled java.exe yerel dizinlerde bulunamadi. Sistem PATH uzerindeki 'java' komutu denenecek.");
            useSystemJava = true;
        }

        if (useSystemJava) {
            commands.add("java");
        } else {
            logger.info("Secilen Java motoru: {}", javaFile.getAbsolutePath());
            commands.add(javaFile.getAbsolutePath());
        }

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
