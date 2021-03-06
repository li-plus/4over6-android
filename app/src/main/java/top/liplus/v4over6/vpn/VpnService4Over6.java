package top.liplus.v4over6.vpn;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.util.Log;

import java.io.IOException;

public class VpnService4Over6 extends VpnService {
    private static final String TAG = VpnService4Over6.class.getSimpleName();

    private ParcelFileDescriptor parcelFileDescriptor;

    public int start(Ipv4Config config) {
        Builder builder = new Builder()
                .setMtu(1400)
                .allowFamily(OsConstants.AF_INET)
                .addAddress(config.ipv4, 32)
                .addRoute(config.route, 0)
                .addDnsServer(config.dns1)
                .setSession("Session " + config.ipv4);
        if (!config.dns2.equals("0.0.0.0")) builder.addDnsServer(config.dns2);
        if (!config.dns3.equals("0.0.0.0")) builder.addDnsServer(config.dns3);
        parcelFileDescriptor = builder.establish();
        if (parcelFileDescriptor == null) {
            return -1;
        } else {
            return parcelFileDescriptor.getFd();
        }
    }

    public void stop() throws IOException {
        parcelFileDescriptor.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "VpnService destroyed");
        V4over6.disconnectSocket();
    }
}
