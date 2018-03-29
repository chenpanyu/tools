package cn.com.cpy.tools.gitlog.util;

import java.util.ArrayList;
import java.util.List;

public class ProductInfo {
	private String fileName;
	private String filePath;
	private String fileLocation;
	private String productName;
	private List<String> pluginsName = new ArrayList<>();
	private List<String> featuresName = new ArrayList<>();

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFileLocation() {
		return fileLocation;
	}

	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	public String getProductName() {
		if (productName == null || productName.isEmpty()) {
			int dot = fileName.lastIndexOf(".");
			if (dot > 0) {
				setProductName(fileName.substring(0, dot));
			}
		}
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public List<String> getPluginsName() {
		return pluginsName;
	}

	public void setPluginsName(List<String> pluginsName) {
		this.pluginsName = pluginsName;
	}

	public List<String> getFeaturesName() {
		return featuresName;
	}

	public void setFeaturesName(List<String> featuresName) {
		this.featuresName = featuresName;
	}

}
