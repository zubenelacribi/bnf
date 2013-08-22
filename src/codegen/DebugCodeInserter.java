package codegen;

import codegen.Annotations.Type;
import bnf.ParseTree;
import bnf.Tree;

public class DebugCodeInserter {

	private ParseTree parseTree;
	private Annotations ann;
	private String bridgeName;

	public DebugCodeInserter(ParseTree parseTree, Annotations ann, String bridgeName) {
		this.parseTree = parseTree;
		this.ann = ann;
		this.bridgeName = bridgeName;
	}

	public DebugCodeInserter run() {
		insertDebugCode(parseTree.tree);
		return this;
	}

	private void insertDebugCode(Tree tree) {
		if (tree.def.parent.node.equals("CompilationUnit")) {
			
			for (Tree t: tree.branches.get(2).branches) { // {TypeDeclaration}
				if (t.branches.get(0) != null) { // ClassOrInterfaceDeclaration | ';'
					insertDebugCode(t.branches.get(0)); 
				}
			}
			
		} else if (tree.def.parent.node.equals("ClassOrInterfaceDeclaration")) {
			
			tree = tree.branches.get(1);
			if (tree.branches.get(0) != null) {
				insertDebugCode(tree.branches.get(0)); // ClassDeclaration
			} else {
				insertDebugCode(tree.branches.get(1)); // InterfaceDeclaration
			}
			
		} else if (tree.def.parent.node.equals("ClassDeclaration")) {
			
			if (tree.branches.get(0) != null) { // NormalClassDeclaration
				insertDebugCode(tree.branches.get(0));
			} else { // EnumDeclaration
				insertDebugCode(tree.branches.get(1));
			}
			
		} else if (tree.def.parent.node.equals("InterfaceDeclaration")) {
			
			if (tree.branches.get(0) != null) { // NormalInterfaceDeclaration
				tree = tree.branches.get(4); // InterfaceBody
				tree = tree.branches.get(1); // { InterfaceBodyDeclaration }
				for (Tree t: tree.branches) {
					if (t.branches.get(1) != null) { // {Modifier} InterfaceMemberDecl
						t = t.branches.get(1).branches.get(1);
						if (t.branches.get(3) != null) { // ClassDeclaration
							insertDebugCode(t.branches.get(3));
						} else if (t.branches.get(4) != null) { // InterfaceDeclaration
							insertDebugCode(t.branches.get(4));
						}
					}
				}
			}
			
		} else if (tree.def.parent.node.equals("NormalClassDeclaration")) {
			
			insertDebugCode(tree.branches.get(5)); // ClassBody
			
		} else if (tree.def.parent.node.equals("EnumDeclaration")) {	
			
			if (tree.branches.get(1).node.length() > 0) {
				insertDebugCode(tree.branches.get(1).branches.get(0)); // EnumConstant
			}
			for (Tree t: tree.branches.get(2).branches) { // { ',' EnumConstant }
				insertDebugCode(t.branches.get(1));
			}
			if (tree.branches.get(4).node.length() > 0) { // [EnumBodyDeclarations]
				tree = tree.branches.get(4).branches.get(0); // ';' {ClassBodyDeclaration}
				tree = tree.branches.get(1);
				for (Tree t: tree.branches) {
					insertDebugCode(t);
				}
			}

		} else if (tree.def.parent.node.equals("EnumConstant")) {	

			if (tree.branches.get(3).node.length() > 0) { // [ClassBody]
				insertDebugCode(tree.branches.get(3).branches.get(0));
			}
			
		} else if (tree.def.parent.node.equals("ClassBody")) {	

			tree = tree.branches.get(1); // { ClassBodyDeclaration }
			for (Tree t: tree.branches) {
				insertDebugCode(t);
			}

		} else if (tree.def.parent.node.equals("ClassBodyDeclaration")) {

			if (tree.branches.get(1) != null) { // {Modifier} MemberDecl
				insertDebugCode(tree.branches.get(1).branches.get(1)); // MemberDecl
			} else if (tree.branches.get(2) != null) { // ['static'] Block
				tree = tree.branches.get(2);
				block(tree, null, null, tree.branches.get(1)); // No args.
				insertDebugCode(tree.branches.get(1)); // Block
			}

		} else if (tree.def.parent.node.equals("MemberDecl")) {

			Tree memberDecl = tree;
			if (tree.branches.get(0) != null) { // MethodOrFieldDecl
				tree = tree.branches.get(0).branches.get(2); // MethodOrFieldRest
				if (tree.branches.get(0) != null) { // FieldDeclaratorsRest ';'
					insertDebugCode(tree.branches.get(0).branches.get(0));
				} else { // MethodDeclaratorRest
					tree = tree.branches.get(1); // FormalParameters {'[' ']'} ['throws' QualifiedIdentifierList] (Block | ';')
					method(memberDecl, tree.branches.get(0), tree.branches.get(2), tree.branches.get(3));
				}
			} else if (tree.branches.get(1) != null) { // 'void' Identifier VoidMethodDeclaratorRest
				tree = tree.branches.get(1).branches.get(2); // FormalParameters ['throws' QualifiedIdentifierList] (Block | ';')
				method(memberDecl, tree.branches.get(0), tree.branches.get(1), tree.branches.get(2));
			} else if (tree.branches.get(2) != null) { // Identifier ConstructorDeclaratorRest
				tree = tree.branches.get(2).branches.get(1); // FormalParameters ['throws' QualifiedIdentifierList] Block
				method(memberDecl, tree.branches.get(0), tree.branches.get(1), tree.branches.get(2));
			} else if (tree.branches.get(3) != null) { // GenericMethodOrConstructorDecl: TypeParameters GenericMethodOrConstructorRest
				tree = tree.branches.get(3).branches.get(1); // (Type | 'void') Identifier MethodDeclaratorRest | Identifier ConstructorDeclaratorRest
				if (tree.branches.get(0) != null) { // (Type | 'void') Identifier MethodDeclaratorRest
					tree = tree.branches.get(0).branches.get(2); // FormalParameters {'[' ']'} ['throws' QualifiedIdentifierList] (Block | ';')
					method(memberDecl, tree.branches.get(0), tree.branches.get(2), tree.branches.get(3));
				} else { // Identifier ConstructorDeclaratorRest
					tree = tree.branches.get(1).branches.get(1); // FormalParameters ['throws' QualifiedIdentifierList] Block
					method(memberDecl, tree.branches.get(0), tree.branches.get(1), tree.branches.get(2));
				}
			} else if (tree.branches.get(4) != null) { // ClassDeclaration
				insertDebugCode(tree.branches.get(4));
			} else if (tree.branches.get(5) != null) { // InterfaceDeclaration
				insertDebugCode(tree.branches.get(5));
			}
			
		} else if (tree.def.parent.node.equals("FieldDeclaratorsRest")) {

			// TODO
			
		} else if (tree.def.parent.node.equals("Block")) {

			for (Tree t: tree.branches.get(1).branches) { // '{' BlockStatements '}'
				insertDebugCode(t); // BlockStatement
			}

		} else if (tree.def.parent.node.equals("ArrayInitializer")) {

			// TODO
			
		} else if (tree.def.parent.node.equals("BlockStatement")) {

			if (tree.branches.get(0) != null) { // LocalVariableDeclarationStatement
				
			} else if (tree.branches.get(1) != null) { // ClassOrInterfaceDeclaration
				insertDebugCode(tree.branches.get(1));
			} else { // [Identifier ':'] Statement
				tree = tree.branches.get(2).branches.get(1);
				/*
				 * Statement: (
				 * Block |
				 * ';' |
				 * Identifier ':' Statement |
				 * StatementExpression ';' |
				 * 'if' ParExpression Statement ['else' Statement] |
				 * 'assert' Expression [':' Expression] ';' |
				 * 'switch' ParExpression '{' SwitchBlockStatementGroups '}' |
				 * 'while' ParExpression Statement |
				 * 'do' Statement 'while' ParExpression ';' |
				 * 'for' '(' ForControl ')' Statement |
				 * 'break' [Identifier] ';' |
				 * 'continue' [Identifier] ';' |
				 * 'return' [Expression] ';' |
				 * 'throw' Expression ';' |
				 * 'synchronized' ParExpression Block |
				 * 'try' Block ( [Catches] Finally | Catches ) |
				 * 'try' ResourceSpecification Block [Catches] [Finally])
				 */
				if (tree.branches.get(0) != null) { // Block
					insertDebugCode(tree.branches.get(0));
				} else if (tree.branches.get(1) != null) { // ';'
					return;
				} else if (tree.branches.get(2) != null) { // Identifier ':' Statement
					insertDebugCode(tree.branches.get(2));
				} else if (tree.branches.get(3) != null) { // StatementExpression ';'
					expression(tree.branches.get(0)); // StatementExpression: Expression
				} else if (tree.branches.get(4) != null) { // 'if' ParExpression Statement ['else' Statement]
					tree = tree.branches.get(4);
					insertDebugCode(tree.branches.get(1).branches.get(1)); // ParExpression: '(' Expression ')'
					insertDebugCode(tree.branches.get(2));
					if (tree.branches.get(3).node.length() > 0) {
						insertDebugCode(tree.branches.get(3).branches.get(1));
					}
				} else if (tree.branches.get(5) != null) { // 'assert' Expression [':' Expression] ';'
					tree = tree.branches.get(5);
					insertDebugCode(tree.branches.get(1));
					if (tree.branches.get(2).node.length() > 0) {
						insertDebugCode(tree.branches.get(2).branches.get(1));
					}
				} else if (tree.branches.get(6) != null) { // 'switch' ParExpression '{' SwitchBlockStatementGroups '}'
					tree = tree.branches.get(6);
					insertDebugCode(tree.branches.get(1));
					tree = tree.branches.get(3); // { SwitchBlockStatementGroup }
					for (Tree t: tree.branches) { // SwitchLabels BlockStatements
						insertDebugCode(t.branches.get(1));
					}
				} else if (tree.branches.get(7) != null) { // 'while' ParExpression Statement
					// TODO BLOCK
					tree = tree.branches.get(7);
					insertDebugCode(tree.branches.get(1).branches.get(1));
					insertDebugCode(tree.branches.get(2));
				} else if (tree.branches.get(8) != null) { // 'do' Statement 'while' ParExpression ';'
					// TODO BLOCK
					tree = tree.branches.get(8);
					insertDebugCode(tree.branches.get(1));
					insertDebugCode(tree.branches.get(3).branches.get(1));
				} else if (tree.branches.get(9) != null) { // 'for' '(' ForControl ')' Statement
					// TODO BLOCK
					tree = tree.branches.get(9); // ForVarControl | [ForInit] ';' [Expression] ';' [ForUpdate]
					if (tree.branches.get(0) != null) {
						tree = tree.branches.get(0); // {VariableModifier} Type VariableDeclaratorId ForVarControlRest
						tree = tree.branches.get(3); // ForVariableDeclaratorsRest ';' [Expression] ';' [ForUpdate] | ':' Expression
						if (tree.branches.get(0) != null) {
							tree = tree.branches.get(0); // ForVariableDeclaratorsRest ';' [Expression] ';' [ForUpdate]
							if (tree.branches.get(0).node.length() > 0) { // [ '=' VariableInitializer ] { ',' VariableDeclarator }
								insertDebugCode(tree.branches.get(0).branches.get(1));
							}
						} else {
							tree = tree.branches.get(1); // ':' Expression
							// TODO
						}
					} else {
						tree = tree.branches.get(1); // [ForInit] ';' [Expression] ';' [ForUpdate]
						// TODO
					}
				} else if (tree.branches.get(10) != null) { // 'break' [Identifier] ';'
					tree.prefix = step(tree) + "; ";
				} else if (tree.branches.get(11) != null) { // 'continue' [Identifier] ';'
					tree.prefix = step(tree) + "; ";
				} else if (tree.branches.get(12) != null) { // 'return' [Expression] ';'
					tree = tree.branches.get(12);
					if (tree.branches.get(1).node.length() > 0) {
						insertDebugCode(tree.branches.get(1));
					} else {
						tree.prefix = step(tree) + "; ";
					}
				} else if (tree.branches.get(13) != null) { // 'throw' Expression ';'
					tree = tree.branches.get(13);
					if (tree.branches.get(1).node.length() > 0) {
						insertDebugCode(tree.branches.get(1));
					}
				} else if (tree.branches.get(14) != null) { // 'synchronized' ParExpression Block
					tree = tree.branches.get(14);
					insertDebugCode(tree.branches.get(1).branches.get(1)); // ParExpression: '(' Expression ')'
					insertDebugCode(tree.branches.get(2));
				} else if (tree.branches.get(15) != null) { // 'try' Block ( [Catches] Finally | Catches )
					tree = tree.branches.get(15);
					insertDebugCode(tree.branches.get(1));
					tree = tree.branches.get(2);
					Tree catches;
					if (tree.branches.get(0) != null) { // [Catches] Finally
						catches = tree.branches.get(0).branches.get(0);
					} else { // CatchClause { CatchClause }
						catches = tree.branches.get(1);
					}
					if (catches.node.length() > 0) { // CatchClause { CatchClause }
						// CatchClause: 'catch' '(' {VariableModifier} CatchType Identifier ')' Block
						insertDebugCode(tree.branches.get(0).branches.get(6));
						for (Tree t: tree.branches.get(1).branches) {
							insertDebugCode(t.branches.get(6));
						}
					}
					if (tree.branches.get(0) != null) { // [Catches] Finally
						insertDebugCode(tree.branches.get(0).branches.get(1).branches.get(1)); // Block
					}
				} else { // 'try' ResourceSpecification Block [Catches] [Finally])
					tree = tree.branches.get(16);
					Tree res = tree.branches.get(1); // '(' Resources [';'] ')'
					res = res.branches.get(1); // Resource { ';' Resource }
					// Resource: {VariableModifier} ReferenceType VariableDeclaratorId '=' Expression
					insertDebugCode(res.branches.get(0).branches.get(4));
					for (Tree t: res.branches.get(1).branches) {
						insertDebugCode(t.branches.get(1).branches.get(4));
					}
					insertDebugCode(tree.branches.get(2)); // Block
					if (tree.branches.get(3).node.length() > 0) { // [Catches]
						Tree catches = tree.branches.get(3); // CatchClause { CatchClause }
						// CatchClause: 'catch' '(' {VariableModifier} CatchType Identifier ')' Block
						insertDebugCode(catches.branches.get(0).branches.get(6));
						for (Tree t: catches.branches.get(1).branches) {
							insertDebugCode(t.branches.get(6));
						}
					}
					if (tree.branches.get(4).node.length() > 0) { // [Finally]
						insertDebugCode(tree.branches.get(4).branches.get(1)); // Block
					}
				}
			}
			
		}
	}
	
