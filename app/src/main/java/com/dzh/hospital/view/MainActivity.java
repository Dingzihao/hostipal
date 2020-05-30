package com.dzh.hospital.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.afollestad.materialdialogs.MaterialDialog;
import com.blankj.utilcode.util.DeviceUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.dzh.hospital.R;
import com.dzh.hospital.databinding.ActivityMainBinding;
import com.dzh.hospital.util.ChineseToSpeech;
import com.dzh.hospital.util.TTSUtils;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.functions.Consumer;

/**
 * @author 丁子豪
 * @desc 主页
 * @data on 2020/5/28 14:10
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_CODE = 123;
    private ActivityMainBinding mDataBinding;
    ChineseToSpeech mSpeech;
    String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mDataBinding.setHandler(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            initPermission();
        } else {
            TTSUtils.getInstance().init(this);
        }
        initView();
    }

    private void initView() {
        mSpeech = new ChineseToSpeech(this);
        String mac = DeviceUtils.getMacAddress();
        String ipAddress = NetworkUtils.getIPAddress(true);

        mDataBinding.webView.getSettings().setJavaScriptEnabled(true);
        //不显示垂直滚动条
        mDataBinding.webView.setVerticalScrollBarEnabled(false);
        mDataBinding.webView.clearCache(true);
        mDataBinding.webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mDataBinding.webView.getSettings().setUseWideViewPort(true);
        mDataBinding.webView.getSettings().setLoadWithOverviewMode(true);
        mDataBinding.webView.getSettings().setDomStorageEnabled(true);
        mDataBinding.webView.getSettings().setLoadsImagesAutomatically(true);
        mDataBinding.webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        //解决网页对话框无法显示
        mDataBinding.webView.setWebChromeClient(new WebChromeClient());
        mDataBinding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                setMacAndIp(mac, ipAddress);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String newurl) {
                return super.shouldOverrideUrlLoading(view, newurl);
            }
        });
        mDataBinding.webView.addJavascriptInterface(new DecoObject(), "android");
        mDataBinding.webView.loadUrl("https://www.baidu.com");

    }


    private void setMacAndIp(String macAddress, String ip) {
        Log.d(TAG, "MAC:" + macAddress);
        Log.d(TAG, "IP:" + ip);
        mDataBinding.webView.evaluateJavascript("javascript:dealWithData(" + macAddress + "," + ip + ")", value -> {
        });
    }

    public void speak() {
        TTSUtils.getInstance().speak("请125号丁春秋到五诊区12诊室就诊");
//        mSpeech.speech("请125号丁春秋到五诊区12诊室就诊");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mSpeech) {
            mSpeech.destroy();
        }
    }

    public class DecoObject {
        @JavascriptInterface
        public void speak(String data) {
            TTSUtils.getInstance().speak(data);
//            mSpeech.speech(data);
        }
    }

    private void initPermission() {
        ArrayList<String> toApplyList = new ArrayList<>();
        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), PERMISSIONS_CODE);
        } else {
            TTSUtils.getInstance().init(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_CODE) {
            Map<String, Integer> perms = new HashMap<>();
            perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
            for (int i = 0; i < permissions.length; i++) {
                perms.put(permissions[i], grantResults[i]);
            }
            if (perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                TTSUtils.getInstance().init(this);
            } else {//弹出对话框引导用户去设置
                new MaterialDialog.Builder(this)
                        .title("提示")
                        .content("需要所有权限才能正常使用")
                        .negativeText("关闭应用")
                        .onNegative((dialog, which) -> finish())
                        .positiveText("继续申请")
                        .onPositive((dialog, which) -> initPermission())
                        .show();
            }
        }
    }

}
