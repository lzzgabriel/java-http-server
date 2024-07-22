package demo.http.server.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {

    private static FileManager instance;

    private final Path baseDir;

    private FileManager(String baseDir) {
        this.baseDir = Path.of(baseDir);
    }

    public static void initialize(String baseDir) {
        instance = new FileManager(baseDir);
    }

    public static FileManager getInstance() {
        if (instance == null)
            throw new IllegalStateException("demo.http.server.java.FileManager not initialized");
        return instance;
    }

    public Path getFile(String fileName) {
        return baseDir.resolve(fileName);
    }

    public Path overwriteFile(String fileName) throws IOException {
        var file = baseDir.resolve(fileName);
        Files.deleteIfExists(file);
        Files.createDirectories(baseDir);
        return Files.createFile(file);
    }

}
