package cn.com.cpy.tools.gitlog;

import cn.com.cpy.tools.gitlog.util.PluginInfo;
import cn.com.cpy.tools.gitlog.util.ProductInfo;
import cn.com.cpy.tools.gitlog.util.ProductInfoUtil;
import cn.com.cpy.tools.gitlog.util.UTF8Properties;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Created by chenpanyu on 2017/12/7.
 */
public class GitLogMain {

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

    private static final SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddHHmm");

    private static final SimpleDateFormat format2 = new SimpleDateFormat("yyyyMMdd");

    private static final String DOT_PROJECT = ".project";

    private static final Map<File, String> findedDirCacheMap = new ConcurrentHashMap<>();

    private static final String ENCODING = "UTF-8";

    private static String getProjectName(File projectFile) throws Exception {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(projectFile));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("<name>")) {
                    return line.replaceAll("<name>", "").replaceAll("</name>", "");
                }
            }
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                }
        }
        return "<" + projectFile.getAbsolutePath() + ">";
    }

    private static void execute(File repositoryDir, File logFile, File absProductFile, File abcProductFile, Date startDate, Date endDate) throws Exception {
        String gitDir = repositoryDir + "/.git";
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        try (Repository repository = repositoryBuilder.setGitDir(new File(gitDir)).readEnvironment().findGitDir()
                .setMustExist(true).build();
             Git git = new Git(repository);
             FileOutputStream fos = new FileOutputStream(logFile)) {
            List<String> projectNameList = new ArrayList<>();
            StringBuilder str = new StringBuilder();
            Iterable<RevCommit> iterable1 = git.log().call();
            Iterable<RevCommit> iterable2 = git.log().call();
            Iterator<RevCommit> iterator1 = iterable1.iterator();
            Iterator<RevCommit> iterator2 = iterable2.iterator();
            // 让iterator2指向第二个commit
            iterator2.next();
            // 遍历commit
            while (iterator1.hasNext()) {
                RevCommit newCommit = iterator1.next();
                RevCommit oldCommit = iterator2.next();
                // endDate不为空且commit时间大于endDate
                if (endDate != null && endDate.compareTo(newCommit.getCommitterIdent().getWhen()) < 0) {
                    continue;
                }
                // commit时间小于startDate
                if (startDate.compareTo(newCommit.getCommitterIdent().getWhen()) > 0) {
                    break;
                }
                AbstractTreeIterator newTreeParser = prepareTreeParser(repository, newCommit);
                AbstractTreeIterator oldTreeParser = prepareTreeParser(repository, oldCommit);
                // 拼装每个commit
                str.append("===============================================================================================================\n");
                str.append(" commit " + newCommit.getName() + "\n");
                PersonIdent author = newCommit.getAuthorIdent();
                str.append(" Author: " + author.getName() + " <" + author.getEmailAddress() + "> " + author.getWhen() + "\n");
                PersonIdent committer = newCommit.getCommitterIdent();
                str.append(" Commiter: " + committer.getName() + " <" + committer.getEmailAddress() + "> " + committer.getWhen() + "\n");
                str.append("\n   " + newCommit.getShortMessage() + "\n\n");
                List<DiffEntry> diffEntries = git.diff().setNewTree(newTreeParser).setOldTree(oldTreeParser).call();
                for (DiffEntry entry : diffEntries) {
                    // 遍历diff文件所在的父目录,获取diff文件所属工程
                    for (File diff = new File(repositoryDir, entry.getNewPath()).getParentFile(); !diff.equals(repositoryDir); diff = diff.getParentFile()) {
                        // 过滤已经查找过的目录,提高效率
                        if (findedDirCacheMap.containsKey(diff)) continue;
                        File dotProjectFile = new File(diff, DOT_PROJECT);
                        if (dotProjectFile.exists()) {
                            projectNameList.add(getProjectName(dotProjectFile));
                        }
                        findedDirCacheMap.put(diff, "");
                    }
                    str.append(entry.getChangeType().toString().substring(0, 1) + " " + entry.getNewPath() + "\n");
                }
                str.append("\n");
            }
            // 过滤不需要的插件
            str.append("===============================================================================================================\n");
            ProductInfo absInfo = ProductInfoUtil.loadProduct(absProductFile);
            ProductInfo abcInfo = ProductInfoUtil.loadProduct(abcProductFile);
            for (Iterator<String> it = projectNameList.iterator(); it.hasNext(); ) {
                String projectName = it.next();
                if (absInfo != null && !absInfo.getPluginsName().contains(projectName)
                        && abcInfo != null && !abcInfo.getPluginsName().contains(projectName)) {
                    it.remove();
                    continue;
                }
                System.out.println(projectName);
                str.append("[" + projectName + "]\n");
            }
            // 输出到log文件
            fos.write(str.toString().getBytes(ENCODING));
            fos.flush();
        }
    }

    /**
     * 获取对应commit的Tree对象
     *
     * @param repository 仓库
     * @param commit     提交
     * @return
     * @throws IOException
     */
    private static AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit) throws IOException {
        RevTree tree = commit.getTree();
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        try (ObjectReader reader = repository.newObjectReader()) {
            treeParser.reset(reader, tree.getId());
        }
        return treeParser;
    }

    /**
     * 获取配置
     *
     * @param config 配置文件
     * @return
     */
    private static Properties getProperties(File config) {
        if (!config.exists()) {
            errorLog("配置文件[" + config.getAbsolutePath() + "]不存在");
            return null;
        }
        Properties props = new UTF8Properties();
        try (FileInputStream is = new FileInputStream(config)) {
            props.load(is);
            return props;
        } catch (IOException e) {
            errorLog("读取配置文件[" + config.getAbsolutePath() + "]错误");
            return null;
        }
    }

    /**
     * 获取差量插件列表
     *
     * @param props
     */
    private static void getDiffPluginList(Properties props) throws Exception {
        // 相关配置
        String startTime = props.getProperty("startTime");
        String endTime = props.getProperty("endTime");
        String exportDirPath = props.getProperty("exportDirPath");
        String abRepositoryDirPath = props.getProperty("abRepositoryDirPath");
        String adoreRepositoryDirPath = props.getProperty("adoreRepositoryDirPath");
        String absProductPath = props.getProperty("absProductPath");
        String abcProductPath = props.getProperty("abcProductPath");

        // 判断目录是否存在
        File exportDir = new File(exportDirPath);
        File version_ = new File(exportDir, "version_" + endTime);
        File abRepositoryDir = new File(abRepositoryDirPath);
        File adoreRepositoryDir = new File(adoreRepositoryDirPath);
        File absProductFile = new File(absProductPath);
        File abcProductFile = new File(abcProductPath);
        System.out.println("目录[" + exportDir.getAbsolutePath() + "]是否存在:" + exportDir.exists());
        if (!exportDir.exists()) {
            errorLog("版本存放目录不存在,程序退出!");
            return;
        }
        System.out.println("目录[" + abRepositoryDir.getAbsolutePath() + "]是否存在:" + abRepositoryDir.exists());
        System.out.println("目录[" + adoreRepositoryDir.getAbsolutePath() + "]是否存在:" + adoreRepositoryDir.exists());
        if (!abRepositoryDir.exists() || !adoreRepositoryDir.exists()) {
            errorLog("ab或adore仓库目录不存在,程序退出!");
            return;
        }
        System.out.println("文件[" + absProductFile.getAbsolutePath() + "]是否存在:" + absProductFile.exists() + "," + (absProductFile.exists() ? "过滤服务端插件" : "不过滤服务端插件"));
        System.out.println("文件[" + abcProductFile.getAbsolutePath() + "]是否存在:" + abcProductFile.exists() + "," + (abcProductFile.exists() ? "过滤客户端插件" : "不过滤客户端插件"));
        // 创建{version}目录
        if (!version_.exists()) version_.mkdirs();
        // 创建{log}/plugins目录
        File versionPluginsDir = new File(version_, "plugins");
        if (!versionPluginsDir.exists()) versionPluginsDir.mkdirs();
        // 创建{log}/update/plugins目录
        File versionUpdatePluginsDir = new File(version_, "update/plugins");
        if (!versionUpdatePluginsDir.exists()) versionUpdatePluginsDir.mkdirs();
        // 生成xx_git.log文件并导出修改工程名
        File abGitLogFile = new File(version_, abRepositoryDir.getName() + "_git.changelog");
        File adoreGitLogFile = new File(version_, adoreRepositoryDir.getName() + "_git.changelog");
        if (!abGitLogFile.exists()) abGitLogFile.createNewFile();
        if (!adoreGitLogFile.exists()) adoreGitLogFile.createNewFile();
        System.out.println("==========================ab==============================");
        execute(abRepositoryDir, abGitLogFile, absProductFile, abcProductFile, format.parse(startTime), endTime == null ? null : format.parse(endTime));
        System.out.println("==========================adore==============================");
        execute(adoreRepositoryDir, adoreGitLogFile, absProductFile, abcProductFile, format.parse(startTime), endTime == null ? null : format.parse(endTime));
    }

    /**
     * 扫描插件目录并获取更新记录
     *
     * @param props
     * @throws Exception
     */
    private static void getPluginUpdateLog(Properties props) throws Exception {
        // 相关配置
        String scanPlginsDirPath = props.getProperty("scanPlginsDirPath");
        String abRepositoryDirPath = props.getProperty("abRepositoryDirPath");
        String adoreRepositoryDirPath = props.getProperty("adoreRepositoryDirPath");

        // 判断目录是否存在
        File scanPlginsDir = new File(scanPlginsDirPath);
        File abRepositoryDir = new File(abRepositoryDirPath);
        File adoreRepositoryDir = new File(adoreRepositoryDirPath);
        System.out.println("目录[" + scanPlginsDir.getAbsolutePath() + "]是否存在:" + scanPlginsDir.exists());
        if (!scanPlginsDir.exists()) {
            errorLog("扫描插件目录不存在,程序退出!");
            return;
        }
        System.out.println("目录[" + abRepositoryDir.getAbsolutePath() + "]是否存在:" + abRepositoryDir.exists());
        System.out.println("目录[" + adoreRepositoryDir.getAbsolutePath() + "]是否存在:" + adoreRepositoryDir.exists());
        if (!abRepositoryDir.exists() || !adoreRepositoryDir.exists()) {
            errorLog("ab或adore仓库目录不存在,程序退出!");
            return;
        }
        List<PluginInfo> infos = new ArrayList<>();
        Date[] oldestDate = new Date[1];
        // 扫描
        scanPlgins(infos, oldestDate, scanPlginsDir);
        // 清理update.log
        File logFile = new File(scanPlginsDir, "update.log");
        if (logFile.exists()) {
            logFile.delete();
        }
        execute(infos, abRepositoryDir, logFile, oldestDate[0]);
        execute(infos, adoreRepositoryDir, logFile, oldestDate[0]);
        System.out.println("扫描插件目录并获取更新记录结束,请查看文件[" + logFile.getAbsolutePath() + "]");
    }

    private static void execute(List<PluginInfo> plugins, File repositoryDir, File logFile, Date oldestDate) throws Exception {
        String gitDir = repositoryDir + "/.git";
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        try (Repository repository = repositoryBuilder.setGitDir(new File(gitDir)).readEnvironment().findGitDir()
                .setMustExist(true).build();
             Git git = new Git(repository);
             FileOutputStream fos = new FileOutputStream(logFile, true)) {
            Iterable<RevCommit> iterable1 = git.log().call();
            Iterable<RevCommit> iterable2 = git.log().call();
            Iterator<RevCommit> iterator1 = iterable1.iterator();
            Iterator<RevCommit> iterator2 = iterable2.iterator();
            // 让iterator2指向第二个commit
            iterator2.next();
            // 遍历commit
            StringBuilder str = new StringBuilder();
            while (iterator1.hasNext()) {
                RevCommit newCommit = iterator1.next();
                RevCommit oldCommit = iterator2.next();
                // commit时间小于startDate
                if (oldestDate.compareTo(newCommit.getCommitterIdent().getWhen()) > 0) {
                    break;
                }
                AbstractTreeIterator newTreeParser = prepareTreeParser(repository, newCommit);
                AbstractTreeIterator oldTreeParser = prepareTreeParser(repository, oldCommit);
                List<DiffEntry> diffEntries = git.diff().setNewTree(newTreeParser).setOldTree(oldTreeParser).call();
                StringBuilder diffStr = new StringBuilder();
                findedDirCacheMap.clear();
                for (DiffEntry entry : diffEntries) {
                    // 遍历diff文件所在的父目录,获取diff文件所属工程
                    boolean isBreak1 = false;
                    for (File diff = new File(repositoryDir, entry.getNewPath()).getParentFile(); !diff.equals(repositoryDir); diff = diff.getParentFile()) {
                        // 过滤已经查找过的目录,提高效率
                        if (findedDirCacheMap.containsKey(diff)) continue;
                        File dotProjectFile = new File(diff, DOT_PROJECT);
                        // 判断projectName以及commit时间
                        if (dotProjectFile.exists()) {
                            String projectName = getProjectName(dotProjectFile);
                            Date newCommitDate = newCommit.getCommitterIdent().getWhen();
                            // 判断projectName是否相等和newCommitDate是否大于等于插件日期
                            for(PluginInfo pi : plugins){
                                if (pi.id.equals(projectName) && pi.date.compareTo(newCommitDate) <= 0) {
                                    diffStr.append("   " + pi.fileName + "\n");
                                    isBreak1 = true;
                                    break;
                                }
                            }
                        }
                        findedDirCacheMap.put(diff, "");
                        if (isBreak1) {
                            break;
                        }
                    }
                }
                if (diffStr.toString().length() > 0) {
                    str.append("[" + repositoryDir.getName() + "Repository] commit " + newCommit.getName() + " 涉及更新插件为:\n");
                    //str.append("===============================================================================================================\n");
                    str.append(diffStr);
                    str.append("\n===============================================================================================================\n\n\n");
                }
            }
            // 输出到log文件
            fos.write(str.toString().getBytes(ENCODING));
            fos.flush();
        }
    }

    /**
     * 扫描插件目录并生成插件信息和最旧插件时间
     *
     * @param infos         插件信息
     * @param oldestDate    最旧插件时间
     * @param scanPlginsDir
     */
    private static void scanPlgins(List<PluginInfo> infos, Date[] oldestDate, File scanPlginsDir) {
        if (infos == null) {
            return;
        }
        File[] files = scanPlginsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // 获取目录
                if (new File(dir, name).isDirectory()) {
                    return true;
                }
                // 获取cn.com.agree.xxxxxx.jar文件
                if (name.startsWith("cn.com.agree") && name.endsWith(".jar")) {
                    return true;
                }
                return false;
            }
        });
        for (File file : files) {
            if (file.isFile()) {
                try (JarFile jarFile = new JarFile(file);) {
                    ZipEntry mfEntry = jarFile.getEntry("META-INF/MANIFEST.MF");
                    if (mfEntry == null) {
                        errorLog("无法从文件[" + file.getAbsolutePath()
                                + "]中获得MANIFEST.MF条目");
                        continue;
                    }
                    try (InputStream manifestInputStream = jarFile.getInputStream(mfEntry);) {
                        Manifest mf = new Manifest(manifestInputStream);
                        Attributes attrs = mf.getMainAttributes();
                        // 生成PluginInfo信息
                        PluginInfo info = new PluginInfo();
                        info.fileName = file.getName();
                        info.id = getPluginId(attrs.getValue("Bundle-SymbolicName"));
                        String version = attrs.getValue("Bundle-Version");
                        info.version = version;
                        info.date = getDateByVersion(version);
                        // 保留最旧插件日期
                        if (oldestDate[0] == null || oldestDate[0].after(info.date)) {
                            oldestDate[0] = info.date;
                        }
                        infos.add(info);
                    }
                } catch (ParseException e) {
                    errorLog("无法从文件[" + file.getAbsolutePath()
                            + "]中生成PluginInfo信息");
                } catch (IOException e) {
                    errorLog("无法从文件[" + file.getAbsolutePath()
                            + "]中获得MANIFEST.MF条目");
                }
            } else {
                scanPlgins(infos, oldestDate, file);
            }
        }
    }

    private static String getPluginId(String symbolicName) throws ParseException {
        String[] array = symbolicName.split(";");
        return array[0];
    }

    private static Date getDateByVersion(String version) throws ParseException {
        version = version.replace("v", "");
        String[] array = version.split("\\.");
        Date date;
        try {
            date = format1.parse(array[array.length - 1]);
        } catch (ParseException e) {
            try {
                date = format2.parse(array[array.length - 1]);
            } catch (ParseException e1) {
                throw e1;
            }
        }
        return date;
    }

    private static void errorLog(String msg) {
        System.out.println("ERROR  " + msg);
    }

    public static void main(String[] args) throws Exception {
        File config = new File("gitlog.properties");
        Properties props = getProperties(config);
        if (props == null) {
            errorLog("获取配置失败,退出程序!");
            return;
        }
//        getPluginUpdateLog(props);
        if ("getDiffPluginList".equals(args[0])) {
            getDiffPluginList(props);
        } else if ("getPluginUpdateLog".equals(args[0])) {
            getPluginUpdateLog(props);
        } else {
            errorLog("程序不支持[" + args[0] + "]指令");
        }
    }
}
