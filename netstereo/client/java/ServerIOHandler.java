/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Handle I/O with the Stereo Server.
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

public class ServerIOHandler implements Runnable
{
	private StereoClient client = null;
	private BufferedReader in = null;
	private PrintWriter out = null;
	String newPlaylistName = "";
	Vector newPlaylist = null;
	Vector newAvailablePlaylists = null;
	Vector newAvailableSongs = null;

	public ServerIOHandler(InputStream inStream, OutputStream outStream,
		StereoClient client)
	{
		this.client = client;
		in = new BufferedReader(new InputStreamReader(inStream));
		out = new PrintWriter(outStream, true);
		newPlaylist = new Vector();
		newAvailablePlaylists = new Vector();
		newAvailableSongs = new Vector();
	}

	public void run()
	{
		sendToServer("AUTH\tNULL");

		try
		{
			String inputLine;

			// Loop, reading responses from the server and sending
			// them to the display thread.
			while((inputLine = in.readLine()) != null)
			{
				processInput(inputLine);
			}
		}
		catch (IOException e)
		{
			System.err.println("Error reading responses from socket");
			e.printStackTrace();
		}
	}

	// Grok input from server and call the appropriate method in StereoClient
	private void processInput(String inputLine)
	{
		// StringTokenizer doesn't return empty tokens, i.e. two delim
		// characters with nothing in between them.  So we insert a space
		// at every such occurance before handing the string off to the
		// tokenizer.  Note also that readLine doesn't return the line
		// termination characters, so we could end up with an empty token
		// at the end of the string.
		char [] inputChars = inputLine.toCharArray();
		StringBuffer inputLineBuffer = new StringBuffer(inputLine);
		boolean spaceInserted = false;
		for (int i=0 ; i<inputChars.length-1 ; i++)
		{
			if (inputChars[i] == '\t' && inputChars[i+1] == '\t')
			{
				inputLineBuffer.insert(i+1, ' ');
				spaceInserted = true;
			}
			if (inputChars[i+1] == '\t' && i+1 == inputChars.length-1)
			{
				inputLineBuffer.insert(i+2, ' ');
				spaceInserted = true;
			}
			if (spaceInserted)
			{
				inputChars = inputLineBuffer.toString().toCharArray();
				i++;  // Not strictly necessary, but "right"
				spaceInserted = false;
			}
		}
		inputLine = inputLineBuffer.toString();

		// The default set of deliminators includes the space
		// character, which we don't want because song names could
		// contain spaces.  Our protocol uses tabs to deliminate
		// fields.
		String [] tokenizedCommand =
			Tokenize.tokenize(inputLine, "\t\n\r\f");

		if (tokenizedCommand.length == 0)
		{
			System.err.println("Invalid input:  " + inputLine);
			return;
		}

		if (tokenizedCommand[0].equals("ARTIST"))
		{
			if (tokenizedCommand.length == 2)
			{
				client.setArtist(tokenizedCommand[1]);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
		else if (tokenizedCommand[0].equals("ALBUM"))
		{
			if (tokenizedCommand.length == 2)
			{
				client.setAlbum(tokenizedCommand[1]);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
		else if (tokenizedCommand[0].equals("SONG"))
		{
			if (tokenizedCommand.length == 2)
			{
				client.setSong(tokenizedCommand[1]);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
		else if (tokenizedCommand[0].equals("SONGINFO"))
		{
			if (tokenizedCommand.length == 2)
			{
				client.setSongInfo(tokenizedCommand[1]);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
		else if (tokenizedCommand[0].equals("PLAYEDSECONDS"))
		{
			if (tokenizedCommand.length == 2)
			{
				int playedSeconds = 0;

				try
				{
					playedSeconds = Integer.parseInt(tokenizedCommand[1]);
				}
				catch (NumberFormatException nfeException)
				{
					System.err.println("Invalid input:  " + inputLine);
					return;
				}

				client.setPlayedSeconds(playedSeconds);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
		else if (tokenizedCommand[0].equals("TOTALSECONDS"))
		{
			if (tokenizedCommand.length == 2)
			{
				int totalSeconds = 0;

				try
				{
					totalSeconds = Integer.parseInt(tokenizedCommand[1]);
				}
				catch (NumberFormatException nfeException)
				{
					System.err.println("Invalid input:  " + inputLine);
					return;
				}

				client.setTotalSeconds(totalSeconds);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
		else if (tokenizedCommand[0].equals("CURRENTPLAYLISTINDEX"))
		{
			if (tokenizedCommand.length == 2)
			{
				int currentPlaylistIndex = 0;

				try
				{
					currentPlaylistIndex =
						Integer.parseInt(tokenizedCommand[1]);
				}
				catch (NumberFormatException nfeException)
				{
					System.err.println("Invalid input:  " + inputLine);
					return;
				}

				client.setCurrentPlaylistIndex(currentPlaylistIndex);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
		else if (tokenizedCommand[0].equals("PLAYSTATE"))
		{
			if (tokenizedCommand.length == 2)
			{
				int playState = 0;

				try
				{
					playState = Integer.parseInt(tokenizedCommand[1]);
				}
				catch (NumberFormatException nfeException)
				{
					System.err.println("Invalid input:  " + inputLine);
					return;
				}

				client.setPlayState(playState);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
		else if (tokenizedCommand[0].equals("SHUFFLEENABLED"))
		{
			if (tokenizedCommand.length == 2)
			{
				boolean shuffleValue =
					Boolean.valueOf(tokenizedCommand[1]).booleanValue();

				client.setShuffle(shuffleValue);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
		else if (tokenizedCommand[0].equals("LOOPENABLED"))
		{
			if (tokenizedCommand.length == 2)
			{
				boolean loopValue =
					Boolean.valueOf(tokenizedCommand[1]).booleanValue();

				client.setLoop(loopValue);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
		else if (tokenizedCommand[0].equals("PLAYLIST"))
		{
			if (tokenizedCommand.length == 2)
			{
				newPlaylist.removeAllElements();
				newPlaylistName = tokenizedCommand[1];
			}
		}
		else if (tokenizedCommand[0].equals("PLAYLISTSONG"))
		{
			newPlaylist.addElement(tokenizedCommand[1]);
		}
		else if (tokenizedCommand[0].equals("END_PLAYLIST"))
		{
			client.setPlaylist(newPlaylistName, newPlaylist);
		}
		else if (tokenizedCommand[0].equals("AVAIL_PLAYLISTS"))
		{
			if (tokenizedCommand.length == 1)
			{
				newAvailablePlaylists.removeAllElements();
			}
		}
		else if (tokenizedCommand[0].equals("AVAIL_PLAYLIST"))
		{
			newAvailablePlaylists.addElement(tokenizedCommand[1]);
		}
		else if (tokenizedCommand[0].equals("END_AVAIL_PLAYLISTS"))
		{
			client.setAvailablePlaylists(newAvailablePlaylists);
		}
		else if (tokenizedCommand[0].equals("AVAIL_SONGS"))
		{
			if (tokenizedCommand.length == 1)
			{
				newAvailableSongs.removeAllElements();
			}
		}
		else if (tokenizedCommand[0].equals("AVAIL_SONG"))
		{
			newAvailableSongs.addElement(tokenizedCommand[1]);
		}
		else if (tokenizedCommand[0].equals("END_AVAIL_SONGS"))
		{
			client.setAvailableSongs(newAvailableSongs);
		}
		else if (tokenizedCommand[0].equals("ERROR"))
		{
			if (tokenizedCommand.length == 2)
			{
				client.setError(tokenizedCommand[1]);
			}
			else
			{
				System.err.println("Invalid input:  " + inputLine);
				return;
			}
		}
	}

	// These are all of the methods for sending commands to the server

	private void sendToServer(String command)
	{
		out.println(command);
	}

	public void sendPlay(int indexOfSongToPlay)
	{
		sendToServer("PLAY\t" + indexOfSongToPlay);
	}

	public void sendPause()
	{
		sendToServer("PAUSE");
	}

	public void sendStop()
	{
		sendToServer("STOP");
	}

	public void sendSkipBack()
	{
		sendToServer("SKIP_BACK");
	}

	public void sendSkipForward()
	{
		sendToServer("SKIP_FORWARD");
	}

	public void sendPlaylist(String playlistName)
	{
		sendToServer("PLAYLIST\t" + playlistName);
	}

	public void sendAddSongToPlaylist(String song, int position)
	{
		sendToServer("ADD_SONG\t" + song + "\t" + position);
	}

	public void sendDeleteSongFromPlaylist(String song)
	{
		sendToServer("DELETE_SONG\t" + song);
	}

	public void sendClearPlaylist()
	{
		sendToServer("CLEAR_PLAYLIST");
	}

	public void sendShuffle(boolean shuffleEnabled)
	{
		sendToServer("SET_SHUFFLE\t" + shuffleEnabled);
	}

	public void sendLoop(boolean loopEnabled)
	{
		sendToServer("SET_LOOP\t" + loopEnabled);
	}

	public void sendGetAvailablePlaylists()
	{
		sendToServer("GET_AVAIL_PLAYLISTS");
	}

	public void sendGetAvailableSongs()
	{
		sendToServer("GET_AVAIL_SONGS");
	}
}

