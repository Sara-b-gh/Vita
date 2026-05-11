package com.vita.devora.utils;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;

public class LocalServer {

    private static HttpServer server;

    public static void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(9090), 0);
        server.createContext("/captcha", exchange -> {

            // ← GÉRER TOUTES LES MÉTHODES HTTP
            String method = exchange.getRequestMethod();
            System.out.println("📥 Requête reçue : " + method);

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            InputStream is = LocalServer.class
                    .getResourceAsStream("/com/vita/devora/captcha.html");

            if (is == null) {
                System.out.println("❌ captcha.html NON TROUVÉ !");
                String error = "<h1>FICHIER NON TROUVE</h1>";
                byte[] bytes = error.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
                return;
            }

            System.out.println("✅ captcha.html trouvé !");
            byte[] bytes = is.readAllBytes();

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");

            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });

        server.setExecutor(null);
        server.start();
        System.out.println("✅ Serveur démarré sur http://localhost:9090/captcha");
    }

    public static void stop() {
        if (server != null) server.stop(0);
    }
}