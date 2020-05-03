package top.liplus.v4over6.common;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import top.liplus.v4over6.vpn.ServerConfig;

public class GlobalConfig {

    public static int getConfigIndex(@NonNull Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(Defs.KEY_GLOBAL, Context.MODE_PRIVATE);
        return sp.getInt(Defs.KEY_CONFIG_INDEX, -1);
    }

    public static void setConfigIndex(@NonNull Context ctx, int configIndex) {
        SharedPreferences sp = ctx.getSharedPreferences(Defs.KEY_GLOBAL, Context.MODE_PRIVATE);
        sp.edit().putInt(Defs.KEY_CONFIG_INDEX, configIndex).apply();
    }

    public static List<ServerConfig> getServerConfigs(@NonNull Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(Defs.KEY_GLOBAL, Context.MODE_PRIVATE);
        String strConfigs = sp.getString(Defs.KEY_SERVER_CONFIGS, "");
        if (strConfigs.isEmpty()) {
            return new ArrayList<>();
        }

        List<ServerConfig> serverConfigs = new ArrayList<>();
        for (String strConfig : strConfigs.split("\n\n")) {
            String[] serverInfo = strConfig.split("\n");
            if (serverInfo.length != 5 && serverInfo.length != 4) {
                continue;
            }
            try {
                String name = serverInfo[0];
                String ipv6 = serverInfo[1];
                int port = Integer.parseInt(serverInfo[2]);
                boolean enable_encryption = Boolean.parseBoolean(serverInfo[3]);
                String uuid = "";
                if (serverInfo.length > 4) {
                    uuid = serverInfo[4];
                }
                serverConfigs.add(new ServerConfig(name, ipv6, port, enable_encryption, uuid));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return serverConfigs;
    }

    public static void setServerConfigs(@NonNull Context ctx, List<ServerConfig> configs) {
        SharedPreferences sp = ctx.getSharedPreferences(Defs.KEY_GLOBAL, Context.MODE_PRIVATE);
        StringBuilder builder = new StringBuilder();
        for (ServerConfig config : configs) {
            builder.append(config.name).append('\n')
                    .append(config.host).append('\n')
                    .append(config.port).append("\n")
                    .append(config.enable_encrypt);
            if (config.enable_encrypt || !config.uuid.isEmpty()) {
                builder.append('\n').append(config.uuid);
            }
            builder.append("\n\n");
        }
        sp.edit().putString(Defs.KEY_SERVER_CONFIGS, builder.toString()).apply();
    }
}
