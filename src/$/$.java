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
	
	private static final long STEP     = 0x0l;
	private static final long EXPR     = 0x1l;
	private static final long SCOPE    = 0x2l;
	private static final long ENDSCOPE = 0x3l;
	private static final long VAR      = 0x4l;
	private static final long VARDECL  = 0x5l;
	private static final long ARG      = 0x6l;

	// Initializes the logging system globally.
	static {
		
	}
	
	/**
	 * A step.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 */
	@SuppressWarnings("all")
	public static void $(long annotID) {
		
	}
	
	/**
	 * Examines a boolean value.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static boolean $(long annotID, boolean value) {
		track(annotID, null, value ? 1 : 0);
		return value;
	}

	/**
	 * Examines a boolean value.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static byte $(long annotID, byte value) {
		track(annotID, null, value & 0xffl);
		return value;
	}

	/**
	 * Examines a short value.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static short $(long annotID, short value) {
		track(annotID, null, value & 0xffffl);
		return value;
	}

	/**
	 * Examines a char value.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static char $(long annotID, char value) {
		track(annotID, null, value & 0xffffl);
		return value;
	}

	/**
	 * Examines an int value.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static int $(long annotID, int value) {
		track(annotID, null, value & 0xffffffffl);
		return value;
	}

	/**
	 * Examines a long value.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static long $(long annotID, long value) {
		track(annotID, null, value);
		return value;
	}

	/**
	 * Examines a float value.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static float $(long annotID, float value) {
		track(annotID, null, value);
		return value;
	}

	/**
	 * Examines a double value.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static double $(long annotID, double value) {
		track(annotID, null, value);
		return value;
	}

	/**
	 * Examines an object value.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param value the result of an expression.
	 * @return the 'value' argument.
	 */
	@SuppressWarnings("all")
	public static <T> T $(long annotID, T obj) {
		track(annotID, null, obj);
		return obj;
	}
	
	/**
	 * Tracks an integer value encoded in 8 bytes
	 * (the maximum data size necessary for a primitive type).
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name a variable name if this value is assigned to a variable.
	 * @param value the value bits.
	 */
	private static void track(long annotID, String name, long value) {
		
	}
	
	/**
	 * Tracks a floating point value encoded in 8 bytes.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name a variable name if this value is assigned to a variable.
	 * @param value the floating point value.
	 */
	private static void track(long annotID, String name, double value) {
		
	}
	
	/**
	 * Tracks an object value.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name a variable name if this value is assigned to a variable.
	 * @param o an object that is going to be saved into a weak reference in order
	 *   to be used later on when taking a snapshot.
	 */
	private static void track(long annotId, String name, Object o) {
		
	}
	
	/**
	 * Creates a new scope. This is translated as a new stack frame by the debugger.
	 * New scopes are defined for loops also.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 */
	public static void scope(long annotID) {
		
	}
	
	/**
	 * Examines a boolean local variable.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name the argument name
	 * @param value the argument value
	 */
	public static void var(long annotID, String name, boolean value) {
		track(annotID, name, value);
	}

	/**
	 * Examines a byte local variable.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name the argument name
	 * @param value the argument value
	 */
	public static void var(long annotID, String name, byte value) {
		track(annotID, name, value);
	}

	/**
	 * Examines a short local variable.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name the argument name
	 * @param value the argument value
	 */
	public static void var(long annotID, String name, short value) {
		track(annotID, name, value);
	}

	/**
	 * Examines a char local variable.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name the argument name
	 * @param value the argument value
	 */
	public static void var(long annotID, String name, char value) {
		track(annotID, name, value);
	}

	/**
	 * Examines an int local variable.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name the argument name
	 * @param value the argument value
	 */
	public static void var(long annotID, String name, int value) {
		track(annotID, name, value);
	}

	/**
	 * Examines a long local variable.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name the argument name
	 * @param value the argument value
	 */
	public static void var(long annotID, String name, long value) {
		track(annotID, name, value);
	}

	/**
	 * Examines a float local variable.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name the argument name
	 * @param value the argument value
	 */
	public static void var(long annotID, String name, float value) {
		track(annotID, name, value);
	}

	/**
	 * Examines a double local variable.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name the argument name
	 * @param value the argument value
	 */
	public static void var(long annotID, String name, double value) {
		track(annotID, name, value);
	}

	/**
	 * Examines an object local variable.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 * @param name the argument name
	 * @param value the argument value
	 */
	public static void var(long annotID, String name, Object value) {
		track(annotID, name, value);
	}

	/**
	 * Ends the scope started by scope(). This is translated as end of stack frame by the debugger.
	 * @param annotID the ID of the place of the tracked code we are currently on.
	 */
	public static void endscope(long annotID) {
		
	}

}
