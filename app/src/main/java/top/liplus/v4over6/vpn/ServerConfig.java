package top.liplus.v4over6.vpn;

import java.util.regex.Pattern;

public class ServerConfig {
    public String name = "";
    public String host = "";
    public int port = -1;
    public boolean enable_encrypt = false;
    public String uuid = "";
    public String ipv6 = "";

    public ServerConfig() {
    }

    public ServerConfig(String name, String host, int port, boolean enable_encrypt, String uuid) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.enable_encrypt = enable_encrypt;
        this.uuid = uuid;
    }

    public static final Pattern IPV6_STD_PATTERN = Pattern
            .compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    public static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern
            .compile("^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");

    public static final Pattern HOSTNAME_PATTERN = Pattern.compile("^[a-z0-9]+([.][a-z0-9]+)+$");

    public static boolean isIPv6Address(final String input) {
        return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
    }

    public static boolean isIPv6StdAddress(final String input) {
        return IPV6_STD_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6HexCompressedAddress(final String input) {
        return IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
    }

    public static boolean isHostname(final String input) {
        return HOSTNAME_PATTERN.matcher(input).matches();
    }
}
