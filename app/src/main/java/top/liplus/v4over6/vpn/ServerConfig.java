package top.liplus.v4over6.vpn;

public class ServerConfig {
    public String name = "";
    public String ipv6 = "";
    public int port = -1;

    public ServerConfig() {
    }

    public ServerConfig(String name, String ipv6, int port) {
        this.name = name;
        this.ipv6 = ipv6;
        this.port = port;
    }
}
