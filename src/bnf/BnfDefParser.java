/*
 * Simple BNF definition parser.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a simple Backus-Naur Form parser that parses BNF definitions
 * and builds a parse tree which provides a structured view over the
 * parsed text. The parse tree is then supplied to Parser which then
 * uses these definitions to parse anything described with the
 * grammar given to this BNF parser, i.e. this is a specialized BNF
 * definition parser while Parser is a general purpose parser that
 * uses definitions provided in the form of parse trees by this
 * BNF definition parser.
 * 
 * Every definition consists of:
 * 1. Unique name, consisting of alphanumeric ASCII symbols.
 * 2. Colon ':' immediately after the definition identifier.
 * 3. Any expression that uses tokens (terminals, these are surrounded
 *    by single quotes), identifiers (refer to BNF definitions),
 *    round brackets (groups expressions), square brackets (optional
 *    expressions), curly brackets (repetition of an expression 0 or
 *    more times) and the synmbol pipe '|' that means choice.
 * 
 * Expressions are choice of expressions or sequences of groups,
 * optionals, repetitions, tokens and identifiers.
 * 
 * @author Zuben El Acribi
 *
 */
public class BnfDefParser {

	public enum Type { token, identifier, sequence, choice, token_keyword, identifier_keyword, new_line_keyword, optional, repetition }

	public static class Tree {

		public String s;
		public int begin, end;
		public String node;
		public Type type;
		public Tree def;
		public Tree parent;
		public String prefix, suffix; // This node may be surrounded by strings like opening and closing bracket.
		public boolean hide; // If 'true' then this node is hidden on toString().

		public ArrayList<Tree> branches = new ArrayList<Tree>();

		public Tree(Type type, Tree annotation) {
			this.type = type;
			this.begin = this.end = -1;
			this.def = annotation;
		}

		public Tree(String s, int begin, int end, Tree annotation) {
			this.s = s;
			this.begin = begin;
			this.end = end;
			this.node = s.substring(begin, end);
			this.def = annotation;

			if (node.length() == 0 || node.startsWith("'") ||
					(annotation != null && (annotation.type == Type.token || annotation.type == Type.new_line_keyword))) {
				type = Type.token;
			} else if (node.equals("TOKEN")) {
				type = Type.token_keyword;
			} else if (node.equals("IDENTIFIER")) {
				type = Type.identifier_keyword;
			} else if (node.equals("NEW_LINE")) {
				type = Type.new_line_keyword;
			} else {
				type = Type.identifier;
			}
		}

		public void addBranch(Tree t) {
			if (t == null) {
				branches.add(null);
				return;
			}
			if (s == null) {
				this.s = t.s;
			}
			if (this.begin < 0) {
				this.begin = t.begin;
			}
			if (this.end < t.end) {
				this.end = t.end;
			}
			this.node = s.substring(begin, end);
			branches.add(t);
			t.parent = this;
		}
		
		public Tree removeLastBranch() {
			Tree t = branches.remove(branches.size() - 1);
			if (t == null) {
				end = begin;
			} else {
				this.end = t.begin;
				this.node = s.substring(begin, end);
			}
			return t;
		}

//		@Override
//		public String toString() {
//			HashMap<Integer, List<String>> prefixes = getPrefixes();
//			HashMap<Integer, List<String>> suffixes = getSuffixes();
//			Iterator<Tree> hideenIter = getHidden().iterator();
//			Tree hiddenRange = hideenIter.hasNext() ? hideenIter.next() : null;
//			if (prefixes.size() > 0 || suffixes.size() > 0) {
//				StringBuffer buff = new StringBuffer();
//				List<String> pr = prefixes.get(begin);
//				if (pr != null && begin == end) {
//					for (Iterator<String> iter = pr.iterator(); iter.hasNext(); ) {
//						buff.append(iter.next());
//					}
//				}
//				for (int i = begin; i < end; i++) {
//					while (hiddenRange != null && i > hiddenRange.end) {
//						hiddenRange = hideenIter.hasNext() ? hideenIter.next() : null;
//					}
//					List<String> p = prefixes.get(i);
//					List<String> s = suffixes.get(i);
//					String step = null;
//					if (p != null) {
//						for (Iterator<String> iter = p.iterator(); iter.hasNext(); ) {
//							String pref = iter.next();
//							if (pref.trim().startsWith("$.$.step(") && suffixes.get(i) != null) {
//								step = pref;
//								continue;
//							}
//							buff.append(pref);
//						}
//					}
//					if (i < end && node.charAt(i - begin) != '\n') {
//						if (!(hiddenRange != null && i >= hiddenRange.begin && i < hiddenRange.end)) {
//							buff.append(node.charAt(i - begin));
//						}
//					}
//					if (s != null) {
//						for (Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
//							buff.append(iter.next());
//						}
//					}
//					if (step != null) {
//						buff.append(step);
//					}
//					if (i < end && node.charAt(i - begin) == '\n') {
//						buff.append('\n');
//					}
//				}
//				List<String> s = suffixes.get(end);
//				if (s != null) {
//					for (Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
//						buff.append(iter.next());
//					}
//				}
//				return buff.toString();
//			}
//			return node;
//		}
//		
//		public HashMap<Integer, List<String>> getPrefixes() {
//			return getBrackets(0);
//		}
//		
//		public HashMap<Integer, List<String>> getSuffixes() {
//			return getBrackets(1);
//		}

