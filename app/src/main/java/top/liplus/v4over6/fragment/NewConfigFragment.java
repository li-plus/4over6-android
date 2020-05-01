package top.liplus.v4over6.fragment;

import android.os.Bundle;
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

public class NewConfigFragment extends BaseFragment {
    @BindView(R.id.et_server_name)
    protected EditText etServerName;
    @BindView(R.id.et_server_addr)
    protected EditText etServerAddr;
    @BindView(R.id.et_server_port)
    protected EditText etServerPort;
    @BindView(R.id.mt_top_bar)
    protected MaterialToolbar mtTopBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_new_config, container, false);
        ButterKnife.bind(this, root);
        mtTopBar.setOnMenuItemClickListener((MenuItem item) -> {
            if (item.getItemId() == R.id.menu_done) {
                // check inputs
                if (!validateIpv6Address(etServerAddr.getText().toString())) {
                    makeToast("Invalid IPv6 Address");
                    return false;
                }
                if (!validatePort(etServerPort.getText().toString())) {
                    makeToast("Invalid Port");
                    return false;
                }
                if (!validateServerName(etServerName.getText().toString())) {
                    makeToast("Invalid Server Name");
                    return false;
                }
                // submit change
                String name = etServerName.getText().toString();
                String addr = etServerAddr.getText().toString();
                int port = Integer.parseInt(etServerPort.getText().toString());

                List<ServerConfig> serverConfigs = GlobalConfig.getServerConfigs(getContext());
                serverConfigs.add(new ServerConfig(name, addr, port));
                GlobalConfig.setServerConfigs(getContext(), serverConfigs);
                getBaseFragmentActivity().stopFragment();
                return true;
            } else {
                return false;
            }
        });
        return root;
    }

    private void makeToast(String text) {
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
