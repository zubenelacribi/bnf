/*
 * Converter that instruments the source code of a whole Java project.
 * Copyright (C) 2013  Zuben El Acribi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package codegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import codegen.Annotations.FileVersion;

import bnf.JavaParser;
import bnf.ParseTree;
import bnf.ParserInitializationException;

import util.FileUtil;

/**
 * 
 * A utility that transforms all source file from a Java project.
 * The type of the transformation depends in the implementation,
 * e.g. the DebugCodeInserterGenerator transforms a whole Java project
 * into a Java project with inserted debug code which can be
 * used for debugging in the forward and the backward direction of
 * the instruction flow (time machine).
 * 
 * @author Zuben El Acribi
 *
 */
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
	 * Initializes the generator.
	 */
	public abstract void initialize();
	
	/**
	 * Performs the job for which this generator is intended.
	 * @param p a parsed source file. The annotation will be written in this object.
	 */
	public abstract void doJob(ParseTree p);
	
	/**
	 * Shuts down the generator.
	 */
	public abstract void shutdown();
	
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
		return f.isDirectory() && (f.getName().equals("bin") || f.getName().equals("build") || f.getName().startsWith("."));
	}

// ------------------------------------------------------------------------------------------------
// Main method.
// ------------------------------------------------------------------------------------------------
	
	private static final boolean JUST_VISIT_FILES = false;
	private static final boolean DO_TRANSFORMATIONS = true;
	
	/**
	 * @param cleanTargetDir deletes the target directory to startup cleanly.
	 */
	public void transformCode(boolean cleanTargetDir) {
		long start = System.currentTimeMillis();
		if (cleanTargetDir) {
			FileUtil.delTree(getTargetPath());
		}
		initialize();
		visit(getPath(), JUST_VISIT_FILES);
		visit(getPath(), DO_TRANSFORMATIONS);
		shutdown();
		getAnnotations().shutdown();
		System.out.println("Job done in " + (System.currentTimeMillis() - start) + " ms.");
	}

	/**
	 * @return the annotation pool which will be used by the current generator.
	 */
	public Annotations getAnnotations() {
		if (ann == null) {
			try {
				ann = new Annotations(this, getSourceCache().getCanonicalPath() + File.separatorChar + ".annotations");
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		return ann;
	}
	
// ------------------------------------------------------------------------------------------------
// Private fields & methods.
// ------------------------------------------------------------------------------------------------
	
	private Annotations ann;
	private int filesToTransform;
	private int currentFileToTransform;
	private int filesToCopy;
	private int currentFileToCopy;
	private long bytesToCopy;
	private long copiedBytes;
	private JavaParser parser;

	/**
	 * @return the Java parser which will be used by the current pool.
	 */
	private JavaParser getJavaParser() {
		if (parser == null) {
			try {
				parser = new JavaParser();
			} catch (ParserInitializationException ex) {
				throw new RuntimeException(ex);
			}
		}
		return parser;
	}

	/**
	 * Visits all the file recursively in the given directory and determines whether
	 * they will be transformed, copied or skipped.
	 * @param dir a directory.
	 * @param doJob 'true' to do the code transformation job or 'false' in order just to
	 *   count files.
	 */
	private void visit(File dir, boolean doJob) {
		if (!dir.exists()) {
			throw new RuntimeException("Path " + dir.getAbsolutePath() + " does not exist");
		}
		if (!dir.isDirectory()) {
			throw new RuntimeException("Path " + dir.getAbsolutePath() + " is not directory");
		}
		for (File entry: dir.listFiles()) {
			if (entry.isDirectory()) {
				if (accept(entry) || !refuse(entry)) {
					visit(entry, doJob);
				}
			} else if (entry.isFile() && !entry.isHidden()) {
				if (accept(entry)) {
					transform(entry, doJob);
				} else if (!refuse(entry)) {
					copyFile(entry, doJob);
				}
			}
		}
	}
	
	/**
	 * Transforms the given Java source file.
	 * @param f a Java source file.
	 * @param doJob 'true' to do the code transformation job or 'false' in order just to
	 *   count up.
	 */
	private void transform(File f, boolean doJob) {

		if (getAnnotations().isUpToDate(f)) {
			return;
		}

		if (!doJob) {
			filesToTransform++;
			return;
		}
		
		try {
			System.out.print("Transforming file " + f.getCanonicalPath() + "(" + (++currentFileToTransform) + "/" + filesToTransform + ")... ");
			FileVersion file = ann.newSourceFile(f.getCanonicalPath());
			ParseTree p = getJavaParser().parse(f);
			doJob(p);
			PrintWriter out = new PrintWriter(new FileWriter(getTarget(f, getTargetPath())));
			out.print(p.tree);
			out.close();
			FileUtil.copyFile(f, new File(file.mappedPath));
			System.out.println("done.");
		} catch (StackOverflowError ex) {
			System.err.println("Stack overflow while parsing file " + f.getAbsolutePath());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Copies the given file.
	 * @param f a which is not accepted as a source file but not refused to be copied.
	 * @param doJob 'true' to copy the file or 'false' in order just to count up.
	 */
	private void copyFile(File f, boolean doJob) {

		if (getAnnotations().isUpToDate(f)) {
			return;
		}

		if (!doJob) {
			filesToCopy++;
			bytesToCopy += f.length();
			return;
		}
		try {
			System.out.print("Copying file " + f.getCanonicalPath() + "(" + (++currentFileToCopy) + "/" + filesToCopy + ", bytes " + copiedBytes + "/" + bytesToCopy + ")... ");
			FileUtil.copyFile(f, getTarget(f, getTargetPath()));
			copiedBytes += f.length();
			ann.newFile(f.getCanonicalPath());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		System.out.println("done.");
	}
	
	/**
	 * Transforms the given path into relative to the source path and
	 * then the relative path is transform to an absolute path using the
	 * given target directory. The missing subdirectories will be created.
	 * @param f a file from the source path.
	 * @param targetDir the target path.
	 * @return a file from the target path; the missing subdirectories in
	 *   the target path will be created.
	 */
	private File getTarget(File f, File targetDir) {
		File target = new File(targetDir, f.getAbsolutePath().substring(getPath().getAbsolutePath().length()));		
		if (!target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
			throw new RuntimeException("Cannot create target path " + target.getParent());
		}
		return target;
	}
	
}