	/**
	 * Inserts debug code in an expression.
	 * @param t the expression tree.
	 */
	private void expression(Tree t) {
		
	}

	/**
	 * Enters a section of executable code -- method, constructor or initializer block.
	 * Methods and constructors have argument and exception lists while initializer blocks
	 * don't have ones, so they may be null.
	 * @param memberDecl the root definition that uses the given block section. 
	 * @param args a reference to the argument list or null if we are entering an initializer.
	 * @param exceptions a reference to the exception list or null if we are entering an initializer.
	 * @param block the block of executable code; it may be (Block | ';') which is common
	 *   for different method declarations, i.e. may denote a method without a body in which
	 *   case we have nothing to do.
	 */
	private void method(Tree memberDecl, Tree args, Tree exceptions, Tree block) {
		if (block.def.node.equals("(Block | ';')")) {
			if (block.branches.get(0) == null) {
				return;
			}
			block = block.branches.get(0);
		}
		if (exceptions != null) { 
			if (exceptions.node.length() > 0) { // ['throws' QualifiedIdentifierList]
				exceptions = exceptions.branches.get(1);
			} else {
				exceptions = null;
			}
		}
		block(memberDecl, args, exceptions, block); // Mark entering and exiting from the block.
		insertDebugCode(block); // Continue with the statements in the block.
	}
	
