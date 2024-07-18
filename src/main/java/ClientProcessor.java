import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            byte[] response = parseRequest(client);

            client.getOutputStream().write(response);
            client.close();
        } catch (IOException e) {
            logger.severe(e.toString());
        }
    }

    private static byte[] parseRequest(Socket clientSocket) throws IOException {
        var isr = new InputStreamReader(clientSocket.getInputStream());
        var reader = new BufferedReader(isr);

        // request line
        String[] requestLine = reader.readLine().split(" ");
        String method = requestLine[0];
        String target = requestLine[1];
        String version = requestLine[2];

        // headers
        Map<String, String> headers = parseHeaders(reader);

        String responseHeader = "";
        byte[] responseBody = new byte[0];
        Matcher matcher;

        if (target.equals("/")) {
            responseHeader = "HTTP/1.1 200 OK\r\n";
        } else if ((matcher = Pattern.compile("/echo/(\\w*)").matcher(target)).matches()) {
            responseBody = matcher.group(1).getBytes();
            responseHeader = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n";
        } else if (target.equals("/user-agent")) {
            responseBody = headers.get("User-Agent").getBytes();
            responseHeader = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n";
        } else if ((matcher = Pattern.compile("/files/(\\w+)").matcher(target)).matches()) {
            String fileName = matcher.group(1);
            if (method.equals("GET")) {
                Path file = FileManager.getInstance().getFile(fileName);
                if (!file.toFile().exists()) {
                    responseHeader = "HTTP/1.1 404 Not Found\r\n";
                } else {
                    responseBody = Files.readAllBytes(file);
                    responseHeader = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\n";
                }
            } else if (method.equals("POST")) {
                // read request body
                char[] body = new char[Integer.parseInt(headers.get("Content-Length"))];
                reader.read(body);

                Path file = FileManager.getInstance().overwriteFile(fileName);
                if (Files.exists(file)) {
                    Files.writeString(file, new String(body));
                    responseHeader = "HTTP/1.1 201 Created\r\n";
                }
            }
        } else {
            responseHeader = "HTTP/1.1 404 Not Found\r\n";
        }

        // Compressing
        if (Arrays.stream(headers.get("Accept-Encoding").split(", ")).anyMatch(CompressionScheme::isSupported)) {
            responseHeader += "Content-Encoding: " + headers.get("Accept-Encoding").trim().toLowerCase() + "\r\n";
        }

        if (responseBody.length > 0) {
            responseHeader += "Content-Length: " + responseBody.length + "\r\n";
        }

        responseHeader += "\r\n";

        byte[] responseBytes = responseHeader.getBytes();
        byte[] toReturn = new byte[responseBytes.length + responseBody.length];
        System.arraycopy(responseBytes, 0, toReturn, 0, responseBytes.length);
        System.arraycopy(responseBody, 0, toReturn, responseBytes.length, responseBody.length);

        return toReturn;
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
}
