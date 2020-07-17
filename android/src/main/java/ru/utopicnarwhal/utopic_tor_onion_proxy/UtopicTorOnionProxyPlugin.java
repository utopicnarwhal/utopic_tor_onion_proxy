package ru.utopicnarwhal.utopic_tor_onion_proxy;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;

import java.io.IOException;
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
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "utopic_tor_onion_proxy");
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
        Result methodResult = new MethodResultWrapper(rawResult);

        switch (call.method) {
            case "startTor":
                startTor(methodResult);
                break;
            case "stopTor":
                stopTor(methodResult);
                break;
            case "isTorRunning":
                isTorRunning(methodResult);
                break;
            default:
                methodResult.notImplemented();
                break;
        }
    }

    private void startTor(final Result result) {
        if (tor == null) {
            tor = new AndroidOnionProxyManager(this.context, "tor_files");
        }

        new TorStarter(this.tor, result).execute();
    }

    private void stopTor(final Result result) {
        if (tor == null) {
            result.success(true);
            return;
        }

        try {
            tor.stop();
            result.success(true);
        } catch (IOException e) {
            result.error("0", e.toString(), e.getStackTrace());
        }
    }

    private void isTorRunning(final Result result) {
        if (tor == null) {
            result.success(false);
        }

        try {
            result.success(tor.isRunning());
        } catch (IOException e) {
            result.error("0", e.toString(), e.getStackTrace());
        }
    }

    private static class TorStarter extends AsyncTask<Void, Void, AsyncTaskResult<Integer>> {
        private OnionProxyManager tor;
        private Result methodResult;

        TorStarter(OnionProxyManager tor, Result result) {
            this.tor = tor;
            this.methodResult = result;
        }

        @Override
        protected AsyncTaskResult<Integer> doInBackground(Void... params) {
            int totalSecondsPerTorStartup = (int) TimeUnit.MINUTES.toSeconds(1);
            int totalTriesPerTorStartup = 1;
            try {
                boolean ok = tor.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
                if (!ok) {
                    return new AsyncTaskResult<>(new Exception("Can't start Tor onion proxy. Try again."));
                }
                int awaitCounter = 0;
                while (!tor.isRunning() || awaitCounter != 30) {
                    Thread.sleep(100);
                    awaitCounter++;
                }
                if (tor.isRunning()) {
                    return new AsyncTaskResult<>(tor.getIPv4LocalHostSocksPort());
                }
                return new AsyncTaskResult<>(new Exception("Tor is not running after start"));
            } catch (Exception e) {
                return new AsyncTaskResult<>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<Integer> asyncTaskResult) {
            super.onPostExecute(asyncTaskResult);
            Integer port = asyncTaskResult.getResult();
            if (port != null) {
                methodResult.success(asyncTaskResult.getResult());
                return;
            }
            Exception error = asyncTaskResult.getError();
            if (error != null) {
                methodResult.error("1", error.getMessage(), error.getStackTrace());
                return;
            }
            methodResult.error("2", "Something strange", "");
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }
}

class AsyncTaskResult<T> {
    private T result;
    private Exception error;

    public T getResult() {
        return result;
    }

    public Exception getError() {
        return error;
    }

    public AsyncTaskResult(T result) {
        super();
        this.result = result;
    }

    public AsyncTaskResult(Exception error) {
        super();
        this.error = error;
    }
}