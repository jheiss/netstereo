/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Pass information between the SongPlayer and the client.
 *
 * This is done by converting Java variables into text messages (or vice
 * versa).  The text messages are sent/received on some form of communications
 * media (the details of which we are blissfully unaware).
 *****************************************************************************
 * $Log$
 *****************************************************************************
 */

/* Imports */
import java.io.*;
import java.util.*;

/* Constants */

public class ProtocolTranslator implements Runnable
{
	private ClientConnection client = null;
	private StereoServer server = null;
	private SongPlayer songPlayer = null;
	private String clientIdentity = "";
	private boolean authorized = false;
	private boolean sendPlaylists = true;  // Send playlists to client?
	private boolean run = false;

	public ProtocolTranslator(ClientConnection client, StereoServer server,
		String clientIdentity)
	{
		this.client = client;
		this.server = server;
		this.clientIdentity = clientIdentity;

		songPlayer = server.getSongPlayer();
	}

	public void run()
	{
		String inputLine;

		// Run until someone tells us to stop (via shutdown())
		run = true;

		// Loop, reading commands from the client and sending
		// them to the SongPlayer
		while (run == true && (inputLine = client.readLine()) != null)
		{
			processInput(inputLine);
		}
	}

	public void shutdown()
	{
		run = false;
	}

	private void processInput(String inputLine)
	{
		System.err.println("Input from " + clientIdentity + ":  \"" +
			inputLine + "\"");

		// The default set of deliminators includes the space
		// character, which we don't want because song names could
		// contain spaces.  Our protocol uses tabs to deliminate
		// fields.
		String [] tokenizedCommand = Tokenize.tokenize(inputLine, "\t\n\r\f");

		if (tokenizedCommand.length == 0)
		{
			invalid(inputLine);
			return;
		}

		if (tokenizedCommand[0].equals("AUTH"))
		{
			if (tokenizedCommand.length == 2 || tokenizedCommand.length == 3)
			{
				if (tokenizedCommand[1].equals("NULL"))
				{
					authorized = true;
				}
				else
				{
					invalid(inputLine, true);
					return;
				}

				if (tokenizedCommand.length == 3)
				{
					if (tokenizedCommand[2].equals("NOPLAYLISTS"))
					{
						sendPlaylists = false;
					}
					else if (tokenizedCommand[2].equals("PLAYLISTS"))
					{
						sendPlaylists = true;
					}
					else
					{
						invalid(inputLine, true);
						return;
					}
				}
			}
			else
			{
				invalid(inputLine, true);
				return;
			}
	
			if (authorized)
			{
				// Now that we're ready, register with the SongPlayer so that
				// it can send us updates.
				songPlayer.addClient(this);
			}
		}
		else
		{
			if (! authorized)
			{
				error("Not authorized, try the AUTH command", true);
				return;
			}

			if (tokenizedCommand[0].equals("PLAY"))
			{
				if (tokenizedCommand.length == 2)
				{
					int indexOfSongToPlay = 0;
					try
					{
						indexOfSongToPlay =
							Integer.parseInt(tokenizedCommand[1]);
					}
					catch (NumberFormatException nfeException)
					{
						invalid(inputLine);
						return;
					}

					songPlayer.play(indexOfSongToPlay);
				}
				else
				{
					invalid(inputLine);
					return;
				}
			}
			else if (tokenizedCommand[0].equals("PAUSE"))
			{
				songPlayer.pause();
			}
			else if (tokenizedCommand[0].equals("STOP"))
			{
				songPlayer.stop();
			}
			else if (tokenizedCommand[0].equals("SKIP_BACK"))
			{
				songPlayer.skipBack();
			}
			else if (tokenizedCommand[0].equals("SKIP_FORWARD"))
			{
				songPlayer.skipForward();
			}
			else if (tokenizedCommand[0].equals("PLAYLIST"))
			{
				if (tokenizedCommand.length == 2)
				{
					String newPlaylistName = tokenizedCommand[1];
					songPlayer.setPlaylist(newPlaylistName);
				}
				else
				{
					invalid(inputLine);
					return;
				}
			}
			else if (tokenizedCommand[0].equals("ADD_SONG"))
			{
				if (tokenizedCommand.length == 3)
				{
					String songToAdd = tokenizedCommand[1];
					int indexOfSongToAdd = 0;
					try
					{
						indexOfSongToAdd =
							Integer.parseInt(tokenizedCommand[2]);
					}
					catch (NumberFormatException nfeException)
					{
						invalid(inputLine);
						return;
					}

					songPlayer.addSongToPlaylist(songToAdd, indexOfSongToAdd);
				}
				else
				{
					invalid(inputLine);
					return;
				}
			}
			else if (tokenizedCommand[0].equals("DELETE_SONG"))
			{
				if (tokenizedCommand.length == 2)
				{
					String songToDelete = tokenizedCommand[1];
					songPlayer.deleteSongFromPlaylist(songToDelete);
				}
				else
				{
					invalid(inputLine);
					return;
				}
			}
			else if (tokenizedCommand[0].equals("CLEAR_PLAYLIST"))
			{
				songPlayer.clearPlaylist();
			}
			else if (tokenizedCommand[0].equals("SET_SHUFFLE"))
			{
				if (tokenizedCommand.length == 2)
				{
					boolean shuffleValue =
						Boolean.valueOf(tokenizedCommand[1]).booleanValue();

					songPlayer.setShuffle(shuffleValue);
				}
				else
				{
					invalid(inputLine);
					return;
				}
			}
			else if (tokenizedCommand[0].equals("SET_LOOP"))
			{
				if (tokenizedCommand.length == 2)
				{
					boolean loopValue =
						Boolean.valueOf(tokenizedCommand[1]).booleanValue();

					songPlayer.setLoop(loopValue);
				}
				else
				{
					invalid(inputLine);
					return;
				}
			}
			else if (tokenizedCommand[0].equals("GET_AVAIL_PLAYLISTS"))
			{
				songPlayer.getAvailablePlaylists(this);
			}
			else if (tokenizedCommand[0].equals("GET_AVAIL_SONGS"))
			{
				songPlayer.getAvailableSongs(this);
			}
			else if (tokenizedCommand[0].equals("STATUS"))
			{
				songPlayer.sendAllState(this);
			}
			/* This goofy command allows the client to send us a random
			 * string that we just log.  Used mostly to debug the Palm
			 * client.
			 */
			else if (tokenizedCommand[0].equals("JUNK"))
			{
				System.err.println("Junk from " + clientIdentity + ":  \"" +
					inputLine + "\"");
			}
			else
			{
				invalid(inputLine);
				return;
			}
		}
	}

