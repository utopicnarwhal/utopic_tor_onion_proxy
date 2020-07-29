package ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.android;

import android.content.Context;
import ru.utopicnarwhal.utopic_tor_onion_proxy.thali_sources.DefaultSettings;

public class AndroidDefaultTorSettings extends DefaultSettings {

    private final Context context;

    public AndroidDefaultTorSettings(Context context) {
        this.context = context;
    }

    @Override
    public String getSocksPort() {
        return "auto";
    }

    @Override
    public boolean runAsDaemon() {
        return true;
    }

    @Override
    public String transPort() {
        return "auto";
    }

    @Override
    public boolean hasCookieAuthentication() {
        return true;
    }
}