		@Override
		public String toString() {
			HashMap<Integer, List<Tree>> map = getPrefixesAndSuffixes();
			Iterator<Tree> hiddenIter = getHidden().iterator();
			Tree hidden = hiddenIter.hasNext() ? hiddenIter.next() : null;
			if (map.size() == 0 && hidden == null) {
				return node;
			}
			StringBuffer buff = new StringBuffer();
			for (int i = begin; i <= end; i++) {
				List<Tree> l = map.get(i);
				if (l != null) {
					while (l.size() > 0) {
						Tree biggestRange = null;
						for (Tree t: l) {
							if (t.end == i) {
								if (t.prefix == null) {
									biggestRange = t;
									break;
								} else if (biggestRange != null) {
									if (t.begin > biggestRange.begin) {
										biggestRange = t;
									} else if (t.begin == biggestRange.begin && t.end == biggestRange.end) {
										for (Tree u = t.parent; u != null; u = u.parent) {
											if (u == biggestRange) {
												biggestRange = t;
												break;
											}
										}
									}
								} else {
									biggestRange = t;
								}
							} else if (t.begin == i) {
								if (biggestRange != null && biggestRange.end == i) {
									continue;
								}
								if (t.suffix == null) {
									if (biggestRange == null) {
										biggestRange = t;
									}
								} else if (biggestRange != null) {
									if (t.end > biggestRange.end) {
										biggestRange = t;
									} else if (t.begin == biggestRange.begin && t.end == biggestRange.end) {
										for (Tree u = biggestRange.parent; u != null; u = u.parent) {
											if (u == t) {
												biggestRange = t;
												break;
											}
										}
									}
								} else {
									biggestRange = t;
								}
							}
						}
						if (i == biggestRange.begin) {
							buff.append(biggestRange.prefix);
						} else {
							buff.append(biggestRange.suffix);
						}
						l.remove(biggestRange);
					}
				}
				if (i < end && !(hidden != null && hidden.begin <= i && i < hidden.end)) {
					buff.append(s.charAt(i));
				}
				if (hidden != null && i >= hidden.end) {
					hidden = hiddenIter.hasNext() ? hiddenIter.next() : null;
				}
			}
			return buff.toString();
		}
		
		public HashMap<Integer, List<Tree>> getPrefixesAndSuffixes() {
			HashMap<Integer, List<Tree>> res = new HashMap<Integer, List<Tree>>();
			if (prefix != null || suffix != null) {
				if (prefix != null) {
					List<Tree> l = new LinkedList<Tree>();
					l.add(this);
					res.put(begin, l);
				}
				if (suffix != null) {
					List<Tree> l = new LinkedList<Tree>();
					l.add(this);
					res.put(end, l);
				}
			}
			for (Tree b: branches) {
				if (b == null || b.node.length() == 0) {
					continue;
				}
				HashMap<Integer, List<Tree>> map = b.getPrefixesAndSuffixes();
				for (Integer n: map.keySet()) {
					List<Tree> l1 = res.get(n);
					List<Tree> l2 = map.get(n);
					if (l1 != null) {
						l1.addAll(l2);
					} else {
						res.put(n, l2);
					}
				}
			}
			return res;
		}
		
