package com.oneorigin.union;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * Union v0.0.1 Android shell.
 *
 * A small, auditable WebView host. The packaged web surface is served from a
 * loopback-only asset server so it gets a secure origin: IndexedDB, WebCrypto
 * and the service worker all behave as they do on the web. Nothing is fetched
 * from the network unless the user configures a relay or opens a link.
 */
public class MainActivity extends Activity {
    private static final int REQUEST_FILE = 0x554e;

    private WebView web;
    private AssetServer server;
    private UnionHost host;
    private ValueCallback<Uri[]> pendingFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#06070C"));
        getWindow().setNavigationBarColor(Color.parseColor("#06070C"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }

        host = new UnionHost(this);

        String startUrl;
        try {
            server = AssetServer.start(getAssets(), "web");
            startUrl = server.url();
        } catch (Exception error) {
            Toast.makeText(this, "Union could not open its local surface.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#06070C"));
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setBlockNetworkLoads(false);
        settings.setBlockNetworkImage(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        settings.setSaveFormData(false);
        settings.setGeolocationEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        settings.setUserAgentString(settings.getUserAgentString() + " Union/0.0.1");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return route(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return route(Uri.parse(url));
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
            ) {
                if (pendingFiles != null) pendingFiles.onReceiveValue(null);
                pendingFiles = callback;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                    "image/*",
                    "text/*",
                    "text/markdown",
                    "application/json",
                    "application/octet-stream"
                });
                try {
                    startActivityForResult(intent, REQUEST_FILE);
                    return true;
                } catch (Exception error) {
                    pendingFiles = null;
                    Toast.makeText(
                        MainActivity.this,
                        "No file picker is available on this device.",
                        Toast.LENGTH_LONG
                    ).show();
                    return false;
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                return false;
            }
        });

        web.addJavascriptInterface(host, "UnionHost");

        setContentView(
            web,
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        );

        if (savedInstanceState == null) {
            web.loadUrl(startUrl);
        } else {
            web.restoreState(savedInstanceState);
        }
    }

    /** Loopback is ours; everything else leaves for the system browser. */
    private boolean route(Uri uri) {
        if (uri == null) return true;
        String scheme = uri.getScheme();
        String hostName = uri.getHost();
        if ("http".equals(scheme) && ("127.0.0.1".equals(hostName) || "localhost".equals(hostName))) {
            return false;
        }
        if ("https".equals(scheme) || "mailto".equals(scheme) || "tel".equals(scheme)) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {
                // no browser installed: the WebView simply does not navigate
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FILE) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    result = new Uri[count];
                    for (int i = 0; i < count; i += 1) {
                        result[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    result = new Uri[] { data.getData() };
                }
            }
            if (pendingFiles != null) {
                pendingFiles.onReceiveValue(result);
                pendingFiles = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web == null) {
            super.onBackPressed();
            return;
        }
        web.evaluateJavascript(
            "(function(){try{return window.unionAndroidBack?window.unionAndroidBack():false}catch(e){return false}})()",
            value -> {
                if (!"true".equals(value)) {
                    if (web.canGoBack()) {
                        web.goBack();
                    } else {
                        moveTaskToBack(true);
                    }
                }
            }
        );
    }

    @Override
    protected void onPause() {
        if (web != null) web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (web != null) web.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("UnionHost");
            web.destroy();
            web = null;
        }
        if (server != null) {
            server.close();
            server = null;
        }
        super.onDestroy();
    }
}
