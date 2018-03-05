package cn.com.cpy.tools.gitlog;

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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by chenpanyu on 2017/12/7.
 */
public class GitLogMain {

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

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

    private static void execute(File repositoryDir, File logFile, Date startDate, Date endDate) throws Exception {
        String gitDir = repositoryDir + "/.git";
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        try (Repository repository = repositoryBuilder.setGitDir(new File(gitDir)).readEnvironment().findGitDir()
                .setMustExist(true).build();
             Git git = new Git(repository);
             FileOutputStream fos = new FileOutputStream(logFile)) {
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
                StringBuffer str = new StringBuffer();
                str.append("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
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
                            System.out.println(getProjectName(dotProjectFile));
                        }
                        findedDirCacheMap.put(diff, "");
                    }
                    str.append(entry.getChangeType().toString().substring(0, 1) + " " + entry.getNewPath() + "\n");
                }
                str.append("\n");
                // 输出到log文件
                fos.write(str.toString().getBytes(ENCODING));
                fos.flush();
            }
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
            System.out.println("配置文件[" + config.getAbsolutePath() + "]不存在");
            return null;
        }
        Properties props = new UTF8Properties();
        try (FileInputStream is = new FileInputStream(config)) {
            props.load(is);
            return props;
        } catch (IOException e) {
            System.out.println("读取配置文件[" + config.getAbsolutePath() + "]错误");
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        File config = new File("gitlog.properties");
        Properties props = getProperties(config);
        if (props == null) {
            System.out.println("获取配置失败,退出程序!");
            return;
        }
        // 相关配置
        String startTime = props.getProperty("startTime");
        String endTime = props.getProperty("endTime");
        String exportDirPath = props.getProperty("exportDirPath");
        String abRepositoryDirPath = props.getProperty("abRepositoryDirPath");
        String adoreRepositoryDirPath = props.getProperty("adoreRepositoryDirPath");

        // 判断目录是否存在
        File exportDir = new File(exportDirPath);
        File version_ = new File(exportDir, "version_" + endTime);
        File abRepositoryDir = new File(abRepositoryDirPath);
        File adoreRepositoryDir = new File(adoreRepositoryDirPath);
        System.out.println("目录[" + exportDir.getAbsolutePath() + "]是否存在:" + exportDir.exists());
        if (!exportDir.exists()) {
            System.out.println("版本存放目录不存在,程序退出!");
            return;
        }
        System.out.println("目录[" + abRepositoryDir.getAbsolutePath() + "]是否存在:" + abRepositoryDir.exists());
        System.out.println("目录[" + adoreRepositoryDir.getAbsolutePath() + "]是否存在:" + adoreRepositoryDir.exists());
        if (!abRepositoryDir.exists() || !adoreRepositoryDir.exists()) {
            System.out.println("ab或adore仓库目录不存在,程序退出!");
            return;
        }
        // 创建{version}目录
        if (!version_.exists()) version_.mkdirs();
        // 创建{log}/plugins目录
        File versionPluginsDir = new File(version_, "plugins");
        if (!versionPluginsDir.exists()) versionPluginsDir.mkdirs();
        // 创建{log}/update/plugins目录
        File versionUpdatePluginsDir = new File(version_, "update/plugins");
        if (!versionUpdatePluginsDir.exists()) versionUpdatePluginsDir.mkdirs();
        // 生成xx_git.log文件并导出修改工程名
        File abGitLogFile = new File(version_, abRepositoryDir.getName() + "_git.log");
        File adoreGitLogFile = new File(version_, adoreRepositoryDir.getName() + "_git.log");
        if (!abGitLogFile.exists()) abGitLogFile.createNewFile();
        if (!adoreGitLogFile.exists()) adoreGitLogFile.createNewFile();
        System.out.println("==========================ab==============================");
        execute(abRepositoryDir, abGitLogFile, format.parse(startTime), endTime.isEmpty() ? null : format.parse(endTime));
        System.out.println("==========================adore==============================");
        execute(adoreRepositoryDir, adoreGitLogFile, format.parse(startTime), endTime.isEmpty() ? null : format.parse(endTime));
    }
}
