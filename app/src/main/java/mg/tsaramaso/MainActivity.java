package mg.tsaramaso;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    WebView dashboardView;
    WebView gameView;
    FrameLayout container;
    boolean showingGame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        container = new FrameLayout(this);
        setContentView(container);

        // ── WebView Dashboard ──
        dashboardView = createWebView();
        dashboardView.loadUrl("file:///android_asset/index.html");
        container.addView(dashboardView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // ── WebView Jeux (bet261 plein écran) ──
        gameView = createWebView();
        gameView.loadUrl("https://bet261.mg/instant-games/llc/Aviator");
        gameView.setVisibility(android.view.View.GONE);
        container.addView(gameView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // ── Bridge JS → Java pour changer de page ──
        dashboardView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void showGame() {
                runOnUiThread(() -> {
                    gameView.setVisibility(android.view.View.VISIBLE);
                    dashboardView.setVisibility(android.view.View.GONE);
                    showingGame = true;
                });
            }

            @android.webkit.JavascriptInterface
            public void showDashboard() {
                runOnUiThread(() -> {
                    dashboardView.setVisibility(android.view.View.VISIBLE);
                    gameView.setVisibility(android.view.View.GONE);
                    showingGame = false;
                });
            }
        }, "NativeApp");
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

    @Override
    public void onBackPressed() {
        if (showingGame) {
            dashboardView.setVisibility(android.view.View.VISIBLE);
            gameView.setVisibility(android.view.View.GONE);
            showingGame = false;
        } else if (dashboardView.canGoBack()) {
            dashboardView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
