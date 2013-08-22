/*
 * Logging utility.
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

package $;

/**
 * 
 * A logging utility which tracks every piece of executable code.
 * Used by the instrumented code.
 * 
 * @author Zuben El Acribi
 *
 */
public class $ {
	
	// Initializes the logging system globally.
	static {
		
	}
	
	/**
	 * A step.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 */
	@SuppressWarnings("all")
	public static void step(long annotID, long scopeID) {
		track(annotID, scopeID, null);
	}
	
	/**
	 * Examines a boolean value.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name variable/field name.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static boolean $(long annotID, long scopeID, boolean value) {
		track(annotID, scopeID, value ? 1 : 0);
		return value;
	}

	/**
	 * Examines a boolean value.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name variable/field name.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static byte $(long annotID, long scopeID, byte value) {
		track(annotID, scopeID, value & 0xffl);
		return value;
	}

	/**
	 * Examines a short value.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name variable/field name.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static short $(long annotID, long scopeID, short value) {
		track(annotID, scopeID, value & 0xffffl);
		return value;
	}

	/**
	 * Examines a char value.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name variable/field name.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static char $(long annotID, long scopeID, char value) {
		track(annotID, scopeID, value & 0xffffl);
		return value;
	}

	/**
	 * Examines an int value.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name variable/field name.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static int $(long annotID, long scopeID, int value) {
		track(annotID, scopeID, value & 0xffffffffl);
		return value;
	}

	/**
	 * Examines a long value.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name variable/field name.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static long $(long annotID, long scopeID, long value) {
		track(annotID, scopeID, value);
		return value;
	}

	/**
	 * Examines a float value.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name variable/field name.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static float $(long annotID, long scopeID, float value) {
		track(annotID, scopeID, value);
		return value;
	}

	/**
	 * Examines a double value.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name variable/field name.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static double $(long annotID, long scopeID, double value) {
		track(annotID, scopeID, value);
		return value;
	}

	/**
	 * Examines an object value.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name variable/field name.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static <T> T $(long annotID, long scopeID, T obj) {
		track(annotID, scopeID, obj);
		return obj;
	}
	
	/**
	 * Tracks an integer value encoded in 8 bytes
	 * (the maximum data size necessary for a primitive type).
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name a variable name if this value is assigned to a variable.
	 * @param value the value bits.
	 */
	private static void track(long annotID, long scopeID, long value) {
		
	}
	
	/**
	 * Tracks a floating point value encoded in 8 bytes.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name a variable name if this value is assigned to a variable.
	 * @param value the floating point value.
	 */
	private static void track(long annotID, long scopeID, double value) {
		track(annotID, scopeID, Double.doubleToRawLongBits(value));
	}
	
	/**
	 * Tracks an object value.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @param scopeID the dynamic ID of the currently executed block.
	 * @param name a variable name if this value is assigned to a variable.
	 * @param o an object that is going to be saved into a weak reference in order
	 *   to be used later on when taking a snapshot.
	 */
	private static void track(long annotId, long scopeID, Object o) {
		
	}
	
	/**
	 * Creates a new scope. This is translated as a new stack frame by the debugger.
	 * New scopes are defined for loops also.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @return the new scope ID.
	 */
	public static long scope(long annotID) {
		return 0;
	}
	
	/**
	 * Ends the scope started by scope(). This is translated as end of stack frame by the debugger.
	 * @param annotID the static ID of the place of the tracked code we are currently on.
	 * @return the previous scope ID.
	 */
	public static long endScope(long annotID, long scopeID) {
		return 0;
	}
	
	public static void exception(long annotID, long scopeID, Throwable ex) {
		
	}
	
	public static void arg(long scopeID, int argNameID, boolean value) {
		
	}

	public static void arg(long scopeID, int argNameID, byte value) {
		
	}

	public static void arg(long scopeID, int argNameID, short value) {
		
	}

	public static void arg(long scopeID, int argNameID, char value) {
		
	}

	public static void arg(long scopeID, int argNameID, int value) {
		
	}

	public static void arg(long scopeID, int argNameID, long value) {
		
	}

	public static void arg(long scopeID, int argNameID, float value) {
		
	}

	public static void arg(long scopeID, int argNameID, double value) {
		
	}

	public static <T> void arg(long scopeID, int argNameID, T value) {
		
	}

	public static void var(long annotID, long scopeID, int argNameID, boolean value) {
		
	}

	public static void var(long annotID, long scopeID, int argNameID, byte value) {
		
	}

	public static void var(long annotID, long scopeID, int argNameID, short value) {
		
	}

	public static void var(long annotID, long scopeID, int argNameID, char value) {
		
	}

	public static void var(long annotID, long scopeID, int argNameID, int value) {
		
	}

	public static void var(long annotID, long scopeID, int argNameID, long value) {
		
	}

	public static void var(long annotID, long scopeID, int argNameID, float value) {
		
	}

	public static void var(long annotID, long scopeID, int argNameID, double value) {
		
	}

	public static <T> void var(long annotID, long scopeID, int argNameID, T value) {
		
	}

}
