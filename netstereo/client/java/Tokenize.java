/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Various methods for splitting up strings.
 *****************************************************************************
 * $Log$
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

