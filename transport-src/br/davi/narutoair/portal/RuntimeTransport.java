package br.davi.narutoair.portal;

import android.webkit.CookieManager;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Bounded, streaming transport for AIR's loopback asset URLs. */
public final class RuntimeTransport {
    private static final int MAX_BODY = 2 * 1024 * 1024;
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(
        4, 4, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(32),
        new ThreadFactory() {
            public Thread newThread(Runnable task) {
                Thread t = new Thread(task, "NarutoAssets");
                t.setDaemon(true);
                return t;
            }
        });
    static { WORKERS.allowCoreThreadTimeOut(true); }

    public static void submit(final PortalContext owner, final Socket socket,
                              final String origin, final String referer, final String ua) {
        try {
            WORKERS.execute(new Runnable() {
                public void run() { handle(owner, socket, origin, referer, ua); }
            });
        } catch (RejectedExecutionException ex) {
            try { reply(socket.getOutputStream(), 503, "Fila de recursos ocupada"); }
            catch (IOException ignored) { }
            close(socket);
        }
    }

    public static void handle(PortalContext owner, Socket socket, String origin,
                              String referer, String ua) {
        HttpURLConnection connection = null;
        StaticAssetCache.Writer cacheWrite = null;
        boolean responseStarted = false;
        try {
            socket.setSoTimeout(30000);
            InputStream input = new BufferedInputStream(socket.getInputStream());
            OutputStream output = new BufferedOutputStream(socket.getOutputStream());
            String first = line(input);
            if (first == null) return;
            String[] request = first.split(" ", 3);
            if (request.length != 3) throw new IOException("request line");
            String method = request[0].toUpperCase(Locale.US);
            if (!Arrays.asList("GET", "HEAD", "POST", "PUT", "OPTIONS").contains(method)) {
                reply(output, 405, "Metodo nao permitido"); return;
            }
            Map<String, String> headers = new HashMap<String, String>();
            int total = first.length();
            for (int count = 0; ; count++) {
                String row = line(input);
                if (row == null) throw new EOFException("headers");
                total += row.length();
                if (total > 65536 || count > 100) throw new IOException("headers limit");
                if (row.isEmpty()) break;
                int colon = row.indexOf(':');
                if (colon <= 0) throw new IOException("header syntax");
                String key = row.substring(0, colon).trim().toLowerCase(Locale.US);
                if (headers.containsKey(key) && key.equals("content-length"))
                    throw new IOException("duplicate length");
                headers.put(key, row.substring(colon + 1).trim());
            }
            if (headers.containsKey("transfer-encoding")) {
                reply(output, 400, "Use Content-Length"); return;
            }
            long length = Long.parseLong(headers.containsKey("content-length") ? headers.get("content-length") : "0");
            if (length < 0 || length > MAX_BODY) {
                reply(output, 413, "Requisicao muito grande"); return;
            }
            byte[] body = new byte[(int) length];
            new DataInputStream(input).readFully(body);
            String target = request[1];
            URL base = new URL(origin);
            URL url = resolve(base, target);
            if (url.getPath().equals("/crossdomain.xml")) {
                byte[] policy = ("<?xml version=\"1.0\"?><cross-domain-policy>" +
                    "<allow-access-from domain=\"*\" secure=\"false\"/>" +
                    "<allow-http-request-headers-from domain=\"*\" headers=\"*\" secure=\"false\"/>" +
                    "</cross-domain-policy>").getBytes(StandardCharsets.UTF_8);
                output.write(("HTTP/1.1 200 OK\r\nContent-Type: text/x-cross-domain-policy\r\nContent-Length: " +
                    policy.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
                if (!method.equals("HEAD")) output.write(policy);
                output.flush(); return;
            }
            if (method.equals("OPTIONS")) {
                output.write(("HTTP/1.1 204 No Content\r\nAccess-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Methods: GET, HEAD, POST, PUT, OPTIONS\r\n" +
                    "Access-Control-Allow-Headers: Range, Content-Type, If-Range, If-None-Match, If-Modified-Since\r\n" +
                    "Content-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
                output.flush(); return;
            }
            if (KaguyaResources.fullRequest(method,headers,length)) {
                byte[] mod = KaguyaResources.read(url);
                if (mod != null) {
                    responseStarted = true;
                    localHeaders(output,mod.length,contentType(url.getPath()),"Kaguya");
                    if (!method.equals("HEAD")) output.write(mod);
                    output.flush(); return;
                }
            }
            boolean cacheEligible = StaticAssetCache.eligible(url,method,headers,length);
            if (cacheEligible) {
                StaticAssetCache.Hit hit = StaticAssetCache.read(url);
                if (hit != null) try (StaticAssetCache.Hit cached = hit) {
                    responseStarted = true;localHeaders(output,cached.length,cached.type,"Disk");
                    byte[] buffer = new byte[32768];
                    for (int n; (n=cached.stream.read(buffer))!=-1;) output.write(buffer,0,n);
                    output.flush(); return;
                }
            }
            for (int redirects = 0; ; redirects++) {
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(false);
                connection.setUseCaches(false);
                connection.setRequestMethod(method);
                connection.setRequestProperty("Accept-Encoding", "identity");
                connection.setRequestProperty("Accept", "*/*");
                if (ua != null && !ua.isEmpty()) connection.setRequestProperty("User-Agent", ua);
                if (referer != null && !referer.isEmpty()) connection.setRequestProperty("Referer", referer);
                String cookie = CookieManager.getInstance().getCookie(url.toExternalForm());
                if (cookie != null && !cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
                for (String key : Arrays.asList("range", "if-range", "if-none-match", "if-modified-since", "if-match", "if-unmodified-since", "content-type"))
                    if (headers.containsKey(key)) connection.setRequestProperty(key, headers.get(key));
                if (method.equals("POST") || method.equals("PUT")) {
                    connection.setDoOutput(true);
                    connection.setFixedLengthStreamingMode(body.length);
                    try (OutputStream upstream = connection.getOutputStream()) { upstream.write(body); }
                }
                int status = connection.getResponseCode();
                saveCookies(connection, url);
                if (Arrays.asList(301, 302, 303, 307, 308).contains(status)) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || redirects >= 5) throw new IOException("redirect limit");
                    URL next = new URL(url, location);
                    if (!allowedRedirect(base, url, next)) throw new IOException("redirect origin");
                    connection.disconnect(); connection = null;
                    if (status == 303 && !method.equals("HEAD") || (status == 301 || status == 302) && method.equals("POST")) {
                        method = "GET"; body = new byte[0];
                    }
                    url = next; continue;
                }
                if (status < 100) throw new IOException("status");
                if (EntryCompatibility.eligible(method, base, url, headers, status)) {
                    String encoding = connection.getHeaderField("Content-Encoding");
                    if (encoding != null && !encoding.equalsIgnoreCase("identity")) throw new IOException("Encoded entry response");
                    EntryCompatibility.Result adapted;
                    try (InputStream stream = connection.getInputStream()) {
                        adapted = EntryCompatibility.adapt(EntryCompatibility.readBounded(stream,4*1024*1024));
                    } catch (IOException ex) {
                    PortalContext.access$1600(owner,"ENTRY COMPAT 1.3.4 FAIL: " + sanitizeLog(ex.getMessage()));
                        throw ex;
                    }
                    responseStarted = true;
                    output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/x-shockwave-flash\r\nContent-Length: " +
                        adapted.bytes.length + "\r\nCache-Control: no-store\r\nX-Naruto-AIR-Compat: " + adapted.references +
                        "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
                    output.write(adapted.bytes);output.flush();
                    PortalContext.access$1600(owner,"ENTRY COMPAT 1.3.4: Security=" + adapted.securityReferences + "; Loader=" + adapted.loaderReferences + "; Resources=" + adapted.resourceReferences +
                        "; Page=" + adapted.pageReferences + "; Menu=" + adapted.menuGuards + "; bytes=" + adapted.bytes.length);
                    return;
                }
                boolean noBody = method.equals("HEAD") || status == 204 || status == 304;
                StringBuilder response = new StringBuilder("HTTP/1.1 ").append(status).append(" Status\r\n");
                String type = connection.getContentType();
                response.append("Content-Type: ").append(clean(type == null ? contentType(url.getPath()) : type)).append("\r\n");
                for (String key : Arrays.asList("Content-Range", "Accept-Ranges", "Content-Encoding", "ETag", "Last-Modified", "Cache-Control", "Expires")) {
                    String value = connection.getHeaderField(key);
                    if (value != null) response.append(key).append(": ").append(clean(value)).append("\r\n");
                }
                String size = connection.getHeaderField("Content-Length");
                if (cacheEligible && redirects == 0 && status == 200 && type != null && connection.getHeaderField("Vary") == null &&
                    type.split(";",2)[0].trim().equalsIgnoreCase(contentType(url.getPath()))) {
                    cacheWrite=StaticAssetCache.begin(url,connection.getContentLengthLong(),
                        connection.getHeaderField("Cache-Control"),connection.getHeaderField("Content-Encoding"),
                        connection.getHeaderField("Set-Cookie") != null,
                        Math.max(connection.getHeaderFieldLong("Age",0),Math.max(0,(System.currentTimeMillis()-connection.getHeaderFieldDate("Date",System.currentTimeMillis()))/1000)));
                }
                if (size != null) response.append("Content-Length: ").append(Long.parseLong(size)).append("\r\n");
                else if (noBody && !method.equals("HEAD")) response.append("Content-Length: 0\r\n");
                response.append("Access-Control-Allow-Origin: *\r\nAccess-Control-Expose-Headers: Content-Range, Accept-Ranges, ETag\r\nConnection: close\r\n\r\n");
                responseStarted = true;
                output.write(response.toString().getBytes(StandardCharsets.ISO_8859_1));
                long copied = 0;
                if (!noBody) {
                    InputStream upstream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
                    if (upstream != null) {
                        try (InputStream stream = upstream) {
                            byte[] buffer = new byte[32768];
                            for (int n; (n = stream.read(buffer)) != -1; ) {
                                output.write(buffer, 0, n); copied += n;
                                if (cacheWrite != null) cacheWrite.write(buffer,0,n);
                            }
                        }
                    }
                }
                output.flush();
                if (cacheWrite != null) {cacheWrite.finish();cacheWrite=null;}
                // Paths contain no query/token; cookie/header values are never logged.
                // Release: no per-asset trace formatting or UI dispatch.
                return;
            }
        } catch (Exception ex) {
            // HTTP failure is returned to the requesting loader.
            if (!responseStarted) try { reply(socket.getOutputStream(), 502, "Falha ao carregar recurso"); }
            catch (IOException ignored) { }
        } finally {
            if (cacheWrite != null) cacheWrite.abort();
            if (connection != null) connection.disconnect();
            close(socket);
        }
    }

    static URL resolve(URL base, String target) throws IOException {
        if (!target.startsWith("/") || target.startsWith("//") || target.indexOf('\\') >= 0)
            throw new IOException("invalid target");
        URL url = new URL(base, target);
        if (!sameOrigin(base, url) || url.getUserInfo() != null) throw new IOException("origin");
        return url;
    }
    static boolean sameOrigin(URL a, URL b) {
        return a.getProtocol().equalsIgnoreCase(b.getProtocol()) && a.getHost().equalsIgnoreCase(b.getHost()) &&
            effectivePort(a) == effectivePort(b);
    }
    private static int effectivePort(URL url) { return url.getPort() < 0 ? url.getDefaultPort() : url.getPort(); }
    static boolean allowedRedirect(URL base, URL from, URL to) {
        if (to.getUserInfo() != null || !(to.getProtocol().equals("https") || to.getProtocol().equals("http"))) return false;
        if (from.getProtocol().equals("https") && !to.getProtocol().equals("https")) return false;
        if (sameOrigin(base, to)) return true;
        String host = to.getHost().toLowerCase(Locale.US);
        return to.getProtocol().equals("https") && effectivePort(to) == 443 &&
            (host.equals("oasgames.com") || host.endsWith(".oasgames.com") ||
             host.equals("narutowebgame.com") || host.endsWith(".narutowebgame.com"));
    }
    private static void saveCookies(HttpURLConnection connection, URL url) {
        CookieManager manager = CookieManager.getInstance();
        boolean updated = false;
        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet())
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("Set-Cookie"))
                for (String value : entry.getValue()) { manager.setCookie(url.toExternalForm(), value); updated = true; }
        if (updated) manager.flush();
    }
    private static String line(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int previous = -1; ; ) {
            int value = input.read();
            if (value < 0) { if (bytes.size() == 0) return null; throw new EOFException("line"); }
            if (previous == 13 && value == 10) {
                byte[] row = bytes.toByteArray();
                return new String(row, 0, row.length - 1, StandardCharsets.ISO_8859_1);
            }
            bytes.write(value);
            if (bytes.size() > 8192) throw new IOException("line limit");
            previous = value;
        }
    }
    private static String clean(String value) { return value.replace("\r", "").replace("\n", ""); }
    private static void localHeaders(OutputStream output,long length,String type,String source) throws IOException {
        output.write(("HTTP/1.1 200 OK\r\nContent-Type: " + type + "\r\nContent-Length: " + length +
            "\r\nCache-Control: no-store\r\nAccess-Control-Allow-Origin: *\r\nX-Naruto-Resource: " + source +
            "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
    }
    public static String sanitizeLog(String value) {
        if (value == null) return "";
        return value.replaceAll("(https?://[^\\s?#]+)[?#][^\\s]*", "$1?[sessao omitida]")
            .replaceAll("(?i)(password|passwd|pwd|token|sign|cookie|authorization|email|username|user_name)\\s*[:=]\\s*[^\\s,;]+", "$1=[omitido]");
    }
    private static String contentType(String path) {
        String p = path.toLowerCase(Locale.US);
        if (p.endsWith(".swf")) return "application/x-shockwave-flash";
        if (p.endsWith(".xml")) return "application/xml";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
    private static void reply(OutputStream output, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        output.write(("HTTP/1.1 " + status + " Status\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: " +
            bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
        output.write(bytes); output.flush();
    }
    private static void close(Socket socket) { try { socket.close(); } catch (IOException ignored) { } }
}
