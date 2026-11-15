package com.oneorigin.union;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A tiny loopback-only HTTP server for the packaged web surface.
 *
 * The WebView could load file://, but browsers treat local files as an opaque
 * origin with no IndexedDB and no service worker. Serving the same bytes from
 * http://127.0.0.1 gives Union a real, secure origin while keeping every asset
 * inside the APK. It binds to loopback only, never the network.
 */
final class AssetServer implements Closeable {

    private static final Map<String, String> TYPES = new HashMap<>();

    static {
        TYPES.put("html", "text/html; charset=utf-8");
        TYPES.put("htm", "text/html; charset=utf-8");
        TYPES.put("js", "text/javascript; charset=utf-8");
        TYPES.put("mjs", "text/javascript; charset=utf-8");
        TYPES.put("css", "text/css; charset=utf-8");
        TYPES.put("json", "application/json; charset=utf-8");
        TYPES.put("webmanifest", "application/manifest+json; charset=utf-8");
        TYPES.put("txt", "text/plain; charset=utf-8");
        TYPES.put("md", "text/markdown; charset=utf-8");
        TYPES.put("svg", "image/svg+xml");
        TYPES.put("png", "image/png");
        TYPES.put("jpg", "image/jpeg");
        TYPES.put("jpeg", "image/jpeg");
        TYPES.put("webp", "image/webp");
        TYPES.put("ico", "image/x-icon");
        TYPES.put("woff2", "font/woff2");
        TYPES.put("wasm", "application/wasm");
        TYPES.put("map", "application/json; charset=utf-8");
    }

    private final AssetManager assets;
    private final String root;
    private final ServerSocket server;
    private final ExecutorService pool;
    private final Thread acceptor;
    private volatile boolean running = true;

    static AssetServer start(AssetManager assets, String root) throws IOException {
        ServerSocket socket = new ServerSocket(0, 24, InetAddress.getByName("127.0.0.1"));
        return new AssetServer(assets, root, socket);
    }

    private AssetServer(AssetManager assets, String root, ServerSocket server) {
        this.assets = assets;
        this.root = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
        this.server = server;
        this.pool = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "union-assets");
            thread.setDaemon(true);
            return thread;
        });
        this.acceptor = new Thread(this::acceptLoop, "union-assets-accept");
        this.acceptor.setDaemon(true);
        this.acceptor.start();
    }

    int port() {
        return server.getLocalPort();
    }

    String url() {
        return "http://127.0.0.1:" + port() + "/";
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = server.accept();
                pool.execute(() -> serve(client));
            } catch (IOException error) {
                if (running) {
                    // A closed socket or a transient accept failure should not
                    // take the web surface down; the next accept will follow.
                    continue;
                }
            }
        }
    }

    private void serve(Socket client) {
        try (Socket socket = client) {
            socket.setSoTimeout(10_000);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1)
            );
            String request = reader.readLine();
            if (request == null || request.isEmpty()) return;
            String[] parts = request.split(" ");
            if (parts.length < 2) return;
            String method = parts[0];
            String target = parts[1];

            String line;
            int guard = 0;
            while ((line = reader.readLine()) != null && !line.isEmpty() && guard++ < 80) {
                // headers are irrelevant to a static loopback server
            }

            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                respond(socket, 405, "text/plain; charset=utf-8", "Method not allowed", "no-store", false);
                return;
            }

            int query = target.indexOf('?');
            if (query >= 0) target = target.substring(0, query);
            int hash = target.indexOf('#');
            if (hash >= 0) target = target.substring(0, hash);
            try {
                target = URLDecoder.decode(target, "UTF-8");
            } catch (Exception ignored) {
                // keep the raw target; a malformed escape is a 404, not a crash
            }
            if (target.isEmpty() || "/".equals(target)) target = "/index.html";
            if (target.contains("..") || target.indexOf('\0') >= 0) {
                respond(socket, 400, "text/plain; charset=utf-8", "Bad request", "no-store", false);
                return;
            }

            byte[] body;
            try (InputStream input = assets.open(root + target)) {
                body = readAll(input);
            } catch (IOException missing) {
                respond(socket, 404, "text/plain; charset=utf-8", "Not found", "no-store", false);
                return;
            }

            String cache = cacheFor(target);
            respond(socket, 200, typeFor(target), body, cache, "HEAD".equals(method));
        } catch (Exception ignored) {
            // Loopback probes and cancelled loads are routine; stay quiet.
        }
    }

    private static String typeFor(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) return "application/octet-stream";
        String extension = path.substring(dot + 1).toLowerCase(Locale.US);
        String type = TYPES.get(extension);
        return type == null ? "application/octet-stream" : type;
    }

    private static String cacheFor(String path) {
        if (path.endsWith(".html") || path.endsWith(".webmanifest") || path.endsWith("sw.js") || path.endsWith("build.json")) {
            return "no-store";
        }
        // Vite fingerprints JS, CSS, fonts and WASM, so those can be cached.
        return "public, max-age=86400";
    }

    private static void respond(Socket socket, int status, String type, String body, String cache, boolean headOnly)
        throws IOException {
        respond(socket, status, type, body.getBytes(StandardCharsets.UTF_8), cache, headOnly);
    }

    private static void respond(Socket socket, int status, String type, byte[] body, String cache, boolean headOnly)
        throws IOException {
        String reason;
        switch (status) {
            case 200: reason = "OK"; break;
            case 400: reason = "Bad Request"; break;
            case 404: reason = "Not Found"; break;
            case 405: reason = "Method Not Allowed"; break;
            default: reason = "Error"; break;
        }
        String headers = "HTTP/1.1 " + status + " " + reason + "\r\n"
            + "Content-Type: " + type + "\r\n"
            + "Content-Length: " + body.length + "\r\n"
            + "Cache-Control: " + cache + "\r\n"
            + "X-Content-Type-Options: nosniff\r\n"
            + "Referrer-Policy: no-referrer\r\n"
            + "Connection: close\r\n\r\n";
        OutputStream output = socket.getOutputStream();
        output.write(headers.getBytes(StandardCharsets.ISO_8859_1));
        if (!headOnly) output.write(body);
        output.flush();
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    @Override
    public void close() {
        running = false;
        try {
            server.close();
        } catch (IOException ignored) {
            // already closed
        }
        pool.shutdownNow();
    }
}
