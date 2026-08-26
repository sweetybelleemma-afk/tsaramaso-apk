package mg.tsaramaso;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.graphics.Color;
import java.util.Map;
import java.util.HashMap;

public class MainActivity extends Activity {
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#0b0f19"));
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Intercepter les requêtes vers bet261 pour supprimer X-Frame-Options
                String url = request.getUrl().toString();
                if (url.contains("bet261.mg")) {
                    try {
                        java.net.URL u = new java.net.URL(url);
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                        conn.setRequestMethod(request.getMethod());
                        // Copier les headers de la requête originale
                        Map<String, String> reqHeaders = request.getRequestHeaders();
                        for (Map.Entry<String, String> entry : reqHeaders.entrySet()) {
                            conn.setRequestProperty(entry.getKey(), entry.getValue());
                        }
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");
                        conn.connect();

                        // Filtrer les headers de réponse — supprimer X-Frame-Options
                        Map<String, String> responseHeaders = new HashMap<>();
                        Map<String, java.util.List<String>> headerFields = conn.getHeaderFields();
                        for (Map.Entry<String, java.util.List<String>> entry : headerFields.entrySet()) {
                            String key = entry.getKey();
                            if (key == null) continue;
                            // Supprimer les headers qui bloquent l'iframe
                            if (key.equalsIgnoreCase("X-Frame-Options")) continue;
                            if (key.equalsIgnoreCase("Content-Security-Policy")) continue;
                            responseHeaders.put(key, entry.getValue().get(0));
                        }

                        String mimeType = conn.getContentType();
                        if (mimeType == null) mimeType = "text/html";
                        if (mimeType.contains(";")) mimeType = mimeType.split(";")[0].trim();

                        return new WebResourceResponse(
                            mimeType,
                            "utf-8",
                            conn.getResponseCode(),
                            conn.getResponseMessage(),
                            responseHeaders,
                            conn.getInputStream()
                        );
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            }
        });

        webView.loadUrl("http://127.0.0.1:8080");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
