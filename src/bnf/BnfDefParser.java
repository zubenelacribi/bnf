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

/**
 * This is a simple Backus-Naur Form parser that parses BNF definitions
 * and builds a parse tree which provides a structured view over the
 * parsed text.<br/>
 * The parse tree is then supplied to Parser which then
 * uses these definitions to parse anything described with the
 * grammar given to this BNF parser, i.e. this is a specialized BNF
 * definition parser while Parser is a general purpose parser that
 * uses definitions provided in the form of parse trees by this
 * BNF definition parser.<br/>
 * <br/>
 * Every definition consists of:<br/>
 * 1. Unique name, consisting of alphanumeric ASCII symbols.<br/>
 * 2. Colon ':' immediately after the definition identifier.<br/>
 * 3. Any expression that uses tokens (terminals, these are surrounded
 *    by single quotes), identifiers (refer to BNF definitions),
 *    round brackets (groups expressions), square brackets (optional
 *    expressions), curly brackets (repetition of an expression 0 or
 *    more times) and the synmbol pipe '|' that means choice.<br/>
 * <br/>
 * Expressions are choice of expressions or sequences of groups,
 * optionals, repetitions, tokens and identifiers.<br/>
 * 
 * @author Zuben El Acribi
 *
 */
public class BnfDefParser {

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
					Tree res = new Tree(NodeType.choice, null);
					res.addBranch(parse(bnfDef, begin, i - 1));
					Tree t = parse(bnfDef, i + 2, end);
					if (t.type == NodeType.choice) {
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
			Tree res = new Tree(NodeType.sequence, null);
			res.addBranch(parse(bnfDef, begin, firstSpacePos));
			Tree t = parse(bnfDef, firstSpacePos + 1, end);
			if (t.type == NodeType.sequence) {
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
			Tree res = new Tree(NodeType.optional, null);
			res.addBranch(t);
			res.begin -= leftOffset;
			res.end += rightOffset;
			res.node = res.s.substring(res.begin, res.end);
			return res;
		}
		if (ch == '{') {
			Tree t = parse(bnfDef, begin + leftOffset, end - rightOffset);
			Tree res = new Tree(NodeType.repetition, null);
			res.addBranch(t);
			res.begin -= leftOffset;
			res.end += rightOffset;
			res.node = res.s.substring(res.begin, res.end);
			return res;
		}
		return new Tree(bnfDef, begin, end, null);
	}

}
