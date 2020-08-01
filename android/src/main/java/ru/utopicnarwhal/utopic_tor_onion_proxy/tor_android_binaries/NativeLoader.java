package ru.utopicnarwhal.utopic_tor_onion_proxy.tor_android_binaries;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class NativeLoader {

    private final static String LIB_SO_NAME = "tor.so";

    private final static String TAG = "TorNativeLoader";

    private static boolean loadFromZip(Context context, File destLocalFile, String arch) {
        ZipFile zipFile = null;
        ZipFile splitApkZipFile = null;
        InputStream stream = null;

        try {
            zipFile = new ZipFile(context.getApplicationInfo().sourceDir);

            ZipEntry entry = zipFile.getEntry("lib/" + arch + "/" + LIB_SO_NAME);
            if (entry == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    for (String splitSourcePath: context.getApplicationInfo().splitSourceDirs) {
                        String[] splittedBySlash = splitSourcePath.split("/");
                        String sourceFileName = splittedBySlash[splittedBySlash.length - 1];
                        String[] stringSplitted = sourceFileName.split("\\.");

                        if (stringSplitted.length > 2 && arch.replace('-', '_').equalsIgnoreCase(stringSplitted[stringSplitted.length - 2])) {
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

            OutputStream out = new FileOutputStream(destLocalFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = stream.read(buf)) > 0) {
                Thread.yield();
                out.write(buf, 0, len);
            }
            out.close();

            destLocalFile.setReadable(true, false);
            destLocalFile.setExecutable(true, false);
            destLocalFile.setWritable(true);

            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
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
        return false;
    }

    public static synchronized File initNativeLibs(Context context, File destLocalFile) {
        try {
            String folder = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                folder = Build.CPU_ABI;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                folder = Build.SUPPORTED_ABIS[0];
            }

            String javaArch = System.getProperty("os.arch");
            if (javaArch != null && javaArch.contains("686")) {
                folder = "x86";
            }

            if (loadFromZip(context, destLocalFile, folder)) {
                return destLocalFile;
            }

        } catch (Throwable e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }
}