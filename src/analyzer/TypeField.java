/*
 * A field in a type.
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

import java.lang.reflect.Field;

import bnf.Tree;

/**
 * 
 * Denotes a type field. A type field can be created out of parsed source
 * code (Tree) or compiled Java code (Class<?>).
 * 
 * @author Zuben El Acribi
 *
 */
public class TypeField {

	/**
	 * A field may have 'public', 'protected', 'private' or no visibility.
	 * In the latter case it is said that it has 'package scope' visibility.
	 */
	public enum Visibility { PUBLIC, PROTECTED, PRIVATE, PACKAGE_SCOPE }
	
	private Visibility visibility;
	private boolean isStatic;
	private boolean isFinal;
	private boolean isTransient;
	
	/**
	 * This field will be non-null if it is created out of parsed source code.
	 */
	private Tree t;
	
	/**
	 * The class containing this field. Non-null if this field is created out
	 * of compiled code.
	 */
	private Class<?> parentClass;
	
	/**
	 * This field will be non-null if it is created out of compiled code.
	 */
	private Field f;
	
	/**
	 * Creates a field out of parsed source code.
	 * @param t a parse tree.
	 */
	public TypeField(Tree t) {
		this.t = t;
		extractInfo();
	}
	
	public TypeField(Class<?> parent, Field field) {
		this.parentClass = parent;
		this.f = f;
		// TODO IMPLEMENT !!!
	}
	
// --------------------------------------------------------------------------------------------------------------------
// Public methods.
// --------------------------------------------------------------------------------------------------------------------

	public Visibility getVisibility() {
		return visibility;
	}
	
	public boolean isStatic() {
		return isStatic;
	}
	
	public boolean isFinal() {
		return isFinal;
	}
	
	public boolean isTransient() {
		return isTransient;
	}
	
// --------------------------------------------------------------------------------------------------------------------
// Private methods.
// --------------------------------------------------------------------------------------------------------------------

	private void extractInfo() {
		// TODO IMPLEMENT !!!
		if (t.def.node.equals("Identifier")) {
			Tree u = t.parent;
			if (u.def.parent.node.equals("MethodOrFieldDecl")) {
				// TODO IMPLEMENT !!!
			}
		}
	}
	
}
