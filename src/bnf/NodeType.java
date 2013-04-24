/*
 * An enumeration of the different node types.
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
 * An enumeration of the parse tree node types.
 * Every tree node has a type which is explicitly given
 * on construction time or inferred (when parsing a BNF definition).
 * 
 * @author Zuben El Acribi
 *
 */
public enum NodeType {
	/**
	 * A string surrounded by quotes.
	 */
	token,
	
	/**
	 * A string that contains identifier symbols only.
	 */
	identifier,
	
	/**
	 * A sequence of expressions.
	 */
	sequence,
	
	/**
	 * Expressions separated by '|'. Just one of them is expected to match.
	 */
	choice,
	
	/**
	 * The reserved keyword TOKEN (used when parsing BNF definitions only.
	 */
	token_keyword,
	
	/**
	 * The reserved keyword IDENTIFIER (used when parsing BNF definitions only.
	 */
	identifier_keyword,
	
	/**
	 * The reserved keyword NEW_LINE (used when parsing BNF definitions only.
	 */
	new_line_keyword,
	
	/**
	 * An expression surrounded by square brackets.
	 * It is non-obligatory, i.e. may match 0 or 1 time.
	 */
	optional,
	
	/**
	 * An expression surrounded by curly brackets.
	 * It defines a repetitive expression, i.e. an expression which may match
	 * 0 or more times.
	 */
	repetition
}
