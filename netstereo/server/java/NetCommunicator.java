/*****************************************************************************
 * $Id$
 *****************************************************************************
 * This class implements the network daemon portion of the StereoServer.
 * It listens on the requested TCP port and spawns off NetClient's for
 * each connection that is received.
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
 * Revision 1.1  2001/03/20 06:33:04  jheiss
 * Initial revision
 *
 *****************************************************************************
 */

/* Imports */
import java.io.*;
import java.net.*;
import java.util.*;


public class NetCommunicator implements Runnable
{
	private StereoServer server;
	private int port;
	private Vector clients;

	public NetCommunicator(StereoServer server, int port)
	{
		this.server = server;
		this.port = port;

		clients = new Vector();
	}

	public void run()
	{
		ServerSocket serverSocket = null;
		boolean listening = true;

		/* Open the server socket */
		try
		{
			serverSocket = new ServerSocket(port);
		}
		catch (IOException e)
		{
			System.err.println("Could not listen on port:  " + port);
			return;
		}

		/* Listen for and accept connections, creating a NetClient thread
		 * object for each client which connects. */
		while (listening)
		{
			Socket clientSocket = null;
			NetClient client = null;

			try
			{
				clientSocket = serverSocket.accept();
			}
			catch (IOException e)
			{
				System.err.println("I/O error while waiting for connection");
				return;
			}

			client = new NetClient(clientSocket, server);
			clients.addElement(client);

			for (Enumeration e = clients.elements(); e.hasMoreElements();)
			{
				client = (NetClient) e.nextElement();
				if (! client.connected())
				{
					clients.removeElement(client);
				}
			}
		}

		try
		{
			serverSocket.close();
		}
		catch (IOException e)
		{
			System.err.println("I/O error while closing server socket");
			// No reason to bother doing anything about it
		}
	}
}

