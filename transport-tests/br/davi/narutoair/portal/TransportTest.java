package br.davi.narutoair.portal;

import android.webkit.CookieManager;
import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Real HTTP/socket regression tests; no game account or live session. */
public class TransportTest {
    private static String base;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static class Response {
        String headers, smallBody;
        long size;
    }
    private static Response request(String method, String path, String headers, boolean capture) throws Exception {
        try (ServerSocket proxy = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            Thread task = new Thread(() -> {
                try { RuntimeTransport.handle(new PortalContext(), proxy.accept(), base, base + "/game", "TestAgent"); }
                catch (IOException ex) { throw new RuntimeException(ex); }
            });
            task.start();
            Response response = new Response();
            try (Socket socket = new Socket("127.0.0.1", proxy.getLocalPort())) {
                socket.getOutputStream().write((method + " " + path + " HTTP/1.1\r\nHost: localhost\r\n" + headers + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
                InputStream input = socket.getInputStream();
                ByteArrayOutputStream head = new ByteArrayOutputStream();
                int pattern = 0;
                for (int value; (value = input.read()) != -1; ) {
                    head.write(value);
                    int[] expected = {13,10,13,10};
                    pattern = value == expected[pattern] ? pattern + 1 : (value == 13 ? 1 : 0);
                    if (pattern == 4) break;
                }
                response.headers = new String(head.toByteArray(), StandardCharsets.ISO_8859_1);
                ByteArrayOutputStream body = capture ? new ByteArrayOutputStream() : null;
                byte[] buffer = new byte[32768];
                for (int n; (n = input.read(buffer)) != -1; ) {
                    response.size += n;
                    if (capture) body.write(buffer,0,n);
                }
                response.smallBody = capture ? new String(body.toByteArray(),StandardCharsets.UTF_8) : "";
            }
            task.join(35000);
            check(!task.isAlive(), "worker leaked");
            return response;
        }
    }
    private static void respond(HttpExchange exchange, int status, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes); exchange.close();
    }
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        server.setExecutor(workers);
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/range", exchange -> {
            check("bytes=2-5".equals(exchange.getRequestHeaders().getFirst("Range")), "Range missing");
            check("TestAgent".equals(exchange.getRequestHeaders().getFirst("User-Agent")), "UA missing");
            check((base + "/game").equals(exchange.getRequestHeaders().getFirst("Referer")), "Referer missing");
            exchange.getResponseHeaders().set("Content-Range", "bytes 2-5/10");
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
            exchange.getResponseHeaders().set("ETag", "test-etag");
            respond(exchange,206,"2345");
        });
        server.createContext("/head", exchange -> {
            exchange.getResponseHeaders().set("Content-Length", "12345");
            exchange.sendResponseHeaders(200,-1); exchange.close();
        });
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "session=new; Path=/");
            exchange.getResponseHeaders().set("Location", "/cookie");
            exchange.sendResponseHeaders(302,-1); exchange.close();
        });
        server.createContext("/cookie", exchange -> {
            check(exchange.getRequestHeaders().getFirst("Cookie").contains("session=new"), "redirect session missing");
            respond(exchange,200,"session updated");
        });
        server.createContext("/notfound", exchange -> respond(exchange,404,"missing"));
        server.createContext("/large", exchange -> {
            int size=64*1024*1024;
            exchange.sendResponseHeaders(200,size);
            byte[] buffer=new byte[32768];
            for(int n=0;n<size;n+=buffer.length)exchange.getResponseBody().write(buffer);
            exchange.close();
        });
        server.createContext("/unavailable", exchange -> respond(exchange,403,"denied"));
        server.createContext("/unknown", exchange -> {
            exchange.sendResponseHeaders(200,0);
            exchange.getResponseBody().write("chunked upstream".getBytes(StandardCharsets.UTF_8));exchange.close();
        });
        server.createContext("/PT_NarutoAlpha9.35Build301/assets/sound/music.mp3", exchange -> respond(exchange,403,"audio denied"));
        server.start();
        try {
            AudioResourceLog.clear();
            Response audio=request("GET","/PT_NarutoAlpha9.35Build301/assets/sound/music.mp3?token=SECRET","",true);
            check(audio.headers.contains("403"),"audio upstream error preserved");
            String audioReport=AudioResourceLog.snapshot();
            check(audioReport.contains("HTTP=403") && audioReport.contains("bytes=12"),"audio HTTP metadata missing");
            check(!audioReport.contains("SECRET") && !audioReport.contains("token"),"audio query leaked");
            Response range=request("GET","/range","Range: bytes=2-5\r\n",true);
            check(range.headers.contains("206") && range.headers.contains("Content-Range: bytes 2-5/10") && range.headers.contains("ETag: test-etag"),"range response headers");
            check(range.smallBody.equals("2345"),"range bytes");
            Response head=request("HEAD","/head","",true);
            check(head.size==0 && head.headers.contains("Content-Length: 12345"),"HEAD resource length");
            check(request("GET","/redirect","",true).smallBody.equals("session updated"),"redirect");
            check(request("GET","/cookie","",true).smallBody.equals("session updated"),"fresh cookie");
            Response missing=request("GET","/notfound","",true);
            check(missing.headers.contains("404") && missing.smallBody.equals("missing"),"404 preserved");
            check(request("GET","/unavailable","",true).headers.contains("403"),"403 preserved");
            Response large=request("GET","/large","",false);
            check(large.size==64*1024*1024,"stream truncated");
            check(request("GET","/unknown","",true).smallBody.equals("chunked upstream"),"unknown length");
            check(request("HEAD","/crossdomain.xml","",true).size==0,"policy HEAD");
            check(!request("GET","/crossdomain.xml","",true).headers.contains("Content-Range"),"invalid policy range");
            check(request("POST","/range","Content-Length: 2147483647\r\n",true).headers.contains("413"),"oversized body");
            check(request("OPTIONS","/range","",true).headers.contains("204"),"preflight");
            check(request("GET","//evil.example/file","",true).headers.contains("502"),"origin escape");
            check(!RuntimeTransport.allowedRedirect(new URL("https://cdnnaruto-pt.oasgames.com/"),new URL("https://cdnnaruto-pt.oasgames.com/"),new URL("http://cdnnaruto-pt.oasgames.com/")),"TLS downgrade");
            check(!RuntimeTransport.allowedRedirect(new URL(base),new URL(base),new URL("https://oasgames.com.evil.test/")),"domain boundary");
            String privateLog=RuntimeTransport.sanitizeLog("GET https://oasgames.com/game?token=SECRET&sign=PRIVATE cookie=HIDDEN");
            check(!privateLog.contains("SECRET") && !privateLog.contains("PRIVATE") && !privateLog.contains("HIDDEN"),"private diagnostics");
            System.out.println("PASS: HTTP/socket regressions, private diagnostics; 64 MiB streamed with a 24 MiB Java heap.");
        } finally {server.stop(0);workers.shutdownNow();}
    }
}
