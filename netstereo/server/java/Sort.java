/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Implementations of various sort routines.
 *****************************************************************************
 * Copyright (C) 2000-2001  Jason Heiss (jheiss@ofb.net)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *****************************************************************************
 * $Log$
 * Revision 1.1  2001/03/20 06:35:39  jheiss
 * Initial revision
 *
 *****************************************************************************
 */

/* Imports */
import java.text.*;

public class Sort
{
	// String comparisons are done using Collator, which supposedly gives
	// us a locale-aware comparison.  Seems to work ok for English...

	// Standard bubblesort with some simple optimizations
	public static void bubbleSort(String[] a)
	{
		Collator myCollator = Collator.getInstance();
		boolean switched = true;

		for (int pass=0 ; pass<a.length-1 && switched ; pass++)
		{
			switched = false;
			for (int i=0 ; i<a.length-pass-1 ; i++)
			{
				if (myCollator.compare(a[i], a[i+1]) > 0)
				{
					switched = true;
	    			String temp = a[i];
	    			a[i] = a[i+1];
	    			a[i+1] = temp;
				}
    		}
		}
	}

	public static void quickSort(String[] a)
	{
		quickSort(a, 0, a.length-1);
	}

	private static void quickSort(String[] a, int lb, int ub)
	{
		if (lb >= ub)
		{
			return;
		}

		// Split a in half, where everything below splitpoint is less
		// that the value there and everything above is greater.
		int splitpoint = partition(a, lb, ub);

		// Now sort the subarrays above and below the splitpoint.
		quickSort(a, lb, splitpoint-1);
		quickSort(a, splitpoint+1, ub);
	}

	private static int partition(String[] a, int lb, int ub)
	{
		String splitentry = a[lb];
		int up, down;
		Collator myCollator = Collator.getInstance();

		up = ub;
		down = lb;
		while (down < up)
		{
			while (myCollator.compare(a[down], splitentry) <= 0 && down < ub)
			{
				down++;
			}
			while (myCollator.compare(a[up], splitentry) > 0)
			{
				up--;
			}
			if (down < up)
			{
				String temp = a[down];
				a[down] = a[up];
				a[up] = temp;
			}
		}
		a[lb] = a[up];
		a[up] = splitentry;
		return up;
	}
}

