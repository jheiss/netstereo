/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Open up a serial port and create an ProtocolTranslator.
 *****************************************************************************
 * $Log$
 *****************************************************************************
 */

/* Imports */
import java.io.*;
import java.util.*;
import javax.comm.*;

public class SerialCommunicator implements ClientConnection
{
	private StereoServer server = null;
	private SerialPort serialPort = null;
	private BufferedReader in = null;
	private PrintWriter out = null;
	private ProtocolTranslator translator = null;

	public SerialCommunicator(StereoServer server, String serialPortName,
		int serialBaudRate)
	{
		this.server = server;

		CommPortIdentifier portId = null;
		try
		{
			portId = CommPortIdentifier.getPortIdentifier(serialPortName);
		}
		catch (NoSuchPortException e)
		{
			System.err.println("No such port:  " + serialPortName);
			return;
		}

		try
		{
			System.out.println("Opening port " + portId.getName());
			serialPort = (SerialPort) portId.open("StereoServer", 2000);
		}
		catch (PortInUseException e)
		{
			System.err.println("Serial port in use by " + e.currentOwner);
			System.err.println("If nothing is using the port, check the " +
				"permissions on the device in /dev and the permissions on " +
				"the lock directory (i.e. /var/lock)");
			return;
		}

		try
		{
			serialPort.setSerialPortParams(serialBaudRate,
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
			return;
		}

		System.err.println("CD:  " + serialPort.isCD());
		System.err.println("CTS:  " + serialPort.isCTS());
		System.err.println("DSR:  " + serialPort.isDSR());
		System.err.println("DTR:  " + serialPort.isDTR());
		System.err.println("RI:  " + serialPort.isRI());
		System.err.println("RTS:  " + serialPort.isRTS());

		try
		{
			in = new BufferedReader(
                new InputStreamReader(serialPort.getInputStream()));
			out = new PrintWriter(serialPort.getOutputStream(), true);
		}
		catch (IOException e)
		{
			System.err.println("I/O error opening I/O streams from serial " +
				"port");
			return;
		}

		translator = new ProtocolTranslator(this, server,
				"serial:" + serialPortName);
		Thread translatorThread = new Thread(translator,
			"ProtocolTranslator for SerialCommunicator");
		translatorThread.start();
	}

	public String readLine()
	{
		String inputLine = null;

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
			try
			{
				System.out.println("Shutting down SerialCommunicator");

				in.close();
				out.close();
				serialPort.close();
				translator.shutdown();
			}
			catch (IOException e)
			{
				// Doesn't much matter
			}
		}

		return inputLine;
	}

	public void println(String line)
	{
		/* If the serial port isn't ready for data we just return
		 * (thus discarding this message) since we don't want to block.
		 */
		if (! serialPort.isCTS())
		{
			return;
		}

		out.println(line);
	}
}

