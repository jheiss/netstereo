/*****************************************************************************
 * $Id$
 *****************************************************************************
 * This class represents a connection to a client via the network.
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
 * Revision 1.1  2001/03/20 06:32:48  jheiss
 * Initial revision
 *
 *****************************************************************************
 */

/* Imports */
import java.io.*;
import java.net.*;

public class NetClient implements ClientConnection
{
	private Socket socket = null;
	private BufferedReader in = null;
	private PrintWriter out = null;
	private StereoServer server = null;
	private ProtocolTranslator translator = null;
	private boolean connected = false;

	public NetClient(Socket socket, StereoServer server)
	{
		this.socket = socket;
		this.server = server;

		try
		{
			in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		}
		catch (IOException e)
		{
			System.err.println("I/O error opening I/O streams from " +
				"client socket");
			return;
		}

		translator = new ProtocolTranslator(this, server,
			socket.getInetAddress().getHostName() + ":" +
			socket.getPort());
		Thread translatorThread = new Thread(translator,
			"ProtocolTranslator for NetClient");
		translatorThread.start();

		connected = true;
	}

	public String readLine()
	{
		String inputLine = null;

		if (connected == false)
		{
			return null;
		}

		try
		{
			inputLine = in.readLine();
		}
		catch (IOException e)
		{
			System.err.println("Error reading commands from stream");
		}

		if (inputLine == null)
		{
			System.out.println("Shutting down NetClient for " +
				socket.getInetAddress().getHostName());

			try
			{
				in.close();
				out.close();
				socket.close();
				translator.shutdown();
			}
			catch (IOException e)
			{
				// Doesn't much matter
			}

			connected = false;
		}

		return inputLine;
	}

	public void println(String line)
	{
		out.println(line);
	}

	public boolean connected()
	{
		return connected;
	}
}

