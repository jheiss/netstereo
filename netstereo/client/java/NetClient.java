/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Initiate I/O with the Stereo Server over the network.
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
 *****************************************************************************
 */

/* Imports */
import java.io.*;
import java.net.*;

public class NetClient
{
	public static ServerIOHandler createIOHandler(StereoClient client,
		String server, int port)
	{
		Socket socket = null;
		InputStream in = null;
		OutputStream out = null;

		try
		{
			socket = new Socket(server, port);
		}
		catch (IOException e)
		{
			System.err.println("Error creating connection to " + server +
				" on port " + port);
			e.printStackTrace();
		}

		try
		{
			in = socket.getInputStream();
			out = socket.getOutputStream();
		}
		catch (IOException e)
		{
			System.err.println("Error creating socket I/O streams");
			e.printStackTrace();
			return(null);
		}

		ServerIOHandler ioHandler = new ServerIOHandler(in, out, client);
		return(ioHandler);
	}
}

