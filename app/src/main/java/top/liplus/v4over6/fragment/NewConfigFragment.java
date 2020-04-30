package top.liplus.v4over6.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.MaterialToolbar;

import butterknife.BindView;
import butterknife.ButterKnife;
import top.liplus.v4over6.R;

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
        mtTopBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_new) {
                    getBaseFragmentActivity().startFragment(new NewConfigFragment());
                    return true;
                } else {
                    return false;
                }
            }
        });
        return root;
    }
}
