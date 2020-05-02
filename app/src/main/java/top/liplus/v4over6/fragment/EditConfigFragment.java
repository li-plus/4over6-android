package top.liplus.v4over6.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import top.liplus.v4over6.R;
import top.liplus.v4over6.common.GlobalConfig;
import top.liplus.v4over6.vpn.ServerConfig;

public class EditConfigFragment extends BaseFragment implements OnShowToastListener {
    public static final String TAG = EditConfigFragment.class.getSimpleName();

    @BindView(R.id.et_server_name)
    protected EditText etServerName;
    @BindView(R.id.et_server_addr)
    protected EditText etServerAddr;
    @BindView(R.id.et_server_port)
    protected EditText etServerPort;
    @BindView(R.id.mt_top_bar)
    protected MaterialToolbar mtTopBar;

    private int configIndex;

    public EditConfigFragment(int configIndex) {
        this.configIndex = configIndex;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_edit_config, container, false);
        ButterKnife.bind(this, root);
        mtTopBar.setOnMenuItemClickListener((MenuItem item) -> {
            if (item.getItemId() == R.id.menu_done) {
                // check inputs
                if (!validateServerName(etServerName.getText().toString())) {
                    showToast("Invalid Server Name");
                    return false;
                }
                if (!validateIpv6Address(etServerAddr.getText().toString())) {
                    showToast("Invalid IPv6 Address");
                    return false;
                }
                if (!validatePort(etServerPort.getText().toString())) {
                    showToast("Invalid Port");
                    return false;
                }
                String name = etServerName.getText().toString();
                String addr = etServerAddr.getText().toString();
                int port = Integer.parseInt(etServerPort.getText().toString());
                ServerConfig config = new ServerConfig(name, addr, port);

                // submit change
                List<ServerConfig> serverConfigs = GlobalConfig.getServerConfigs(getContext());
                if (configIndex < 0) {
                    // create new config
                    serverConfigs.add(config);
                    showToast("New config created");
                } else if (configIndex < serverConfigs.size()) {
                    // update config
                    serverConfigs.set(configIndex, config);
                    showToast("Config updated");
                } else {
                    showToast("Internal error");
                }
                GlobalConfig.setServerConfigs(getContext(), serverConfigs);
                getBaseFragmentActivity().stopFragment();
                return true;
            } else {
                return false;
            }
        });
        List<ServerConfig> serverConfigs = GlobalConfig.getServerConfigs(getContext());
        mtTopBar.setTitle(configIndex < 0 ? R.string.new_config : R.string.edit_config);
        mtTopBar.setNavigationOnClickListener((View view) -> {
            getBaseFragmentActivity().stopFragment();
        });
        if (0 <= configIndex && configIndex < serverConfigs.size()) {
            ServerConfig config = serverConfigs.get(configIndex);
            etServerName.setText(config.name);
            etServerAddr.setText(config.ipv6);
            etServerPort.setText(String.valueOf(config.port));
        }
        Log.i(TAG, "Editing config " + configIndex);
        return root;
    }

    @Override
    public void showToast(String text) {
        Snackbar.make(getView(), text, BaseTransientBottomBar.LENGTH_SHORT).show();
    }

    private static boolean validateIpv6Address(String addr) {
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

    private static boolean validateServerName(String name) {
        return !name.isEmpty();
    }
}
