package ru.utopicnarwhal.utopic_tor_onion_proxy;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;

import java.util.concurrent.TimeUnit;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class UtopicTorOnionProxyPlugin implements FlutterPlugin, MethodCallHandler {

    private MethodChannel channel;
    private OnionProxyManager tor;
    private Context context;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "utopic_tor_onion_proxy");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "utopic_tor_onion_proxy");
        channel.setMethodCallHandler(new UtopicTorOnionProxyPlugin());
    }

    // MethodChannel.Result wrapper that responds on the platform thread.
    private static class MethodResultWrapper implements Result {
        private Result methodResult;
        private Handler handler;

        MethodResultWrapper(Result result) {
            methodResult = result;
            handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void success(final Object result) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    methodResult.success(result);
                }
            });
        }

        @Override
        public void error(final String errorCode, final String errorMessage, final Object errorDetails) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    methodResult.error(errorCode, errorMessage, errorDetails);
                }
            });
        }

        @Override
        public void notImplemented() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    methodResult.notImplemented();
                }
            });
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result rawResult) {
        Result result = new MethodResultWrapper(rawResult);

        if (call.method.equals("startTor")) {
            startTor(result);
        } else {
            result.notImplemented();
        }
    }

    private void startTor(final Result result) {
        if (tor == null) {
            tor = new AndroidOnionProxyManager(this.context, "tor_files");
        }

        new TorStarter(this.tor, result).execute();
    }

    private static class TorStarter extends AsyncTask<Void, Void, Void> {
        private OnionProxyManager tor;
        private Result result;

        TorStarter(OnionProxyManager tor, Result result) {
            this.tor = tor;
            this.result = result;
        }

        @Override
        protected Void doInBackground(Void... params) {
            int totalSecondsPerTorStartup = (int) TimeUnit.MINUTES.toSeconds(3);
            // количество попыток запуска
            int totalTriesPerTorStartup = 1;
            try {
                boolean ok = tor.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
                if (!ok) {
                    result.error("", "error message", "no details");
                    return null;
                }
                if (tor.isRunning()) {
                    result.success(String.valueOf(tor.getIPv4LocalHostSocksPort()));
                    return null;
                }
                result.error("", "error message", "no details");
            } catch (Exception e) {
                result.error("0", e.toString(), "tuk");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }
}
