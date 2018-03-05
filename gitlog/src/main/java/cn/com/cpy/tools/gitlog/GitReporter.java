package cn.com.cpy.tools.gitlog;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GitReporter {
	private static List<DiffEntry> listDiff(Git git, ObjectId oldCommit, ObjectId newCommit) throws Exception {
		Repository repository = git.getRepository();
		List<DiffEntry> diffs = git.diff().setOldTree(prepareTreeParser(repository, oldCommit))
				.setNewTree(prepareTreeParser(repository, newCommit)).call();

		return diffs;
	}

	private static AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
		RevWalk walk = new RevWalk(repository);
		RevCommit commit = walk.parseCommit(objectId);
		RevTree tree = walk.parseTree(commit.getTree().getId());

		CanonicalTreeParser treeParser = new CanonicalTreeParser();
		try (ObjectReader reader = repository.newObjectReader()) {
			treeParser.reset(reader, tree.getId());
		}
		walk.dispose();

		return treeParser;
	}

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

	private static String getProject(String path) throws Exception {
		File file = new File(path);
		if (file.exists()) {
			File parent;
			if (file.isDirectory()) {
				parent = file;
			} else {
				parent = file.getParentFile();
			}
			while (parent != null) {
				File projectFile = new File(parent, ".project");
				if (projectFile.exists()) {
					return getProjectName(projectFile);
				}
				parent = parent.getParentFile();
			}
		} else {
			// 文件不存在，就找其父亲
			int lastIndex = path.lastIndexOf('/');
			if (lastIndex != -1) {
				return getProject(path.substring(0, lastIndex));
			}
		}
		return "<" + path + ">";
	}

	private static String getProject(String repositoryDir, String path) throws Exception {
		return getProject(repositoryDir + "/" + path);
	}

	private static List<RevCommit> getCommits(Git git, ObjectId start, ObjectId end) throws Exception {
		List<RevCommit> commits = new LinkedList<>();
		Iterator<RevCommit> it = git.log().addRange(start, end).call().iterator();
		while (it.hasNext()) {
			commits.add(0, it.next());
		}
		return commits;
	}

	private static Set<String> getProject(String repositoryDir, List<DiffEntry> diffs) throws Exception {
		Set<String> projects = new HashSet<>();

		// /dev/null
		for (DiffEntry diff : diffs) {
			String oldPath = diff.getOldPath();
			String newPath = diff.getNewPath();
			ChangeType ct = diff.getChangeType();

			if (ChangeType.ADD.equals(ct) || ChangeType.MODIFY.equals(ct)) {
				projects.add(getProject(repositoryDir, newPath));
			} else if (ChangeType.DELETE.equals(ct)) {
				projects.add(getProject(repositoryDir, oldPath));
			} else if (ChangeType.COPY.equals(ct) || ChangeType.RENAME.equals(ct)) {
				projects.add(getProject(repositoryDir, oldPath));
				projects.add(getProject(repositoryDir, newPath));
			}
		}

		return projects;
	}

	private static Map<String, List<RevCommit>> getProjectCommitsMap(Git git, String repositoryDir,
			List<RevCommit> commits) throws Exception {
		Map<String, List<RevCommit>> projectCommitsMap = new HashMap<>();

		Iterator<RevCommit> it = commits.iterator();
		if (it.hasNext()) {
			RevCommit lastCommit = it.next();

			while (it.hasNext()) {
				RevCommit curCommit = it.next();

				List<DiffEntry> diffs = listDiff(git, lastCommit, curCommit);
				Set<String> projects = getProject(repositoryDir, diffs);

				for (String project : projects) {
					List<RevCommit> projectCommits = projectCommitsMap.get(project);
					if (projectCommits == null) {
						projectCommits = new LinkedList<>();
						projectCommitsMap.put(project, projectCommits);
					}
					projectCommits.add(curCommit);
				}

				lastCommit = curCommit;
			}
		}

		return projectCommitsMap;
	}

	private static void dump(String repositoryDir, String start, String end) throws Exception {
		String gitDir = repositoryDir + "/.git";

		FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
		Repository repository = repositoryBuilder.setGitDir(new File(gitDir)).readEnvironment().findGitDir()
				.setMustExist(true).build();

		repository.getDirectory();

		Git git = new Git(repository);

		ObjectId startCommitId = repository.resolve(start);
		ObjectId endCommitId = repository.resolve(end);

		List<RevCommit> commits = getCommits(git, startCommitId, endCommitId);
		Map<String, List<RevCommit>> projectCommitsMap = getProjectCommitsMap(git, repositoryDir, commits);

		for (RevCommit commit : commits) {
			System.out.println(commit.getId().abbreviate(8).name() + "  " + commit.getShortMessage());
		}

		System.out.println();

		List<String> projects = new ArrayList<>();
		projects.addAll(projectCommitsMap.keySet());
		Collections.sort(projects);

		for (String project : projects) {
			System.out.println();
			System.out.println(project);

			List<RevCommit> projectCommits = projectCommitsMap.get(project);
			for (RevCommit commit : projectCommits) {
				System.out.println("\t" + commit.getId().abbreviate(8).name() + "  " + commit.getShortMessage());
			}
		}
	}

	public static void main(String[] args) throws Exception {
		String abRepositoryDir = "D:\\working\\project\\AMEBA_platform\\master\\ab";
		String abStart = "96520c";
		String abEnd = "a995f2";

		System.out.println("=== AB ===");
		dump(abRepositoryDir, abStart, abEnd);

		String adoreRepositoryDir = "D:\\working\\project\\AMEBA_platform\\master\\adore";
		String adoreStart = "4b9ef2";
		String adoreEnd = "38a54c";

		System.out.println();
		System.out.println("=== Adore ===");

		dump(adoreRepositoryDir, adoreStart, adoreEnd);
	}
}
