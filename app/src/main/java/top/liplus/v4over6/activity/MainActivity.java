package top.liplus.v4over6.activity;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import top.liplus.v4over6.R;
import top.liplus.v4over6.adapter.ServerConfigAdapter;
import top.liplus.v4over6.vpn.Ipv4Config;
import top.liplus.v4over6.vpn.ServerConfig;
import top.liplus.v4over6.vpn.Statistics;
import top.liplus.v4over6.vpn.VpnService4Over6;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    //    @BindView(R.id.et_server_addr)
//    protected EditText etAddr;
//    @BindView(R.id.et_server_port)
//    protected EditText etPort;
    @BindView(R.id.fab_connect)
    protected FloatingActionButton fabConnect;
    @BindView(R.id.tv_download_bytes)
    protected TextView tvDownloadBytes;
    @BindView(R.id.tv_download_speed)
    protected TextView tvDownloadSpeed;
    @BindView(R.id.tv_upload_bytes)
    protected TextView tvUploadBytes;
    @BindView(R.id.tv_upload_speed)
    protected TextView tvUploadSpeed;
    @BindView(R.id.tv_connect_status)
    protected TextView tvConnectStatus;
    @BindView(R.id.tv_running_time)
    protected TextView tvRunningTime;
    @BindView(R.id.rv_server_config)
    protected RecyclerView rvServerConfig;
    @BindView(R.id.tv_ipv4)
    protected TextView tvIpv4;
    @BindView(R.id.tv_route)
    protected TextView tvRoute;
    @BindView(R.id.tv_dns)
    protected TextView tvDns;

    private enum ConnectionStatus {
        NO_CONNECTION, CONNECTING, CONNECTED
    }

    private ServerConfigAdapter adapter;

    private static VpnService4Over6 vpnService = new VpnService4Over6();

    private int prevDownloadBytes = 0;
    private int prevUploadBytes = 0;
    private Statistics stats = new Statistics();
    private Ipv4Config ipv4Config = new Ipv4Config();
    private boolean isConnected;
    private int socketFd = -1;
    private static Timer statsUpdater = new Timer("statsUpdater");
    private boolean enableStatsUpdater = false;
    private static long startTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        List<ServerConfig> data = new ArrayList<>();
        data.add(new ServerConfig("240e:360:6f0b:6a00:1e77:2bd4:d4f3:c6960000000000000000000000", 5551));
        data.add(new ServerConfig("240e:360:6f0b:6a00:1e77:2bd4:d4f3:c696", 5552));
        data.add(new ServerConfig("240e:360:6f0b:6a00:1e77:2bd4:d4f3:c696", 5553));
        data.add(new ServerConfig("240e:360:6f0b:6a00:1e77:2bd4:d4f3:c696", 5554));
        data.add(new ServerConfig("240e:360:6f0b:6a00:1e77:2bd4:d4f3:c695", 5555));
        data.add(new ServerConfig("240e:360:6f0b:6a00:1e77:2bd4:d4f3:c696", 5556));
        data.add(new ServerConfig("240e:360:6f0b:6a00:1e77:2bd4:d4f3:c697", 5557));
        data.add(new ServerConfig("240e:360:6f0b:6a00:1e77:2bd4:d4f3:c697", 5558));
        data.add(new ServerConfig("240e:360:6f0b:6a00:1e77:2bd4:d4f3:c697", 5559));
        data.add(new ServerConfig("240e:360:6f0b:6a00:1e77:2bd4:d4f3:c697", 5560));

        adapter = new ServerConfigAdapter(this, data);
        rvServerConfig.setAdapter(adapter);
        rvServerConfig.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        rvServerConfig.addItemDecoration(new DividerItemDecoration(rvServerConfig.getContext(), LinearLayoutManager.VERTICAL));
        rvServerConfig.setItemAnimator(new DefaultItemAnimator());

        if (isRunning()) {
            isConnected = true;
            getStatistics(stats);
            prevDownloadBytes = stats.downloadBytes;
            prevUploadBytes = stats.uploadBytes;
            getIpv4Config(ipv4Config);
            ServerConfig serverConfig = new ServerConfig();
            getServerConfig(serverConfig);
//            etAddr.setText(serverConfig.ipv6);
//            etPort.setText(String.valueOf(serverConfig.port));
            switchControls(ConnectionStatus.CONNECTED);
        } else {
            switchControls(ConnectionStatus.NO_CONNECTION);
        }

        statsUpdater.schedule(new TimerTask() {
            @Override
            public void run() {
                if (enableStatsUpdater) {
                    getStatistics(stats);
                    int uploadSpeed = stats.uploadBytes - prevUploadBytes;
                    int downloadSpeed = stats.downloadBytes - prevDownloadBytes;
                    prevUploadBytes = stats.uploadBytes;
                    prevDownloadBytes = stats.downloadBytes;

                    fabConnect.post(() -> {
                        tvDownloadBytes.setText(String.format(getString(R.string.pattern_bytes),
                                Formatter.formatFileSize(fabConnect.getContext(), stats.downloadBytes),
                                stats.downloadPackets));
                        tvUploadBytes.setText(String.format(getString(R.string.pattern_bytes),
                                Formatter.formatFileSize(fabConnect.getContext(), stats.uploadBytes),
                                stats.uploadPackets));
                        tvUploadSpeed.setText(String.format(getString(R.string.pattern_upload_speed),
                                Formatter.formatFileSize(fabConnect.getContext(), uploadSpeed)));
                        tvDownloadSpeed.setText(String.format(getString(R.string.pattern_download_speed),
                                Formatter.formatFileSize(fabConnect.getContext(), downloadSpeed)));

                        long runTimeMs = System.currentTimeMillis() - startTime;
                        long hours = TimeUnit.MILLISECONDS.toHours(runTimeMs);
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(runTimeMs) % 60;
                        long seconds = TimeUnit.MILLISECONDS.toSeconds(runTimeMs) % 60;
                        tvRunningTime.setText(String.format(getString(R.string.pattern_time),
                                hours, minutes, seconds));

                        tvIpv4.setText(String.format("IPv4: %s", ipv4Config.ipv4));
                        tvRoute.setText(String.format("Route: %s", ipv4Config.route));
                        tvDns.setText(String.format("DNS: %s %s %s", ipv4Config.dns1, ipv4Config.dns2, ipv4Config.dns3));
                    });
                }
            }
        }, 0, 1000);
    }

    @OnClick(R.id.fab_connect)
    void handleClickConnect(View view) {
        if (isConnected) {
            disconnectSocket();
            try {
                vpnService.stop();
                Log.i(TAG, "VPN stopped");
            } catch (IOException e) {
                Log.i(TAG, "Cannot stop VPN");
            }

            isConnected = false;
            prevDownloadBytes = 0;
            prevUploadBytes = 0;
            stats = new Statistics();
            ipv4Config = new Ipv4Config();
            socketFd = -1;

            switchControls(ConnectionStatus.NO_CONNECTION);
            return;
        }

        // validate user inputs
        if (adapter.selectedIndex < 0) {
            Toast.makeText(this, "Please select config", Toast.LENGTH_SHORT).show();
            return;
        }
        ServerConfig config = adapter.getData().get(adapter.selectedIndex);
        if (!validateIpv6Address(config.ipv6)) {
            view.post(() -> {
                Toast.makeText(this, "Invalid IPv6 address", Toast.LENGTH_SHORT).show();
                switchControls(ConnectionStatus.NO_CONNECTION);
            });
            return;
        }
        String addr = config.ipv6;
        int port = config.port;
//        if (!validatePort(etPort.getText().toString())) {
//            view.post(() -> {
//                Toast.makeText(this, "Invalid port", Toast.LENGTH_SHORT).show();
//                switchControls(false);
//            });
//            return;
//        }
//        int port = Integer.parseInt(etPort.getText().toString());

        // connecting
        switchControls(ConnectionStatus.CONNECTING);

        Log.i(TAG, "Connecting to [" + addr + "]:" + port);

        socketFd = connectSocket(addr, port);
        if (socketFd < 0) {
            view.post(() -> {
                Toast.makeText(this, "Cannot connect to server", Toast.LENGTH_SHORT).show();
                switchControls(ConnectionStatus.NO_CONNECTION);
            });
            return;
        }

        int ret = requestIpv4Config();
        if (ret < 0) {
            disconnectSocket();
            view.post(() -> {
                Toast.makeText(this, "Cannot get ipv4 config", Toast.LENGTH_SHORT).show();
                switchControls(ConnectionStatus.NO_CONNECTION);
            });
            return;
        }
        getIpv4Config(ipv4Config);

        Intent vpnIndent = VpnService.prepare(this);
        if (vpnIndent != null) {
            startActivityForResult(vpnIndent, 0);
        } else {
            startVpn();
        }
    }

    void resetConnectionInfo() {
        tvDownloadBytes.setText(String.format(getString(R.string.pattern_bytes), "0 B", 0));
        tvUploadBytes.setText(String.format(getString(R.string.pattern_bytes), "0 B", 0));
        tvUploadSpeed.setText(String.format(getString(R.string.pattern_upload_speed), "0 B"));
        tvDownloadSpeed.setText(String.format(getString(R.string.pattern_download_speed), "0 B"));
        tvRunningTime.setText(String.format(getString(R.string.pattern_time), 0, 0, 0));

        tvIpv4.setText(String.format("IPv4: %s", "-"));
        tvRoute.setText(String.format("Route: %s", "-"));
        tvDns.setText(String.format("DNS: %s %s %s", "-", "-", "-"));
    }

    private static boolean validateIpv6Address(String addr) {
//        try {
//            Inet6Address.getByName(addr);
//            return true;
//        } catch (UnknownHostException e) {
//            return false;
//        }
        return !addr.isEmpty() && addr.length() < 40;
    }

    private static boolean validatePort(String port) {
        try {
            int portInt = Integer.parseInt(port);
            return 0 <= portInt && portInt < 65536;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void startVpn() {
        Log.d(TAG, "Starting VPN service");
        vpnService.protect(socketFd);
        int tunnelFd = vpnService.start(ipv4Config);
        if (tunnelFd < 0) {
            disconnectSocket();
            return;
        }
        setupTunnel(tunnelFd);
        isConnected = true;
        startTime = System.currentTimeMillis();
//        fabConnect.post(() -> {
//            Toast.makeText(this, "Successfully connected", Toast.LENGTH_SHORT).show();
//        });
//        Set<String> a = new HashSet<>(1);
//
//        SharedPreferences.Editor sp =getSharedPreferences(Defs.SP_GLOBAL,  MODE_PRIVATE).edit();
//        sp.putStringSet(Defs.SP_SERVER_CONFIG, );
//        sp.apply();
        switchControls(ConnectionStatus.CONNECTED);
    }

    void switchControls(ConnectionStatus status) {
        if (status == ConnectionStatus.NO_CONNECTION) {
            tvConnectStatus.setText(R.string.no_connection);
            enableStatsUpdater = false;
            resetConnectionInfo();
        } else if (status == ConnectionStatus.CONNECTING) {
            tvConnectStatus.setText(R.string.connecting);
            enableStatsUpdater = false;
            resetConnectionInfo();
        } else {
            tvConnectStatus.setText(R.string.connected);
            enableStatsUpdater = true;
        }
//        btnConnect.setText(isConnected ? R.string.disconnect : R.string.connect);
//        etAddr.setEnabled(!isConnected);
//        etPort.setEnabled(!isConnected);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                startVpn();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                switchControls(ConnectionStatus.NO_CONNECTION);
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

    private native void getServerConfig(ServerConfig config);

    private native boolean isRunning();
}
