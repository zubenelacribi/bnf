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
 *    more times) and the pipe symbol '|' that means choice.<br/>
 * <br/>
 * Expressions are choice of expressions or sequences of groups,
 * optionals, repetitions, tokens and identifiers.<br/>
 * 
 * @author Zuben El Acribi
 *
 */
public class BnfDefParser {

	/**
	 * Parses a single BNF definition.
	 * @param bnfDef a string containing a single BNF definition.
	 * @return the parse tree of the BNF expression.
	 */
	public static Tree parse(String bnfDef) throws ParseException {
		return parse(bnfDef, 0, bnfDef.length());
	}

	/**
	 * Parses a part of a BNF definition (a substring).
	 * @param bnfDef a string containing a single BNF definition.
	 * @param begin the substring beginning  (inclusive).
	 * @param end the substring end (exclusive).
	 * @return the parse tree of the BNF expression.
	 */
	private static Tree parse(String bnfDef, int begin, int end) throws ParseException {
		int firstSpacePos = -1; // We should remember the place of the first space.
		int bracketCount = 0; // Bracket nesting count.
		boolean quotes = false; // Are we positioned within a string (quotes).
		
		// For every char do:
		for (int i = begin; i < end; i++) {
			char ch = bnfDef.charAt(i);
			// If we are not within a string then count up and down when meeting an opening or closing bracket.
			if (!quotes && (ch == '(' || ch == '[' || ch == '{')) {
				bracketCount++;
			} else if (!quotes && (ch == ')' || ch == ']' || ch == '}')) {
				bracketCount--;
			} else if (ch == '\'') { // If we meet quote then change 'within string' flag to the opposite (entering/exiting a string).
				quotes = !quotes;
			} else if (quotes && ch == '\\') { // If we are within a string then care about escaping with backslash (skip next char if backslash met).
				i++;
			} else if (!quotes && bracketCount == 0) { // If we are not within a string and brackets then check for operators.
				if (ch == ' ' && firstSpacePos < 0 && i < end - 1 && // We have met the first space in this substring (no operator/bracket after it).
						bnfDef.charAt(i + 1) != '|' &&
						bnfDef.charAt(i + 1) != ')' &&
						bnfDef.charAt(i + 1) != ']' &&
						bnfDef.charAt(i + 1) != '}') {
					firstSpacePos = i; // Remember its position.
				} else if (ch == '|') { // We have met a choice operator (lower priority than sequence, i.e. space).
					Tree res = new Tree(NodeType.choice, null); // This node is a 'choice'.
					res.addBranch(parse(bnfDef, begin, i - 1)); // Parse the left part.
					Tree t = parse(bnfDef, i + 2, end); // Parse the right part. It may contain more 'choices'.
					if (t.type == NodeType.choice) { // If the right part contains 'choices' then flatten then parse tree (more optimized representation).
						for (Tree b : t.branches) {
							res.addBranch(b);
						}
					} else { // Otherwise we have nothing else to do with the right part, just add it to the node 'res'.  
						res.addBranch(t);
					}
					return res; // 'Choice' has been parsed.
				}
			}
		}

		// No 'choices' outside brackets.
		// Check whether we have met a space (just space without the 'choice' operator or a closing bracket).
		if (firstSpacePos > 0) {
			Tree res = new Tree(NodeType.sequence, null); // If so then this node is a 'sequence'.
			res.addBranch(parse(bnfDef, begin, firstSpacePos)); // The left part is from the beginning to that space.
			Tree t = parse(bnfDef, firstSpacePos + 1, end); // The right part is after the space. The result tree node may be a 'sequence'.
			if (t.type == NodeType.sequence) { // If so then flatten the parse tree (more optimized representation).
				for (Tree b : t.branches) {
					res.addBranch(b);
				}
			} else { // Otherwise we have nothing else to do with the right part, just add it to the node 'res'.
				res.addBranch(t);
			}
			return res; // 'Sequence' has been parsed.
		}

		// There are no 'choice' and 'sequence' operators outside brackets.
		// Therefore the whole expression should be surrounded by brackets or it should be an atom:
		// string, identifier or keyword (TOKEN, IDENTIFIER, NEW_LINE).
		char ch = bnfDef.charAt(begin);
		
		// Define an offset for spaces/new lines before and after the brackets
		// (if the substring represents an expression surrounded by brackets).
		int leftOffset = 1;
		if (begin + leftOffset < end && Character.isWhitespace(bnfDef.charAt(begin + leftOffset))) {
			leftOffset++;
		}
		int rightOffset = 1;
		if (end - rightOffset - 1 >= begin && Character.isWhitespace(bnfDef.charAt(end - rightOffset - 1))) {
			rightOffset++;
		}
		
		// Check for round brackets. These brackets just groups expressions. 
		if (ch == '(') {
			if (bnfDef.charAt(end - 1) != ')') {
				throw new ParseException("The expression in [" + begin + ", " + (end - 1) + "]: '" +
						bnfDef.substring(begin, end) + "' should be surrounded by round brackets");
			}
			Tree res = parse(bnfDef, begin + leftOffset, end - rightOffset);
			res.begin -= leftOffset;
			res.end += rightOffset;
			res.node = res.s.substring(res.begin, res.end);
			return res;
		}
		
		// Check for square brackets. These brackets surround an optional expression.
		if (ch == '[') {
			if (bnfDef.charAt(end - 1) != ']') {
				throw new ParseException("The expression in [" + begin + ", " + (end - 1) + "]: '" +
						bnfDef.substring(begin, end) + "' should be surrounded by square brackets");
			}
			Tree t = parse(bnfDef, begin + leftOffset, end - rightOffset);
			Tree res = new Tree(NodeType.optional, null);
			res.addBranch(t);
			res.begin -= leftOffset;
			res.end += rightOffset;
			res.node = res.s.substring(res.begin, res.end);
			return res;
		}
		
		// Check for curly brackets. These brackets surround a repetitive expression (0 or more times).
		if (ch == '{') {
			if (bnfDef.charAt(end - 1) != '}') {
				throw new ParseException("The expression in [" + begin + ", " + (end - 1) + "]: '" +
						bnfDef.substring(begin, end) + "' should be surrounded by curly brackets");
			}
			Tree t = parse(bnfDef, begin + leftOffset, end - rightOffset);
			Tree res = new Tree(NodeType.repetition, null);
			res.addBranch(t);
			res.begin -= leftOffset;
			res.end += rightOffset;
			res.node = res.s.substring(res.begin, res.end);
			return res;
		}
		
		// No brackets found. This is an atom (string, identifier or keyword).
		return new Tree(bnfDef, begin, end, null);
	}

}
