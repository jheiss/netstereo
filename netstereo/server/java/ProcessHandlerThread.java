/*****************************************************************************
 * $Id$
 *****************************************************************************
 * This class starts up a system process and passes input and output to and
 * from any classes which implement the ProcessOutputHandler interface
 * and which register with this object.
 *****************************************************************************
 * $Log$
 *****************************************************************************
 */

/* Imports */
import java.io.*;
import java.util.*;

/* Constants */

public class ProcessHandlerThread extends Thread
{
	private static final int SLEEP_INTERVAL = 10;

	private String processCommand = null;
	private boolean persist = false;  // Should we restart the process?
	private Process process = null;
	private PrintWriter in = null;
	private BufferedReader out = null;
	private BufferedReader err = null;
	private Vector outputHandlers = null;

	public ProcessHandlerThread(String processCommand, boolean persist)
	{
		setName("ProcessHandlerThread");
		this.processCommand = processCommand;
		this.persist = persist;
		outputHandlers = new Vector();
	}

	public ProcessHandlerThread(String processCommand)
	{
		this(processCommand, false);
	}

	private void startProcess()
	{
		try
		{
			System.err.println("Starting process:  " + processCommand);
			process = Runtime.getRuntime().exec(processCommand);
		}
		catch (IOException ioException)
		{
			ioException.printStackTrace();
		}

		// stdin
		in = new PrintWriter(process.getOutputStream(), true);
		// stdout
		out = new BufferedReader(new
			InputStreamReader(process.getInputStream()));
		// stderr
		err = new BufferedReader(new
			InputStreamReader(process.getErrorStream()));
	}

	public void run()
	{
		startProcess();

		while (true)
		{
			try
			{
				int exitValue = process.exitValue();
				if (exitValue == 0 && persist)
				{
					System.err.println("Restarting process");
					startProcess();
				}
				else
				{
					System.err.println("Process exited with non-zero value, " +
						"stopping ProcessHandlerThread thread");
					return;
				}
			}
			catch (IllegalThreadStateException itsException)
			{
				// Don't do anything, process hasn't exited
			}

			try
			{
				while (out.ready())
				{
					String outputLine = out.readLine();

					for (Enumeration e = outputHandlers.elements();
						e.hasMoreElements();)
					{
						((ProcessOutputHandler) e.nextElement()).
							outputFromProcess(outputLine);
					}
				}
				while (err.ready())
				{
					String errorLine = err.readLine();

					for (Enumeration e = outputHandlers.elements();
						e.hasMoreElements();)
					{
						((ProcessOutputHandler) e.nextElement()).
							errorFromProcess(errorLine);
					}
				}
			}
			catch (IOException ioException)
			{
				ioException.printStackTrace();
			}

			try
			{
				sleep(SLEEP_INTERVAL);
			}
			catch (InterruptedException e)
			{
				 //Do nothing
			}
		}

		//process.destroy();
	}

	public void inputToProcess(String inputLine)
	{
		in.println(inputLine);
	}

	public void addHandler(ProcessOutputHandler handler)
	{
		outputHandlers.addElement(handler);
	}

	protected void finalize()
	{
		System.out.println("Killing process");
		process.destroy();
	}
}

