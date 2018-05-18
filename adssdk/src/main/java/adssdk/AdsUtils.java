package adssdk;

import com.alibaba.fastjson.JSON;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

/**
 * Created by chenpanyu on 2018/4/21.
 */
public class AdsUtils {

    private static final String encoding = "UTF-8";

    // 与ADS信息交互通讯超时 10min
    private static int commTimeout = 60000 * 10;

    // 连接ADS超时
    private static int connTimeout = 3000;

    private static AdsSocketServer server;

    public static int getCommTimeout() {
        return commTimeout;
    }

    public static void setCommTimeout(int commTimeout) {
        AdsUtils.commTimeout = commTimeout;
    }

    public static int getConnTimeout() {
        return connTimeout;
    }

    public static void setConnTimeout(int connTimeout) {
        AdsUtils.connTimeout = connTimeout;
    }

    /**
     * 消息发送
     *
     * @param message 发送报文
     * @param ip      ip
     * @param port    端口
     * @return
     * @throws IOException
     */
    private static String sendToAds(String message, String ip, int port) throws IOException {
        System.out.println("连接ads地址为:" + ip + ":" + port);
        Socket socket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        try {
            socket = createSocket(ip, port, commTimeout, connTimeout);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            byte[] sendBytes = message.getBytes(encoding);
            dos.writeInt(sendBytes.length);
            dos.flush();
            dos.write(sendBytes);
            dos.flush();
            int recLen = dis.readInt();
            byte[] recBytes = new byte[recLen];
            dis.readFully(recBytes);
            return new String(recBytes, encoding);
        } catch (SocketException e) {
            throw new IOException("调用外设服务失败：" + e.getMessage(), e);
        } finally {
            if (dos != null) dos.close();
            if (dis != null) dis.close();
            if (socket != null) socket.close();
        }
    }

    private static Socket createSocket(String ip, int port, int commTimeout,
                                       int connTimeout) throws IOException {
        InetAddress address = InetAddress.getByName(ip);
        Socket socket = new Socket(); // NOSONAR
        InetSocketAddress socketAddr = new InetSocketAddress(address, port);
        // 数据传输超时
        socket.setSoTimeout(commTimeout);
        socket.setReuseAddress(true);
        socket.setTcpNoDelay(true);
        // 连接超时 如果超时则报timeout 如果连接拒绝，不会等待超时，直接报refused
        socket.connect(socketAddr, connTimeout);
        return socket;
    }

    /**
     * 调用外设服务
     *
     * @param message 外设服务消息{@link AdsMessage}
     * @param ip      外设服务ip
     * @param port    外设服务socket端口
     * @return
     * @throws IOException
     */
    public static Map<String, Object> invokeDeviceService(AdsMessage message, String ip, int port)
            throws IOException {
        String sendMsg = message.toString();
        //System.out.println("sendMsg:" + sendMsg);
        String receiveMsg = sendToAds(sendMsg, ip, port);
        //System.out.println("receiveMsg:" + receiveMsg);
        return (Map<String, Object>) JSON.parse(receiveMsg);
    }

    /**
     * 启动Ads异步消息接收服务
     *
     * @param handler 消息处理器
     * @return 是否成功
     * @throws Exception
     */
    public static boolean startSocketServer(IAdsMessageHandler handler) throws Exception {
        server = new AdsSocketServer(handler);
        return server.start();
    }

    /**
     * 获取Ads异步消息接收服务端口
     * @return
     */
    public static int getSocketServerPort() {
        if (server != null) {
            return server.getPort();
        }
        return -1;
    }

    /**
     * 停止Ads异步消息接收服务
     *
     * @return 是否成功
     */
    public static boolean stopSocketServer() {
        if (server != null) {
            return server.stop();
        }
        return false;
    }
}
