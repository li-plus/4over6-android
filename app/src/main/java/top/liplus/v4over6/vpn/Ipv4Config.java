package top.liplus.v4over6.vpn;

public class Ipv4Config {
    public String ipv4 = "";
    public String route = "";
    public String dns1 = "";
    public String dns2 = "";
    public String dns3 = "";
    // TODO remove
    public int socketFd = -1;
    public String searchDomain = "";
    public String ipv6 = "";
    public String port = "";

    @Override
    public String toString() {
        return String.format("Tunnel IP: %s\n", ipv4) +
                String.format("Route: %s\n", route) +
                String.format("DNS Servers: %s, %s, %s\n", dns1, dns2, dns3) +
                String.format("Search Domain: %s\n", searchDomain);
    }
}
