package adssdk;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于Ads消息组装
 * Created by chenpanyu on 2018/4/22.
 */
public class AdsMessage {

    private final String TYPE = "Type";

    private final String METHOD = "Method";

    private final String ARGS = "Args";

    private final String READ_TIMEOUT = "ReadTimeout";

    private final String WRITE_TIMEOUT = "WriteTimeout";

    private final String IS_OCCUPY = "IsOccupy";

    private final String KEY = "Key";

    private final String LISTEN_PORT = "ListenPort";

    private String key = "";

    private String type = "";

    private String method = "";

    private Map<String, Object> methodArgs = null;

    private int readTimeout = 0;

    private int writeTimeout = 0;

    private boolean occupy = false;

    private int listenPort = -1;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getMethodArgs() {
        return methodArgs;
    }

    public void setMethodArgs(Map<String, Object> methodArgs) {
        this.methodArgs = methodArgs;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public boolean isOccupy() {
        return occupy;
    }

    public void setOccupy(boolean occupy) {
        this.occupy = occupy;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public String toString(){
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(KEY, key);
        map.put(TYPE, type);
        map.put(METHOD, method);
        // ADS要求如没有入参则传入字符串"0"
        map.put(ARGS, methodArgs == null ? "0" : JSON.toJSONString(methodArgs));
        map.put(READ_TIMEOUT, readTimeout);
        map.put(WRITE_TIMEOUT, writeTimeout);
        map.put(IS_OCCUPY, occupy ? "1" : "0");
        map.put(LISTEN_PORT, listenPort);
        return JSON.toJSONString(map);
    }
}
