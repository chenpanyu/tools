package adssdk.example;

import adssdk.AdsMessage;
import adssdk.AdsUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chenpanyu on 2018/4/22.
 */
public class SyncInvokeTest {

    public static void main(String[] args) {
        // 组装AdsMessage
        Map<String, Object> methodArgs = new HashMap<String, Object>();
        methodArgs.put("TellerNo", "90000055");
        methodArgs.put("TellerRating", "5");
        methodArgs.put("TelerName", "测试柜员");
        AdsMessage message = new AdsMessage();
        message.setType("Gwq");
        message.setMethod("DisplayTellerInfo");
        message.setMethodArgs(methodArgs);
        message.setReadTimeout(20);
        message.setWriteTimeout(20);
        message.setKey("syncInvoke");
        message.setOccupy(false);
        message.setListenPort(AdsUtils.getSocketServerPort());
        System.out.println("AdsMessage:" + message.toString());
        // 发送消息
        try {
            System.out.println("result:" + AdsUtils.invokeDeviceService(message, "127.0.0.1", 30101));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
