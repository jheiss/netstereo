/*****************************************************************************
 * $Id$
 *****************************************************************************
 * This interface indicates that the class represents a connection to
 * a client and thus ProtocolHandler can request traffic to and from
 * that client via this interface.
 *****************************************************************************
 * $Log$
 *****************************************************************************
 */

/* Imports */

interface ClientConnection
{
	public String readLine();
	public void println(String line);
}

