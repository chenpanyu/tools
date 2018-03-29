package cn.com.cpy.tools.gitlog.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class ProductInfoUtil {
	public static ProductInfo loadProduct(File productFile) {
		if (!productFile.exists()) {
			return null;
		}
		try {
			ProductInfo info = new ProductInfo();
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(productFile);
			Element root = document.getDocumentElement();
			info.setProductName(root.getAttribute("uid"));
			info.setFileName(productFile.getName());
			info.setFilePath(productFile.getAbsolutePath());
			NodeList pluginNodes = root.getElementsByTagName("plugins");
			if (pluginNodes.getLength() > 0) {
				Node pluginNode = pluginNodes.item(0);
				NodeList plugins = pluginNode.getChildNodes();
				for (int i = 0; i < plugins.getLength(); i++) {
					Node node = plugins.item(i);
					if (node instanceof Element) {
						Element plugin = (Element) node;
						String id = plugin.getAttribute("id");
						if (id != null && !id.isEmpty()) {
							info.getPluginsName().add(id);
						}
					}
				}
			}
			NodeList featuresNodes = root.getElementsByTagName("features");
			if (featuresNodes.getLength() > 0) {
				Node featureNodes = featuresNodes.item(0);
				NodeList features = featureNodes.getChildNodes();
				for (int i = 0; i < features.getLength(); i++) {
					Node node = features.item(i);
					if (node instanceof Element) {
						Element feature = (Element) node;
						String id = feature.getAttribute("id");
						if (id != null && !id.isEmpty()) {
							info.getPluginsName().add(id);
						}
					}
				}
			}
			return info;
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
		return null;
	}
}
