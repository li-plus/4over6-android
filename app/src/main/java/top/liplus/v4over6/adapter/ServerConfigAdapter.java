package top.liplus.v4over6.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import top.liplus.v4over6.R;
import top.liplus.v4over6.vpn.ServerConfig;

public class ServerConfigAdapter extends BaseRecyclerViewAdapter<ServerConfig, BaseRecyclerViewAdapter.BaseRecyclerViewHolder> {
    private Context context;
    public int selectedIndex = -1;

    public ServerConfigAdapter(Context context, List<ServerConfig> data) {
        super(data);
        this.context = context;
    }

    @NonNull
    @Override
    public BaseRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BaseRecyclerViewHolder vh = new BaseRecyclerViewHolder(parent, R.layout.item_server_config);
        vh.getView().setOnClickListener((View view) -> {
            notifyItemChanged(selectedIndex);
            selectedIndex = vh.getAdapterPosition();
            ((RadioButton) vh.findViewById(R.id.rb_curr_config)).setChecked(true);
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseRecyclerViewAdapter.BaseRecyclerViewHolder holder, int position) {
        ServerConfig config = getData().get(position);
        // setup server address and port
        ((TextView) holder.findViewById(R.id.tv_server_addr)).setText(config.ipv6);
        ((TextView) holder.findViewById(R.id.tv_server_port)).setText(String.valueOf(config.port));
        // setup radio button
        ((RadioButton) holder.findViewById(R.id.rb_curr_config)).setChecked(selectedIndex == position);
        // setup delete handler
        holder.findViewById(R.id.iv_delete).setOnClickListener((View view) -> {
            if (selectedIndex == holder.getAdapterPosition()) {
                selectedIndex = -1;
            } else if (holder.getAdapterPosition() < selectedIndex) {
                selectedIndex -= 1;
            }
            removeItemImmediately(holder.getAdapterPosition());
        });

        // setup edit handler
        holder.findViewById(R.id.iv_edit).setOnClickListener((View view) -> {

        });
    }
}