	/* Methods for logging failure to act on commands received from
	 * clients.  I.e. the command was valid but we couldn't act on
	 * it for some reason.
     */
	private void error(String errorMessage)
	{
		error(errorMessage, false);
	}
	private void error(String errorMessage, boolean sendWithoutAuth)
	{
		System.err.println("Problem with command from " + clientIdentity +
			":  \"" + errorMessage + "\"");
		sendError(errorMessage, sendWithoutAuth);
	}

	/* Methods for logging invalid commands received from clients. */
	private void invalid(String badInput)
	{
		invalid(badInput, false);
	}
	private void invalid(String badInput, boolean sendWithoutAuth)
	{
		System.err.println("Invalid input from " + clientIdentity + ":  \"" +
			badInput + "\"");
		sendError("Invalid input:  \"" + badInput + "\"", sendWithoutAuth);
	}
		
	/* The remaining methods are used by the SongPlayer class to
	 * communicate to the client through us.  We serve as a babelfish,
	 * taking the provided info and turning it into something that we
	 * can send to the client.
	 */

	public void sendSongState(
		String currentArtist,
		String currentAlbum,
		String currentSong,
		String currentSongInfo,
		int playedSeconds,
		int totalSeconds,
		int currentPlaylistIndex,
		int playState,
		boolean shuffleEnabled,
		boolean loopEnabled)
	{
		if (! authorized)
		{
			return;
		}

		client.println("ARTIST\t" + currentArtist);
		client.println("ALBUM\t" + currentAlbum);
		client.println("SONG\t" + currentSong);
		client.println("SONGINFO\t" + currentSongInfo);
		client.println("PLAYEDSECONDS\t" + playedSeconds);
		client.println("TOTALSECONDS\t" + totalSeconds);
		client.println("CURRENTPLAYLISTINDEX\t" + currentPlaylistIndex);
		client.println("PLAYSTATE\t" + playState);
		client.println("SHUFFLEENABLED\t" + shuffleEnabled);
		client.println("LOOPENABLED\t" + loopEnabled);
	}

	// Because this bit of info will be sent quite often, we provide
	// a method just for sending it.
	public void sendPlayedSeconds(int playedSeconds)
	{
		if (! authorized)
		{
			return;
		}

		client.println("PLAYEDSECONDS\t" + playedSeconds);
	}

	public void sendPlaylist(
		String playlistName,
		Vector playlist)
	{
		if (! authorized)
		{
			return;
		}

		client.println("PLAYLIST\t" + playlistName);
		if (sendPlaylists)
		{
			for (Enumeration e = playlist.elements();
				e.hasMoreElements();)
			{
				client.println("PLAYLISTSONG\t" + (String) e.nextElement());
			}
		}
		client.println("END_PLAYLIST");
	}

	public void sendAvailablePlaylists(Vector playlists)
	{
		if (! authorized)
		{
			return;
		}

		client.println("AVAIL_PLAYLISTS\t" + playlists.size());
		for (Enumeration e = playlists.elements();
			e.hasMoreElements();)
		{
			client.println("AVAIL_PLAYLIST\t" + (String) e.nextElement());
		}
		client.println("END_AVAIL_PLAYLISTS");
	}

	public void sendAvailableSongs(Vector songs)
	{
		if (! authorized)
		{
			return;
		}

		client.println("AVAIL_SONGS");
		for (Enumeration e = songs.elements();
			e.hasMoreElements();)
		{
			client.println("AVAIL_SONG\t" + (String) e.nextElement());
		}
		client.println("END_AVAIL_SONGS");
	}

	public void sendError(String errorMessage)
	{
		sendError(errorMessage, false);
	}

	public void sendError(String errorMessage, boolean sendWithoutAuth)
	{
		// For error messages, we allow the caller to request that the
		// message be sent even if the client is not authorized.  This
		// should only be used for error messages about authorization
		// issues.

		if (! authorized && ! sendWithoutAuth)
		{
			return;
		}

		client.println("ERROR\t" + errorMessage);
	}
}

