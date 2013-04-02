package codegen;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import bnf.JavaParser;
import bnf.BnfDefParser.Tree;

import util.FileUtil;

public abstract class Generator {

// ------------------------------------------------------------------------------------------------
// Methods which are definable and changeable by the project.
// ------------------------------------------------------------------------------------------------
	
	/**
	 * @return path to the Java project.
	 */
	public abstract File getPath();
	
	/**
	 * @return where the Java project with the debug code will be copied.
	 */
	public abstract File getTargetPath();
	
	/**
	 * @return the path where annotations and the current state of the sources will be kept.
	 *   These sources will be used when debugging.
	 */
	public File getSourceCache() {
		return new File("../.sourcecache");
	}
	
	/**
	 * @return 'true' if the file is to be transformed. If 'false' and refuse(f) is also 'false'
	 *   then the file is simply copied.
	 */
	public boolean accept(File f) {
		return f.getName().endsWith(".java");
	}
	
	/**
	 * @return 'true' if the file is not to be copied / the directory is not to be followed.
	 */
	public boolean refuse(File f) {
		return f.isDirectory() && (f.getName().equals("bin") || f.getName().equals("build"));
	}
	
// ------------------------------------------------------------------------------------------------
// Main method.
// ------------------------------------------------------------------------------------------------
	
	public void copyProjectWithDebugCode() {
		visit(getPath());
		getAnnotations().shutdown();
	}

// ------------------------------------------------------------------------------------------------
// Private methods.
// ------------------------------------------------------------------------------------------------
	
	private Annotations ann;
	
	private Annotations getAnnotations() {
		if (ann == null) {
			ann = new Annotations(getSourceCache().getAbsolutePath() + File.separatorChar + ".annotations");
		}
		return ann;
	}
	
	private void visit(File dir) {
		if (!dir.exists()) {
			throw new RuntimeException("Path " + dir.getAbsolutePath() + " does not exist");
		}
		if (!dir.isDirectory()) {
			throw new RuntimeException("Path " + dir.getAbsolutePath() + " is not directory");
		}
		for (File entry: dir.listFiles()) {
			if (entry.isDirectory()) {
				if (accept(entry) || !refuse(entry)) {
					visit(entry);
				}
			} else if (entry.isFile() && !entry.isHidden()) {
				if (accept(entry)) {
					transform(entry);
				} else if (!refuse(entry)) {
					copyFile(entry);
				}
			}
		}
	}
	
	private void transform(File f) {
		try {
			Tree t = new DebugCodeInserter(new JavaParser().parse("CompilationUnit", f), ann).run().getParseTree().tree;
			PrintWriter out = new PrintWriter(new FileWriter(getTarget(f, getTargetPath())));
			out.print(t);
			out.close();
			File target = getTarget(f, getSourceCache());
			FileUtil.copyFile(f, target);
			serializeTree(t, new File(target.getParentFile(), target.getName() + ".tree"));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private void copyFile(File f) {
		FileUtil.copyFile(f, getTarget(f, getTargetPath()));
	}
	
	private File getTarget(File f, File targetDir) {
		File target = new File(targetDir, f.getAbsolutePath().substring(getPath().getAbsolutePath().length()));		
		if (!target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
			throw new RuntimeException("Cannot create target path " + target.getParent());
		}
		return target;
	}
	
	private void serializeTree(Tree t, File f) {
		
	}
	
}
