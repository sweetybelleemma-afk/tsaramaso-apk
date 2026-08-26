package mg.tsaramaso;
import android.app.Activity;
import android.os.Bundle;
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

    // Script injecté dans l'iframe du jeu Aviator
    static final String SCRAPE_SCRIPT =
        "if (!window._tsaramasoActif) {" +
        "  window._tsaramasoActif = true;" +
        "  var _farany = '';" +
        // HUD overlay
        "  var hud = document.createElement('div');" +
        "  hud.id = 'ts-hud';" +
        "  hud.style.cssText = 'position:fixed;top:8px;left:50%;transform:translateX(-50%);background:rgba(15,23,42,0.9);color:#e2e8f0;font-size:11px;font-weight:700;padding:5px 14px;border-radius:20px;z-index:2147483647;pointer-events:none;white-space:nowrap;';" +
        "  hud.innerHTML = '<span style=\"color:#f59e0b\">● IA - Acquisition...</span>';" +
        "  document.body.appendChild(hud);" +
        // Scraping
        "  setInterval(function() {" +
        "    var allEls = document.querySelectorAll('*');" +
        "    var envoleEl = null;" +
        "    for (var i=0; i<allEls.length; i++) {" +
        "      var t = (allEls[i].innerText||'').trim();" +
        "      if (/ENVOL/i.test(t) && allEls[i].children.length <= 2) {" +
        "        envoleEl = allEls[i]; break;" +
        "      }" +
        "    }" +
        "    if (!envoleEl) return;" +
        "    var coteStr = null;" +
        "    var xReg = /([0-9]+[.,][0-9]+)\\s*[xX]?/;" +
        // Chercher dans le parent
        "    var parent = envoleEl.parentElement;" +
        "    if (parent) {" +
        "      var siblings = parent.querySelectorAll('*');" +
        "      for (var j=0; j<siblings.length; j++) {" +
        "        var st = (siblings[j].innerText||'').trim();" +
        "        if (/^[0-9]+[.,][0-9]+[xX]?$/.test(st)) {" +
        "          coteStr = st.replace(/[xX]/g,'').replace(',','.'); break;" +
        "        }" +
        "      }" +
        "    }" +
        // Chercher dans les voisins si pas trouvé
        "    if (!coteStr) {" +
        "      var next = envoleEl.nextElementSibling;" +
        "      for (var k=0; k<5&&next; k++) {" +
        "        var nt = (next.innerText||'').trim();" +
        "        var nm = nt.match(/([0-9]+[.,][0-9]+)/);" +
        "        if (nm) { coteStr = nm[1].replace(',','.'); break; }" +
        "        next = next.nextElementSibling;" +
        "      }" +
        "    }" +
        "    if (!coteStr) return;" +
        "    var num = parseFloat(coteStr);" +
        "    if (isNaN(num)||num<1.0||num>999) return;" +
        "    var final_ = num.toFixed(2);" +
        "    if (_farany===final_) return;" +
        "    _farany = final_;" +
        "    TsaramasoNative.onEnvole(final_);" +
        "  }, 800);" +
        "}";

    // Script injecté dans la PAGE PRINCIPALE bet261 pour accéder aux iframes
    static final String IFRAME_INJECT_SCRIPT =
        "(function() {" +
        "  function injectIntoIframe(iframe) {" +
        "    try {" +
        "      var doc = iframe.contentDocument || iframe.contentWindow.document;" +
        "      if (!doc || !doc.body) return;" +
        "      if (doc.body._tsaramasoInjected) return;" +
        "      doc.body._tsaramasoInjected = true;" +
        "      var s = doc.createElement('script');" +
        "      s.textContent = `" +
        "        if (!window._tsaramasoActif) {" +
        "          window._tsaramasoActif = true;" +
        "          var _farany = '';" +
        "          setInterval(function() {" +
        "            var allEls = document.querySelectorAll('*');" +
        "            var envoleEl = null;" +
        "            for (var i=0; i<allEls.length; i++) {" +
        "              var t = (allEls[i].innerText||'').trim();" +
        "              if (/ENVOL/i.test(t) && allEls[i].children.length <= 2) {" +
        "                envoleEl = allEls[i]; break;" +
        "              }" +
        "            }" +
        "            if (!envoleEl) return;" +
        "            var coteStr = null;" +
        "            var parent = envoleEl.parentElement;" +
        "            if (parent) {" +
        "              var siblings = parent.querySelectorAll('*');" +
        "              for (var j=0; j<siblings.length; j++) {" +
        "                var st = (siblings[j].innerText||'').trim();" +
        "                if (/^[0-9]+[.,][0-9]+[xX]?$/.test(st)) {" +
        "                  coteStr = st.replace(/[xX]/g,'').replace(',','.'); break;" +
        "                }" +
        "              }" +
        "            }" +
        "            if (!coteStr) {" +
        "              var next = envoleEl.nextElementSibling;" +
        "              for (var k=0; k<5&&next; k++) {" +
        "                var nt = (next.innerText||'').trim();" +
        "                var nm = nt.match(/([0-9]+[.,][0-9]+)/);" +
        "                if (nm) { coteStr = nm[1].replace(',','.'); break; }" +
        "                next = next.nextElementSibling;" +
        "              }" +
        "            }" +
        "            if (!coteStr) return;" +
        "            var num = parseFloat(coteStr);" +
        "            if (isNaN(num)||num<1.0||num>999) return;" +
        "            var final_ = num.toFixed(2);" +
        "            if (_farany===final_) return;" +
        "            _farany = final_;" +
        "            window.top.TsaramasoNative.onEnvole(final_);" +
        "          }, 800);" +
        "        }" +
        "      `;" +
        "      doc.head.appendChild(s);" +
        "    } catch(e) {}" +
        "  }" +
        "  setInterval(function() {" +
        "    var iframes = document.querySelectorAll('iframe');" +
        "    for (var i=0; i<iframes.length; i++) injectIntoIframe(iframes[i]);" +
        "  }, 2000);" +
        "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        container = new FrameLayout(this);
        setContentView(container);

        // ── Dashboard ──
        dashboardView = createWebView();
        dashboardView.loadUrl("file:///android_asset/index.html");
        container.addView(dashboardView, matchParent());

        // ── Jeu ──
        gameView = createWebView();
        gameView.setVisibility(android.view.View.GONE);
        container.addView(gameView, matchParent());

        gameView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return false; }
            @Override
            public void onPageFinished(WebView view, String url) {
                // Injecter dans la page principale (pour accéder aux iframes)
                view.evaluateJavascript(IFRAME_INJECT_SCRIPT, null);
                // Injecter aussi directement (si pas d'iframe)
                view.evaluateJavascript(SCRAPE_SCRIPT, null);
            }
        });

        // ── Bridge natif scraping ──
        gameView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onEnvole(String cote) {
                runOnUiThread(() -> gameView.evaluateJavascript(
                    "(function(){var h=document.getElementById('ts-hud');if(h)h.innerHTML='<span style=\"color:#3b82f6\">⏳ " + cote + "x...</span>';})()", null
                ));
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
        }, "TsaramasoNative");

        // ── Fix upload fichier dans dashboard WebView ──
        dashboardView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb, FileChooserParams params) {
                fileUploadCallback = cb;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Choisir une image"), FILE_CHOOSER_REQUEST);
                return true;
            }
        });

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

        // ── Bouton retour ──
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

        dashboardView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void showGame() { runOnUiThread(() -> afficherJeu()); }
        }, "NativeApp");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (fileUploadCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
                fileUploadCallback.onReceiveValue(results);
                fileUploadCallback = null;
            }
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
