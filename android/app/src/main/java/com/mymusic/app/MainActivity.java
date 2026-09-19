package com.mymusic.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * MyMusic 安卓壳：WebView 加载已部署的音乐站点。
 * - 首次启动（或地址失效时）通过原生设置页输入服务器地址，保存到本地
 * - 支持站点内音频播放（含后台播放）、文件上传（系统文件选择器）
 */
public class MainActivity extends Activity {

    private static final String PREFS = "mymusic";
    private static final String KEY_SERVER_URL = "server_url";

    private WebView webView;
    private View settingsView;
    private View errorView;
    private TextView errorText;
    private EditText urlInput;
    private Button btnRetry;

    /** 当前生效的服务器地址（带 scheme，无尾部斜杠） */
    private String serverUrl;

    /** 站点上传歌曲时 <input type="file"> 的回调 */
    private ValueCallback<Uri[]> fileChooserCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.web_view);
        settingsView = findViewById(R.id.settings_view);
        errorView = findViewById(R.id.error_view);
        errorText = findViewById(R.id.error_text);
        urlInput = findViewById(R.id.url_input);
        btnRetry = findViewById(R.id.btn_retry);
        View btnChange = findViewById(R.id.btn_change);
        View btnSave = findViewById(R.id.btn_save);

        setupWebView();

        btnRetry.setOnClickListener(v -> {
            if (serverUrl != null) {
                hideError();
                webView.loadUrl(serverUrl);
            } else {
                showSettings();
            }
        });
        btnChange.setOnClickListener(v -> showSettings());
        btnSave.setOnClickListener(v -> saveAndEnter());

        serverUrl = getPrefs().getString(KEY_SERVER_URL, null);
        if (serverUrl != null && !serverUrl.isEmpty()) {
            enterWeb();
        } else {
            showSettings();
        }
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        // 允许播放器自动起播；切后台不调用 onPause，音频可持续播放
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        webView.setBackgroundColor(0x00000000);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                // 站内 http(s) 由 WebView 自己处理，其余协议交给系统
                if (scheme.equals("http") || scheme.equals("https")) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showError();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }
                fileChooserCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(
                            Intent.createChooser(intent, getString(R.string.pick_file)), 1001);
                } catch (Exception e) {
                    fileChooserCallback = null;
                    return false;
                }
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001 && fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(
                    WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileChooserCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /** 读取输入框地址，规范化后保存并进入 WebView */
    private void saveAndEnter() {
        String url = normalizeUrl(urlInput.getText().toString());
        if (url == null) {
            urlInput.setError(getString(R.string.invalid_url));
            return;
        }
        serverUrl = url;
        getPrefs().edit().putString(KEY_SERVER_URL, url).apply();
        enterWeb();
    }

    private void enterWeb() {
        settingsView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(serverUrl);
    }

    private void showSettings() {
        errorView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        settingsView.setVisibility(View.VISIBLE);
        urlInput.setText(serverUrl == null ? "" : serverUrl);
        urlInput.setError(null);
    }

    private void showError() {
        errorText.setText(getString(R.string.load_failed, serverUrl));
        errorView.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorView.setVisibility(View.GONE);
    }

    /** 补全 scheme、去掉尾部斜杠；无效返回 null */
    private static String normalizeUrl(String raw) {
        if (raw == null) return null;
        String url = raw.trim();
        if (url.isEmpty()) return null;
        if (!url.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            url = "http://" + url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        // scheme 之后必须还有主机名
        if (url.replaceFirst("(?i)^[a-z][a-z0-9+.-]*://", "").isEmpty()) {
            return null;
        }
        return url;
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.getVisibility() == View.VISIBLE
                && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(null);
            fileChooserCallback = null;
        }
        webView.destroy();
        super.onDestroy();
    }
}
