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
import android.net.Uri;
import android.content.Intent;

public class MainActivity extends Activity {
    WebView dashboardView;
    WebView gameView;
    FrameLayout container;
    Button btnRetour;
    boolean showingGame = false;
    static final String API = "https://tsaramaso-backend.onrender.com";
    ValueCallback<Uri[]> fileUploadCallback;
    static final int FILE_CHOOSER_REQUEST = 1;
    Handler scrapHandler = new Handler();
    String derniereValeur = "";

    // Script qui cherche ENVOLÉ dans TOUS les documents (page + iframes)
    static final String FIND_ENVOLE =
        "(function() {" +
        "  function chercher(doc) {" +
        "    try {" +
        "      var els = doc.querySelectorAll('*');" +
        "      var envoleIdx = -1;" +
        "      for (var i=0; i<els.length; i++) {" +
        "        var t = (els[i].innerText||'').trim();" +
        "        if (/ENVOL/i.test(t) && els[i].children.length <= 3) {" +
        "          envoleIdx = i; break;" +
        "        }" +
        "      }" +
        "      if (envoleIdx < 0) return null;" +
        "      var el = els[envoleIdx];" +
        "      var parent = el.parentElement;" +
        "      var zone = parent ? parent : el;" +
        "      var texte = zone.innerText || '';" +
        "      var m = texte.match(/([0-9]+[.,][0-9]+)/);" +
        "      if (m) return m[1].replace(',','.');" +
        "      var next = el.nextElementSibling;" +
        "      for (var k=0; k<5&&next; k++) {" +
        "        var nt = (next.innerText||'').trim();" +
        "        var nm = nt.match(/([0-9]+[.,][0-9]+)/);" +
        "        if (nm) return nm[1].replace(',','.');" +
        "        next = next.nextElementSibling;" +
        "      }" +
        "    } catch(e) {}" +
        "    return null;" +
        "  }" +
        // Chercher dans la page principale
        "  var r = chercher(document);" +
        "  if (r) return r;" +
        // Chercher dans chaque iframe
        "  var iframes = document.querySelectorAll('iframe');" +
        "  for (var i=0; i<iframes.length; i++) {" +
        "    try {" +
        "      var doc2 = iframes[i].contentDocument || iframes[i].contentWindow.document;" +
        "      var r2 = chercher(doc2);" +
        "      if (r2) return r2;" +
        "      var iframes2 = doc2.querySelectorAll('iframe');" +
        "      for (var j=0; j<iframes2.length; j++) {" +
        "        try {" +
        "          var doc3 = iframes2[j].contentDocument || iframes2[j].contentWindow.document;" +
        "          var r3 = chercher(doc3);" +
        "          if (r3) return r3;" +
        "        } catch(e) {}" +
        "      }" +
        "    } catch(e) {}" +
        "  }" +
        "  return '';" +
        "})()";

    static final String HUD_SCRIPT =
        "if (!window._hudOk) {" +
        "  window._hudOk = true;" +
        "  var h = document.createElement('div');" +
        "  h.id = 'ts-hud';" +
        "  h.style.cssText = 'position:fixed;top:8px;left:50%;transform:translateX(-50%);background:rgba(15,23,42,0.9);color:#e2e8f0;font-size:11px;font-weight:700;padding:5px 14px;border-radius:20px;z-index:2147483647;pointer-events:none;white-space:nowrap;';" +
        "  h.innerHTML = '<span style=\"color:#f59e0b\">● IA - Acquisition...</span>';" +
        "  document.body.appendChild(h);" +
        "}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        container = new FrameLayout(this);
        setContentView(container);

