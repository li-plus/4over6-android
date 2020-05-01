package top.liplus.v4over6.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.VpnService;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import top.liplus.v4over6.R;
import top.liplus.v4over6.adapter.ServerConfigAdapter;
import top.liplus.v4over6.common.GlobalConfig;
import top.liplus.v4over6.vpn.Ipv4Config;
import top.liplus.v4over6.vpn.ServerConfig;
import top.liplus.v4over6.vpn.Statistics;
import top.liplus.v4over6.vpn.V4over6;
import top.liplus.v4over6.vpn.VpnService4Over6;

import static android.app.Activity.RESULT_OK;

public class ConfigListFragment extends BaseFragment {
    private static final String TAG = ConfigListFragment.class.getSimpleName();

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
    @BindView(R.id.mt_top_bar)
    protected MaterialToolbar topAppBar;

    private enum ConnectionStatus {
        NO_CONNECTION, CONNECTING, CONNECTED
    }

    private ServerConfigAdapter adapter;

    private static VpnService4Over6 vpnService = new VpnService4Over6();

    private int prevDownloadBytes = 0;
    private int prevUploadBytes = 0;
    private Statistics stats = new Statistics();
    private Ipv4Config ipv4Config = new Ipv4Config();
    private ConfigListFragment.ConnectionStatus status = ConfigListFragment.ConnectionStatus.NO_CONNECTION;
    private int socketFd = -1;
    private Timer statsUpdater = null;
    private boolean enableStatsUpdater = false;
    private static long startTime = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_config_list, container, false);
        ButterKnife.bind(this, root);

        List<ServerConfig> data = GlobalConfig.getServerConfigs(getContext());
        int configIndex = GlobalConfig.getConfigIndex(getContext());

        adapter = new ServerConfigAdapter(getBaseFragmentActivity(), data, configIndex);
        rvServerConfig.setAdapter(adapter);
        rvServerConfig.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        rvServerConfig.addItemDecoration(new DividerItemDecoration(rvServerConfig.getContext(), LinearLayoutManager.VERTICAL));
        rvServerConfig.setItemAnimator(new DefaultItemAnimator());

        topAppBar.setOnMenuItemClickListener((MenuItem item) -> {
            if (item.getItemId() == R.id.menu_new) {
                getBaseFragmentActivity().startFragment(new NewConfigFragment());
                return true;
            } else {
                return false;
            }
        });

        if (V4over6.isRunning()) {
            V4over6.getStatistics(stats);
            prevDownloadBytes = stats.downloadBytes;
            prevUploadBytes = stats.uploadBytes;
            V4over6.getIpv4Config(ipv4Config);
            ServerConfig serverConfig = new ServerConfig();
            V4over6.getServerConfig(serverConfig);
            switchStatus(ConfigListFragment.ConnectionStatus.CONNECTED);
        } else {
            switchStatus(ConfigListFragment.ConnectionStatus.NO_CONNECTION);
        }

        statsUpdater = new Timer("statsUpdater");
        statsUpdater.schedule(new TimerTask() {
            @Override
            public void run() {
                if (enableStatsUpdater) {
                    V4over6.getStatistics(stats);
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

        return root;
    }

    @Override
    public void onDestroyView() {
        statsUpdater.cancel();
        super.onDestroyView();
    }

    @Override
    public TransitionConfig getTransitionConfig() {
        return FADE_TRANSITION_CONFIG;
    }

    @OnClick(R.id.fab_connect)
    void handleClickConnect(View view) {
        if (status == ConfigListFragment.ConnectionStatus.CONNECTED) {
            V4over6.disconnectSocket();
            try {
                vpnService.stop();
                Log.i(TAG, "VPN stopped");
            } catch (IOException e) {
                Log.i(TAG, "Cannot stop VPN");
            }

            prevDownloadBytes = 0;
            prevUploadBytes = 0;
            stats = new Statistics();
            ipv4Config = new Ipv4Config();
            socketFd = -1;

            switchStatus(ConfigListFragment.ConnectionStatus.NO_CONNECTION);
            return;
        }

        // validate user inputs
        if (adapter.selectedIndex < 0) {
            makeToast("Please select a config");
            return;
        }
        ServerConfig config = adapter.getData().get(adapter.selectedIndex);

        // connecting
        switchStatus(ConfigListFragment.ConnectionStatus.CONNECTING);

        Log.i(TAG, "Connecting to [" + config.ipv6 + "]:" + config.port);

        socketFd = V4over6.connectSocket(config.ipv6, config.port);
        if (socketFd < 0) {
            view.post(() -> {
                makeToast("Cannot connect to server");
                switchStatus(ConfigListFragment.ConnectionStatus.NO_CONNECTION);
            });
            return;
        }

        int ret = V4over6.requestIpv4Config();
        if (ret < 0) {
            V4over6.disconnectSocket();
            view.post(() -> {
                makeToast("Cannot get ipv4 config");
                switchStatus(ConfigListFragment.ConnectionStatus.NO_CONNECTION);
            });
            return;
        }
        V4over6.getIpv4Config(ipv4Config);

        Intent vpnIndent = VpnService.prepare(getContext());
        if (vpnIndent != null) {
            startActivityForResult(vpnIndent, 0);
        } else {
            startVpn();
        }
    }

    private void makeToast(String text) {
        Snackbar.make(fabConnect, text, BaseTransientBottomBar.LENGTH_SHORT)
                .setAnchorView(fabConnect)
                .show();
    }

    private void resetConnectionInfo() {
        tvDownloadBytes.setText(String.format(getString(R.string.pattern_bytes), "0 B", 0));
        tvUploadBytes.setText(String.format(getString(R.string.pattern_bytes), "0 B", 0));
        tvUploadSpeed.setText(String.format(getString(R.string.pattern_upload_speed), "0 B"));
        tvDownloadSpeed.setText(String.format(getString(R.string.pattern_download_speed), "0 B"));
        tvRunningTime.setText(String.format(getString(R.string.pattern_time), 0, 0, 0));

        tvIpv4.setText(String.format("IPv4: %s", "-"));
        tvRoute.setText(String.format("Route: %s", "-"));
        tvDns.setText(String.format("DNS: %s %s %s", "-", "-", "-"));
    }

    private void startVpn() {
        Log.d(TAG, "Starting VPN service");
        vpnService.protect(socketFd);
        int tunnelFd = vpnService.start(ipv4Config);
        if (tunnelFd < 0) {
            V4over6.disconnectSocket();
            return;
        }
        V4over6.setupTunnel(tunnelFd);
        startTime = System.currentTimeMillis();
        makeToast("Successfully connected");
        switchStatus(ConfigListFragment.ConnectionStatus.CONNECTED);
    }

    void switchStatus(ConfigListFragment.ConnectionStatus status) {
        this.status = status;
        if (status == ConfigListFragment.ConnectionStatus.NO_CONNECTION) {
            tvConnectStatus.setText(R.string.no_connection);
            tvConnectStatus.setTextColor(getContext().getColor(R.color.red_9));
            enableStatsUpdater = false;
            fabConnect.setEnabled(true);
            fabConnect.setBackgroundTintList(ColorStateList.valueOf(getContext().getColor(R.color.gray_b)));
            resetConnectionInfo();
        } else if (status == ConfigListFragment.ConnectionStatus.CONNECTING) {
            tvConnectStatus.setText(R.string.connecting);
            tvConnectStatus.setTextColor(getContext().getColor(R.color.yellow_9));
            enableStatsUpdater = false;
            fabConnect.setEnabled(false);
            fabConnect.setBackgroundTintList(ColorStateList.valueOf(getContext().getColor(R.color.connected_green)));
            resetConnectionInfo();
        } else {
            tvConnectStatus.setText(R.string.connected);
            tvConnectStatus.setTextColor(getContext().getColor(R.color.green_9));
            enableStatsUpdater = true;
            fabConnect.setEnabled(true);
            fabConnect.setBackgroundTintList(ColorStateList.valueOf(getContext().getColor(R.color.connected_green)));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                startVpn();
            } else {
                makeToast("Permission denied");
                switchStatus(ConfigListFragment.ConnectionStatus.NO_CONNECTION);
            }
        }
    }
}
