package top.liplus.v4over6.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import top.liplus.v4over6.R;
import top.liplus.v4over6.activity.BaseFragmentActivity;
import top.liplus.v4over6.common.GlobalConfig;
import top.liplus.v4over6.fragment.NewConfigFragment;
import top.liplus.v4over6.vpn.ServerConfig;

public class ServerConfigAdapter extends BaseRecyclerViewAdapter<ServerConfig, BaseRecyclerViewAdapter.BaseRecyclerViewHolder> {
    private BaseFragmentActivity activity;
    public int selectedIndex;

    public ServerConfigAdapter(BaseFragmentActivity activity, List<ServerConfig> data, int configIndex) {
        super(data);
        this.activity = activity;
        this.selectedIndex = configIndex;
    }

    @NonNull
    @Override
    public BaseRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BaseRecyclerViewHolder vh = new BaseRecyclerViewHolder(parent, R.layout.item_server_config);
        vh.getView().setOnClickListener((View view) -> {
            notifyItemChanged(selectedIndex);
            selectedIndex = vh.getAdapterPosition();
            GlobalConfig.setConfigIndex(activity, selectedIndex);
            ((RadioButton) vh.findViewById(R.id.rb_curr_config)).setChecked(true);
            notifyItemChanged(selectedIndex);
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseRecyclerViewAdapter.BaseRecyclerViewHolder holder, int position) {
        ServerConfig config = getData().get(position);
        // setup server address and port
        ((TextView) holder.findViewById(R.id.tv_server_name)).setText(config.name);
        ((TextView) holder.findViewById(R.id.tv_server_addr_port)).setText(String.format("[%s]:%d", config.ipv6, config.port));
        // setup radio button
        ((RadioButton) holder.findViewById(R.id.rb_curr_config)).setChecked(selectedIndex == position);
        // setup delete handler
        holder.findViewById(R.id.iv_delete).setOnClickListener((View view) -> {
            if (selectedIndex == holder.getAdapterPosition()) {
                selectedIndex = -1;
            } else if (holder.getAdapterPosition() < selectedIndex) {
                selectedIndex -= 1;
            }
            removeItemImmediate(holder.getAdapterPosition());
            GlobalConfig.setServerConfigs(activity, getData());
        });

        // setup edit handler
        holder.findViewById(R.id.iv_edit).setOnClickListener((View view) -> {
            activity.startFragment(new NewConfigFragment());
        });
    }
}
