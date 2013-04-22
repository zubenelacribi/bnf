/*
 * General purpose parser based on the BNF definition parser.
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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import util.FileUtil;

public abstract class Parser {

	private static class StackElem {

		public Tree t;
		public int begin, end;

		public StackElem(Tree t, int begin, int end) {
			this.t = t;
			this.begin = begin;
			this.end = end;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof StackElem) {
				StackElem s = (StackElem) o;
				return s.t.equals(t) && s.begin == begin && s.end == end;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return t.hashCode() + begin * 1007 + end * 10001;
		}

	}

	HashMap<String, Tree> definitions = new HashMap<String, Tree>();
	int[] linePositions;
	int[] columnPositions;

	HashSet<StackElem> stack;

	public Parser() {
		initialize();
		checkForMissingDefinitions();
	}

	public abstract void initialize();
	
	protected abstract boolean keyword(String s);

	private void checkForMissingDefinitions() {
		Set<String> missing = new HashSet<String>();
		for (String s : definitions.keySet()) {
			Tree t = definitions.get(s);
			if (t != null) { // Definitions with null body are parser-specific.
				missing.addAll(checkTreeForMissingDefinitions(t));
			}
		}
		if (missing.size() > 0) {
			StringBuffer msg = new StringBuffer("The following definitions are missing");
			boolean comma = false;
			for (String s : missing) {
				if (comma) {
					msg.append(", ");
				} else {
					msg.append(": ");
				}
				msg.append(s);
				comma = true;
			}
			throw new RuntimeException(msg.toString());
		}
	}

	private Set<String> checkTreeForMissingDefinitions(Tree t) {
		Set<String> missing = new HashSet<String>();
		if (t.type == NodeType.identifier) {
			if (!definitions.containsKey(t.node)) {
				missing.add(t.node);
			}
		} else if (t.type == NodeType.choice || t.type == NodeType.sequence ||
				t.type == NodeType.optional || t.type == NodeType.repetition) {
			for (Tree b : t.branches) {
				if (b != null) {
					missing.addAll(checkTreeForMissingDefinitions(b));
				}
			}
		}
		return missing;
	}

	public ParseTree parse(String def, File f) throws ParseException {
		return new ParseTree(f.getAbsolutePath(), parse(def, FileUtil.readFile(f)));
	}
	
	public Tree parse(String def, String s) throws ParseException {
		Tree t = definitions.get(def);
		if (t == null) {
			throw new RuntimeException("Unknown definition: " + def);
		}
		calcPos(s);
		stack = new HashSet<StackElem>();
		Tree res = parse(t, s, 0, s.length());
		int end = skipWhiteSpace(s, res.end, s.length());
		res.node = s.substring(res.begin, res.end);
		stack = null;
		if (end != s.length()) {
			if (end == 0) {
				throw new ParseException("Unrecognized");
			} else {
				throw new ParseException("Recognized up to" + pos(res.end) + " because the parser didn't manage to parse" + pos(maxParsedChar));
			}
		} else {
			return res;
		}
	}

	private void calcPos(String s) {
		linePositions = new int[s.length()];
		columnPositions = new int[s.length()];
		int line = 1;
		int column = 1;
		for (int i = 0; i < s.length(); i++) {
			linePositions[i] = line;
			columnPositions[i] = column;
			if (s.charAt(i) == '\n') {
				line++;
				column = 1;
			} else if (s.charAt(i) == '\t') {
				column += 2;
			} else {
				column++;
			}
		}
	}

	protected abstract Tree extension(Tree t, String s, int begin, int end) throws ParseException;
	
	protected int skipWhiteSpace(String s, int begin, int end) throws ParseException {
		while (begin < end && Character.isWhitespace(s.charAt(begin))) {
			begin++;
		}
		return begin;
	}
	
	protected int maxParsedChar = 0;
	
	protected Tree parse(Tree t, String s, int begin, int end) throws ParseException {
		StackElem st = new StackElem(t, begin, end);
		if (stack.contains(st)) {
			throw new ParseException("Infinite recursion");
		}
		stack.add(st);

		try {
			if (t.type == NodeType.token_keyword) {
				begin = skipWhiteSpace(s, begin, end);
				if (begin >= end) {
					throw new ParseException("begin(" + begin + ") > end(" + end + ")");
				}
				if (s.charAt(begin) != '\'') {
					throw new ParseException("Single quote expected at" + pos(begin));
				}
				for (int i = begin + 1; i < end; i++) {
					char ch = s.charAt(i);
					if (ch == '\\') {
						i++;
					} else if (ch == '\'') {
						maxParsed(i + 1);
						return new Tree(s, begin, i + 1, t);
					}
				}
				throw new ParseException("Closing single quote expected at" + pos(end - 1));

			} else if (t.type == NodeType.identifier_keyword) {

				begin = skipWhiteSpace(s, begin, end);
				if (begin >= end) {
					throw new ParseException("begin(" + begin + ") > end(" + end + ")");
				}
				for (int i = begin; i < end; i++) {
					if (!Character.isJavaIdentifierPart(s.charAt(i))) {
						if (i > begin) {
							if (!keyword(s.substring(begin, i)) && Character.isJavaIdentifierStart(s.charAt(begin))) {
								maxParsed(i);
								return new Tree(s, begin, i, t);
							} else {
								throw new ParseException("Identifier expected at" + pos(begin));
							}
						} else {
							throw new ParseException("Identifier expected at" + pos(begin));
						}
					}
				}
				return new Tree(s, begin, end, t);

			} else if (t.type == NodeType.new_line_keyword) {
				if (begin >= end) {
					throw new ParseException("begin(" + begin + ") > end(" + end + ")");
				}
				if (s.charAt(begin) == '\n') {
					maxParsed(begin + 1);
					return new Tree(s, begin, begin + 1, t);
				} else {
					throw new ParseException("New line expected at" + pos(begin));
				}

			} else if (t.type == NodeType.token) {

				begin = skipWhiteSpace(s, begin, end);
				if (begin >= end) {
					throw new ParseException("begin(" + begin + ") > end(" + end + ")");
				}
				String token = escape(t.node.substring(1, t.node.length() - 1));
				if (end - begin >= token.length() && s.substring(begin, begin + token.length()).equals(token)) {
					if (Character.isLetter(token.charAt(token.length() - 1)) && begin + token.length() < s.length() &&
							Character.isLetter(s.charAt(begin + token.length()))) {
						throw new ParseException("May not jump to the middle of a token at" + pos(begin + token.length()));
					}
					maxParsed(begin + token.length());
					boolean ok = true;
					if (keyword(s.substring(begin, begin + token.length()))) {
						ok = begin + token.length() >= end ||
								(begin + token.length() < end && !Character.isJavaIdentifierPart(s.charAt(begin + token.length())));
					}
					if (ok) {
						return new Tree(s, begin, begin + token.length(), t);
					}
				}
				throw new ParseException("Token '" + token + "' expected at" + pos(begin));

			} else if (t.type == NodeType.identifier) {

				Tree def = definitions.get(t.node);
				if (def == null) {
					begin = skipWhiteSpace(s, begin, end);
					if (begin >= end) {
						throw new ParseException("begin(" + begin + ") > end(" + end + ")");
					}
					Tree res = extension(t, s, begin, end);
					if (res == null) {
						throw new ParseException("Unknown definition: " + t.node);
					} else {
						return res;
					}
				}
				return parse(def, s, begin, end);

			} else if (t.type == NodeType.sequence) {

				Tree res = new Tree(NodeType.sequence, t);
				for (Tree b : t.branches) {
					Tree u = parse(b, s, begin, end);
					begin = u.end;
					res.addBranch(u);
				}
				return res;

			} else if (t.type == NodeType.choice) {

				Tree res = new Tree(NodeType.choice, t);
				StringBuffer accumulatedError = new StringBuffer();
				for (Tree b : t.branches) {
					try {
						Tree u = parse(b, s, begin, end);
						res.addBranch(u);
						return res;
					} catch (ParseException ex) {
						accumulatedError.append(ex.getMessage());
						accumulatedError.append('\n');
						res.addBranch(null);
					}
				}
				throw new ParseException(accumulatedError.toString());

			} else if (t.type == NodeType.optional) {

				try {
					return parse(t.branches.get(0), s, begin, end);
				} catch (ParseException ex) {
					return new Tree(s, begin, begin, t);
				}

			} else if (t.type == NodeType.repetition) {

				Tree res = new Tree(s, begin, begin, t);
				res.type = NodeType.repetition;
				while (true) {
					try {
						Tree u = parse(t.branches.get(0), s, begin, end);
						begin = u.end;
						res.addBranch(u);
					} catch (ParseException ex) {
						break;
					}
				}
				return res;

			} else {
				throw new RuntimeException("Unknown tree type: " + t.type);
			}

		} finally {
			stack.remove(st);
		}
	}
	
	private void maxParsed(int pos) {
		if (pos > maxParsedChar) {
			maxParsedChar = pos;
		}
	}

	protected String pos(int strPos) {
		return " line " + linePositions[strPos] + ", column " + columnPositions[strPos];
	}

	private String escape(String s) {
		if (s.indexOf('\\') < 0) {
			return s;
		}
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (ch == '\\') {
				ch = s.charAt(++i);
			}
			buff.append(ch);
		}
		return buff.toString();
	}

}
