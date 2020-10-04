package ru.utopicnarwhal.utopic_tor_onion_proxy.tor_android_binaries;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static ru.utopicnarwhal.utopic_tor_onion_proxy.tor_android_binaries.TorServiceConstants.TOR_ASSET_KEY;

public class NativeLoader {

    private final static String LIB_SO_NAME = TOR_ASSET_KEY + ".so";

    private final static String TAG = "TorNativeLoader";

    private static boolean loadFromZip(Context context, File destLocalFile, String arch) {
        ZipFile zipFile = null;
        ZipFile splitApkZipFile = null;
        InputStream stream = null;
        Boolean result = false;

        try {
            zipFile = new ZipFile(context.getApplicationInfo().sourceDir);

            ZipEntry entry = zipFile.getEntry("lib/" + arch + "/" + LIB_SO_NAME);

            if (entry == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    for (String splitSourcePath: context.getApplicationInfo().splitSourceDirs) {
                        String[] splitBySlash = splitSourcePath.split("/");
                        String sourceFileName = splitBySlash[splitBySlash.length - 1];
                        String[] stringSplit = sourceFileName.split("\\.");

                        if (stringSplit.length > 2 && arch.replace('-', '_').equalsIgnoreCase(stringSplit[stringSplit.length - 2])) {
                            splitApkZipFile = new ZipFile(splitSourcePath);
                            break;
                        }
                    }
                }
                if (splitApkZipFile == null)
                    throw new Exception("Unable to find splitApkZipFile:" + "lib/" + arch + "/" + LIB_SO_NAME);

                entry = splitApkZipFile.getEntry("lib/" + arch + "/" + LIB_SO_NAME);

                if (entry == null)
                    throw new Exception("Unable to find file in splitApkZipFile:" + "lib/" + arch + "/" + LIB_SO_NAME);

                stream = splitApkZipFile.getInputStream(entry);
            } else {
                //how we wrap this in another stream because the native .so is zipped itself
                stream = zipFile.getInputStream(entry);
            }

            destLocalFile.setReadable(true, false);
            destLocalFile.setExecutable(true, false);
            destLocalFile.setWritable(true);

            OutputStream out = new FileOutputStream(destLocalFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = stream.read(buf)) > 0) {
                Thread.yield();
                out.write(buf, 0, len);
            }
            out.close();

            result = true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            if (splitApkZipFile != null) {
                try {
                    splitApkZipFile.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return result;
    }

    public static synchronized File initNativeLibs(Context context, File destLocalFile) {
        try {
            String arch = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                arch = Build.CPU_ABI;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                arch = Build.SUPPORTED_ABIS[0];
            }

            String javaArch = System.getProperty("os.arch");
            if (javaArch != null && javaArch.contains("686")) {
                arch = "x86";
            }

            if (loadFromZip(context, destLocalFile, arch)) {
                return destLocalFile;
            }

        } catch (Throwable e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }
}