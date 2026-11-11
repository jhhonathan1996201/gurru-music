package com.gurru.music;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inicio de sesión en Facebook DENTRO de Gurru Music (NUEVO13, prueba).
 *
 * Facebook no tiene buscador público de música ni API para buscar videos:
 * yt-dlp solo baja videos con su link directo. Para que la app tenga una
 * caja de búsqueda real de Facebook, el usuario inicia sesión en una
 * ventana WebView dentro de la app y la búsqueda se hace con esa sesión:
 *   1. login(): abre un diálogo con un WebView de Facebook; el usuario
 *      inicia sesión y toca «Listo». Las cookies se guardan en un archivo
 *      (formato Netscape) para que yt-dlp las use al bajar el audio.
 *   2. searchVideos({query, limit}): pide la página de resultados de
 *      m.facebook.com/search/videos/ con las cookies de la sesión y saca
 *      los links de los videos (id, título, página, duración). Es un
 *      parseo de HTML: Facebook cambia su página seguido, así que esto
 *      es la parte frágil y experimental de la integración.
 *   3. Los videos se escuchan y se descargan con el mismo camino de
 *      YouTube online (caché invisible + Descargar), pasándole a yt-dlp
 *      las cookies cuando la URL es de facebook.com.
 *
 * Las cookies viven en el CookieManager del WebView (persisten entre
 * aperturas) y en el archivo fb_cookies.txt (privado de la app).
 * Facebook puede pedir verificación o bloquear inicios de sesión que
 * considere sospechosos; eso se ve en el WebView, como en el navegador.
 */
@CapacitorPlugin(name = "FacebookAuth")
public class FacebookAuthPlugin extends Plugin {

    private static final String TAG = "FacebookAuth";
    private static final String COOKIES_FILE = "fb_cookies.txt";
    private static final String FB_MOBILE = "https://m.facebook.com/";
    private static final String FB_SEARCH = "https://m.facebook.com/search/videos/?q=";
    private static final String UA_MOBILE =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ------------------------------------------------------------------
    // Cookies de la sesión
    // ------------------------------------------------------------------

    /** Archivo (privado de la app) con las cookies de Facebook en formato
        Netscape, para pasárselo a yt-dlp con --cookies. */
    public static File cookiesFile(Context ctx) {
        return new File(ctx.getFilesDir(), COOKIES_FILE);
    }

