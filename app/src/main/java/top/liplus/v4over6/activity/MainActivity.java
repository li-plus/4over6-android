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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import top.liplus.v4over6.R;
import top.liplus.v4over6.vpn.Ipv4Config;
import top.liplus.v4over6.vpn.Statistics;
import top.liplus.v4over6.vpn.VpnService4Over6;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.btn_connect)
    protected Button btnConnect;
    @BindView(R.id.et_server_addr)
    protected EditText etAddr;
    @BindView(R.id.et_server_port)
    protected EditText etPort;
//    private EditText etInfo;

    private Statistics statistics = new Statistics();
    private Ipv4Config ipv4Config = new Ipv4Config();
    private boolean isConnected;

    private VpnService4Over6 vpnService = new VpnService4Over6();
    private String addr = "";
    private String port = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

//        Statistics statistics = getStatistics(new Statistics());
//        if (statistics.state) {
//            Log.i(TAG, "VPN still connected");
////            this.statistics = statistics;
////            ipv4Config = requestIpv4Config(ipv4Config);
//            // TODO check success
//        }
    }

    @OnClick(R.id.btn_connect)
    void handleClickConnect(View view) {
        if (isConnected) {
            disconnect();
            try {
                vpnService.stop();
                Log.i(TAG, "VPN stopped");
            } catch (IOException e) {
                Log.i(TAG, "Cannot stop VPN");
            }
//            statisticsUpdater.dispose();
            isConnected = false;
            switchControls(false);
            return;
        }

        addr = etAddr.getText().toString();
        port = etPort.getText().toString();

//        InetAddressVali
//        if (!isValidIpv6Address(addr)) {
//
//        }
//        if (!isValidPort(port)) {
//
//        }

        switchControls(true);

        Log.i(TAG, "Connecting to [" + addr + "]:" + port);
//        etInfo.setText("Connecting to " + addr + ":" + port);

        boolean isSuccess = connect(addr, port);
        if (isSuccess) {
            Toast.makeText(this, "Successfully connected", Toast.LENGTH_SHORT).show();
            ipv4Config = requestIpv4Config(ipv4Config);
            // TODO check success
            Intent vpnIndent = VpnService.prepare(this);
            if (vpnIndent != null) {
                startActivityForResult(vpnIndent, 0);
            } else {
                startVpn();
            }
        } else {
            Toast.makeText(this, "Cannot connect to server", Toast.LENGTH_SHORT).show();
            switchControls(false);
        }
    }

    void startVpn() {
        Log.d(TAG, "Starting VPN service");
        vpnService.protect(ipv4Config.socketFd);
        int tunnelFd = vpnService.start(ipv4Config);
        initTunnel(tunnelFd);
        isConnected = true;
        // TODO setup statistics
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
        System.loadLibrary("lib4over6");
    }

    private native boolean connect(String addr, String port);

    private native void disconnect();

    private native Ipv4Config requestIpv4Config(Ipv4Config config);

    private native void initTunnel(int tunnel_fd);

    private native Statistics getStatistics(Statistics data);
}
