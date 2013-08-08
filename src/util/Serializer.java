/*
 * Object serializer.
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

package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

/**
 * 
 * Serializes to an output stream objects and primitive values passed as arguments
 * to methods in stack frames, which frames form a stack for a given thread.
 * 
 * @author Zuben El Acribi
 *
 */
public class Serializer {

	/**
	 * 
	 * Represents a stack frame.
	 * Contains the name of the method we have invoked
	 * and the arg names + values.
	 * 
	 */
	private static class Frame {
		
		/**
		 * The name of the method we have invoked.
		 */
		public String methodName;
		
		/**
		 * The names of the arguments.
		 */
		public String[] argNames;
		
		/**
		 * The values of the arguments.
		 */
		public WeakReference<Object>[] values;
		
		/**
		 * Constructs a frame using the provided data for the method call.<br/>
		 * We should provide an equal number of arguments and values.
		 * @param methodName the name of the method we have invoked.
		 * @param arguments a comma-separated list of the argument names.
		 * @param values the values of the arguments.
		 */
		@SuppressWarnings("all")
		public Frame(String methodName, String arguments, Object... values) {
			this.methodName = methodName;
			this.argNames = arguments.split(",");
			this.values = (WeakReference<Object>[]) new WeakReference<?>[values.length];
			
			assert argNames.length == values.length :
				"Number of arguments not equal to number of values: arguments=" + argNames.length + ", value=" + values.length;
			for (int i = 0; i < argNames.length; i++) {
				argNames[i] = argNames[i].trim();
				values[i] = new WeakReference<Object>(values[i]);
			}
		}
		
	}

	/**
	 * A class with which the Serializer is initialized.<br/>
	 * This class tells the Serializer when to take snapshots.
	 */
	public static class SnapshotMaker {
		
		/**
		 * Number of times mayTakeSnapshot() has been invoked.
		 */
		private int count;
		
		/**
		 * A stream where to place the objects from all the frames from all the stacks.
		 */
		private OutputStream stream;
		
		/**
		 * Tells the Serializer whether it can take a snapshot of the current state.
		 */
		public synchronized boolean mayTakeSnapshot() {
			return (++count & 0x7f) == 0;
		}
		
		/**
		 * @return an output stream where the objects are going to be serialized.
		 */
		public OutputStream provideOutputStream() {
			if (stream == null) {
				try {
					File f = new File(System.getProperty("snapshot.path", "./snapshot-" + datetime() + '-' + randomNumber()));
					stream = new FileOutputStream(f);
					Runtime.getRuntime().addShutdownHook(new Thread() {
						@Override
						public void run() {
							try {
								stream.close();
							} catch (IOException ex) {
								throw new RuntimeException(ex);
							}
						}
					});
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
			return stream;
		}
		
		/**
		 * @return the date + time as string which is used to form a unique file name.
		 */
		private String datetime() {
			Calendar cal = Calendar.getInstance();
			char[] c = new char[] { '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0' };
			c[0] += (cal.get(Calendar.YEAR) / 10) % 10;
			c[1] += cal.get(Calendar.YEAR) % 10;
			c[2] += (cal.get(Calendar.MONTH) / 10) % 10;
			c[3] += cal.get(Calendar.MONTH) % 10;
			c[4] += (cal.get(Calendar.DATE) / 10) % 10;
			c[5] += cal.get(Calendar.DATE) % 10;
			c[6] += (cal.get(Calendar.HOUR_OF_DAY) / 10) % 10;
			c[7] += cal.get(Calendar.HOUR_OF_DAY) % 10;
			c[8] += (cal.get(Calendar.MINUTE) / 10) % 10;
			c[9] += cal.get(Calendar.MINUTE) % 10;
			c[10] += (cal.get(Calendar.SECOND) / 10) % 10;
			c[11] += cal.get(Calendar.SECOND) % 10;
			return new String(c);
		}
		
		/**
		 * @return a random number which is used to form a unique file name.
		 */
		private String randomNumber() {
			Random rand = new Random();
			char[] c = new char[] { '0', '0', '0', '0', '0', '0' };
			for (int i = 0; i < c.length; i++) {
				c[i] += rand.nextInt(10);
			}
			return new String(c);
		}
		
	}
	
	/**
	 * The whole state is a set of the stacks of all threads.
	 * Each stack is composed of frames with info about method names, argument names and
	 * argument values.
	 */
	private static HashMap<Thread, Stack<Frame>> state = new HashMap<Thread, Stack<Frame>>();
	
	/**
	 * A utility which tells the logging system when to take a snapshot
	 * and where to place the serialized objects.
	 */
	private static SnapshotMaker snapShotMaker = new SnapshotMaker();
	
	/**
	 * Sets a SnapshotMaker which may implement different logic.
	 * @param snap a SnapshotMaker instance.
	 */
	public static void setSnapShotMaker(SnapshotMaker snap) {
		snapShotMaker = snap;
	}
	
	/**
	 * Entering a method.<br/>
	 * Saves objects in memory and makes them available for taking snapshots
	 * of the current state.
	 * @param methodName the name of the method in which we entered.
	 * @param argNames a comma-separated list of the argument names.
	 * @param args the arguments themselves.
	 */
	public static void startMethod(String methodName, String argNames, Object... args) {
		Stack<Frame> stack;
		synchronized (state) {
			stack = state.get(Thread.currentThread());
			if (stack == null) {
				state.put(Thread.currentThread(), stack = new Stack<Frame>());
			}
		}
		stack.push(new Frame(methodName, argNames, args));
		if (snapShotMaker.mayTakeSnapshot()) {
			takeSnapshot();
		}
	}
	
	/**
	 * Leaving a method.<br/>
	 * Pops the last stack frame.
	 */
	public static void endMethod() {
		Stack<Frame> stack;
		synchronized (state) {
			stack = state.get(Thread.currentThread());
			assert stack != null : "An error occured: no stack available";
			assert stack.size() > 0 : "An error occured: we are leaving a method which we have never entered.";
			stack.pop();
			if (stack.size() == 0) {
				state.remove(Thread.currentThread());
			}
		}
	}
	
	/**
	 * Serializes the whole current state to the output stream provided by the snapshot maker.
	 */
	public static synchronized void takeSnapshot() {
		
	}
	
}
