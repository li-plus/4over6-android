package top.liplus.v4over6.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.List;

import top.liplus.v4over6.R;
import top.liplus.v4over6.vpn.ServerConfig;

public class ServerConfigAdapter extends BaseRecyclerViewAdapter<ServerConfig, BaseRecyclerViewHolder> {
    private Context context;
    public ServerConfig selected;

    public ServerConfigAdapter(Context context, List<ServerConfig> data) {
        super(data);
        this.context = context;
    }

    @NonNull
    @Override
    public BaseRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BaseRecyclerViewHolder vh = new BaseRecyclerViewHolder(parent, R.layout.item_server_config);
        vh.getView().setOnClickListener((View view) -> {
            selected = getData().get(vh.getAdapterPosition());
            Toast.makeText(context, "[" + selected.ipv6 + "]:" + selected.port, Toast.LENGTH_SHORT).show();
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseRecyclerViewHolder holder, int position) {
        ServerConfig config = getData().get(position);
        ((TextView) holder.findViewById(R.id.tv_server_addr)).setText(config.ipv6);
        ((TextView) holder.findViewById(R.id.tv_server_port)).setText(String.valueOf(config.port));
    }
}
