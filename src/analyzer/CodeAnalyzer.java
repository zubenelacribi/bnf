/*
 * Code analyzer.
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

package analyzer;

import java.util.HashMap;

import bnf.ParseTree;
import bnf.Tree;

/**
 * 
 * The code analyzer annotates the names in the given parsed code.
 * Names can be local variables, member variables, methods, inner types.
 * They can be fully qualified or partial. In order the code instrumentation
 * to run correctly, it is essential to know what each names stands for
 * in its local context.<br/>
 * <br/>
 * Names that are not part of the given code are outside definitions.
 * These can be public names defined in jars or can be part of the Java
 * libraries. If the outside names are from other libraries then the
 * classpath should be specified when running the code analyzer.<br/>
 * 
 * @author Zuben El Acribi
 *
 */
public class CodeAnalyzer {

	/**
	 * Parsed Java code.
	 */
	private ParseTree[] code;
	
	/**
	 * Map for fully qualified type name to its definition.
	 */
	private HashMap<String, Tree> fullyQualifiedTypeNames = new HashMap<String, Tree>();

	/**
	 * Map for fully qualified method name to its definition.
	 */
	private HashMap<String, Tree> fullyQualifiedMethodNames = new HashMap<String, Tree>();

	/**
	 * Map for fully qualified member name to its definition.
	 */
	private HashMap<String, Tree> fullyQualifiedMemberNames = new HashMap<String, Tree>();

	/**
	 * A mapping from the parsed tree's root tree to the
	 * parsed tree itself. Used for resolving which tree to
	 * which parsed tree belongs. The parsed tree contains
	 * the tree and the file name.
	 */
	private HashMap<Tree, ParseTree> tree2parsedTree = new HashMap<Tree, ParseTree>();
	
	/**
	 * Constructs a code analyzer out of parsed Java files.
	 * @param t parse tree for every Java file.
	 */
	public CodeAnalyzer(ParseTree... t) {
		this.code = t;
		mapTree2ParsedTree();
		analyze();
	}

	/**
	 * Populates tree2parsedTree.
	 */
	private void mapTree2ParsedTree() {
		for (ParseTree p: code) {
			tree2parsedTree.put(p.tree, p);
		}
	}

	/**
	 * Do code analysis.
	 */
	private void analyze() {
		for (ParseTree t: code) {
			analyzeDirectMembers(t.tree, null);
		}
	}
	
	/**
	 * @return for the given tree returns the ParseTree it belongs to.
	 */
	private ParseTree getParsedTree(Tree t) {
		while (t.parent != null) {
			t = t.parent;
		}
		return tree2parsedTree.get(t);
	}
	
	/**
	 * The first step of analysis is to traverse type definitions
	 * and to determine the members directly defined in them.
	 * @param t a parse tree;
	 * @param typeContext the name of the type in which scope we are currently in.
	 */
	private void analyzeDirectMembers(Tree t, String typeContext) {
		if (t.def.parent.node.equals("CompilationUnit")) {
			
			t = t.branches.get(2);
			for (Tree u: t.branches) {
				analyzeDirectMembers(u, typeContext);
			}
			
		} else if (t.def.parent.node.equals("TypeDeclaration")) {
			
			if (t.branches.get(0) != null) {
				analyzeDirectMembers(t.branches.get(0), typeContext);
			}
			
		} else if (t.def.parent.node.equals("ClassOrInterfaceDeclaration")) {
			
			if (t.branches.get(1).branches.get(0) != null) {
				analyzeDirectMembers(t.branches.get(1).branches.get(0), typeContext); // ClassDeclaration
			} else {
				analyzeDirectMembers(t.branches.get(1).branches.get(1), typeContext); // InterfaceDeclaration
			}
			
		} else if (t.def.parent.node.equals("ClassDeclaration")) {
			
			if (t.branches.get(0) != null) {
				analyzeDirectMembers(t.branches.get(0), typeContext); // NormalClassDeclaration
			} else {
				analyzeDirectMembers(t.branches.get(1), typeContext); // EnumDeclaration
			}
			
		} else if (t.def.parent.node.equals("InterfaceDeclaration")) {

			if (t.branches.get(0) != null) {
				analyzeDirectMembers(t.branches.get(0), typeContext); // NormalInterfaceDeclaration
			}
			// Skip AnnotationTypeDeclaration as we don't need to use annotation names.
			
		} else if (t.def.parent.node.equals("EnumDeclaration")) {
			
			typeContext = context(typeContext, t.branches.get(1).node);
			t = t.branches.get(3); // EnumBody
			if (t.branches.get(1).node.length() > 0) { // EnumConstant
				enumConstant(t.branches.get(1), typeContext);
			}
			for (Tree u: t.branches.get(2).branches) { //  { ',' EnumConstant }
				enumConstant(u.branches.get(1), typeContext);
			}
			if (t.branches.get(4).node.length() > 0) { // [EnumBodyDeclarations]
				t = t.branches.get(4).branches.get(0).branches.get(1); // {ClassBodyDeclaration}
				analyzeDirectMembers(t, typeContext);
			}
			
		} else if (t.def.parent.node.equals("NormalClassDeclaration")) {

			typeContext = context(typeContext, t.branches.get(1).node);
			t = t.branches.get(5).branches.get(1); // { ClassBodyDeclaration }
			for (Tree u: t.branches) {
				analyzeDirectMembers(u, typeContext);
			}

		} else if (t.def.parent.node.equals("NormalInterfaceDeclaration")) {

			typeContext = context(typeContext, t.branches.get(1).node);
			t = t.branches.get(4).branches.get(1); // { InterfaceBodyDeclaration }
			for (Tree u: t.branches) {
				analyzeDirectMembers(u, typeContext);
			}
			
		}
	}

	/**
	 * For the fully qualified type name entering the specified context.
	 * @param originalTypeContext our current context;
	 * @param typeName the type we are going to enter.
	 * @return the result fully qualified type name; inner types are separated by
	 *   the outer ones with '$'.
	 */
	private String context(String originalTypeContext, String typeName) {
		if (originalTypeContext == null) {
			return typeName;
		} else {
			return originalTypeContext + '$' + typeName;
		}
	}
	
	/**
	 * Maps the enum constant to its definition.
	 * @param t an EnumConstant definition.
	 * @param typeContext the current type context.
	 */
	private void enumConstant(Tree t, String typeContext) {
		Tree c = t.branches.get(1).branches.get(1); // Identifier
		fullyQualifiedMemberNames.put(typeContext + c.node, c);
	}
	
}
