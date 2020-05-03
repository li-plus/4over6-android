package top.liplus.v4over6.vpn;

public class V4over6 {
    static {
        System.loadLibrary("v4over6");
    }

    public static native int connectSocket(String addr, int port);

    public static native void disconnectSocket();

    public static native int requestIpv4Config();

    public static native void setupTunnel(int tunnel_fd);

    public static native void getStatistics(Statistics stats);

    public static native void getIpv4Config(Ipv4Config config);

    public static native boolean isRunning();
}
