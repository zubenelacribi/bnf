package codegen;

import java.io.File;

import util.FileUtil;

import bnf.ParseTree;

public abstract class DebugCodeInserterGenerator extends Generator {
	
// ------------------------------------------------------------------------------------------------
// Abstract methods.
// ------------------------------------------------------------------------------------------------
	
	/**
	 * @return the source folder where the bridge $.java logging utility is going to be copied.
	 */
	public abstract File getBridgeTargetPath();

// ------------------------------------------------------------------------------------------------
// Public methods.
// ------------------------------------------------------------------------------------------------
	
	/**
	 * @return the name of the logging utility. If the name is X then
	 *   in the source code then most of the generated logging methods will have
	 *   name of X.X.X -- the package, class and method names will be the same X.<br/>
	 *   The bridge name is $ by default.
	 */
	public String getBridgeName() {
		return "$";
	}
	
	@Override
	public void initialize() {
	}
	
	@Override
	public void doJob(ParseTree p) {
		new DebugCodeInserter(p, getAnnotations(), getBridgeName()).run();
	}

	@Override
	public void shutdown() {
		copyBridge();
	}
	
// ------------------------------------------------------------------------------------------------
// Private methods.
// ------------------------------------------------------------------------------------------------
	
	private void copyBridge() {
		File dest = new File(getBridgeTargetPath(), getBridgeName());
		dest.mkdirs();
		File destFile = new File(dest, getBridgeName() + ".java");
		FileUtil.copyFile(new File("src/$/$.java"), destFile);
		FileUtil.replaceAll(destFile, "$", getBridgeName());
	}
	
}
