package mg.tsaramaso;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.*;
import android.graphics.Color;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

public class MainActivity extends Activity {
    WebView dashboardView;
    WebView gameView;
    FrameLayout container;
    ImageButton btnRetour;
    boolean showingGame = false;
    Handler scrapHandler = new Handler();
    String derniereValeur = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        container = new FrameLayout(this);
        setContentView(container);

        // ── Dashboard WebView ──────────────────────────
        dashboardView = createWebView();
        dashboardView.loadUrl("file:///android_asset/index.html");
        container.addView(dashboardView, matchParent());

        // ── Game WebView ───────────────────────────────
        gameView = createWebView();
        gameView.loadUrl("https://bet261.mg/instant-games/llc/Aviator");
        gameView.setVisibility(android.view.View.GONE);
        container.addView(gameView, matchParent());

        // ── Bouton retour flottant ─────────────────────
        btnRetour = new ImageButton(this);
        btnRetour.setText("⬅ Dashboard");
        ((android.widget.TextView) btnRetour).setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1e40af"));
        bg.setCornerRadius(dp(24));
        btnRetour.setBackground(bg);
        btnRetour.setTextColor(Color.WHITE);
        btnRetour.setPadding(dp(16), dp(10), dp(16), dp(10));
        btnRetour.setVisibility(android.view.View.GONE);
        btnRetour.setOnClickListener(v -> afficherDashboard());

        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.gravity = Gravity.TOP | Gravity.END;
        btnParams.topMargin = dp(48);
        btnParams.rightMargin = dp(12);
        container.addView(btnRetour, btnParams);

        // ── Bridge JS → Java ───────────────────────────
        dashboardView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void showGame() {
                runOnUiThread(() -> afficherJeu());
            }
        }, "NativeApp");

        // ── Scraping toutes les secondes ───────────────
        demarrerScraping();
    }

    void afficherJeu() {
        gameView.setVisibility(android.view.View.VISIBLE);
        dashboardView.setVisibility(android.view.View.GONE);
        btnRetour.setVisibility(android.view.View.VISIBLE);
        showingGame = true;
    }

    void afficherDashboard() {
        dashboardView.setVisibility(android.view.View.VISIBLE);
        gameView.setVisibility(android.view.View.GONE);
        btnRetour.setVisibility(android.view.View.GONE);
        showingGame = false;
    }

    void demarrerScraping() {
        scrapHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (gameView != null) {
                    gameView.evaluateJavascript(
                        "(function() {" +
                        "  var els = document.querySelectorAll('*');" +
                        "  var result = '';" +
                        "  for (var i = 0; i < els.length; i++) {" +
                        "    var t = (els[i].innerText || '').trim();" +
                        "    if (/ENVOL/i.test(t) || /FLEW/i.test(t)) {" +
                        "      var m = t.match(/([0-9]+[.,][0-9]+)\\s*[xX]?/);" +
                        "      if (m) return m[1].replace(',','.');" +
                        "    }" +
                        "  }" +
                        "  return '';" +
                        "})()",
                        value -> {
                            if (value != null && !value.equals("\"\"") && !value.equals("null")) {
                                String cote = value.replace("\"", "").trim();
                                if (!cote.equals(derniereValeur) && !cote.isEmpty()) {
                                    derniereValeur = cote;
                                    envoyerCote(cote);
                                }
                            }
                        }
                    );
                }
                scrapHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    void envoyerCote(String cote) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://tsaramaso-backend.onrender.com/api/nouveau_tour");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                String body = "{\"cote\":" + cote + "}";
                conn.getOutputStream().write(body.getBytes());
                java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream())
                );
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                final String response = sb.toString();
                conn.disconnect();
                // Mettre à jour le dashboard
                runOnUiThread(() -> {
                    dashboardView.evaluateJavascript(
                        "updateFromNative(" + response + ")", null
                    );
                });
            } catch (Exception e) {}
        }).start();
    }

    private WebView createWebView() {
        WebView wv = new WebView(this);
        wv.setBackgroundColor(Color.parseColor("#0b0f19"));
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return false;
            }
        });
        return wv;
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    private int dp(int val) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, val,
            getResources().getDisplayMetrics()
        ));
    }

    @Override
    public void onBackPressed() {
        if (showingGame) afficherDashboard();
        else if (dashboardView.canGoBack()) dashboardView.goBack();
        else super.onBackPressed();
    }
}
