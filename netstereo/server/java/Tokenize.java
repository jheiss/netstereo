/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Various methods for splitting up strings.
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
 * Revision 1.1  2001/03/20 06:36:42  jheiss
 * Initial revision
 *
 *****************************************************************************
 */

/* Imports */
import java.util.*;

public class Tokenize
{
	public static String[] tokenize(String tokenString)
	{
		return tokenize(tokenString, null);
	}

	public static String[] tokenize(String tokenString, String delim)
	{
		StringTokenizer st = null;
		String[] tokenizedString;
		int i = 0;

		if (delim == null)
		{
			st = new StringTokenizer(tokenString);
		}
		else
		{
			st = new StringTokenizer(tokenString, delim);
		}
		tokenizedString = new String[st.countTokens()];
		while (st.hasMoreTokens())
		{
			tokenizedString[i] = st.nextToken();
			++i;
		}

		return tokenizedString;
	}

	// StringTokenizer interprets delim as a set of independent,
	// single-character deliminators.  This method interprets delim
	// as a multi-character deliminator.
	// BROKEN!
	public static String[] tokenizeWithMultiCharDelim(String tokenString, String delim)
	{
		StringTokenizer st = null;
		Vector tokenizedStringVector = new Vector();
		String[] tokenizedString;
		int i = 0;
		int tokenIndex = 0;

		if (delim == null)
		{
			return tokenize(tokenString);
		}

		while (tokenIndex != -1)
		{
			int nextIndex = tokenString.indexOf(delim, tokenIndex);
			if (nextIndex != -1)
			{
				tokenizedStringVector.addElement(
					tokenString.substring(tokenIndex,
					nextIndex - delim.length()));
			}
			else
			{
				tokenizedStringVector.addElement(
					tokenString.substring(tokenIndex));
			}
			tokenIndex = nextIndex;
			++i;
		}

		// Now we know how big of an array we need
		tokenizedString = new String[i];
		i = 0;
		for (Enumeration e = tokenizedStringVector.elements();
			e.hasMoreElements();)
		{
			tokenizedString[i] = (String) e.nextElement();
			++i;
		}
		return tokenizedString;
	}
}

