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
// Constructor.
// ------------------------------------------------------------------------------------------------

	public Generator() {
		long start = System.currentTimeMillis();
		copyProjectWithDebugCode();
		System.out.println("Job done in " + (System.currentTimeMillis() - start) + " ms.");
	}
	
// ------------------------------------------------------------------------------------------------
// Main method.
// ------------------------------------------------------------------------------------------------
	
	public void copyProjectWithDebugCode() {
		visit(getPath(), false);
		visit(getPath(), true);
		getAnnotations().shutdown();
	}

// ------------------------------------------------------------------------------------------------
// Private methods.
// ------------------------------------------------------------------------------------------------
	
	private Annotations ann;
	private int filesToTransform;
	private int currentFileToTransform;
	private int filesToCopy;
	private int currentFileToCopy;
	private long bytesToCopy;
	private long copiedBytes;
	
	private Annotations getAnnotations() {
		if (ann == null) {
			ann = new Annotations(getSourceCache().getAbsolutePath() + File.separatorChar + ".annotations");
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
		
		if (!doJob) {
			filesToTransform++;
			return;
		}
		
		try {
			System.out.print("Transforming file " + f.getAbsolutePath() + "(" + (++currentFileToTransform) + "/" + filesToTransform + ")... ");
			Tree t = new DebugCodeInserter(new JavaParser().parse("CompilationUnit", f), getAnnotations()).run().getParseTree().tree;
			PrintWriter out = new PrintWriter(new FileWriter(getTarget(f, getTargetPath())));
			out.print(t);
			out.close();
			File target = getTarget(f, getSourceCache());
			FileUtil.copyFile(f, target);
			serializeTree(t, new File(target.getParentFile(), target.getName() + ".tree"));
			System.out.println("done.");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private void copyFile(File f, boolean doJob) {
		if (!doJob) {
			filesToCopy++;
			bytesToCopy += f.length();
			return;
		}
		System.out.print("Copying file " + f.getAbsolutePath() + "(" + (++currentFileToCopy) + "/" + filesToCopy + ", bytes " + copiedBytes + "/" + bytesToCopy + ")... ");
		FileUtil.copyFile(f, getTarget(f, getTargetPath()));
		copiedBytes += f.length();
		System.out.println("done.");
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
