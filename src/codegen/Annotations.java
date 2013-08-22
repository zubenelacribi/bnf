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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * A pool for creating annotations. An annotation is a 5-int vector, the first one ow which points to a file number
 * (which corresponds to a file name), the second and the third one -- the line and column number of the beginning,
 * the fourth and the fifth one -- the line and column number of the end.
 * An annotation is a long which addresses these 5-int vectors and has its 4 lowest bits reserved for 
 * annotation type (scope begin, scope end, step, value of expr).
 */
public class Annotations {

	/**
	 * Annotations in different parts of the code are of different type,
	 * e.g. annotations before executing a block expression is a 'step',
	 * annotations put in the beginnings of the methods to denote method
	 * entering are 'scopes', 'endscopes' mark the end of the methods
	 * respectively; 'var' means that a local variable has accepted a new value;
	 * 'arg' an argument passed to a method; 'expr' is the value of an
	 * expression; 'obj' and 'field' are used to mark the changing of a value of
	 * a field in a given object; 'arr' and 'index' are used to mark
	 * the changing of a value of an array element.
	 */
	public enum Type { step, expr, scope, endscope, var, arg, obj, field, arr, index, exception }
	
	/** The number of bits in the low part of the long representing the annotation which are reserved for annotation type. */
	public static final int RESERVED_BITS = 4;
	
	private static final String ANNOTATIONS_BINARY_FILE = "annotations";
	private static final String FILE_NAMES = "files.txt";
	private static final String IDENTIFIER_NAMES = "identifiers.txt";
	private static final int TUPLE_SIZE_IN_INTS = 5;
	private static final int TUPLE_SIZE_IN_BYTES = TUPLE_SIZE_IN_INTS * 4;
	
	private static final int TAB_WIDTH = 2;
	
	/**
	 * A file version contains the path to the original file and the time it was annotated,
	 * and the path to which this file version maps in the source cache.
	 */
	public class FileVersion {
		
		/** Original path. */
		public String path;
		
		/** File's modification time. */
		public Date modified;
		
		/** The file in the source cache to which this file modified on this date maps. */
		public String mappedPath;
		
		/** The number which is used in the binary annotations file to refer to this FileVersion. */
		public int fileNumber;
		
		/** Creates a FileVersion object by reading files.txt, each line of which corresponds to one FileVersion. */
		@SuppressWarnings("deprecation")
		public FileVersion(String recordLine) {
			String[] components = recordLine.split("\t");
			if (components.length != 3) {
				throw new RuntimeException("Invalid entry in " + FILE_NAMES + ": '" + recordLine + "'");
			}
			path = components[0];
			if (components[1].indexOf(' ') < 0) {
				throw new RuntimeException("Invaid date: '" + components[1] + "'");
			}
			String s = components[1].substring(components[1].lastIndexOf(' ') + 1);
			boolean isYear = false;
			if (s.length() == 4) {
				isYear = true;
				try {
					Integer.parseInt(s);
				} catch (NumberFormatException ex) {
					isYear = false;
				}
			}
			if (isYear) {
				components[1] = components[1].substring(0, components[1].lastIndexOf(' '));
			}
			if (components[1].startsWith("Mon") || components[1].startsWith("Tue") || components[1].startsWith("Wed") ||
					components[1].startsWith("Thu") || components[1].startsWith("Fri") || components[1].startsWith("Sat") || components[1].startsWith("Sun")) {
				components[1] = components[1].substring(3).trim();
			}
			if (components[1].indexOf(' ') < 0) {
				throw new RuntimeException("Invaid date: '" + components[1] + "'");
			}
			String t = components[1].substring(components[1].lastIndexOf(' ') + 1);
			boolean lettersOnly = true;
			for (int i = 0; i < t.length(); i++) {
				if (Character.toLowerCase(t.charAt(i)) < 'a' || Character.toLowerCase(t.charAt(i)) > 'z') {
					lettersOnly = false;
				}
			}
			if (lettersOnly) {
				components[1] = components[1].substring(0, components[1].lastIndexOf(' '));
			}
			components[1] = s + ' ' + components[1];
			modified = new Date(components[1]);
			mappedPath = components[2];
		}
		
