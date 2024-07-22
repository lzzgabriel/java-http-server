package demo.http.server.java.compression;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public interface Compression {

    byte[] compress(byte[] arr) throws IOException;

    static Compression getCompressionScheme(CompressionScheme scheme) {
        try {
            var clazz = Class.forName("demo.http.server.java.compression.impl" + scheme.name() + "CompressionScheme");
            return (Compression) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | NoSuchMethodException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
