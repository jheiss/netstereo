/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Framework for interfacing with a song-playing program.
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
 * Revision 1.2  2001/03/20 06:42:59  jheiss
 * Added copyright and GPL message.
 *
 * Revision 1.1  2001/03/20 06:35:25  jheiss
 * Initial revision
 *
 *****************************************************************************
 */

/* Imports */
import java.util.*;
import java.io.*;

abstract class SongPlayer
{
	protected Vector clients = null;
	protected String currentArtist = "";
	protected String currentAlbum = "";
	protected String currentSong = "";
	protected String currentSongInfo = "";
	protected int playedSeconds = 0, totalSeconds = 0;
	protected static final int PS_STOPPED = 0, PS_PLAYING = 1, PS_PAUSED = 2;
	protected int playState = PS_STOPPED;
	private String playlistName = "";
	//protected String workingDirectory = "";
	protected Vector songPlaylist = null, workingPlaylist = null;
	protected int currentPlaylistIndex = 0;
	protected boolean shuffleEnabled = false;
	protected boolean loopEnabled = false;

	// These should eventually be user-selected
	protected Vector songExtensions = null;
	protected Vector playlistExtensions = null;
	
	public SongPlayer()
	//public SongPlayer(String newWorkingDirectory)
	{
		clients = new Vector();
		songPlaylist = new Vector();
		workingPlaylist = new Vector();

		//workingDirectory = newWorkingDirectory;

		// Create lists of extensions for file selection
		songExtensions = new Vector();
		songExtensions.addElement("mp3");
		playlistExtensions = new Vector();
		playlistExtensions.addElement("m3u");
	}

	// Add a client to the list of translators to be notified of status updates.
	public void addClient(ProtocolTranslator translator)
	{
		clients.addElement(translator);
		sendAllState(translator);
	}

	// The next five methods are used to send state changes to the
	// clients by way of the client translators.

	// This method builds a list of client translators to send the update
	// to.  This allows us to cleanly send updates to one client
	// or all of them.
	private Vector buildTargetClientVector(ProtocolTranslator translator)
	{
		Vector targets = null;

		if (translator != null)
		{
			targets = new Vector();
			targets.addElement(translator);
		}
		else
		{
			targets = clients;
		}

		return targets;
	}

	protected void sendAllState(ProtocolTranslator translator)
	{
		sendSongState(translator);
		sendPlaylist(translator);
	}

	// Send all of the state related to the currently playing song.
	protected void sendSongState(ProtocolTranslator translator)
	{
		for (Enumeration e = buildTargetClientVector(translator).elements();
			e.hasMoreElements();)
		{
			((ProtocolTranslator) e.nextElement()).sendSongState(
				currentArtist,
				currentAlbum,
				currentSong,
				currentSongInfo,
				playedSeconds,
				totalSeconds,
				currentPlaylistIndex,
				playState,
				shuffleEnabled,
				loopEnabled);
		}
	}

	// Because this bit of info will be sent quite often, we provide
	// a method just for sending it.
	protected void sendPlayedSeconds(ProtocolTranslator translator)
	{
		for (Enumeration e = buildTargetClientVector(translator).elements();
			e.hasMoreElements();)
		{
			((ProtocolTranslator) e.nextElement()).sendPlayedSeconds(
				playedSeconds);
		}
	}

	// Send the current working playlist.
	protected void sendPlaylist(ProtocolTranslator translator)
	{
		for (Enumeration e = buildTargetClientVector(translator).elements();
			e.hasMoreElements();)
		{
			((ProtocolTranslator) e.nextElement()).sendPlaylist(
				playlistName,
				workingPlaylist);
		}
	}

	protected void sendError(String errorMessage)
	{
		for (Enumeration e = clients.elements(); e.hasMoreElements();)
		{
			((ProtocolTranslator) e.nextElement()).sendError(errorMessage);
		}
	}

	abstract public void play(int indexOfSongToPlay);
	abstract public void pause();
	abstract public void stop();
	// These will necessarily be dependant on the song-playing program
	// and medium, but if possible should skip 5-15 seconds back or
	// forward in the current song.  At worst they should skip to the
	// start or end of the song.
	abstract public void skipBack();
	abstract public void skipForward();

	protected void switchToStoppedPlayState()
	{
		clearState();
		sendSongState(null);
	}