	/**
	 * Appends entering and exiting scope code for the given block.
	 * @param memberDecl the root definition that uses the given block section. 
	 * @param args a reference to the arguments if we are entering a constructor or a method;
	 *   initializer blocks have args=null.
	 * @param exceptions a reference to the exception list if we are entering a constructor or
	 *   a method wit declared 'throws' clause; initializer blocks have exceptions=null.
	 * @param block a reference to a block of instructions.
	 */
	private void block(Tree memberDecl, Tree args, Tree exceptions, Tree block) { // Block: '{' BlockStatements '}'
		String scopeVar = scopeVar();
		StringBuffer buff = new StringBuffer();
		scopeArgs(memberDecl, block, scopeVar, "" , buff); // Insert inspection code for 'this' for current and wrapping classes.
		if (args != null && args.branches.get(1).node.length() > 0) { // '(' [FormalParameterDecls] ')'
			visitArgs(args.branches.get(1), scopeVar, buff); // FormalParameterDecls
		}
		getAppropriateBranch(block).suffix = enterScope(block, scopeVar, buff.toString());
		block.branches.get(2).prefix = endScope(exceptions, block, scopeVar);
	}
	
	/**
	 * Inserts inspection code for 'this' for current and wrapping classes.
	 * @param memberDecl the root definition that uses the given block section. 
	 * @param block a reference to a block of instructions.
	 * @param scopeVar the name of the variable which holds the runtime info for the current scope.
	 * @param buff a string buffer where the inspection code will be accumulated.
	 */
	private void scopeArgs(Tree memberDecl, Tree block, String scopeVar, String className, StringBuffer buff) {
		if (memberDecl.parent.def.node.equals("{Modifier} MemberDecl")) {
			memberDecl = memberDecl.parent;
			for (Tree t: memberDecl.branches.get(0).branches) {
				if (t.node.equals("static")) {
					return; // A static context does not have reference to 'this'.
				}
			}
		} else if (memberDecl.def.node.equals("['static'] Block") &&
				memberDecl.branches.get(0).node.length() > 0) {
			return; // A static context does not have reference to 'this'.
		}
		
		variableId(className + "this", "arg", scopeVar, buff);
		
		do {
			memberDecl = memberDecl.parent;
		} while (memberDecl.parent != null && !memberDecl.parent.def.node.equals("{Modifier} MemberDecl") &&
				!memberDecl.parent.def.node.equals("['static'] Block"));
		if (memberDecl.parent != null) {
			Tree t = memberDecl;
			while (t != null && !t.def.parent.node.equals("NormalClassDeclaration") &&
					!t.def.parent.node.equals("EnumDeclaration") &&
					!t.def.parent.node.equals("NormalInterfaceDeclaration")) {
				t = t.parent;
			}
			className = t.branches.get(1).node + '.' + className;
			scopeArgs(memberDecl, block, scopeVar, className, buff);
		}
	}

