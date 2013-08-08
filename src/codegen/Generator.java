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

import bnf.JavaParser;
import bnf.Tree;

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
	 * @return the source folder where the bridge $.java logging utility is going to be copied.
	 */
	public abstract File getBridgeTargetPath();
	
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
	public void copyProjectWithDebugCode(boolean cleanTargetDir, String bridgeName) {
		this.bridgeName = bridgeName;
		long start = System.currentTimeMillis();
		if (cleanTargetDir) {
			FileUtil.delTree(getTargetPath());
		}
		visit(getPath(), JUST_VISIT_FILES);
		visit(getPath(), DO_TRANSFORMATIONS);
		getAnnotations().shutdown();
		copyBridge();
		System.out.println("Job done in " + (System.currentTimeMillis() - start) + " ms.");
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
	private String bridgeName;
	
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
			DebugCodeInserter debug = new DebugCodeInserter(new JavaParser().parse(f), getAnnotations(), bridgeName);
			Tree t = debug.run().getParseTree().tree;
			PrintWriter out = new PrintWriter(new FileWriter(getTarget(f, getTargetPath())));
			out.print(t);
			out.close();
			FileUtil.copyFile(f, new File(debug.getFileVersion().mappedPath));
			System.out.println("done.");
		} catch (StackOverflowError ex) {
			System.err.println("Stack overflow while parsing file " + f.getAbsolutePath());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
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
	
	public File getTarget(File f, File targetDir) {
		File target = new File(targetDir, f.getAbsolutePath().substring(getPath().getAbsolutePath().length()));		
		if (!target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
			throw new RuntimeException("Cannot create target path " + target.getParent());
		}
		return target;
	}
	
	private void copyBridge() {
		File dest = new File(getBridgeTargetPath(), bridgeName);
		dest.mkdirs();
		File destFile = new File(dest, bridgeName + ".java");
		FileUtil.copyFile(new File("src/$/$.java"), destFile);
		FileUtil.replaceAll(destFile, "$", bridgeName);
	}
	
}
