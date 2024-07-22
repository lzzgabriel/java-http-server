package demo.http.server.java.compression.impl;

import demo.http.server.java.compression.Compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class GZIPCompressionScheme implements Compression {

    @Override
    public byte[] compress(byte[] original) throws IOException {
        var baos = new ByteArrayOutputStream();
        var gzip = new GZIPOutputStream(baos);
        gzip.write(original);
        gzip.close();
        return baos.toByteArray();
    }
}
