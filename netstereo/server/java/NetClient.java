/*****************************************************************************
 * $Id$
 *****************************************************************************
 * This class represents a connection to a client via the network.
 *****************************************************************************
 * $Log$
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

