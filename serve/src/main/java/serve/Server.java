package serve;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static serve.ContentType.getContentType;

public class Server {

    private volatile boolean running = false;

    public void serve(int port) {
        var server = createServerSocket(port);
        println("Start serving: " + server);
        this.running = true;
        while (this.running) {
            var socket = accept(server);
            CompletableFuture.runAsync(() -> {
                try (var is = socket.getInputStream();
                     var os = socket.getOutputStream()) {
                    handle(is, os);
                    socket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    this.running = false;
                }
            });
        }
    }

    private static void handle(@NotNull InputStream is, @NotNull OutputStream os) {
        try {
            var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            var headerList = new ArrayList<String>();
            String contentLine;
            while ((contentLine = br.readLine()) != null) {
                if (contentLine.isEmpty()) {
                    break;
                }
                headerList.add(contentLine);
            }
            if (headerList.isEmpty()) {
                return;
            }
            var h1 = headerList.get(0).split(" ");
            if (!"GET".equalsIgnoreCase(h1[0])) {
                var body = getBytes("Unsupported Method");
                os.write(getBytes("HTTP/1.1 400 Bad Request\r\n"));
                os.write(getBytes("Content-Length: " + body.length + "\r\n"));
                os.write(getBytes("Content-Type: text/plain; charset=utf-8\r\n"));
                os.write(getBytes("Server: Serve\r\n"));
                os.write(getBytes("\r\n"));
                os.write(body);
                return;
            }
            var path = h1[1].split("\\?")[0];

            if (path.endsWith("/")) {
                path = path + "index.html";
            }

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            var filePath = Paths.get(path);
            if (Files.isDirectory(filePath)) {
                path = path + "/index.html";
                filePath = Paths.get(path);
            }

            if (Files.notExists(filePath)) {
                try (var html404 = Server.class.getResourceAsStream("/404.html")) {
                    var body = Objects.requireNonNull(html404).readAllBytes();
                    os.write(getBytes("HTTP/1.1 404 Not Found\r\n"));
                    os.write(getBytes("Content-Length: " + body.length + "\r\n"));
                    os.write(getBytes("Content-Type: text/html; charset=utf-8\r\n"));
                    os.write(getBytes("Server: Serve\r\n"));
                    os.write(getBytes("\r\n"));
                    os.write(body);
                    return;
                }
            }
            var fileSize = Files.size(filePath);
            os.write(getBytes("HTTP/1.1 200 OK\r\n"));
            os.write(getBytes("Content-Length: " + fileSize + "\r\n"));
            os.write(getBytes("Content-Type: " + getContentType(path) + "\r\n"));
            os.write(getBytes("Server: Serve\r\n"));
            os.write(getBytes("\r\n"));
            Files.copy(filePath, os);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static byte @NotNull [] getBytes(@NotNull String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static @NotNull ServerSocket createServerSocket(int port) {
        try {
            return new ServerSocket(port);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static @NotNull Socket accept(@NotNull ServerSocket socket) {
        try {
            return socket.accept();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void println(String s) {
        System.out.println(s);
    }
}