    public static boolean hasFbCookies(Context ctx) {
        try {
            File f = cookiesFile(ctx);
            return f.exists() && f.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Cookies actuales de facebook.com según el CookieManager del WebView.
        Las cookies de .facebook.com aparecen tanto en www como en m: si se
        concatenan tal cual, cada cookie va DOS VECES en el header Cookie y
        Facebook puede rechazar la petición con HTTP 400 (eso pasó en la
        búsqueda con NUEVO14). Aquí se quitan los duplicados por nombre
        (gana el valor de m.facebook.com, que es el que usa la búsqueda).
        De paso también se arregla el archivo fb_cookies.txt, que se
        escribía con las cookies duplicadas. */
    private static String currentCookies() {
        try {
            CookieManager cm = CookieManager.getInstance();
            String c1 = cm.getCookie("https://www.facebook.com");
            String c2 = cm.getCookie("https://m.facebook.com");
            Map<String, String> all = new LinkedHashMap<>();
            putCookies(all, c1);
            putCookies(all, c2);
            if (all.isEmpty()) return null;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : all.entrySet()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(e.getKey()).append("=").append(e.getValue());
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void putCookies(Map<String, String> out, String raw) {
        if (raw == null) return;
        for (String part : raw.split(";")) {
            String p = part.trim();
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String name = p.substring(0, eq).trim();
            String value = p.substring(eq + 1).trim();
            if (!name.isEmpty()) out.put(name, value);
        }
    }

    /** El id de usuario de Facebook es la cookie «c_user». */
    private static String userIdFromCookies(String cookies) {
        if (cookies == null) return null;
        for (String part : cookies.split(";")) {
            String p = part.trim();
            if (p.startsWith("c_user=")) {
                String v = p.substring("c_user=".length()).trim();
                if (!v.isEmpty()) return v;
            }
        }
        return null;
    }

    /** Guarda las cookies en formato Netscape (el que entiende yt-dlp). */
    private static boolean saveCookiesFile(Context ctx, String cookies) {
        if (cookies == null || cookies.trim().isEmpty()) return false;
        try {
            long expiry = System.currentTimeMillis() / 1000L + 365L * 24L * 3600L;
            StringBuilder sb = new StringBuilder();
            sb.append("# Netscape HTTP Cookie File\n");
            for (String part : cookies.split(";")) {
                String p = part.trim();
                int eq = p.indexOf('=');
                if (eq <= 0) continue;
                String name = p.substring(0, eq).trim();
                String value = p.substring(eq + 1).trim();
                if (name.isEmpty()) continue;
                sb.append(".facebook.com\tTRUE\t/\tTRUE\t")
                        .append(expiry).append("\t")
                        .append(name).append("\t").append(value).append("\n");
            }
            File f = cookiesFile(ctx);
            FileOutputStream fos = new FileOutputStream(f, false);
            OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-8");
            w.write(sb.toString());
            w.close();
            return f.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void runOnUiThread(Runnable r) {
        try {
            if (getActivity() != null) getActivity().runOnUiThread(r);
            else r.run();
        } catch (Exception e) {
            r.run();
        }
    }

    // ------------------------------------------------------------------
    // Métodos expuestos a JavaScript
    // ------------------------------------------------------------------

    /**
     * ¿Hay sesión de Facebook? Resuelve: { loggedIn, userId }.
     * La sesión vive en el CookieManager del WebView y persiste entre
     * aperturas de la app; el archivo de cookies es solo para yt-dlp.
     */
    @PluginMethod
    public void isLoggedIn(final PluginCall call) {
        String cookies = currentCookies();
        final String userId = userIdFromCookies(cookies);
        final boolean ok = userId != null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                JSObject ret = new JSObject();
                ret.put("loggedIn", ok);
                if (userId != null) ret.put("userId", userId);
                call.resolve(ret);
            }
        });
    }

    /**
     * Abre el inicio de sesión de Facebook en una ventana dentro de la
     * app. El usuario escribe su cuenta y contraseña (Facebook puede
     * pedirle verificación, como en el navegador) y toca «Listo».
     * Resuelve: { loggedIn, userId } (loggedIn false si tocó Cancelar
     * o si al tocar Listo no había sesión).
     */
    @PluginMethod
    public void login(final PluginCall call) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (getActivity() == null) {
                        call.reject("No hay actividad para abrir el inicio de sesión.");
                        return;
                    }
                    final Dialog dialog = new Dialog(getActivity());
                    dialog.setTitle("Iniciar sesión en Facebook");
                    dialog.setCancelable(true);

                    LinearLayout root = new LinearLayout(getActivity());
                    root.setOrientation(LinearLayout.VERTICAL);
                    root.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    root.setBackgroundColor(Color.parseColor("#04163C"));

                    TextView hint = new TextView(getActivity());
                    hint.setText("Inicia sesión con tu cuenta de Facebook y luego toca «Listo».\n"
                            + "Tu contraseña solo la ve Facebook, no se guarda en Gurru Music.");
                    hint.setPadding(24, 18, 24, 10);
                    hint.setTextSize(14);
                    hint.setTextColor(Color.parseColor("#CFE8FF"));
                    hint.setTypeface(android.graphics.Typeface.DEFAULT);
                    root.addView(hint);

                    final WebView web = new WebView(getActivity());
                    LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
                    web.setLayoutParams(wp);
                    // Altura mínima de respaldo: si el diálogo midiera su
                    // contenido como WRAP_CONTENT, el peso (weight=1) del
                    // WebView se colapsa a 0 y la ventana de Facebook no
                    // se ve (eso pasó en NUEVO13). Con esto siempre ocupa
                    // espacio aunque el peso fallara.
                    float density = getActivity().getResources().getDisplayMetrics().density;
                    web.setMinimumHeight((int) (420 * density));
                    WebSettings st = web.getSettings();
                    st.setJavaScriptEnabled(true);
                    st.setDomStorageEnabled(true);
                    st.setSupportZoom(true);
                    st.setBuiltInZoomControls(true);
                    st.setDisplayZoomControls(false);
                    st.setUseWideViewPort(true);
                    st.setLoadWithOverviewMode(true);
                    try {
                        CookieManager cm = CookieManager.getInstance();
                        cm.setAcceptCookie(true);
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            cm.setAcceptThirdPartyCookies(web, true);
                        }
                    } catch (Exception ignored) {}
                    web.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            view.loadUrl(url);
                            return true;
                        }
                    });
                    root.addView(web);

                    LinearLayout buttons = new LinearLayout(getActivity());
                    buttons.setOrientation(LinearLayout.HORIZONTAL);
                    buttons.setPadding(16, 10, 16, 16);
                    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    bp.setMargins(8, 0, 8, 0);
                    Button cancelBtn = new Button(getActivity());
                    cancelBtn.setText("Cancelar");
                    cancelBtn.setTypeface(android.graphics.Typeface.DEFAULT);
                    cancelBtn.setLayoutParams(bp);
                    cancelBtn.setOnClickListener(new android.view.View.OnClickListener() {
                        @Override
                        public void onClick(android.view.View v) {
                            try { dialog.dismiss(); } catch (Exception ignored) {}
                            JSObject ret = new JSObject();
                            ret.put("loggedIn", false);
                            call.resolve(ret);
                        }
                    });
                    Button doneBtn = new Button(getActivity());
                    doneBtn.setText("Listo, ya entré");
                    doneBtn.setTypeface(android.graphics.Typeface.DEFAULT);
                    doneBtn.setLayoutParams(bp);
                    doneBtn.setOnClickListener(new android.view.View.OnClickListener() {
                        @Override
                        public void onClick(android.view.View v) {
                            String cookies = currentCookies();
                            String userId = userIdFromCookies(cookies);
                            boolean ok = userId != null;
                            if (ok) saveCookiesFile(getContext(), cookies);
                            try { dialog.dismiss(); } catch (Exception ignored) {}
                            JSObject ret = new JSObject();
                            ret.put("loggedIn", ok);
                            if (userId != null) ret.put("userId", userId);
                            if (!ok) ret.put("message", "No se detectó una sesión de Facebook. Inicia sesión y toca «Listo».");
                            call.resolve(ret);
                        }
                    });
                    buttons.addView(cancelBtn);
                    buttons.addView(doneBtn);
                    root.addView(buttons);

                    dialog.setContentView(root);
                    dialog.setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(android.content.DialogInterface d) {
                            JSObject ret = new JSObject();
                            ret.put("loggedIn", false);
                            call.resolve(ret);
                        }
                    });
                    dialog.show();
                    // El diálogo por defecto mide su ventana como
                    // WRAP_CONTENT: el root MATCH_PARENT se encoge al
                    // contenido y el WebView (altura 0 + peso 1) queda en
                    // 0 px, así que la página de Facebook nunca se veía.
                    // Se fija la ventana casi a pantalla completa para
                    // que el WebView tenga altura real donde escribir la
                    // cuenta y la contraseña.
                    Window win = dialog.getWindow();
                    if (win != null) {
                        DisplayMetrics dm = getActivity().getResources().getDisplayMetrics();
                        win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                                (int) (dm.heightPixels * 0.92));
                        try {
                            win.setBackgroundDrawable(
                                    new ColorDrawable(Color.parseColor("#04163C")));
                        } catch (Exception ignored) {}
                    }
                    web.loadUrl(FB_MOBILE);
                } catch (final Exception e) {
                    call.reject("No se pudo abrir el inicio de sesión: " + e.getMessage());
                }
            }
        });
    }

    /** Cierra la sesión: borra las cookies de Facebook y el archivo para yt-dlp. */
    @PluginMethod
    public void logout(final PluginCall call) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    CookieManager cm = CookieManager.getInstance();
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        cm.removeAllCookies(null);
                        cm.flush();
                    } else {
                        cm.removeAllCookie();
                    }
                } catch (Exception ignored) {}
                try {
                    File f = cookiesFile(getContext());
                    if (f.exists()) f.delete();
                } catch (Exception ignored) {}
                JSObject ret = new JSObject();
                ret.put("loggedIn", false);
                call.resolve(ret);
            }
        });
    }

    /**
     * Busca videos en Facebook con la sesión iniciada.
     * Parámetros JS: { query, limit } (limit por defecto 10, máx 20).
     * Resuelve: { results: [{ id, url, title, page, duration, thumbnail }],
     *             needsLogin: true si Facebook pidió iniciar sesión }.
     * La búsqueda se hace pidiendo la página de resultados de
     * m.facebook.com con las cookies; el parseo es por expresiones
     * regulares sobre el HTML (Facebook lo cambia seguido: si un día
     * deja de traer resultados, hay que revisar el parseo).
     */
    @PluginMethod
    public void searchVideos(final PluginCall call) {
        final String query = call.getString("query", "");
        final int limit = call.getInt("limit", 10);
        if (query == null || query.trim().isEmpty()) {
            call.reject("Escribe el nombre de una canción o artista para buscar en Facebook.");
            return;
        }
        final int n = Math.max(1, Math.min(20, limit));
        final String q = query.trim();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String cookies = currentCookies();
                    if (userIdFromCookies(cookies) == null) {
                        throw new NeedsLoginException();
                    }
                    String encQ;
                    try { encQ = URLEncoder.encode(q, "UTF-8"); }
                    catch (Exception ue) { encQ = q.replace(" ", "+"); }
                    URL url = new URL(FB_SEARCH + encQ);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(30000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", UA_MOBILE);
                    conn.setRequestProperty("Accept-Language", "es-MX,es;q=0.9,en;q=0.5");
                    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml");
                    // Que la petición se parezca a navegar con el navegador:
                    // sin Referer ni las cabeceras Sec-Fetch, Facebook ve
                    // un cliente raro y puede rechazarlo con 400.
                    conn.setRequestProperty("Referer", "https://m.facebook.com/");
                    conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
                    conn.setRequestProperty("Sec-Fetch-Site", "same-origin");
                    conn.setRequestProperty("Sec-Fetch-Mode", "navigate");
                    conn.setRequestProperty("Sec-Fetch-User", "?1");
                    conn.setRequestProperty("Sec-Fetch-Dest", "document");
                    if (cookies != null) conn.setRequestProperty("Cookie", cookies);
                    int code = conn.getResponseCode();
                    String finalUrl = conn.getURL().toString();
                    if (finalUrl.contains("login")) {
                        throw new NeedsLoginException();
                    }
                    if (code < 200 || code >= 300) {
                        throw new Exception(httpErrorDetail(conn, code, finalUrl));
                    }
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[8192];
                    int r;
                    while ((r = br.read(buf)) != -1) sb.append(buf, 0, r);
                    br.close();
                    String html = sb.toString();
                    if (html.contains("login.php")) {
                        throw new NeedsLoginException();
                    }
                    JSONArray results = parseSearchHtml(html, n);
                    final JSONArray resArr = results;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("results", resArr);
                            ret.put("needsLogin", false);
                            call.resolve(ret);
                        }
                    });
                } catch (final NeedsLoginException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("results", new JSONArray());
                            ret.put("needsLogin", true);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject(shortErr(e.getMessage()));
                        }
                    });
                }
            }
        });
    }

    private static class NeedsLoginException extends Exception {}

    /** Detalle corto de un error HTTP para enseñarlo en el aviso: el
        código, la página final y lo que Facebook dice en su respuesta
        (si es que dice algo). Sin esto el 400 era una caja negra: ahora,
        si vuelve a fallar, el motivo completo sale al tocar el aviso y
        dice si fue la sesión, una verificación de seguridad o la página. */
    private static String httpErrorDetail(HttpURLConnection conn, int code, String finalUrl) {
        String where = finalUrl;
        try {
            URL u = new URL(finalUrl);
            where = u.getHost() + u.getPath();
        } catch (Exception ignored) {}
        String snippet = "";
        try {
            InputStream err = conn.getErrorStream();
            if (err == null) err = conn.getInputStream();
            if (err != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(err, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[2048];
                int r;
                while ((r = br.read(buf)) != -1 && sb.length() < 2000) sb.append(buf, 0, r);
                br.close();
                snippet = stripTags(sb.toString());
                if (snippet.length() > 200) snippet = snippet.substring(0, 200) + "…";
            }
        } catch (Exception ignored) {}
        String msg = "Facebook devolvió HTTP " + code + " en " + where;
        if (!snippet.isEmpty()) msg += ": " + snippet;
        return msg;
    }

    private static String shortErr(String err) {
        if (err == null) return "";
        String s = err.replaceAll("\\s+", " ").trim();
        if (s.length() > 300) s = s.substring(s.length() - 300);
        return s;
    }

    // ------------------------------------------------------------------
    // Parseo de la página de resultados de m.facebook.com
    // ------------------------------------------------------------------

    private static String decodeEntities(String s) {
        if (s == null) return "";
        return s.replace("&amp;", "&").replace("&quot;", "\"")
                .replace("&#039;", "'").replace("&apos;", "'")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replace("&nbsp;", " ");
    }

    private static String stripTags(String s) {
        if (s == null) return "";
        String t = s.replaceAll("<[^>]+>", " ");
        t = decodeEntities(t).replaceAll("\\s+", " ").trim();
        return t;
    }

    /** Saca los videos de la página de resultados: cada resultado es un
        enlace a /watch/?v=…, /<página>/videos/… o /reel/… con su título. */
    private static JSONArray parseSearchHtml(String html, int maxResults) {
        JSONArray out = new JSONArray();
        if (html == null || html.isEmpty()) return out;
        Map<String, JSONObject> seen = new LinkedHashMap<>();
        // Cada enlace <a ...> con su texto: ahí van el video y su título.
        Pattern anchor = Pattern.compile(
                "<a\\s([^>]*)href=\"([^\"]+)\"([^>]*)>(.*?)</a>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Pattern videoHref = Pattern.compile(
                "(?:/watch/\\?v=|/videos/|/reel/)(\\d+)",
                Pattern.CASE_INSENSITIVE);
        Pattern durationPat = Pattern.compile("(\\d{1,2}:\\d{2})");
        Matcher am = anchor.matcher(html);
        while (am.find() && seen.size() < maxResults) {
            String href = decodeEntities(am.group(2));
            if (href == null) continue;
            Matcher vm = videoHref.matcher(href);
            if (!vm.find()) continue;
            String id = vm.group(1);
            if (id == null || id.isEmpty() || seen.containsKey(id)) continue;
            String url;
            if (href.startsWith("http")) url = href;
            else url = "https://www.facebook.com" + href;
            // El título es el texto del enlace; si solo trae la imagen,
            // se busca el texto que sigue al enlace en la página.
            String title = stripTags(am.group(4));
            String attrs = (am.group(1) == null ? "" : am.group(1))
                    + " " + (am.group(3) == null ? "" : am.group(3));
            if (title.isEmpty()) {
                Matcher label = Pattern.compile(
                        "aria-label=\"([^\"]+)\"").matcher(attrs);
                if (label.find()) title = decodeEntities(label.group(1)).trim();
            }
            if (title.isEmpty()) {
                int end = am.end();
                int stop = Math.min(html.length(), end + 600);
                String after = stripTags(html.substring(end, stop));
                if (!after.isEmpty()) {
                    // La primera línea con texto suele ser el título.
                    String[] lines = after.split("\\s{2,}|\\n");
                    for (String ln : lines) {
                        String t = ln.trim();
                        if (t.length() >= 3 && t.length() <= 160
                                && !t.matches("(?i)^(me gusta|comentar|compartir|ver más|seguir|reproducir|\\d+.*)$")) {
                            title = t;
                            break;
                        }
                    }
                }
            }
            // La página que publicó el video (del /<página>/videos/<id>/).
            String page = "";
            Matcher pm = Pattern.compile("/([A-Za-z0-9._-]+)/videos/\\d+").matcher(href);
            if (pm.find()) page = pm.group(1);
            // Duración cercana al enlace (p. ej. "3:36"): se busca solo
            // dentro del enlace y justo después, para no tomar la
            // duración del resultado siguiente.
            double duration = 0;
            int start = am.start();
            int stop = Math.min(html.length(), am.end() + 300);
            Matcher dm = durationPat.matcher(html.substring(start, stop));
            if (dm.find()) {
                try {
                    String[] parts = dm.group(1).split(":");
                    duration = Integer.parseInt(parts[0]) * 60
                            + Integer.parseInt(parts[1]);
                } catch (Exception ignored) {}
            }
            if (title.isEmpty()) title = "Video de Facebook";
            try {
                JSONObject r = new JSONObject();
                r.put("id", id);
                r.put("url", url);
                r.put("title", title);
                r.put("page", page);
                r.put("duration", duration);
                r.put("thumbnail", JSONObject.NULL);
                seen.put(id, r);
                out.put(r);
            } catch (Exception ignored) {}
        }
        return out;
    }
}
