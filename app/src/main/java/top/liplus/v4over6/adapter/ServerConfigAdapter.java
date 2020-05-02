package top.liplus.v4over6.adapter;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import top.liplus.v4over6.R;
import top.liplus.v4over6.common.GlobalConfig;
import top.liplus.v4over6.fragment.BaseFragment;
import top.liplus.v4over6.fragment.EditConfigFragment;
import top.liplus.v4over6.fragment.OnShowToastListener;
import top.liplus.v4over6.vpn.ServerConfig;

public class ServerConfigAdapter extends BaseRecyclerViewAdapter<ServerConfig, BaseRecyclerViewAdapter.BaseViewHolder> {
    private BaseFragment fragment;
    public int selectedIndex;
    public boolean isIdle;

    public ServerConfigAdapter(BaseFragment fragment, List<ServerConfig> data, int configIndex) {
        super(data);
        this.fragment = fragment;
        this.selectedIndex = configIndex;
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BaseViewHolder vh = new BaseViewHolder(parent, R.layout.item_server_config);
        vh.getView().setOnClickListener((View view) -> {
            if (isIdle) {
                notifyItemChanged(selectedIndex);
                selectedIndex = vh.getAdapterPosition();
                GlobalConfig.setConfigIndex(fragment.getContext(), selectedIndex);
                ((RadioButton) vh.findViewById(R.id.rb_curr_config)).setChecked(true);
                notifyItemChanged(selectedIndex);
            } else {
                ((OnShowToastListener) fragment).showToast("Please disconnect first");
            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseRecyclerViewAdapter.BaseViewHolder holder, int position) {
        ServerConfig config = getData().get(position);
        // setup server address and port
        ((TextView) holder.findViewById(R.id.tv_server_name)).setText(config.name);
        ((TextView) holder.findViewById(R.id.tv_server_addr_port)).setText(String.format("[%s]:%d", config.host, config.port));
        // setup radio button
        ((RadioButton) holder.findViewById(R.id.rb_curr_config)).setChecked(selectedIndex == position);
        // setup delete handler
        ImageView ivDelete = (ImageView) holder.findViewById(R.id.iv_delete);
        setEnabledImageView(ivDelete, isIdle || selectedIndex != position);
        ivDelete.setOnClickListener((View view) -> {
            if (selectedIndex == holder.getAdapterPosition()) {
                selectedIndex = -1;
            } else if (holder.getAdapterPosition() < selectedIndex) {
                selectedIndex -= 1;
            }
            removeItemImmediate(holder.getAdapterPosition());
            GlobalConfig.setServerConfigs(fragment.getContext(), getData());
            GlobalConfig.setConfigIndex(fragment.getContext(), selectedIndex);
        });
        // setup edit handler
        ImageView ivEdit = (ImageView) holder.findViewById(R.id.iv_edit);
        setEnabledImageView(ivEdit, isIdle || selectedIndex != position);
        ivEdit.setOnClickListener((View view) -> {
            fragment.getBaseFragmentActivity().startFragment(new EditConfigFragment(position));
        });
    }

    private void setEnabledImageView(ImageView iv, boolean enabled) {
        iv.setEnabled(enabled);
        iv.setImageTintList(ColorStateList.valueOf(fragment.getContext().getColor(enabled ? R.color.gray_0 : R.color.gray_b)));
    }
}