	protected void clearState()
	{
		currentPlaylistIndex = 0;
		playState = PS_STOPPED;
		currentArtist = "";
		currentAlbum = "";
		currentSong = "";
		currentSongInfo = "";
		playedSeconds = 0;
		totalSeconds = 0;
	}

	//public void setPlaylist(String newPlaylistName, Vector newPlaylist)
	public void setPlaylist(String newPlaylistName)
	{
		Vector newPlaylist = readPlaylistFile(new File(newPlaylistName),
			new Vector());

		if (newPlaylist.size() > 0)
		{
			songPlaylist = newPlaylist;
			playlistName = newPlaylistName;
			updateWorkingPlaylist();
			sendPlaylist(null);
		}
		else
		{
			// Inform client of problems
		}
	}

	private Vector readPlaylistFile(File playlistFile, Vector loopList)
	{
		Vector playlistVector = new Vector();

		BufferedReader playlistReader = null;
		try
		{
			playlistReader = new BufferedReader(new FileReader(playlistFile));
		}
		catch (FileNotFoundException fnfException)
		{
			System.err.println("Requested playlist " + playlistFile +
				" not found");
			return(new Vector());
		}

		for (Enumeration e = loopList.elements(); e.hasMoreElements();)
		{
			File element = (File) e.nextElement();
			if (element.compareTo(playlistFile) == 0)
			{
				System.err.println("Loop detected in playlist, " +
					playlistFile.getAbsolutePath() + " found twice");
				return(new Vector());
			}
		}

		// Well, we got past the loop detection so add this playlist to
		// the loop list.
		loopList.addElement(playlistFile);

		try
		{
			boolean continueReading = true;

			while (continueReading == true)
			{
				String line = playlistReader.readLine();
				if (line != null)
				{
					// Handle songs and playlists here
					File entryFile = new File(line);
					PlaylistFileFilter playlistFilter =
						new PlaylistFileFilter();
					SongFileFilter songFilter = new SongFileFilter();
					if (playlistFilter.accept(entryFile))
					{
						playlistVector.addAll(readPlaylistFile(entryFile,
							loopList));
					}
					else if (songFilter.accept(entryFile) && entryFile.isFile())
					{
						playlistVector.add(line);
					}
					else
					{
						System.err.println("Invalid entry in playlist:  "
							+ line);
					}
				}
				else
				{
					continueReading = false;
				}
			}

			playlistReader.close();
			return(playlistVector);
		}
		catch (IOException ioException)
		{
			System.err.println("Error reading playlist " +
				playlistFile);
			return(new Vector());
		}
	}

	public void addSongToPlaylist(String song, int position)
	{
		if (position < 0)
		{
			position = 0;
		}
		else if (position > songPlaylist.size())
		{
			position = songPlaylist.size();
		}

		File songFile = new File(song);
		if (songFile.isFile())
		{
			songPlaylist.insertElementAt(song, position);
			updateWorkingPlaylist();

			sendPlaylist(null);
		}
		else
		{
			System.err.println("Requested song " + song + " does not exist " +
				"or is not a file");
		}
	}

	public void deleteSongFromPlaylist(String song)
	{
		// This removes all occurances of the specified song, may not
		// be the behavior we want...
		for (Enumeration e = songPlaylist.elements();
			e.hasMoreElements();)
		{
			String element = (String) e.nextElement();

			if (song.equals(element))
			{
				songPlaylist.removeElement(element);
			}
		}
		updateWorkingPlaylist();

		sendPlaylist(null);
	}

	public void clearPlaylist()
	{
		songPlaylist.removeAllElements();
		playlistName = "";
		updateWorkingPlaylist();

		sendPlaylist(null);
	}

	public void setShuffle(boolean shuffleEnabled)
	{
		this.shuffleEnabled = shuffleEnabled;
		updateWorkingPlaylist();

		sendAllState(null);
	}

	private void updateWorkingPlaylist()
	{
		if (shuffleEnabled == true)
		{
			shufflePlaylist();
		}
		else
		{
			workingPlaylist = songPlaylist;
		}
	}

