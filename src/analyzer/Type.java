/*
 * The root class of all types.
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

import bnf.Tree;

/**
 * 
 * Denotes the root class of all types: classes, interfaces, enums,
 * arrays and primitive types.<br/>
 * <br/>
 * This basic type can be created out of a parsed source code (Tree)
 * or compiled Java code (Class<?>).
 * 
 * @author Zuben El Acribi
 *
 */
public class Type {

	/**
	 * If the type denotes a parsed source code then this is its parse tree.
	 */
	private Tree t;
	
	/**
	 * If the type denotes a compiled Java code then this is its Class object.
	 */
	private Class<?> clazz;
	
	/**
	 * Creates a Type out of parsed source code.
	 * @param t parse tree.
	 */
	public Type(Tree t) {
		this.t = t;
	}
	
	/**
	 * Creates a Type out of compile Java code.
	 * @param clazz the class object.
	 */
	public Type(Class<?> clazz) {
		this.clazz = clazz;
	}
	
// --------------------------------------------------------------------------------------------------------------------
// Public methods.
// --------------------------------------------------------------------------------------------------------------------

	public TypeField[] getFields() {
		// TODO IMPLEMENT !!!
		return null;
	}
	
	public Type[] getInnerTypes() {
		// TODO IMPLEMENT !!!
		return null;
	}

	public MethodDef[] getMethods() {
		// TODO IMPLEMENT !!!
		return null;
	}
	
// --------------------------------------------------------------------------------------------------------------------
// Private methods.
// --------------------------------------------------------------------------------------------------------------------

}
