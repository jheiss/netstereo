/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Return or display various bits of info about the threads associated with
 * this program.
 *****************************************************************************
 * $Log$
 *****************************************************************************
 */

/* Imports */

public class ThreadInfo
{
	public int numberOfThreads()
	{
		//System.out.println("There are " + Thread.activeCount() + " threads");
		return Thread.activeCount();
	}

	public Thread[] currentThreads()
	{
		Thread[] threadArray = new Thread[numberOfThreads()];
		int numStored = Thread.enumerate(threadArray);
		//System.out.println(numStored +
			//" threads put into array by enumerate");
		return threadArray;
	}

	public void displayThreads()
	{
		Thread[] threadArray = currentThreads();
		//System.out.println("Array has " + threadArray.length + " elements");
		for (int i=0 ; i<threadArray.length ; ++i)
		{
			if (threadArray[i] != null)
			{
				System.err.println("  " + i + ":  " + threadArray[i].getName());
			}
		}
	}
}