	private void shufflePlaylist()
	{
		Random randomizer = new Random(System.currentTimeMillis());
		workingPlaylist = new Vector(songPlaylist.size());
		Vector scratchPlaylist = new Vector(songPlaylist.size());

		// Copy the master playlist into a scratch vector
		for (Enumeration shufflePlaylistEnumeration = songPlaylist.elements();
			shufflePlaylistEnumeration.hasMoreElements();)
		{
			scratchPlaylist.addElement( 
				shufflePlaylistEnumeration.nextElement()); 
		}

		// Now randomly pick elements out of the scratch vector and
		// put them into the working playlist
		while (scratchPlaylist.size() > 0)
		{
			// The +1 (hopefully) avoids a race condition with abs()
			// if we get Integer.MIN_VALUE from nextInt().  See the
			// documentation for abs().  Not sure what happens if we
			// get MAX_VALUE though...
			int randInt =
				Math.abs(randomizer.nextInt()+1)%scratchPlaylist.size();
			workingPlaylist.addElement(
				scratchPlaylist.elementAt(randInt));
			scratchPlaylist.removeElementAt(randInt);
		}
	}

	public void setLoop(boolean loopEnabled)
	{
		this.loopEnabled = loopEnabled;

		sendSongState(null);
	}

	public void getAvailablePlaylists(ProtocolTranslator translator)
	{
		String songDirectory = System.getProperty("user.dir");
		File songDirFile = new File(songDirectory);
		//File songDirFile = new File(".");
		//System.out.println("song directory is " + songDirFile.getAbsolutePath());
		File[] playlistFiles = songDirFile.listFiles(new PlaylistFileFilter());
		String[] playlistFilenames = new String[playlistFiles.length];
		for (int i=0 ; i<playlistFiles.length ; i++)
		{
			//playlistFilenames[i] = playlistFiles[i].getAbsolutePath();
			//playlistFilenames[i] = playlistFiles[i].getPath();
			playlistFilenames[i] = playlistFiles[i].getName();
		}
		//Sort.bubbleSort(playlistFilenames);
		Sort.quickSort(playlistFilenames);
		Vector playlistVector = new Vector(playlistFilenames.length);
		for (int i=0 ; i<playlistFilenames.length ; i++)
		{
			playlistVector.addElement(playlistFilenames[i]);
		}
			
		for (Enumeration e = buildTargetClientVector(translator).elements();
			e.hasMoreElements();)
		{
			((ProtocolTranslator) e.nextElement()).sendAvailablePlaylists(
				playlistVector);
		}
	}

	public void getAvailableSongs(ProtocolTranslator translator)
	{
		String songDirectory = System.getProperty("user.dir");
		File songDirFile = new File(songDirectory);
		File[] songFiles = songDirFile.listFiles(new SongFileFilter());
		String[] songFilenames = new String[songFiles.length];
		for (int i=0 ; i<songFiles.length ; i++)
		{
			//songFilenames[i] = songFiles[i].getAbsolutePath();
			//songFilenames[i] = songFiles[i].getPath();
			songFilenames[i] = songFiles[i].getName();
		}
		//Sort.bubbleSort(songFilenames);
		Sort.quickSort(songFilenames);
		Vector songVector = new Vector(songFilenames.length);
		for (int i=0 ; i<songFilenames.length ; i++)
		{
			songVector.addElement(songFilenames[i]);
		}

		for (Enumeration e = buildTargetClientVector(translator).elements();
			e.hasMoreElements();)
		{
			((ProtocolTranslator) e.nextElement()).sendAvailableSongs(
				songVector);
		}
	}

	class SongFileFilter implements FileFilter
	{
		public boolean accept(File f)
		{
			if (! f.isFile())
			{
				return false;
			}

			String s = f.getName();
			for (Enumeration e = songExtensions.elements();
				e.hasMoreElements();)
			{
				if (s.endsWith("." + (String) e.nextElement()))
				{
					return true;
				}
			}

			return false;
		}
	}

	class PlaylistFileFilter implements FileFilter
	{
		public boolean accept(File f)
		{
			//System.out.println("Checking " + f.getAbsolutePath() + " for playlist");
			if (! f.isFile())
			{
				return false;
			}

			String s = f.getName();
			for (Enumeration e = playlistExtensions.elements();
				e.hasMoreElements();)
			{
				if (s.endsWith("." + (String) e.nextElement()))
				{
					return true;
				}
			}

			return false;
		}
	}
}

