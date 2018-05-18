package adssdk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ads异步消息接收服务类
 * Created by chenpanyu on 2018/4/22.
 */
public class AdsSocketServer {

    private static final String encoding = "UTF-8";

    // 用于与接收Ads消息的ServerSocket
    private ServerSocket server;

    private int port = -1;

    // 线程池,用于控制服务端线程数量,并重用线程
    private ExecutorService threadPool;

    private boolean end = false;

    private IAdsMessageHandler adsMessageHandler;

    public AdsSocketServer(IAdsMessageHandler adsMessageHandler) {
        this.adsMessageHandler = adsMessageHandler;
    }

    public int getPort() {
        return port;
    }

    public boolean start() throws Exception {
        try {
            // 初始化ServerSocket
            server = new ServerSocket(0);
            port = server.getLocalPort();
            System.out.println("外设Socket已在：" + port + "端口监听");
            // 初始化线程池
            threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
                private final AtomicLong counter = new AtomicLong(0);

                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("AdsSocketServerThreadPool-"
                            + counter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }

            });
            // 启动监听
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    // 死循环监听不同Ads的连接
                    while (!end) {
                        System.out.println("等待ADS的连接...");
                        Socket socket = null;
                        try {
                            socket = server.accept();
                        } catch (IOException e) {
                            e.printStackTrace();
                            end = true;
                        }
                        if (socket != null) {
                            System.out.println(socket.getRemoteSocketAddress().toString() + " ADS连接了");
                            /*
                             * 创建一个处理线程
                             */
                            Runnable clientHandler = new ClientHandler(socket);
                            // 将任务指派给线程池
                            threadPool.execute(clientHandler);
                        }
                    }

                }
            });
            thread.setName("AdsSocketServerThread");
            thread.setDaemon(true);
            thread.start();
        } catch (Exception e) {
            throw new Exception("启动AdsSocketServer出错", e);
        }
        return true;
    }

    public boolean stop() {
        // 关闭线程池
        end = true;
        threadPool.shutdownNow();
        return true;
    }

    /**
     * 该线程的作用是接收ADS端发送消息并将消息嫁接给AdsMessageHandler
     *
     * @author Administrator
     */
    class ClientHandler implements Runnable {
        // 当前线程用于交流的指定Ads的Socket
        private Socket socket;

        /**
         * 创建线程体时将交互的Socket传入
         *
         * @param socket
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            DataInputStream dis = null;
            DataOutputStream dos = null;
            try {
                dis = new DataInputStream(
                        socket.getInputStream());
                dos = new DataOutputStream(
                        socket.getOutputStream());
                int recLen = dis.readInt();
                System.out.println("接收ADS发送消息长度为：" + recLen);
                byte[] recBytes = new byte[recLen];
                // 获取Ads发送消息
                dis.readFully(recBytes);
                String message = new String(recBytes, encoding);
                System.out.println("接收ADS发送消息为：" + message);
                // 通知Ads消息正常接收
                byte[] sendBytes = "ok".getBytes(encoding);
                dos.writeInt(sendBytes.length);
                dos.flush();
                dos.write(sendBytes);
                dos.flush();
                // 将Ads发送消息转发给AdsMessageHandler
                if (adsMessageHandler != null) {
                    adsMessageHandler.handleMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //System.out.println("准备与ADS断开Socket通讯");
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                        System.out.println("与ADS断开Socket通讯成功");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