	/**
	 * Chooses a parse tree branch after which to generate the code for entering a scope.
	 * Usually this is the branch with opening curly bracket but in case of
	 * entering a constructor which calls another constructor from the same
	 * class -- this(...), or a superclass -- super(...), placing a code
	 * before the constructor call makes the Java code uncompilable because
	 * the call to the constructor should be the first statement in the block.
	 * This case is simply detected by peeking the first block statement
	 * and testing whether it is a call to 'this' or 'super'.
	 * 
	 * @param block a reference to a block of instructions.
	 * @return the branch of the tree where it is appropriate to place debug code --
	 *   the curly bracket of the block or the first block statement if it
	 *   is a call to 'this' or 'super'.
	 */
	private Tree getAppropriateBranch(Tree block) { // Block: '{' BlockStatements '}'
		Tree t = block.branches.get(1);
		if (t.branches.size() > 0) {
			t = t.branches.get(0); // LocalVariableDeclarationStatement | ClassOrInterfaceDeclaration | [Identifier ':'] Statement
			if (t.branches.size() == 3 && t.branches.get(2) != null) {
				t = t.branches.get(2).branches.get(1);
				/*
				 * Statement: (
				 * Block |
				 * ';' |
				 * Identifier ':' Statement |
				 * StatementExpression ';' |
				 * 'if' ParExpression Statement ['else' Statement] |
				 * 'assert' Expression [':' Expression] ';' |
				 * 'switch' ParExpression '{' SwitchBlockStatementGroups '}' |
				 * 'while' ParExpression Statement |
				 * 'do' Statement 'while' ParExpression ';' |
				 * 'for' '(' ForControl ')' Statement |
				 * 'break' [Identifier] ';' |
				 * 'continue' [Identifier] ';' |
				 * 'return' [Expression] ';' |
				 * 'throw' Expression ';' |
				 * 'synchronized' ParExpression Block |
				 * 'try' Block ( [Catches] Finally | Catches ) |
				 * 'try' ResourceSpecification Block [Catches] [Finally])
				 */
				if (t.branches.size() == 4 && t.branches.get(3) != null) {
					t = t.branches.get(3).branches.get(0); // Expression1 [ AssignmentOperator Expression ]
					if (t.branches.get(1).node.length() == 0) { // No assignment.
						t = t.branches.get(0); // Expression2 [ Expression1Rest ]
						if (t.branches.get(1).node.length() == 0) { // No triple operator.
							t = t.branches.get(0); // Expression3 [ Expression2Rest ]
							if (t.branches.get(1).node.length() == 0) { // No 'instanceof' and infix operator.
								t = t.branches.get(0); // PrefixOp Expression3 | '(' ( Type | Expression ) ')' Expression3 | Primary { Selector } { PostfixOp }
								if (t.branches.size() == 3 && t.branches.get(2) != null) {
									t = t.branches.get(2); // Primary { Selector } { PostfixOp }
									if (t.branches.get(1).branches.size() == 0 &&
											t.branches.get(2).branches.size() == 0)	{ // No selector and postfix operator.
										t = t.branches.get(0);
										/*
										 * Primary: (
										 * Literal |
										 * ParExpression |
										 * 'this' [Arguments] |
										 * 'super' SuperSuffix |
										 * 'new' Creator |
										 * NonWildcardTypeArguments ( ExplicitGenericInvocationSuffix | 'this' Arguments ) |
										 * Type '.' 'class' |
										 * Identifier { '.' Identifier } [IdentifierSuffix] |
										 * 'void' '.' 'class')
										 */
										if (t.branches.size() == 3 && t.branches.get(2) != null) { // 'this' [Arguments]
											t = t.branches.get(2);
											if (t.branches.get(1).node.length() > 0) { // Call to this(...).
												return block.branches.get(1).branches.get(0); // Place the debug code after the first block statement.
											}
										}
										if (t.branches.size() == 4 && t.branches.get(3) != null) { // 'super' SuperSuffix
											t = t.branches.get(3).branches.get(1); // Arguments | '.' Identifier [Arguments]
											if (t.branches.get(0) != null) { // Call to super(...).
												return block.branches.get(1).branches.get(0); // Place the debug code after the first block statement.
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return block.branches.get(0); // Place the debug code after the opening curly bracket.
	}
	
	private void visitArgs(Tree args, String scopeVar, StringBuffer buff) {
		if (args.def.parent.node.equals("FormalParameterDecls")) { // {VariableModifier} Type FormalParameterDeclsRest
			visitArgs(args.branches.get(2), scopeVar, buff);
		} else if (args.def.parent.node.equals("FormalParameterDeclsRest")) { // VariableDeclaratorId [ ',' FormalParameterDecls ] | '...' VariableDeclaratorId
			if (args.branches.get(0) != null) {
				args = args.branches.get(0);
				variableId(args.branches.get(0).node, "arg", scopeVar, buff);
				if (args.branches.get(1).node.length() > 0) {
					visitArgs(args.branches.get(1).branches.get(1), scopeVar, buff); // FormalParameterDecls
				}
			} else {
				variableId(args.branches.get(1).branches.get(1).node, "arg", scopeVar, buff);
			}
		}
	}
	
	/**
	 * Appends to buff an inspection code of the given tree node which should be
	 * an identifier, e.g. an argument to a constructor/method or variable declaration.
	 * @param variable the name of the variable to be inspected.
	 * @param bridgeFunction the function that inspects the given identifier, e.g. arg() for
	 *   an argument to a constructor/method or var() for a local variable.
	 * @param buff a string buffer where the result will be written.
	 */
	private void variableId(String variable, String bridgeFunction, String scopeVar, StringBuffer buff) {
		buff.append(' ');
		buff.append(bridge());
		buff.append('.');
		buff.append(bridgeFunction);
		buff.append('(');
		buff.append(scopeVar);
		buff.append(", ");
		buff.append(ann.getIdentifierId(variable));
		buff.append(", ");
		buff.append(variable);
		buff.append(");");
	}
	
	private String enterScope(Tree block, String scopeVar, String args) { // Block: '{' BlockStatements '}'
		return " long " + scopeVar + " = " + bridge() + ".scope(" +
			ann.annotation(Type.scope, block.branches.get(0).begin, block.branches.get(0).end) + "l);" + args + " try {";
	}
	
	private String endScope(Tree exceptions, Tree block, String scopeVar) { // Block: '{' BlockStatements '}'
		long annotation = ann.annotation(Type.endscope, block.branches.get(2).begin, block.branches.get(2).end);
		StringBuffer buff = new StringBuffer();
		boolean hasRuntimeExceptionInList = false;
		boolean hasErrorInList = false;
		if (exceptions != null) { // QualifiedIdentifierList: QualifiedIdentifier { ',' QualifiedIdentifier }
			String exc = exceptions.branches.get(0).node;
			if (!hasRuntimeExceptionInList && (exc.equals("RuntimeException") || exc.equals("java.lang.RuntimeException") || exc.equals("Exception") || exc.equals("java.lang.Exception") || exc.equals("Throwable") || exc.equals("java.lang.Throwable"))) {
				hasRuntimeExceptionInList = true;
			}
			if (!hasErrorInList && (exc.equals("Error") || exc.equals("java.lang.Error") || exc.equals("throwable") || exc.equals("Throwable") || exc.equals("java.lang.Throwable"))) {
				hasErrorInList = true;
			}
			exception(block, exc, scopeVar, buff);
			for (Tree t: exceptions.branches.get(1).branches) {
				exception(block, t.branches.get(1).node, scopeVar, buff);
			}
		}
		if (!hasRuntimeExceptionInList) {
			exception(block, "RuntimeException", scopeVar, buff);
		}
		if (!hasErrorInList) {
			exception(block, "Error", scopeVar, buff);
		}
		return "}" + buff + " finally { " + bridge() + ".endScope(" + annotation + "l, " + scopeVar + "); } ";
	}
	
	private void exception(Tree block, String exception, String scopeVar, StringBuffer buff) {
		String exceptionVar = bridgeName + '_' + bridgeName + "_ex";
		long annotation = ann.annotation(Type.exception, block.branches.get(2).begin, block.branches.get(2).end);
		buff.append(" catch (");
		buff.append(exception);
		buff.append(' ');
		buff.append(exceptionVar);
		buff.append(") { ");
		buff.append(bridge());
		buff.append(".exception(");
		buff.append(annotation);
		buff.append("l, ");
		buff.append(scopeVar);
		buff.append(", ");
		buff.append(exceptionVar);
		buff.append("); throw ");
		buff.append(exceptionVar);
		buff.append("; }");
	}

	private String scopeVar() {
		return bridgeName + "_" + bridgeName;
	}
	
	private String bridge() {
		return bridgeName + '.' + bridgeName;
	}
	
	private String step(Tree t) {
		return bridge() + ".step(" + ann.annotation(Type.step, t.begin, t.end) + ", " + scopeVar() + ')';
	}
	
}