/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Initiate I/O with the Stereo Server over the network using Java Micro
 * Edition API.
 *
 * Not even remotely working.
 *****************************************************************************
 * Copyright (C) 2001  Jason Heiss (jheiss@ofb.net)
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
 *****************************************************************************
 */

/* Imports */
import java.io.*;
//import java.net.*;
import java.microedition.io.*;
//import java.util.*;

public class MicroNetClient
{
	static final String SERVER = "thunder.aput.net";
	//static final String SERVER = "blizzard.aput.net";
	//static final String SERVER = "localhost";
	static final int PORT = 12345;

	//public NetClient(StereoClient client)

	public static ServerIOHandler createIOHandler(StereoClient client)
	{
		//Socket socket = null;
		StreamConnection connection = null;
		InputStream in = null;
		OutputStream out = null;

		try
		{
			connection = Connector.open("stream://" + SERVER + ":" + PORT);
		}
		catch (IllegalArgumentException e)
		{
			System.err.println("Illegal arguments to Connector.open");
			e.printStackTrace();
			return(null);
		}
		catch (ConnectionNotFoundException e)
		{
			System.err.println("Error creating connection to " + SERVER +
				" on port " + PORT);
			e.printStackTrace();
			return(null);
		}
		catch (IOException e)
		{
			System.err.println("IOException when creating connection to "
				+ SERVER + " on port " + PORT);
			e.printStackTrace();
			return(null);
		}

		try
		{
			in = connection.openInputStream();
			out = connection.openOutputStream();
		}
		catch (IOException e)
		{
			System.err.println("Error creating connection I/O streams");
			e.printStackTrace();
			return(null);
		}

		ServerIOHandler ioHandler = new ServerIOHandler(in, out, client);
		return(ioHandler);
	}
}

