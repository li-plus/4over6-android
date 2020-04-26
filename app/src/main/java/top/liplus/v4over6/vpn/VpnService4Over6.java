package top.liplus.v4over6.vpn;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;

public class VpnService4Over6 extends VpnService {
    private static final String TAG = VpnService4Over6.class.getSimpleName();

    private ParcelFileDescriptor parcelFileDescriptor;

    public ParcelFileDescriptor start(InetAddress ipv4, InetAddress route, InetAddress dnsServer) {
        parcelFileDescriptor = new Builder()
                .setMtu(1400)
                .addAddress(ipv4, 32)
                .addRoute(route, 0)
                .addDnsServer(dnsServer)
                .setSession("4over6 to ${ipv6}")
                .establish();
        Log.i(TAG, "VPN started");
        return parcelFileDescriptor;
    }

    public void stop() throws IOException {
        parcelFileDescriptor.close();
        Log.i(TAG, "VPN stopped");
    }
}
