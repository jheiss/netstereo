/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Initiate I/O with the Stereo Server over a serial connection.
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
import java.util.*;
import javax.comm.*;

public class SerialClient
{
	public static ServerIOHandler createIOHandler(StereoClient client,
		String portName, int baudRate)
	{
		CommPortIdentifier portId = null;
		SerialPort serialPort = null;
		InputStream inStream = null;
		OutputStream outStream = null;
		ServerIOHandler ioHandler = null;

		try
		{
			portId = CommPortIdentifier.getPortIdentifier(portName);
		}
		catch (NoSuchPortException e)
		{
			System.err.println("No such port:  " + portName);
			return(null);
		}

		try
		{
			System.out.println("Opening port " + portId.getName());
			serialPort = (SerialPort) portId.open("StereoClient", 2000);
		}
		catch (PortInUseException e)
		{
			System.err.println("Serial port in use by " + e.currentOwner);
			System.err.println("If nothing is using the port, check the " +
				"permissions on the device in /dev and the permissions on " +
				"the lock directory (i.e. /var/lock)");
			return(null);
		}

		try
		{
			serialPort.setSerialPortParams(baudRate,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);

			serialPort.setFlowControlMode(
				SerialPort.FLOWCONTROL_RTSCTS_IN |
				SerialPort.FLOWCONTROL_RTSCTS_OUT);
		}
		catch (UnsupportedCommOperationException e)
		{
			System.err.println("Failed to set params on serial port");
			return(null);
		}

		try
		{
			inStream = serialPort.getInputStream();
			outStream = serialPort.getOutputStream();
		}
		catch (IOException e)
		{
			System.err.println("IOException opening I/O streams");
			return(null);
		}

		ioHandler = new ServerIOHandler(inStream, outStream, client);

		return(ioHandler);
	}
}

