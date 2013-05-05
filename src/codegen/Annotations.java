/*
 * Annotation pool.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A pool for creating annotations. An annotation is a 5-int vector, the first one points to a file number
 * (which corresponds to a file name), the second and third -- the line and column number of the beginning,
 * the fourth and fifth -- the line and column number of the end.
 * An annotation is a long which addresses these 5-int vectors and has its 2 highest bits reserved for 
 * annotation type (scope begin, scope end, step, value of expr).
 */
public class Annotations {

	public enum Type {
		step     { @Override public long getMask() { return 0x0l; } },
		expr     { @Override public long getMask() { return 0x1l; } },
		scope    { @Override public long getMask() { return 0x2l; } },
		endscope { @Override public long getMask() { return 0x3l; } },
		var      { @Override public long getMask() { return 0x4l; } },
		vardecl  { @Override public long getMask() { return 0x5l; } },
		arg      { @Override public long getMask() { return 0x6l; } },
		obj      { @Override public long getMask() { return 0x7l; } },
		field    { @Override public long getMask() { return 0x8l; } },
		arr      { @Override public long getMask() { return 0x9l; } },
		index    { @Override public long getMask() { return 0x10l;} },
		;
		
		public abstract long getMask();
	}
	
	public static final int RESERVED_BITS = 4; // The number of bits in the low part of the long representing the annotation which are reserved for annotation type.
	
	private String path;
	private ArrayList<String> files = new ArrayList<String>();
	private byte[] buffer = new byte[5 * 1048576];
	private int pos;
	private long annotNo;
	private OutputStream out;
	private int[] linePos, columnPos;
	private boolean shutdown;
	
	public Annotations(String path) {
		this.path = path;
		try {
			File dir = new File(path);
			dir.mkdirs();
			File f = new File(dir, "annotations");
			out = new FileOutputStream(f);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public void newFile(String path) {
		try {
			linePos = new int[1024];
			columnPos = new int[1024];
			int pos = 0;
			BufferedReader inp = new BufferedReader(new FileReader(path));
			linePos[pos] = columnPos[pos] = 1;
			pos++;
			while (true) {
				String line = inp.readLine();
				if (line == null) {
					break;
				}
				for (int i = 0; i < line.length() + 1; i++) {
					if (pos >= linePos.length) {
						int[] copy = new int[linePos.length << 1];
						System.arraycopy(linePos, 0, copy, 0, linePos.length);
						linePos = copy;
						copy = new int[columnPos.length << 1];
						System.arraycopy(columnPos, 0, copy, 0, columnPos.length);
						columnPos = copy;
					}
					if (i < line.length()) {
						linePos[pos] = linePos[pos - 1];
						columnPos[pos] = columnPos[pos - 1] + 1;
					} else {
						linePos[pos] = linePos[pos - 1] + 1;
						columnPos[pos] = 1;
					}
					pos++;
				}
			}
			inp.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		files.add(path);
	}
	
	public long annotation(Type t, int startPos, int endPos) {
		if (files.size() == 0) {
			throw new RuntimeException("A file must be specified prior to making an annotation.");
		}
		if (shutdown) {
			throw new RuntimeException("The annotation pool is already shut down.");
		}
		writeInt(files.size() - 1);
		writeInt(linePos[startPos]);
		writeInt(columnPos[startPos]);
		writeInt(linePos[endPos]);
		writeInt(columnPos[endPos]);
		return t.getMask() | (annotNo += (1l << RESERVED_BITS));
	}
	
	private void writeInt(int n) {
		if (pos >= buffer.length) {
			try {
				out.write(buffer);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			Arrays.fill(buffer, (byte) 0);
			pos = 0;
		}
		for (int mask = 24; mask >= 0; mask -= 8) {
			buffer[pos++] = (byte) (n >> mask);
		}
	}
	
	public void shutdown() {
		if (shutdown) {
			return;
		}
		try {
			if (pos > 0) {
				try {
					out.write(buffer, 0, pos);
					out.close();
					PrintWriter pr = new PrintWriter(new File(new File(path), "files.txt"));
					for (String s: files) {
						pr.println(s);
					}
					pr.close();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		} finally {
			shutdown = true;
		}
	}
	
}
