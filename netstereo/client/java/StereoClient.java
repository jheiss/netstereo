/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Base class for a NetStereo client.
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
import java.util.*;

abstract class StereoClient
{
	protected Thread ioThread = null;
	protected ServerIOHandler ioHandler = null;

	protected String currentArtist = "", currentAlbum = "",
		currentSong = "", currentSongInfo = "";
	protected static final int PS_STOPPED = 0, PS_PLAYING = 1, PS_PAUSED = 2;
	protected int playState = PS_STOPPED;
	protected int totalSeconds = 0, playedSeconds = 0;
	protected String playlistName = "";
	protected String songDirectory = "";
	protected Vector playlist = null;
	protected Vector availablePlaylists = null;
	protected Vector availableSongs = null;
	protected int currentPlaylistIndex = 0;

	// These should eventually be user-selected
	protected Vector songExtensions = null;
	protected Vector playlistExtensions = null;
	protected boolean shuffleEnabled = false;
	protected boolean loopEnabled = false;
	protected boolean startNetClient = true;
	protected boolean startSerialClient = false;

	public StereoClient()
	{
		playlist = new Vector();
		availablePlaylists = new Vector();
		availableSongs = new Vector();
		songExtensions = new Vector();  // Subclasses should put something here
		playlistExtensions = new Vector();  // and here
		initiateCommsWithServer();
	}

	private void initiateCommsWithServer()
	{
		if (! (startNetClient || startSerialClient))
		{
			System.err.println("No form of server communication is enabled!");
			System.exit(-1);
		}

		if (startNetClient)
		{
			ioHandler = NetClient.createIOHandler(this, "localhost", 12345);
			if (ioHandler == null)
			{
				System.exit(-1);
			}
			// The User Interface code must start this thread when it is
			// ready.
			ioThread = new Thread(ioHandler, "ServerIOHandler for NetClient");
		}
		else if (startSerialClient)
		{
			ioHandler = SerialClient.createIOHandler(this, "/dev/ttyS0", 9600);
			if (ioHandler == null)
			{
				System.exit(-1);
			}
			// The User Interface code must start this thread when it is
			// ready.
			ioThread = new Thread(ioHandler,
				"ServerIOHandler for SerialClient");
		}
		else
		{
			System.err.println("Unrecognized form of communication with " +
				"server requested");
		}
	}

	public void setArtist(String newArtist)
	{
		currentArtist = newArtist;
	}

	public void setAlbum(String newAlbum)
	{
		currentAlbum = newAlbum;
	}

	public void setSong(String newSong)
	{
		currentSong = newSong;
	}

	public void setSongInfo(String newSongInfo)
	{
		currentSongInfo = newSongInfo;
	}

	public void setPlayedSeconds(int newPlayedSeconds)
	{
		playedSeconds = newPlayedSeconds;
	}

	public void setTotalSeconds(int newTotalSeconds)
	{
		totalSeconds = newTotalSeconds;
	}

	public void setCurrentPlaylistIndex(int newCurrentPlaylistIndex)
	{
		currentPlaylistIndex = newCurrentPlaylistIndex;
	}

	public void setPlayState(int newPlayState)
	{
		playState = newPlayState;
	}

	public void setShuffle(boolean newShuffleEnabled)
	{
		shuffleEnabled = newShuffleEnabled;
	}

	public void setLoop(boolean newLoopEnabled)
	{
		loopEnabled = newLoopEnabled;
	}

	public void setPlaylist(String newPlaylistName, Vector newPlaylist)
	{
		playlistName = newPlaylistName;
		playlist = newPlaylist;
	}

	public void setAvailablePlaylists(Vector newAvailablePlaylists)
	{
		//System.out.println("Setting availablePlaylists to " +
			//newAvailablePlaylists);
		availablePlaylists = newAvailablePlaylists;
	}

	public void setAvailableSongs(Vector newAvailableSongs)
	{
		//System.out.println("Setting availableSongs to " + newAvailableSongs);
		availableSongs = newAvailableSongs;
	}

	abstract public void setError(String errorMessage);
}

