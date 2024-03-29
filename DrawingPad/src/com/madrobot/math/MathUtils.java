/*******************************************************************************
 * Copyright (c) 2011 MadRobot.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *  Elton Kent - initial API and implementation
 ******************************************************************************/
package com.madrobot.math;

public class MathUtils {
	public static double round(double value, int places) {
		if (places < 0) {
			throw new IllegalArgumentException();
		}

		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}

	/**
	 * Rounds the given double value
	 * 
	 * @param value
	 *            the value
	 * @return the rounded value, x.5 and higher is rounded to x + 1.
	 * @since CLDC 1.1
	 */
	public static long round(double value) {
		if (value < 0) {
			return (long) (value - 0.5);
		} else {
			return (long) (value + 0.5);
		}
	}

	/**
	 * Equivalent to Math.max(low, Math.min(high, amount));
	 */
	public static float constrain(final float amount, final float low,
			final float high) {
		return amount < low ? low : amount > high ? high : amount;
	}

	/**
	 * Equivalent to Math.max(low, Math.min(high, amount));
	 */
	public static int constrain(final int amount, final int low, final int high) {
		return amount < low ? low : amount > high ? high : amount;
	}

}
