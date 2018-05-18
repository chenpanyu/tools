package adssdk.example;

import adssdk.AdsMessage;
import adssdk.AdsUtils;
import adssdk.IAdsMessageHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by chenpanyu on 2018/4/22.
 */
public class AsyncInvokeTest {
    private static BlockingQueue<String> asyncResultQueue = new ArrayBlockingQueue<String>(1);

    public static void main(String[] args) {
        // 启动Ads异步消息接收服务
        try {
            AdsUtils.startSocketServer(new IAdsMessageHandler() {
                public void handleMessage(String message) {
                    // 存放队列里
                    asyncResultQueue.add(message);
                }
            });
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        // 组装AdsMessage
        Map<String, Object> methodArgs = new HashMap<String, Object>();
        methodArgs.put("Path", "D:/client/abc_76(KF2)/ads/config/柜外清/PDF/test.pdf");
        methodArgs.put("Location", "300,300,300,300");
        methodArgs.put("Mode", "签名");
        AdsMessage message = new AdsMessage();
        message.setType("Gwq"); //此处应该为异步方法
        message.setMethod("Paperless");
        message.setMethodArgs(methodArgs);
        message.setReadTimeout(60);
        message.setWriteTimeout(20);
        message.setKey("AsyncInvoke");
        message.setOccupy(true);
        message.setListenPort(AdsUtils.getSocketServerPort());
        System.out.println("AdsMessage:" + message.toString());
        // 发送消息
        try {
            System.out.println("result:" + AdsUtils.invokeDeviceService(message, "127.0.0.1", 30101));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 等待队列有值并从队列获取
        try {
            String result = asyncResultQueue.take();
            System.out.println("async result:" + result);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // 解除占用
        // 组装AdsMessage
        message = new AdsMessage();
        message.setType("Gwq"); //此处应该为异步方法
        message.setMethod("ShowMedia");
        message.setMethodArgs(null);
        message.setReadTimeout(60);
        message.setWriteTimeout(20);
        message.setKey("AsyncInvoke");
        message.setOccupy(false);
        message.setListenPort(AdsUtils.getSocketServerPort());
        System.out.println("AdsMessage:" + message.toString());
        // 发送消息
        try {
            System.out.println("result:" + AdsUtils.invokeDeviceService(message, "127.0.0.1", 30101));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 停止Ads异步消息接收服务
        AdsUtils.stopSocketServer();
    }
}