        dashboardView = createWebView();
        dashboardView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb, FileChooserParams p) {
                fileUploadCallback = cb;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Choisir une image"), FILE_CHOOSER_REQUEST);
                return true;
            }
            @Override
            public void onPermissionRequest(PermissionRequest r) { r.grant(r.getResources()); }
        });
        dashboardView.loadUrl("file:///android_asset/index.html");
        container.addView(dashboardView, matchParent());

        gameView = createWebView();
        gameView.setVisibility(android.view.View.GONE);
        container.addView(gameView, matchParent());

        gameView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return false; }
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(HUD_SCRIPT, null);
                demarrerScrapingJava();
            }
        });

        dashboardView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void showGame() { runOnUiThread(() -> afficherJeu()); }
        }, "NativeApp");

        // Réveiller Render
        new Thread(() -> {
            try {
                java.net.URL u = new java.net.URL(API + "/api/etat");
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                c.setConnectTimeout(30000); c.setReadTimeout(30000);
                c.getResponseCode(); c.disconnect();
            } catch (Exception e) {}
        }).start();

        gameView.loadUrl("https://bet261.mg/instant-games/llc/Aviator");

        btnRetour = new Button(this);
        btnRetour.setText("< Dashboard");
        btnRetour.setTextColor(Color.WHITE);
        btnRetour.setBackgroundColor(Color.parseColor("#1e40af"));
        btnRetour.setVisibility(android.view.View.GONE);
        btnRetour.setOnClickListener(v -> afficherDashboard());
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.gravity = Gravity.TOP | Gravity.END;
        bp.topMargin = dp(48); bp.rightMargin = dp(8);
        container.addView(btnRetour, bp);
    }

    // ── Scraping depuis Java directement ──────────────────────
    boolean scrapingDemarre = false;

    void demarrerScrapingJava() {
        if (scrapingDemarre) return;
        scrapingDemarre = true;
        scrapHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                gameView.evaluateJavascript(FIND_ENVOLE, value -> {
                    if (value != null && !value.equals("\"\"") && !value.equals("null") && !value.isEmpty()) {
                        String cote = value.replace("\"", "").trim();
                        try {
                            float num = Float.parseFloat(cote);
                            if (num >= 1.0f && num <= 999f) {
                                String final_ = String.format("%.2f", num);
                                if (!final_.equals(derniereValeur)) {
                                    derniereValeur = final_;
                                    // Mettre à jour HUD
                                    runOnUiThread(() -> gameView.evaluateJavascript(
                                        "(function(){var h=document.getElementById('ts-hud');if(h)h.innerHTML='<span style=\"color:#3b82f6\">⏳ " + final_ + "x...</span>';})()", null
                                    ));
                                    envoyerCote(final_);
                                }
                            }
                        } catch (Exception e) {}
                    }
                });
                scrapHandler.postDelayed(this, 800);
            }
        }, 3000);
    }

    void envoyerCote(String cote) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(API + "/api/nouveau_tour");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
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
                runOnUiThread(() -> {
                    gameView.evaluateJavascript(
                        "(function(){var r=" + response + ";var h=document.getElementById('ts-hud');if(!h)return;var c='#e2e8f0',txt=r.recommandation||'...';if(txt.indexOf('ENTR')>=0)c='#f59e0b';else if(txt.indexOf('VICTOIRE')>=0)c='#22c55e';else if(txt.indexOf('CHEC')>=0)c='#ef4444';else if(txt.indexOf('ACQUI')>=0)c='#3b82f6';h.innerHTML='<span style=\"color:'+c+'\">'+txt+'</span>';if(navigator.vibrate){if(txt.indexOf('ENTR')>=0)navigator.vibrate([600,200,600]);else if(txt.indexOf('VICTOIRE')>=0)navigator.vibrate([150,100,150]);}})()", null);
                    dashboardView.evaluateJavascript("updateFromNative(" + response + ")", null);
                });
            } catch (Exception e) {
                runOnUiThread(() -> gameView.evaluateJavascript(
                    "(function(){var h=document.getElementById('ts-hud');if(h)h.innerHTML='<span style=\"color:#ef4444\">● Erreur réseau</span>';})()", null));
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST && fileUploadCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            fileUploadCallback.onReceiveValue(results);
            fileUploadCallback = null;
        }
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
            public void onPermissionRequest(PermissionRequest request) { request.grant(request.getResources()); }
        });
        return wv;
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int dp(int val) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics()));
    }

    @Override
    public void onBackPressed() {
        if (showingGame) afficherDashboard();
        else if (dashboardView.canGoBack()) dashboardView.goBack();
        else super.onBackPressed();
    }
}
