package cn.com.cpy.tools.replacefilestr;

import java.io.*;

/**
 * Created by chenpanyu on 2018/1/11.
 */
public class ReplaceFileStrMain {

    /**
     * 遍历并替换
     * @param file 文件或目录
     * @param oldStr 原字段
     * @param newStr 新字段
     * @param encoding 编码格式
     * @throws Exception
     */
    private static void loopAndReplace(File file, String oldStr, String newStr, String encoding) throws Exception {
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            for (File f : list) {
                loopAndReplace(f, oldStr, newStr, encoding);
            }
        } else {
            boolean isReplace = false;
            StringBuffer sb = new StringBuffer();
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, encoding);
                 BufferedReader reader = new BufferedReader(isr);) {
                String line = "";
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    if (line.contains(oldStr)) {
                        isReplace = true;
                        line = line.replaceAll(oldStr, newStr);
                        System.out.println("文件[" + file.getAbsolutePath() + "]被替换行号为:" + lineNum);
                    }
                    sb.append(line);
                    sb.append("\n");
                }
            } catch (UnsupportedEncodingException e) {
                System.out.println("不支持[" + encoding + "]编码格式");
                throw e;
            }
            // 判断是否需要替换,降低风险
            if (isReplace) {
                try (FileOutputStream fos = new FileOutputStream(file);
                     OutputStreamWriter isw = new OutputStreamWriter(fos, encoding);
                     BufferedWriter writer = new BufferedWriter(isw);
                     PrintWriter pw = new PrintWriter(writer);) {
                    pw.write(sb.toString());
                    pw.flush();
                } catch (UnsupportedEncodingException e) {
                    System.out.println("不支持[" + encoding + "]编码格式");
                    throw e;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String baseDirPath = args[0];
        String oldStr = args[1];
        String newStr = args[2];
        String encoding = args[3];
        // 测试
//        String baseDirPath = "C:\\Users\\Chris\\Desktop\\share\\CDNSBank";
//        String oldStr = "CDNSBank";
//        String newStrr = "GDRCU";
//        String encoding = "UTF-8";
        File baseDir = new File(baseDirPath);
        if (!baseDir.exists()) {
            System.out.println("目录[" + baseDir.getAbsolutePath() + "]不存在");
        }
        // 遍历baseDir下所有文件并替换
        loopAndReplace(baseDir, oldStr, newStr, encoding);
    }
}
