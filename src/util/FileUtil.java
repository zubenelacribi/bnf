/*
 * Some frequently used file utilities.
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

package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

	public static String readFile(String filename) {
		return readFile(new File(filename));
	}
	
	public static String readFile(File f) {
		try {
			BufferedReader inp = new BufferedReader(new FileReader(f));
			StringBuffer buff = new StringBuffer();
			while (true) {
				String line = inp.readLine();
				if (line == null) {
					break;
				}
				buff.append(line);
				buff.append('\n');
			}
			inp.close();
			return buff.toString();
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
			return null;
		}
	}
	
	public static void copyFile(File src, File dest) {
		try {
			byte[] buffer = new byte[1048576];
			InputStream inp = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest);
			while (true) {
				int bytesRead = inp.read(buffer);
				if (bytesRead <= 0) {
					break;
				}
				out.write(buffer, 0, bytesRead);
			}
			out.close();
			inp.close();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
