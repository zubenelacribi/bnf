/*
 * Utility for insertion of tracing debug code.
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

import java.util.ArrayList;

import codegen.Annotations.Type;

import bnf.Tree;
import bnf.ParseTree;

public class DebugCodeInserter {

	private ParseTree parseTree;
	private Annotations ann;

	public DebugCodeInserter(ParseTree parseTree, Annotations ann) {
		this.parseTree = parseTree;
		this.ann = ann;
		ann.newFile(parseTree.filename);
	}

	public DebugCodeInserter run() {
		insertDebugCode(parseTree.tree);
		return this;
	}

	private void insertDebugCode(Tree tree) {
		if (tree.def.parent.node.equals("MethodDeclaratorRest")) { // MethodDeclaratorRest: FormalParameters {'[' ']'} ['throws' QualifiedIdentifierList] (Block | ';')

			Tree params = tree.branches.get(0);
			tree = tree.branches.get(3).branches.get(0);
			if (tree == null) {
				return;
			}
			block(tree, params);

		} else if (tree.def.parent.node.equals("VoidMethodDeclaratorRest")) { // VoidMethodDeclaratorRest: FormalParameters ['throws' QualifiedIdentifierList] (Block | ';')

			Tree params = tree.branches.get(0);
			tree = tree.branches.get(2).branches.get(0);
			if (tree == null) {
				return;
			}
			block(tree, params);

		} else if (tree.def.parent.node.equals("ConstructorDeclaratorRest")) { // ConstructorDeclaratorRest: FormalParameters ['throws' QualifiedIdentifierList] Block

			block(tree.branches.get(2), tree.branches.get(0));
			
		} else if (tree.def.node.equals("['static'] Block")) { // Initializer.
			
			block(tree.branches.get(1), tree.branches.get(1));
			
		} else if (tree.def.parent.node.equals("Expression")) {

			if (!(tree.parent != null && tree.parent.parent != null && tree.parent.parent.def.node.equals("ForVariableDeclaratorsRest ';' [Expression] ';' [ForUpdate]"))) {
				boolean typeField = typeField(tree);
				String context = typeField ? "-1l, " : "$_$, ";
				ArrayList<String> assignmentVars = new ArrayList<String>();
				if (traceableExpression(tree, assignmentVars) && tree.prefix == null) {
//					tree.prefix = "$.$.$(" + ann.annotation(Type.expr, tree.begin, tree.end) + "l, " + context + "\"\", ";
//					tree.suffix = ")";
				}
				if (assignmentVars.size() > 0) {
					StringBuffer buff = new StringBuffer();
					for (String s: assignmentVars) {
						buff.append(" $.$.$(" + ann.annotation(Type.var, tree.begin, tree.end) + "l, " + context + "\"");
						buff.append(s);
						buff.append("\", ");
						buff.append(s);
						buff.append(");");
					}
					if (tree.parent != null && tree.parent.def.node.equals("StatementExpression ';'")) {
						if (tree.parent.suffix != null) {
							tree.parent.suffix += buff;
						} else {
							tree.parent.suffix = buff.toString();
						}
					}
				}
				if (typeField) {
					return;
				}
			}
			
		} else if (tree.def.parent.node.equals("BlockStatement")) { // BlockStatement: LocalVariableDeclarationStatement | ClassOrInterfaceDeclaration | [Identifier ':'] Statement

			if (tree.branches.get(0) != null) { // LocalVariableDeclarationStatement: { VariableModifier } Type VariableDeclarators ';'
				tree = tree.branches.get(0);
				tree.prefix = " $.$.step(" + ann.annotation(Type.step, tree.begin, tree.end) + "l, $_$); ";
				Tree t = tree.branches.get(2); // VariableDeclarators: VariableDeclarator { ',' VariableDeclarator }
				StringBuffer buff = new StringBuffer();
				String var = t.branches.get(0).branches.get(0).node; // VariableDeclarator: Identifier VariableDeclaratorRest
				if (t.branches.get(0).branches.get(1).branches.get(1).node.length() > 0) { // VariableDeclaratorRest: {'[' ']'} [ '=' VariableInitializer ]
					buff.append(" $.$.$(" + ann.annotation(Type.vardecl, tree.begin, tree.end) + "l, $_$, \"");
					buff.append(var);
					buff.append("\", ");
					buff.append(var);
					buff.append(");");
				}
				t = t.branches.get(1);
				for (Tree b: t.branches) {
					var = b.branches.get(1).branches.get(0).node; // VariableDeclarator: Identifier VariableDeclaratorRest
					if (t.branches.get(0).branches.get(1).branches.get(1).node.length() > 0) { // VariableDeclaratorRest: {'[' ']'} [ '=' VariableInitializer ]
						buff.append(" $.$.$(" + ann.annotation(Type.vardecl, t.begin, t.end) + "l, $_$, \"");
						buff.append(var);
						buff.append("\", ");
						buff.append(var);
						buff.append(");");
					}
				}
				tree.suffix = buff.toString();
			} else if (tree.branches.size() > 2 && tree.branches.get(2) != null) { // [Identifier ':'] Statement
				tree = tree.branches.get(2);
				if (constructorCall(tree)) {
					return;
				} else {
					tree.prefix = " $.$.step(" + ann.annotation(Type.step, tree.begin, tree.end) + "l, $_$); ";
				}
			}
			
		} else if (tree.def.node.startsWith("'if'") && tree.def.branches.size() > 0) { // 'if' ParExpression Statement ['else' Statement]
			
			Tree t = tree.branches.get(2);
			t.prefix = "{ $.$.step(" + ann.annotation(Type.step, tree.begin, tree.end) + "l, $_$); ";
			t.suffix = " }";
			if (tree.branches.size() > 3 && tree.branches.get(3).node.length() > 0) {
				t = tree.branches.get(3).branches.get(1);
				t.prefix = "{ $.$.step(" + ann.annotation(Type.step, tree.begin, tree.end) + "l, $_$); ";
				t.suffix = " }";
			}
			
		} else if (tree.def.node.startsWith("'for'") && tree.def.branches.size() > 0) { // 'for' '(' ForControl ')' Statement
			
			ArrayList<String> declaredVars = new ArrayList<String>();
			Tree t = tree.branches.get(2).branches.get(0); // {VariableModifier} Type VariableDeclaratorId ForVarControlRest
			if (t != null) {
				Tree u = t.branches.get(2).branches.get(0);
				declaredVars.add(u.node);
				u = t.branches.get(3); // ForVariableDeclaratorsRest ';' [Expression] ';' [ForUpdate] | ':' Expression
				if (u.branches.get(0) != null) {
					u = u.branches.get(0).branches.get(0); // [ '=' VariableInitializer ] { ',' VariableDeclarator }
					u = u.branches.get(1);
					for (Tree b: u.branches) {
						declaredVars.add(b.branches.get(1).branches.get(0).node);
					}
				}
			}
			scope(tree);
			StringBuffer buff = new StringBuffer();
			for (String s: declaredVars) {
				buff.append(" $.$.$(" + ann.annotation(Type.vardecl, tree.begin, tree.end) + "l, $_$, \"");
				buff.append(s);
				buff.append("\", ");
				buff.append(s);
				buff.append("); ");
			}
			t = tree.branches.get(4);
			t.prefix = "{ " + buff + " $.$.step(" + ann.annotation(Type.step, tree.begin, tree.end) + "l, $_$); ";
			t.suffix = " } ";

		} else if (tree.def.node.startsWith("'do'") && tree.def.branches.size() > 0) { // 'do' Statement 'while' ParExpression ';'
			
			Tree t = tree.branches.get(1);
			t.prefix = "{ $.$.step(" + ann.annotation(Type.step, tree.begin, tree.end) + "l, $_$); ";
			t.suffix = " } ";
			scope(tree);
			
		} else if (tree.def.node.startsWith("'while'") && tree.def.branches.size() > 0) { // 'while' ParExpression Statement
			
			Tree t = tree.branches.get(2);
			t.prefix = "{ $.$.step(" + ann.annotation(Type.step, tree.begin, tree.end) + "l, $_$); ";
			t.suffix = " } ";
			scope(tree);
			
		} else if (tree.def.parent.node.equals("SwitchLabel")) { // case LABEL:
			
			return; // Do not instrument case labels.
			
		} else if (tree.def.node.equals("'return' [Expression] ';'") && tree.branches.get(1).node.length() > 0) { // return EXPRESSION;
			
			if (tree.branches.get(1).node.equals("Collections.emptyList()")) {
				return;
			}
			
		} else if (tree.def.node.equals("{Modifier}")) { // 'public', 'protected', 'private', 'final'
			
			boolean visibility = false;
			for (Tree b: tree.branches) {
				if (b.node.equals("protected") || b.node.equals("private")) {
					if (!enumConstructor(tree)) {
						b.prefix = "public";
						b.hide = true;
						visibility = true;
					}
				} else if (b.node.equals("public")) {
					visibility = true;
				} else if (b.node.equals("final")) { // Remove 'final' from instance type members without initialization.
					if (!staticModifierPresent(tree) && fieldDeclarationWithoutInitializer(tree)) {
						b.hide = true;
					}
				}
			}
			if (!visibility && !inlineClassDefinition(tree) && !enumConstructor(tree)) {
				tree.parent.prefix = "public ";
				if (tree.begin == tree.end) {
					Tree t = tree.parent;
					int i = 0;
					for (; i < t.branches.size(); i++) {
						if (t.branches.get(i) == tree) {
							break;
						}
					}
					tree.begin = t.branches.get(i + 1).begin;
				}
			}
			
		}

		for (Tree b: tree.branches) {
			if (b != null) {
				insertDebugCode(b);
			}
		}
	}
	
	private void block(Tree t, Tree params) {
		String s = formalParameters(params);
		boolean constructorCall = constructorCall(t); // Call to this() or super() constructor.
		if (constructorCall) {
			t.branches.get(1).branches.get(0).suffix = s + "{";
		} else {
			t.prefix = "{" + s;
		}
		t.suffix = " finally { $.$.endscope(" + ann.annotation(Type.endscope, t.begin, t.end) + "l); } }";
	}
	
	private boolean constructorCall(Tree t) {
		if (t.branches.size() > 1 && t.branches.get(1).def.node.equals("{ BlockStatement }") && t.branches.get(1).branches.size() > 0) {
			t = t.branches.get(1).branches.get(0); // LocalVariableDeclarationStatement | ClassOrInterfaceDeclaration | [Identifier ':'] Statement
		}
		if (t.def.node.equals("LocalVariableDeclarationStatement | ClassOrInterfaceDeclaration | [Identifier ':'] Statement") && t.branches.size() > 2 && t.branches.get(2) != null) {
			t = t.branches.get(2);
		}
		if (t.def.node.equals("[Identifier ':'] Statement")) {
			t = t.branches.get(1);
		}
		if (t.branches.size() > 3 && t.branches.get(3) != null) {
			t = t.branches.get(3).branches.get(0); // StatementExpression ';' --> Expression1 [ AssignmentOperator Expression ]
			t = t.branches.get(0).branches.get(0).branches.get(0);
			if (t.branches.size() > 2 && t.branches.get(2) != null) {
				t = t.branches.get(2); // Primary { Selector } { PostfixOp }
				if (t.branches.get(1).node.length() == 0 && t.branches.get(2).node.length() == 0) {
					t = t.branches.get(0);
					if ((t.branches.size() > 2 && t.branches.get(2) != null) || // 'this' [Arguments]
							(t.branches.size() > 3 && t.branches.get(3) != null)) { // 'super' SuperSuffix
						return true;
					}
				}
			}
		}
		return false;
	}

	private String formalParameters(Tree t) {
		String[] params = getListOfFormalParameters(t);
		StringBuffer buff = new StringBuffer();
		buff.append(" long $_$ = $.$.scope(" + ann.annotation(Type.scope, t.begin, t.end) + "l); ");
		if (!isStatic(t)) {
			buff.append("$.$.$(" + ann.annotation(Type.arg, t.begin, t.end) + "l, $_$, \"this\", this); ");
		}
		for (int i = 0; i < params.length; i++) {
			buff.append("$.$.$(" + ann.annotation(Type.arg, t.begin, t.end) + "l, $_$, \"");
			buff.append(params[i]);
			buff.append("\", ");
			buff.append(params[i]);
			buff.append("); ");
		}
		buff.append("$.$.step(" + ann.annotation(Type.step, t.begin, t.end) + "l, $_$); try ");
		return buff.toString();
	}
	
	private boolean isStatic(Tree t) {
		if (t.parent != null && t.parent.parent != null &&
				(t.parent.parent.def.node.equals("Identifier ConstructorDeclaratorRest") ||
				 t.parent.parent.def.node.equals("'void' Identifier VoidMethodDeclaratorRest")) &&
				t.parent.parent.parent != null && t.parent.parent.parent.parent != null &&
				t.parent.parent.parent.parent.def.node.equals("{Modifier} MemberDecl")) {
			return findStatic(t.parent.parent.parent.parent.branches.get(0));
		}
		if (t.parent != null && t.parent.parent != null &&
				t.parent.parent.def.node.equals("FieldDeclaratorsRest ';' | MethodDeclaratorRest") &&
				t.parent.parent.parent != null && t.parent.parent.parent.parent != null && t.parent.parent.parent.parent.parent != null &&
				t.parent.parent.parent.parent.parent.def.node.equals("{Modifier} MemberDecl")) {
			return findStatic(t.parent.parent.parent.parent.parent.branches.get(0));
		}
		if (t.parent != null && t.parent.parent != null &&
				t.parent.parent.def.node.equals("(Type | 'void') Identifier MethodDeclaratorRest") &&
				t.parent.parent.parent != null && t.parent.parent.parent.parent != null &&
				t.parent.parent.parent.parent.def.parent.node.equals("GenericMethodOrConstructorDecl") &&
				t.parent.parent.parent.parent.parent != null && t.parent.parent.parent.parent.parent.parent != null) {
			return findStatic(t.parent.parent.parent.parent.parent.parent.branches.get(0));
		}
		if (t.parent != null && t.parent.def.node.equals("['static'] Block")) {
			return t.parent.branches.get(0).node.length() != 0;
		}
		return false;
	}
	
	private boolean findStatic(Tree t) {
		for (Tree b: t.branches) {
			if (b.node.equals("static")) {
				return true;
			}
		}
		return false;
	}
	
	private String[] getListOfFormalParameters(Tree t) { // FormalParameters: '(' [FormalParameterDecls] ')'
		if (!t.def.node.equals("'(' [FormalParameterDecls] ')'")) {
			return new String[0];
		}
		ArrayList<String> l = new ArrayList<String>();
		if (t.branches.get(1).node.length() > 0) {
			while (true) {
				t = t.branches.get(1); // FormalParameterDecls: {VariableModifier} Type FormalParameterDeclsRest
				t = t.branches.get(2); // FormalParameterDeclsRest: VariableDeclaratorId [ ',' FormalParameterDecls ] | '...' VariableDeclaratorId
				if (t.branches.get(0) != null) {
					t = t.branches.get(0);
					l.add(t.branches.get(0).branches.get(0).node);
					if (t.branches.get(1).node.length() > 0) {
						t = t.branches.get(1);
					} else {
						break;
					}
				} else {
					t = t.branches.get(1);
					l.add(t.branches.get(1).node);
					break;
				}
			}
		}
		return l.toArray(new String[0]);
	}

	private boolean traceableExpression(Tree t, ArrayList<String> assignmentVars) { // Expression: Expression1 [ AssignmentOperator Expression ]
		// Check whether this is a block statement. Block statements shouldn't be traced.
		Tree v = t;
		for (int count = 0; v.parent != null && count < 4; v = v.parent, count++);
		boolean blockStatement = v.def.parent.node.equals("BlockStatement");
			
		Tree original = t;
		if (t.branches.get(1).node.length() > 0) {
			String name = t.branches.get(0).node;
			boolean isIdentitifer = true;
			for (int i = 0; i < name.length(); i++) {
				if (!Character.isLetterOrDigit(name.charAt(i))) {
					isIdentitifer = false;
					break;
				}
			}
			if (isIdentitifer) {
				assignmentVars.add(name);
			} else {
				inspectField(t.branches.get(0));
			}
			t = t.branches.get(1).branches.get(1);
			traceableExpression(t, assignmentVars);
			return false;
		} else {
			t = t.branches.get(0); // Expression1: Expression2 [ Expression1Rest ]
			if (t.branches.get(1).node.length() == 0) { // Expression1Rest: '?' Expression ':' Expression1
				t = t.branches.get(0); // Expression2: Expression3 [ Expression2Rest ]
				if (t.branches.get(1).node.length() == 0) {
					t = t.branches.get(0); // Expression3: PrefixOp Expression3 | '(' ( Type | Expression ) ')' Expression3 | Primary { Selector } { PostfixOp })
					if (t.branches.size() > 2 && t.branches.get(2) != null) {
						t = t.branches.get(2);
						Tree u = t.branches.get(0).branches.get(0);
						if (u != null) {
							return false; // Literal
						}
						u = t.branches.get(0);
						if (u.branches.size() > 7) {
							u = u.branches.get(7);
							if (u != null) { // Identifier { '.' Identifier } [IdentifierSuffix]
								if (u.branches.size() > 2 && u.branches.get(2).node.length() > 0) {
									return !original.parent.def.node.equals("StatementExpression ';'") && !blockStatement;
								}
							}
						} else if (u.branches.size() > 2 && u.branches.get(2) != null) { // 'this' [Arguments]
							return false;
						} else if (u.branches.size() > 3 && u.branches.get(3) != null) { // 'super' SuperSuffix
							return false;
						}
					}
				}
			}
		}
		return !blockStatement;
	}

	private void scope(Tree t) {
		while (t.parent != null && t.parent.parent != null && t.parent.parent.def.node.equals("Identifier ':' Statement")) {
			t = t.parent.parent; // Statement: Block | ';' | Identifier ':' Statement | ...
		}
		if (t.parent != null && t.parent.parent != null && t.parent.parent.def.node.equals("[Identifier ':'] Statement") && t.parent.parent.branches.get(0).node.length() > 0) {
			t = t.parent.parent; // [Identifier ':'] Statement
		}
		if (t.prefix == null) {
			t.prefix = "";
		}
		t.prefix += "$_$ = $.$.scope(" + ann.annotation(Type.scope, t.begin, t.end) + "l); try { ";
		if (t.suffix == null) {
			t.suffix = "";
		}
		t.suffix += " } finally { $_$ = $.$.endscope(" + ann.annotation(Type.endscope, t.begin, t.end) + "l); } ";
	}

	private void inspectField(Tree t) {
		Tree original = t;
		if (t.def.node.equals("Expression2 [ Expression1Rest ]")) {
			t = t.branches.get(0).branches.get(0);
			if (t.branches.size() > 2 && t.branches.get(2) != null) {
				t = t.branches.get(2); // Primary { Selector } { PostfixOp }
				if (t.branches.get(1).node.length() > 0 && t.branches.get(2).node.length() == 0) {
					Tree u = t.branches.get(1); // { Selector }
					Tree last = u.branches.get(u.branches.size() - 1);
					Tree prev = t.branches.get(0);
					if (u.branches.size() > 1) {
						prev = u.branches.get(u.branches.size() - 2);
					}
					if (last.branches.get(0) != null) {
						last = last.branches.get(0); // '.' Identifier [Arguments]
						if (last.branches.get(2).node.length() == 0) {
							t.prefix = "$.$.$(" + ann.annotation(Type.obj, t.begin, prev.end) + "l, $_$, \"\", ";
							prev.suffix = ")";
							String fieldName = last.branches.get(1).node;
							if (original.parent.def.node.equals("Expression1 [ AssignmentOperator Expression ]") &&
									original.parent.branches.get(1).node.length() > 0) {
								Tree v = original.parent.branches.get(1).branches.get(1);
								String prefix = "$.$.$(" + ann.annotation(Type.field, v.begin, v.end) + "l, $_$, \"" + fieldName + "\", ";
								if (v.prefix == null) {
									v.prefix = prefix;
									v.suffix = ")";
								} else {
									v.prefix += prefix;
									v.suffix += ")";
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean typeField(Tree t) {
		Tree u = t.parent;
		for (int count = 0; u != null && count < 4; u = u.parent, count++) {
			if (u != null && u.def.node.equals("ConstantDeclaratorsRest ';'")) {
				return true;
			}
		}
		if (u != null && u.def.node.equals("FieldDeclaratorsRest ';'")) {
			return true;
		}
		return false;
	}

	private boolean staticModifierPresent(Tree t) {
		for (Tree b: t.branches) {
			if (b.node.equals("static")) {
				return true;
			}
		}
		return false;
	}

	private boolean fieldDeclarationWithoutInitializer(Tree t) {
		if (t.parent.def.node.equals("{Modifier} MemberDecl")) {
			t = t.parent.branches.get(1);
			if (t.branches.get(0) != null) {
				t = t.branches.get(0); // MethodOrFieldDecl: Type Identifier MethodOrFieldRest
				t = t.branches.get(2); // MethodOrFieldRest: FieldDeclaratorsRest ';' | MethodDeclaratorRest
				if (t.branches.get(0) != null) {
					t = t.branches.get(0).branches.get(0); // FieldDeclaratorsRest: VariableDeclaratorRest { ',' VariableDeclarator }
					t = t.branches.get(0); // VariableDeclaratorRest: {'[' ']'} [ '=' VariableInitializer ]
					return t.branches.get(1).node.length() == 0;
				}
			}
		}
		return false;
	}

	private boolean inlineClassDefinition(Tree t) {
		return t.parent != null && t.parent.parent != null && t.parent.parent.parent != null &&
				t.parent.parent.parent.def.node.equals("{ BlockStatement }");
	}

	private boolean enumConstructor(Tree t) {
		if (t.parent != null && t.parent.def.node.equals("{Modifier} MemberDecl") &&
				t.parent.branches.get(1).branches.size() > 2 && t.parent.branches.get(1).branches.get(2) != null &&
						t.parent.branches.get(1).branches.get(2).def.node.equals("Identifier ConstructorDeclaratorRest")) {
			for (int i = 0; i < 6; i++) {
				if (t.parent == null) {
					return false;
				}
				t = t.parent;
			}
			return t != null && t.def.parent.node.equals("EnumDeclaration");
		} else {
			return false;
		}
	}
	
	public ParseTree getParseTree() {
		return parseTree;
	}

}
