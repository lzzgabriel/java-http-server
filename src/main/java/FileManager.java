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
            throw new IllegalStateException("FileManager not initialized");
        return instance;
    }

    public Path getFile(String fileName) {
        return baseDir.resolve(fileName);
    }

}
