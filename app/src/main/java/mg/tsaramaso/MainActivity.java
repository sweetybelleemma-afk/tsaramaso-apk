package mg.tsaramaso;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.*;
import android.graphics.Color;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.Button;
import android.util.TypedValue;

public class MainActivity extends Activity {
    WebView dashboardView;
    WebView gameView;
    FrameLayout container;
    Button btnRetour;
    boolean showingGame = false;
    Handler scrapHandler = new Handler();
    String derniereValeur = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        container = new FrameLayout(this);
        setContentView(container);

        dashboardView = createWebView();
        dashboardView.loadUrl("file:///android_asset/index.html");
        container.addView(dashboardView, matchParent());

        gameView = createWebView();
        gameView.loadUrl("https://bet261.mg/instant-games/llc/Aviator");
        gameView.setVisibility(android.view.View.GONE);
        container.addView(gameView, matchParent());

        btnRetour = new Button(this);
        btnRetour.setText("< Dashboard");
        btnRetour.setTextColor(Color.WHITE);
        btnRetour.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnRetour.setBackgroundColor(Color.parseColor("#1e40af"));
        btnRetour.setVisibility(android.view.View.GONE);
        btnRetour.setOnClickListener(v -> afficherDashboard());

        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.gravity = Gravity.TOP | Gravity.END;
        btnParams.topMargin = dp(48);
        btnParams.rightMargin = dp(8);
        container.addView(btnRetour, btnParams);

        dashboardView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void showGame() {
                runOnUiThread(() -> afficherJeu());
            }
        }, "NativeApp");

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
                        "(function(){" +
                        "var els=document.querySelectorAll('*');" +
                        "for(var i=0;i<els.length;i++){" +
                        "var t=(els[i].innerText||'').trim();" +
                        "if(/ENVOL/i.test(t)||/FLEW/i.test(t)){" +
                        "var m=t.match(/([0-9]+[.,][0-9]+)/);" +
                        "if(m)return m[1].replace(',','.');" +
                        "}}" +
                        "return '';})();",
                        value -> {
                            if (value != null && !value.equals("\"\"") && !value.equals("null")) {
                                String cote = value.replace("\"", "").trim();
                                if (!cote.isEmpty() && !cote.equals(derniereValeur)) {
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
                conn.getOutputStream().write(("{\"cote\":" + cote + "}").getBytes());
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                final String response = sb.toString();
                conn.disconnect();
                runOnUiThread(() -> dashboardView.evaluateJavascript("updateFromNative(" + response + ")", null));
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
