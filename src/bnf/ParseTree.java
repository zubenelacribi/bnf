/*
 * Parse tree which adds the file name to the tree obtained from a parser.
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

package bnf;

/**
 * 
 * Extends Tree by adding the path to the file which has been parsed.
 * 
 * @author Zuben El Acribi
 *
 */
public class ParseTree {

	/**
	 * A parse tree obtained by Parser.
	 */
	public Tree tree;
	
	/**
	 * The path to the file which has been parsed.
	 */
	public String filename;
	
	/**
	 * Constructs a parse tree.
	 * @param filename the path to the file which has been parsed.
	 * @param tree the parse tree obtained by a Parser.
	 */
	public ParseTree(String filename, Tree tree) {
		this.filename = filename;
		this.tree = tree;
	}
	
	@Override
	public String toString() {
		return filename + '\n' + tree;
	}
	
}
