package ru.utopicnarwhal.utopic_tor_onion_proxy;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import net.freehaven.tor.control.EventHandler;

import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.DefaultEventBroadcaster;
import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.EventBroadcaster;
import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.TorConfig;
import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.TorConfigBuilder;
import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.TorSettings;
import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.android.AndroidDefaultTorSettings;
import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.android.AndroidOnionProxyManager;
import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.OnionProxyManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.android.AndroidOnionProxyManagerEventHandler;
import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.android.AndroidTorInstaller;

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
            String fileStorageLocation = "torfiles";

            TorConfig torConfig = TorConfig.createDefault(new File(this.context.getCacheDir(), fileStorageLocation));
            try {
                torConfig.resolveTorrcFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            AndroidTorInstaller torInstaller = new AndroidTorInstaller(this.context, torConfig);
            TorSettings torSettings = new AndroidDefaultTorSettings(this.context);
            EventBroadcaster eventBroadcaster = new DefaultEventBroadcaster();
            EventHandler eventHandler = new AndroidOnionProxyManagerEventHandler();

            tor = new AndroidOnionProxyManager(this.context, torConfig, torInstaller, torSettings, eventBroadcaster, eventHandler);

            try {
                tor.setup();
                final TorConfigBuilder builder = tor.getContext().newConfigBuilder().updateTorConfig();
                tor.getContext().getInstaller().updateTorConfigCustom(builder.asString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        new TorStarter(this.tor, result).execute();
    }

    private void stopTor(final Result result) {
        if (tor == null) {
            result.success(true);
            return;
        }

        new TorStopper(this.tor, result).execute();
    }

    private void isTorRunning(final Result result) {
        if (tor == null) {
            result.success(false);
        }

        new TorRunningChecker(this.tor, result).execute();
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
                boolean ok = tor.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup, false);
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
                methodResult.success(port);
                return;
            }

            Exception error = asyncTaskResult.getError();
            if (error != null) {
                methodResult.error("1", error.getMessage(), "");
                return;
            }
            methodResult.error("2", "Something strange", "");
        }
    }

    private static class TorStopper extends AsyncTask<Void, Void, AsyncTaskResult<Boolean>> {
        private OnionProxyManager tor;
        private Result methodResult;

        TorStopper(OnionProxyManager tor, Result result) {
            this.tor = tor;
            this.methodResult = result;
        }

        @Override
        protected AsyncTaskResult<Boolean> doInBackground(Void... params) {
            try {
                tor.stop();
                return new AsyncTaskResult<>(true);
            } catch (Exception e) {
                return new AsyncTaskResult<>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<Boolean> asyncTaskResult) {
            super.onPostExecute(asyncTaskResult);
            Boolean result = asyncTaskResult.getResult();

            if (result != null) {
                methodResult.success(result);
                return;
            }

            Exception error = asyncTaskResult.getError();
            if (error != null) {
                methodResult.error("1", error.getMessage(), "");
                return;
            }
            methodResult.error("2", "Something strange", "");
        }
    }

    private static class TorRunningChecker extends AsyncTask<Void, Void, AsyncTaskResult<Boolean>> {
        private OnionProxyManager tor;
        private Result methodResult;

        TorRunningChecker(OnionProxyManager tor, Result result) {
            this.tor = tor;
            this.methodResult = result;
        }

        @Override
        protected AsyncTaskResult<Boolean> doInBackground(Void... params) {
            try {
                return new AsyncTaskResult<>(tor.isRunning());
            } catch (Exception e) {
                return new AsyncTaskResult<>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<Boolean> asyncTaskResult) {
            super.onPostExecute(asyncTaskResult);
            Boolean result = asyncTaskResult.getResult();

            if (result != null) {
                methodResult.success(result);
                return;
            }

            Exception error = asyncTaskResult.getError();
            if (error != null) {
                methodResult.error("1", error.getMessage(), "");
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