		public List<Tree> getHidden() {
			LinkedList<Tree> l = new LinkedList<Tree>();
			if (hide) {
				l.add(this);
			} else {
				for (Tree b: branches) {
					if (b != null && b.node.length() > 0) {
						l.addAll(b.getHidden());
					}
				}
			}
			return l;
		}
		
//		private HashMap<Integer, List<String>> getBrackets(int br) {
//			HashMap<Integer, List<String>> res = new HashMap<Integer, List<String>>();
//			if (br == 0 && prefix != null) {
//				List<String> l = new LinkedList<String>();
//				l.add(prefix);
//				res.put(begin, l);
//			} else if (br == 1 && suffix != null) {
//				List<String> l = new LinkedList<String>();
//				l.add(suffix);
//				if (suffix.startsWith(")") || suffix.trim().startsWith("}")) {
//					res.put(end - 1, l);
//				} else {
//					res.put(end, l);
//				}
//			}
//			if (branches.size() > 0) {
//				for (Tree b: branches) {
//					if (b != null) {
//						HashMap<Integer, List<String>> brackets = b.getBrackets(br);
//						for (Iterator<Integer> iter = brackets.keySet().iterator(); iter.hasNext(); ) {
//							Integer n = iter.next();
//							List<String> brl = brackets.get(n);
//							if (res.get(n) != null) {
//								List<String> l = res.get(n);
//								if (br == 0) {
//									l.addAll(brl);
//								} else {
//									l.addAll(0, brl);
//								}
//							} else {
//								res.put(n, brl);
//							}
//						}
//					}
//				}
//			}
//			return res;
//		}
		
		@Override
		public boolean equals(Object o) {
			return o == this;
		}
		
		public boolean deepEquals(Tree t) {
			if (node.equals(t.node) && begin == t.begin && end == t.end && type == t.type && branches.size() == t.branches.size()) {
				for (int i = 0; i < branches.size(); i++) {
					if (!branches.get(i).deepEquals(t.branches.get(i))) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return node.hashCode();
		}

	}

	public static Tree parse(String bnfDef) {
		return parse(bnfDef, 0, bnfDef.length());
	}

	public static Tree parse(String bnfDef, int begin, int end) {
		int firstSpacePos = -1;
		int bracketCount = 0;
		boolean quotes = false;
		for (int i = begin; i < end; i++) {
			char ch = bnfDef.charAt(i);
			if (!quotes && (ch == '(' || ch == '[' || ch == '{')) {
				bracketCount++;
			} else if (!quotes && (ch == ')' || ch == ']' || ch == '}')) {
				bracketCount--;
			} else if (ch == '\'') {
				quotes = !quotes;
			} else if (quotes && ch == '\\') {
				i++;
			} else if (!quotes && bracketCount == 0) {
				if (ch == ' ' && firstSpacePos < 0 && i < end - 1 &&
						bnfDef.charAt(i + 1) != '|' &&
						bnfDef.charAt(i + 1) != ')' &&
						bnfDef.charAt(i + 1) != ']' &&
						bnfDef.charAt(i + 1) != '}') {
					firstSpacePos = i;
				} else if (ch == '|') {
					Tree res = new Tree(Type.choice, null);
					res.addBranch(parse(bnfDef, begin, i - 1));
					Tree t = parse(bnfDef, i + 2, end);
					if (t.type == Type.choice) {
						for (Tree b : t.branches) {
							res.addBranch(b);
						}
					} else {
						res.addBranch(t);
					}
					return res;
				}
			}
		}

		if (firstSpacePos > 0) {
			Tree res = new Tree(Type.sequence, null);
			res.addBranch(parse(bnfDef, begin, firstSpacePos));
			Tree t = parse(bnfDef, firstSpacePos + 1, end);
			if (t.type == Type.sequence) {
				for (Tree b : t.branches) {
					res.addBranch(b);
				}
			} else {
				res.addBranch(t);
			}
			return res;
		}

		char ch = bnfDef.charAt(begin);
		int leftOffset = 1;
		if (begin + leftOffset < end && Character.isWhitespace(bnfDef.charAt(begin + leftOffset))) {
			leftOffset++;
		}
		int rightOffset = 1;
		if (end - rightOffset - 1 >= begin && Character.isWhitespace(bnfDef.charAt(end - rightOffset - 1))) {
			rightOffset++;
		}
		if (ch == '(') {
			Tree res = parse(bnfDef, begin + leftOffset, end - rightOffset);
			res.begin -= leftOffset;
			res.end += rightOffset;
			res.node = res.s.substring(res.begin, res.end);
			return res;
		}
		if (ch == '[') {
			Tree t = parse(bnfDef, begin + leftOffset, end - rightOffset);
			Tree res = new Tree(Type.optional, null);
			res.addBranch(t);
			res.begin -= leftOffset;
			res.end += rightOffset;
			res.node = res.s.substring(res.begin, res.end);
			return res;
		}
		if (ch == '{') {
			Tree t = parse(bnfDef, begin + leftOffset, end - rightOffset);
			Tree res = new Tree(Type.repetition, null);
			res.addBranch(t);
			res.begin -= leftOffset;
			res.end += rightOffset;
			res.node = res.s.substring(res.begin, res.end);
			return res;
		}
		return new Tree(bnfDef, begin, end, null);
	}

}
