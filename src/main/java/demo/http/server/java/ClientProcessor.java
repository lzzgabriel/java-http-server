package demo.http.server.java;

import demo.http.server.java.compression.Compression;
import demo.http.server.java.compression.CompressionScheme;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

public class ClientProcessor implements Runnable {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Socket client;

    public ClientProcessor(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        System.out.println("accepted new connection");

        try {
            byte[] response = parseRequest();

            client.getOutputStream().write(response);
            client.shutdownOutput();
        } catch (IOException e) {
            logger.severe(e.toString());
        } finally {
            closeConnection();
        }
    }

    private byte[] parseRequest() throws IOException {
        while (client.getInputStream().available() <= 0) {
            try {
                Thread.sleep(0, 200);
            } catch (InterruptedException e) {
                logger.severe(e.toString());
            }
        }

        var is = client.getInputStream();

        int bytesExpected = is.available();
        byte[] request = new byte[bytesExpected];
        int readCount = is.read(request);

        var bais = new ByteArrayInputStream(request);
        var reader = new BufferedReader(new InputStreamReader(bais));

        // request line
        String[] requestLine = reader.readLine().split(" ");
        String method = requestLine[0];
        String target = requestLine[1];
        String version = requestLine[2];

        // headers
        Map<String, String> headers = parseHeaders(reader);

        final StringBuilder responseHeader = new StringBuilder();
        byte[] responseBody = new byte[0];
        Matcher matcher;

        if (target.equals("/")) {
            responseHeader.append("HTTP/1.1 200 OK\r\n");
        } else if ((matcher = Pattern.compile("/echo/(\\w*)").matcher(target)).matches()) {
            responseBody = matcher.group(1).getBytes();
            responseHeader.append("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n");
        } else if (target.equals("/user-agent")) {
            responseBody = headers.get("User-Agent").getBytes();
            responseHeader.append("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n");
        } else if ((matcher = Pattern.compile("/files/(\\w+)").matcher(target)).matches()) {
            String fileName = matcher.group(1);
            if (method.equals("GET")) {
                Path file = FileManager.getInstance().getFile(fileName);
                if (!file.toFile().exists()) {
                    responseHeader.append("HTTP/1.1 404 Not Found\r\n");
                } else {
                    responseBody = Files.readAllBytes(file);
                    responseHeader.append("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\n");
                }
            } else if (method.equals("POST")) {
                // read request body
                byte[] body = new byte[Integer.parseInt(headers.get("Content-Length"))];
                System.arraycopy(request, request.length - body.length, body, 0, body.length);

                Path file = FileManager.getInstance().overwriteFile(fileName);
                if (Files.exists(file)) {
                    Files.writeString(file, new String(body));
                    responseHeader.append("HTTP/1.1 201 Created\r\n");
                }
            }
        } else {
            responseHeader.append("HTTP/1.1 404 Not Found\r\n");
        }

        // Compressing
        if (headers.containsKey("Accept-Encoding")) {
            final var responseBodyWrapper = new ByteArrayWrapper(responseBody);
            Arrays.stream(headers.get("Accept-Encoding").split(", "))
                    .filter(CompressionScheme::isSupported)
                    .findAny()
                    .ifPresent(encoding -> {
                        responseHeader.append("Content-Encoding: ").append(encoding.trim().toLowerCase()).append("\r\n");
                        try {
                            var comp = Compression.getCompressionScheme(CompressionScheme.valueOf(encoding.toUpperCase()))
                                    .compress(responseBodyWrapper.getInternal());
                            responseBodyWrapper.setInternal(comp);
                            responseBodyWrapper.markCompleted();
                        } catch (IOException e) {
                            logger.severe(e.toString());
                        }
                    });
            if (responseBodyWrapper.isCompleted()) {
                responseBody = responseBodyWrapper.getInternal();
            }
        }

        if (responseBody.length > 0) {
            responseHeader.append("Content-Length: ").append(responseBody.length).append("\r\n");
        }

        responseHeader.append("\r\n");

        byte[] headerBytes = responseHeader.toString().getBytes();
        byte[] response = new byte[headerBytes.length + responseBody.length];
        System.arraycopy(headerBytes, 0, response, 0, headerBytes.length);
        System.arraycopy(responseBody, 0, response, headerBytes.length, responseBody.length);

        return response;
    }

    private static Map<String, String> parseHeaders(BufferedReader reader) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String header;
        while ((header = reader.readLine()) != null && !header.isBlank()) {
            var arr = header.split(": ");
            headers.put(arr[0], arr[1]);
        }
        return headers;
    }

    private void closeConnection() {
        try {
            client.close();
        } catch (IOException e) {
            logger.severe(e.toString());
        }
    }

}
