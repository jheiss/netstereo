/*****************************************************************************
 * $Id$
 *****************************************************************************
 * This interface indicates that an object is capable of handling the
 * output from a process controlled by a ProcessHandlerThread.
 *****************************************************************************
 * $Log$
 *****************************************************************************
 */

/* Imports */

interface ProcessOutputHandler
{
	public void outputFromProcess(String outputLine);
	public void errorFromProcess(String errorLine);
}

