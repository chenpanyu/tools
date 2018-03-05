package cn.com.cpy.tools.uniform.screensize;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;

public class ScreenSizeMain {

    private static int successCount = 0;

    private static int failueCount = 0;

    public static void main(String[] args) {
        // args[0] 为目录绝对路径
        String dirPath = args[0];
//		String dirPath = "D:\\master_test";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            System.out.println("文件[" + dirPath + "]不存在");
        }
        System.out.println("开始扫描[" + dir.getAbsolutePath() + "]");
        scanLoop(dir);
        System.out.println("扫描结束,成功个数：" + successCount + ",失败个数：" + failueCount);
    }

    /**
     * 循环扫描文件
     *
     * @param src 文件或目录
     */
    private static void scanLoop(File src) {
        if (src.isDirectory()) {
            for (File file : src.listFiles()) {
                scanLoop(file);
            }
        } else {
            if (!src.getName().startsWith(".stamp") && src.getName().endsWith(".abf4a")) {
                System.out.println("开始统一屏幕大小,文件[" + src.getAbsolutePath() + "]");
                try {
                    uniformAbf4a(src);
                    System.out.println("统一屏幕大小成功");
                    System.out.println();
                    successCount++;
                } catch (Exception e) {
                    System.out.println("统一屏幕大小失败，原因:");
                    PrintWriter pw = new PrintWriter(System.out);
                    e.printStackTrace(pw);
                    pw.close();
                    failueCount++;
                }
            }
        }
    }

    /**
     * 统一大中小屏
     *
     * @param abf4a
     */
    private static void uniformAbf4a(File abf4a) throws Exception {
        SAXReader reader = new SAXReader();
        reader.setEncoding("UTF-8");
        Document document = reader.read(abf4a);
        Element rootNode = document.getRootElement();
        listNodes(rootNode);
        try (FileOutputStream out = new FileOutputStream(abf4a);){
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            XMLWriter writer = new XMLWriter(out, format);
            writer.write(document);
            writer.flush();
            writer.close();
        }
    }

    /**
     * 递归遍历所有节点,并修改大中小屏数据
     *
     * @param node
     */
    private static void listNodes(Element node) throws Exception {
        String keyValue = node.attributeValue("key");
        String typeValue = node.attributeValue("type");
        // 修改大中小屏,全改为大屏数据
        if (typeValue != null && keyValue != null && typeValue.equals("ScreenData")
                && (keyValue.equals("constraints#size") || keyValue.equals("constraints#offset"))) {
            Attribute valueAttr = node.attribute("value");
            String[] values = valueAttr.getValue().split(",");
            String newValue = values[2] + "," + values[2] + "," + values[2];
            valueAttr.setValue(newValue);
        }
        // 递归遍历所有节点
        List<Element> listElement = node.elements();
        for (Element e : listElement) {
            listNodes(e);
        }
    }
}