		/** Creates a new FileVersion entry. The original path and mod time are stored and a new file mapping is worked out. */
		public FileVersion(File f) {
			try {
				path = f.getCanonicalPath();
				modified = new Date(f.lastModified());
				
				List<FileVersion> list = originalPathMap.get(path);
				File g = new File(gen.getSourceCache(), path.substring(gen.getPath().getCanonicalFile().getParentFile().getAbsolutePath().length()) + '.' + (list == null ? 1 : list.size() + 1));
				if (g.exists()) {
					if (!f.isFile()) {
						throw new RuntimeException("Not a file: " + g.getCanonicalPath());
					} else if (!f.delete()) {
						throw new RuntimeException("Cannot delete file: " + g.getCanonicalPath());
					}
				}
				mappedPath = g.getCanonicalPath();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		
		@Override
		public String toString() {
			return path + '\t' + modified + '\t' + mappedPath;
		}
		
	}
	
	/** The path to the annotations folder */
	private String path;
	
	/** A reference to the Generator which has created this Annotations object. Used to resolve paths. */
	private Generator gen;
	
	/** All recorded file versions. */
	private ArrayList<FileVersion> files = new ArrayList<FileVersion>();
	
	/** Mapping from the original file name to all of its FileVersions. */
	private HashMap<String, List<FileVersion>> originalPathMap = new HashMap<String, List<FileVersion>>();
	
	/** Mapping from one version of a file name to its FileVersion object. */
	private HashMap<String, FileVersion> mappedPathMap = new HashMap<String, FileVersion>();
	
	/** Read buffer for annotations. */
	private byte[] readBuffer = new byte[TUPLE_SIZE_IN_BYTES * 10000];
	
	/** The absolute position in the annotations file from where the read buffer will be filled. */
	private long absoluteReadPos = -1;
	
	/** Write buffer for annotations. */
	private byte[] writeBuffer = new byte[TUPLE_SIZE_IN_BYTES * 10000];
	
	/** Position in the write buffer for annotations. */
	private int posInWriteBuffer;
	
	/** The absolute position in the annotations file where the next write buffer will be placed. */
	private long absoluteWritePos;
	
	/** Current annotation number. */
	private long annotNo;
	
	/** The annotations binary file. */
	private RandomAccessFile annotFile;
	
	/** Mappings from a given string position to its line & columns number. */
	private int[] linePos, columnPos;
	
	/** A flag which indicates that we have finished working with this annotation pool. */
	private boolean shutdown;
	
	/**
	 * Constructs an annotation pool.
	 * @param gen the code generator which needs annotations.
	 * @param path the path where annotations are going to be stored.
	 */
	public Annotations(Generator gen, String path) {
		this.gen = gen;
		this.path = path;
		try {
			File dir = new File(path);
			dir.mkdirs();
			readFileNames();
			readIdentifiersFile();
			File f = new File(dir, ANNOTATIONS_BINARY_FILE);
			annotFile = new RandomAccessFile(f, "rw");
			if (f.length() > 0) {
				posInWriteBuffer = (int) (f.length() % writeBuffer.length);
				if ((posInWriteBuffer % TUPLE_SIZE_IN_BYTES) != 0) {
					throw new RuntimeException("Annotations file corrupted");
				}
				absoluteWritePos = f.length() - posInWriteBuffer;
				annotFile.seek(absoluteWritePos);
				annotFile.read(writeBuffer, 0, posInWriteBuffer);
				annotNo = f.length() / TUPLE_SIZE_IN_BYTES;
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/** Reads files.txt. */
	private void readFileNames() throws IOException {
		File f = new File(new File(path), FILE_NAMES);
		if (!f.exists()) {
			f = new File(new File(path), ANNOTATIONS_BINARY_FILE);
			if (f.exists()) {
				if (!f.isFile()) {
					throw new RuntimeException("Not a file: " + f.getAbsolutePath());
				}
				if (!f.delete()) {
					throw new RuntimeException("Cannot delete file: " + f.getAbsolutePath());
				}
			}
			return;
		}
		BufferedReader inp = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		while (true) {
			String line = inp.readLine();
			if (line == null) {
				break;
			}
			if (line.length() > 0) {
				FileVersion version = new FileVersion(line);
				addNewFileVersion(version);
			}
		}
		inp.close();
	}
	
	/**
	 * Adds to the database a new record.
	 * @param version a FileVersion object created while parsing the files.txt file or when annotating a file.
	 */
	private void addNewFileVersion(FileVersion version) {
		version.fileNumber = files.size();
		files.add(version);
		List<FileVersion> list = originalPathMap.get(version.path);
		if (list == null) {
			list = new ArrayList<FileVersion>();
			originalPathMap.put(version.path, list);
		}
		list.add(version);
		mappedPathMap.put(version.mappedPath, version);
	}
	
	/**
	 * @param f File to be annotated.
	 * @return 'true' if the file on disk has the same modified time as in the records in files.txt.
	 */
	public boolean isUpToDate(File f) {
		try {
			List<FileVersion> listVersions = originalPathMap.get(f.getCanonicalPath());
			if (listVersions != null) {
				for (FileVersion version: listVersions) {
					if (version.modified.getTime() > f.lastModified()) {
						return false;
					}
				}
				return true;
			}
			return false;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Creates a new entry in the file record.
	 * Reads the file in order to create the mappings from file position to
	 * line & column numbers. Used for source files.
	 * @param path the path to the source file.
	 * @return the new FileVersion entry.
	 */
	public FileVersion newSourceFile(String path) {
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
				for (int i = 0; i <= line.length(); i++) {
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
						columnPos[pos] = columnPos[pos - 1] + (i > 0 && line.charAt(i - 1) == '\t' ? TAB_WIDTH : 1);
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

		return newFile(path);
	}
	
	/**
	 * Creates a new entry in the file record.
	 * @param path the path to the source file.
	 * @return the new FileVersion entry.
	 */
	public FileVersion newFile(String path) {
		FileVersion version = new FileVersion(new File(path));
		addNewFileVersion(version);
		return version;
	}
	
	/**
	 * Creates a new annotation entry.
	 * @param t annotation type;
	 * @param startPos initial position in the file content;
	 * @param endPos end position in the file content.
	 * @return annotation code which is the annotation number mixed with the annotation type info.
	 */
	public long annotation(Type t, int startPos, int endPos) {
		if (files.size() == 0) {
			throw new RuntimeException("A file must be specified prior to making an annotation.");
		}
		if (shutdown) {
			throw new RuntimeException("The annotation pool is already shut down.");
		}
		writeInt(((files.size() - 1) << RESERVED_BITS) | t.ordinal());
		writeInt(linePos[startPos]);
		writeInt(columnPos[startPos]);
		writeInt(linePos[endPos]);
		writeInt(columnPos[endPos]);
		long encodedAnnotation = t.ordinal() | annotNo;
		annotNo += (1l << RESERVED_BITS);
		return encodedAnnotation;
	}
	
	/**
	 * @return the number of file entries.
	 */
	public int getNumberOfFileVersions() {
		return files.size();
	}
	
	/**
	 * @return a file version for a given number.
	 */
	public FileVersion getFileVersion(int number) {
		return files.get(number);
	}
	
	/**
	 * @return the FileVersion object for a given destination path (e.g. .sourcecache/proj/src/Foo.java.1).
	 */
	public FileVersion getOriginalPathForFile(String mappedFile) {
		return mappedPathMap.get(mappedFile);
	}
	
	/**
	 * Writes an integer in the write buffer.
	 */
	private void writeInt(int n) {
		if (posInWriteBuffer >= writeBuffer.length) {
			try {
				annotFile.seek(absoluteWritePos);
				annotFile.write(writeBuffer);
				absoluteWritePos += writeBuffer.length;
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			Arrays.fill(writeBuffer, (byte) 0);
			posInWriteBuffer = 0;
		}
		for (int mask = 24; mask >= 0; mask -= 8) {
			writeBuffer[posInWriteBuffer++] = (byte) (n >> mask);
		}
	}
	
	/**
	 * Returns the type of a given annotation code.
	 * @param annotation the annotation code as it appears in the generated source code.
	 * @return the corresponding annotation type.
	 */
	public Type getAnnotationType(long annotation) {
		return Type.values()[(int) (annotation & ((1 << RESERVED_BITS) - 1))];
	}
	
	/**
	 * Returns the annotation number through which the file version and position in it can be resolved.
	 * @param annotation the annotation code as it appears in the generates source code.
	 * @return the corresponding annotation number.
	 */
	public long getAnnotationNumber(long annotation) {
		return annotation >> RESERVED_BITS;
	}
	
	/**
	 * Returns the file number corresponding to the supplied annotation number.
	 * @param annotationNumber the number retrieved by invoking getAnnotationNumber().
	 * @return the corresponding file number.
	 */
	public int getFileNumber(long annotationNumber) {
		int pos = seek(annotationNumber);
		return readInt(pos) >> RESERVED_BITS;
	}
	
	/**
	 * The annotation type is encoded in the file number field in order
	 * to be possible to be retrieved by reading from the annotations file.
	 * @param annotationNumber a number between 0 and getCurrentAnnotationNumber().
	 * @return the annotation type read from the annotations file.
	 */
	public Type getAnnotationTypeFromFile(long annotationNumber) {
		int pos = seek(annotationNumber);
		return Type.values()[readInt(pos) & ((1 << RESERVED_BITS) - 1)];
	}
	
	/**
	 * Gets the file version object corresponding to the given annotation.
	 * @param annotation the encoded annotation as it appears in the generated source code.
	 * @return the FileVersion objects which this annotation refers.
	 */
	public FileVersion getFileVersionByAnnotation(long annotation) {
		int fileNumber = getFileNumber(getAnnotationNumber(annotation));
		if (fileNumber < 0 || fileNumber >= files.size()) {
			throw new RuntimeException("Annotation " + annotation + "  points to non-existing file number: " + fileNumber);
		}
		return files.get(fileNumber);
	}
	
	/**
	 * Gets the line number from which this annotation starts.
	 * @param annotationNumber a number between 0 and getCurrentAnnotationNumber().
	 * @return the start line number.
	 */
	public int getStartLineNumber(long annotationNumber) {
		return readInt(seek(annotationNumber) + 4);
	}
	
	/**
	 * Gets the column number from which this annotation starts.
	 * @param annotationNumber a number between 0 and getCurrentAnnotationNumber().
	 * @return the start column number.
	 */
	public int getStartColumnNumber(long annotationNumber) {
		return readInt(seek(annotationNumber) + 8);
	}
	
	/**
	 * Gets the line number to which this annotation ends.
	 * @param annotationNumber a number between 0 and getCurrentAnnotationNumber().
	 * @return the end line number.
	 */
	public int getEndLineNumber(long annotationNumber) {
		return readInt(seek(annotationNumber) + 12);
	}
	
	/**
	 * Gets the column number to which this annotation ends.
	 * @param annotationNumber a number between 0 and getCurrentAnnotationNumber().
	 * @return the end column number.
	 */
	public int getEndColumnNumber(long annotationNumber) {
		return readInt(seek(annotationNumber) + 16);
	}
	
	/**
	 * Seeks to the given annotation number.
	 * @param annotationNumber decoded annotation number through the invokation of gtAnnotationNumber().
	 * @return the position in the readBuffer where the annotation starts.
	 */
	private int seek(long annotationNumber) {
		long readPos = annotationNumber * TUPLE_SIZE_IN_BYTES / readBuffer.length;
		if (readPos != absoluteReadPos) {
			try {
				absoluteReadPos = readPos;
				annotFile.seek(absoluteReadPos);
				annotFile.read(readBuffer);
			} catch (IOException ex) {
				throw new RuntimeException("Canot read from annotation file: " + ex.getMessage());
			}
		}
		return (int) ((annotationNumber * TUPLE_SIZE_IN_BYTES) % readBuffer.length);
	}
	
	/**
	 * Reads an int from the read buffer.
	 * @param pos a position in the read buffer.
	 * @return the next 4 bytes packed as int.
	 */
	private int readInt(int pos) {
		int n = 0;
		for (int i = 0; i < 4; i++) {
			n <<= 8;
			n |= readBuffer[pos++] & 0xff;
		}
		return n;
	}
	
	/**
	 * @return the next number which will be associated to an annotation.
	 */
	public long getCurrentAnnotationNumber() {
		return annotNo;
	}
	
	/**
	 * This map is used to give IDs to identifiers, so that the generated code
	 * will inspect variables and class fields by their IDs, not by their name,
	 * which will improve performance. The identifiers' IDs are global, i.e.
	 * all identifiers with the same name will have the same ID.
	 */
	private HashMap<String, Integer> identifierMap = new HashMap<String, Integer>();
	
	/**
	 * Gives the corresponding ID of the specified identifier. If the identifier
	 * is not in the identifierMap yet then it will be given a new ID.
	 * @param identifier a variable/field name.
	 * @return the ID for the given identifier; if the identifier hasn't been
	 *   met then a new ID will be created.
	 */
	public int getIdentifierId(String identifier) {
		if (identifierMap.get(identifier) == null) {
			identifierMap.put(identifier, identifierMap.size());
		}
		return identifierMap.get(identifier);
	}
	
	/**
	 * Invoked by the constructor in order to initialize the identifier map.
	 */
	private void readIdentifiersFile() {
		try {
			File f = new File(new File(path), IDENTIFIER_NAMES);
			if (!f.exists()) {
				return;
			}
			BufferedReader buff = new BufferedReader(new FileReader(f));
			while (true) {
				String line = buff.readLine();
				if (line == null) {
					break;
				}
				getIdentifierId(line); // Creates a new identifier mapping since the identifier map should be empty.
			}
			buff.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Shuts down the annotation pool. The info in the write buffer (if any) will be flushed to disk.
	 */
	public void shutdown() {
		if (shutdown) {
			return;
		}
		try {
			if (posInWriteBuffer > 0) {
				try {
					annotFile.seek(absoluteWritePos);
					annotFile.write(writeBuffer, 0, posInWriteBuffer);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		} finally {
			shutdown = true;
			try {
				// Close annotations file.
				annotFile.close();
				
				// Write file names to files.txt.
				PrintWriter pr = new PrintWriter(new File(new File(path), FILE_NAMES));
				for (FileVersion version: files) {
					pr.println(version);
				}
				pr.close();
				
				// Write the identifier names to identifiers.txt.
				pr = new PrintWriter(new File(new File(path), IDENTIFIER_NAMES));
				for (int i = 0; i < identifierMap.size(); i++) {
					pr.println(identifierMap.get(i));
				}
				pr.close();
				
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
	
}
