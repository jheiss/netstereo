/*****************************************************************************
 * $Id$
 *****************************************************************************
 * The main class of the StereoServer application.  A StereoServer
 * listens for commands on various media (network, serial, etc.) and
 * uses those commands to control a song player.  At a minimum the
 * song player knows how to play MP3 files, and may know how to play
 * other formats.
 *****************************************************************************
 * $Log$
 *****************************************************************************
 */

/* Imports */
import java.io.*;
import java.util.*;

public class StereoServer
{
	SongPlayer player = null;
	//Thread playerThread;
	NetCommunicator netCommunicator;
	//Thread netCommunicatorThread;
	SerialCommunicator serialCommunicator;
	//Thread serialServerThread;

	// Server configuration properties
	static final String propertiesFilename = "server.properties";
	static final String defaultWorkingDirectory = "/weather/music";
	private String workingDirectory;
	static final boolean defaultStartNetCommunicator = true;
	private boolean startNetCommunicator;
	static final int defaultNetPort = 12345;
	private int netPort;
	static final boolean defaultStartSerialCommunicator = false;
	private boolean startSerialCommunicator;
	static final String defaultSerialPort = "/dev/ttyS0";
	private String serialPort;
	//static final String defaultSerialPort = "COM1";
	static final int defaultSerialBaudRate = 9600;
	private int serialBaudRate;
	static final String defaultTypeOfPlayer = "JMF";
	private String typeOfPlayer;
	//static final String defaultTypeOfPlayer = "MPG123";
	static final String defaultStartupMusicFile = "/home/jheiss/sounds/box.mp3";
	private String startupMusicFile;

	public static void main(String[] args) throws IOException
	{
		StereoServer myServer = new StereoServer();

		// This seems kinda wrong...
		while (true)
		{
			try
			{
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException e)
			{
				// Nothing to do
			}
		}
	}

	public StereoServer()
	{
		ThreadInfo ti = new ThreadInfo();

		// Load the properties
		loadProperties();

		// Change to the working directory
		// This doesn't work!
		//System.setProperty("user.dir", workingDirectory);

		// Start a SongPlayer so that it is ready to receive commands
		System.err.println("Starting SongPlayer");
		if (typeOfPlayer.equals("MPG123"))
		{
			player = new MPG123SongPlayer();
			//player = new MPG123SongPlayer(workingDirectory);
		}
		else if (typeOfPlayer.equals("JMF"))
		{
			player = new JMFSongPlayer();
			//player = new JMFSongPlayer(workingDirectory);
			Thread playerThread = new Thread((JMFSongPlayer) player,
				"JMFSongPlayer");
			playerThread.start();
		}
		else
		{
			System.err.println("Unrecognized player type:  " + typeOfPlayer);
			System.exit(-1);
		}

		if (! (startNetCommunicator || startSerialCommunicator))
		{
			System.err.println("No form of client listener is set to run!");
			System.exit(-1);
		}
			
		if (startNetCommunicator)
		{
			System.out.println("Starting a NetCommunicator");
			netCommunicator = new NetCommunicator(this, netPort);
			Thread netCommunicatorThread = new Thread(netCommunicator,
				"NetCommunicator");
			netCommunicatorThread.start();
		}

		if (startSerialCommunicator)
		{
			System.out.println("Starting a SerialCommunicator");
			serialCommunicator = new SerialCommunicator(this, serialPort,
				serialBaudRate);
			//Thread serialServerThread = new Thread(serialCommunicator,
				//"SerialCommunicator");
			//serialServerThread.start();
		}

		/* Play a sound to let the user know that the server is ready */
		System.out.println("Playing " + startupMusicFile +
			" to indicate startup");
		player.clearPlaylist();
		player.addSongToPlaylist(startupMusicFile, 0);
		player.play(0);
		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException ie)
		{
			// Oh well...
		}
		player.stop();
		player.clearPlaylist();

		ti.displayThreads();

		// Install a signal handler to catch SIGINT
	}

	private void loadProperties()
	{
		FileInputStream propertiesFileInputStream = null;
		try
		{
			propertiesFileInputStream =
				new FileInputStream(propertiesFilename);
		}
		catch (FileNotFoundException fnfException)
		{
			System.err.println("Properties file " + propertiesFilename +
				" could not be opened");
			System.exit(-1);
		}
		Properties serverProps = new Properties();
		try
		{
			serverProps.load(propertiesFileInputStream);
		}
		catch (IOException ioException)
		{
			System.err.println("Error reading properties file " +
				propertiesFilename);
			System.exit(-1);
		}
		workingDirectory = serverProps.getProperty("WorkingDirectory",
			defaultWorkingDirectory);
		startNetCommunicator = new Boolean(serverProps.
			getProperty("StartNetCommunicator", new
			Boolean(defaultStartNetCommunicator).toString())).booleanValue();
		netPort = new Integer(serverProps.getProperty("NetPort",
			new Integer(defaultNetPort).toString())).intValue();
		startSerialCommunicator = new Boolean(
			serverProps.getProperty("StartSerialCommunicator", new
			Boolean(defaultStartSerialCommunicator).toString())).booleanValue();
		serialPort = serverProps.getProperty("SerialPort", defaultSerialPort);
		serialBaudRate = new Integer(serverProps.getProperty("SerialBaudRate",
			new Integer(defaultSerialBaudRate).toString())).intValue();
		typeOfPlayer = serverProps.getProperty("TypeOfPlayer",
			defaultTypeOfPlayer);
		startupMusicFile = serverProps.getProperty("StartupMusicFile",
			defaultStartupMusicFile);
	}

	public SongPlayer getSongPlayer()
	{
		return player;
	}
}

