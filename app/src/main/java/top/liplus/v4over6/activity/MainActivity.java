package top.liplus.v4over6.activity;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import top.liplus.v4over6.R;
import top.liplus.v4over6.vpn.Ipv4Config;
import top.liplus.v4over6.vpn.Statistics;
import top.liplus.v4over6.vpn.VpnService4Over6;
//import com.google.common.net.InetAddresses;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.btn_connect)
    protected Button btnConnect;
    @BindView(R.id.et_server_addr)
    protected EditText etAddr;
    @BindView(R.id.et_server_port)
    protected EditText etPort;
    @BindView(R.id.et_info)
    protected EditText etInfo;

    private Statistics statistics = new Statistics();
    private Ipv4Config ipv4Config = new Ipv4Config();
    private boolean isConnected;

    private VpnService4Over6 vpnService = new VpnService4Over6();
    private String addr = "";
    private int port = -1;

    private Timer statUpdater = new Timer("statUpdater");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.btn_connect)
    void handleClickConnect(View view) {
        if (isConnected) {
            disconnectSocket();
            try {
                vpnService.stop();
                Log.i(TAG, "VPN stopped");
            } catch (IOException e) {
                Log.i(TAG, "Cannot stop VPN");
            }
            statUpdater.purge();
            isConnected = false;
            switchControls(false);
            return;
        }

        // validate user inputs
        if (!validateIpv6Address(etAddr.getText().toString())) {
            view.post(() -> {
                Toast.makeText(this, "Invalid IPv6 address", Toast.LENGTH_SHORT).show();
                switchControls(false);
            });
            return;
        }
        addr = etAddr.getText().toString();

        if (!validatePort(etPort.getText().toString())) {
            view.post(() -> {
                Toast.makeText(this, "Invalid port", Toast.LENGTH_SHORT).show();
                switchControls(false);
            });
            return;
        }
        port = Integer.parseInt(etPort.getText().toString());

        // assume connection success
        switchControls(true);

        Log.i(TAG, "Connecting to [" + addr + "]:" + port);

//        etInfo.setText("Connecting to " + addr + ":" + port);
        int ret = connectSocket(addr, port);
        if (ret < 0) {
            view.post(() -> {
                Toast.makeText(this, "Cannot connect to server", Toast.LENGTH_SHORT).show();
                switchControls(false);
            });
            return;
        }

        ret = requestIpv4Config();
        if (ret < 0) {
            view.post(() -> {
                Toast.makeText(this, "Cannot get ipv4 config", Toast.LENGTH_SHORT).show();
                switchControls(false);
            });
            return;
        }
        getIpv4Config(ipv4Config);

        view.post(() -> {
            Toast.makeText(this, "Successfully connected", Toast.LENGTH_SHORT).show();
        });

        Intent vpnIndent = VpnService.prepare(this);
        if (vpnIndent != null) {
            startActivityForResult(vpnIndent, 0);
        } else {
            startVpn();
        }
    }

    private static boolean validateIpv6Address(String addr) {
//        try {
//            Inet6Address.getByName(addr);
//            return true;
//        } catch (UnknownHostException e) {
//            return false;
//        }
        return true;
    }

    private static boolean validatePort(String port) {
        try {
            int port_int = Integer.parseInt(port);
            return 0 <= port_int && port_int < 65536;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void startVpn() {
        Log.d(TAG, "Starting VPN service");
        vpnService.protect(ipv4Config.socketFd);
        int tunnelFd = vpnService.start(ipv4Config);
        if (tunnelFd < 0) {
            disconnectSocket();
            return;
        }
        setupTunnel(tunnelFd);
        isConnected = true;
        // TODO setup statistics

        statUpdater.schedule(new TimerTask() {
            @Override
            public void run() {
                getStatistics(statistics);
                etInfo.post(() -> etInfo.setText(String.format("Download bytes: %d ptk: %d\nUpload bytes: %d ptk: %d",
                        statistics.downloadBytes, statistics.downloadPackets, statistics.uploadBytes, statistics.uploadPackets))
                );
            }
        }, 0, 1000);
    }

    void switchControls(boolean isConnected) {
        btnConnect.setText(isConnected ? R.string.disconnect : R.string.connect);
        etAddr.setEnabled(!isConnected);
        etPort.setEnabled(!isConnected);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                startVpn();
            } else {
                switchControls(false);
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    static {
        System.loadLibrary("v4over6");
    }

    private native int connectSocket(String addr, int port);

    private native void disconnectSocket();

    private native int requestIpv4Config();

    private native void setupTunnel(int tunnel_fd);

    private native void getStatistics(Statistics stats);

    private native void getIpv4Config(Ipv4Config config);
}
