package demo.http.server.java.compression;

public enum CompressionScheme {

    GZIP;

    public static boolean isSupported(String name) {
        if (name == null || name.isBlank()) return false;
        for (var val : CompressionScheme.values()) {
            if (val.name().equalsIgnoreCase(name.trim())) return true;
        }
        return false;
    }

}
