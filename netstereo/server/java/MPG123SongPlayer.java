/*****************************************************************************
 * $Id$
 *****************************************************************************
 * An implementation of SongPlayer using the UNIX mpg123 command line
 * MP3 player.
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
 * Revision 1.3  2001/03/23 05:28:58  jheiss
 * Added logging of errors from mpg123.
 *
 * Revision 1.2  2001/03/20 06:42:59  jheiss
 * Added copyright and GPL message.
 *
 * Revision 1.1  2001/03/20 06:42:06  jheiss
 * Initial revision
 *
 *****************************************************************************
 */

/* Imports */
import java.io.*;
import java.util.*;

public class MPG123SongPlayer extends SongPlayer implements ProcessOutputHandler
{
	private static final int FRAME_JUMP = 500;  // Frames to jump fwd or rewind
	private ProcessHandlerThread player = null;
	private int playedFrames, totalFrames;
	private int previousPlayedSeconds;
	private int previousTotalSeconds;

	public MPG123SongPlayer()
	{
		super();

		player = new ProcessHandlerThread("mpg123 -R foo", true);
		player.addHandler(this);  // We want output from the process
		player.start();
	}

	public void play(int indexOfSongToPlay)
	{
		String playlistEntry = null;

		try
		{
			playlistEntry =
				(String) workingPlaylist.elementAt(indexOfSongToPlay);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			sendError("Requested song index does not exist in current " +
				"playlist");
			return;
		}

		File songFile = new File(playlistEntry);

		if (songFile.exists())
		{
			player.inputToProcess("load " + songFile.getAbsolutePath());
			currentPlaylistIndex = indexOfSongToPlay;
		}
		else
		{
			sendError("Song " + playlistEntry + " does not exist");
			switchToStoppedPlayState();
		}
	}

	public void pause()
	{
		player.inputToProcess("pause");
	}

	public void stop()
	{
		player.inputToProcess("stop");
	}

	public void skipBack()
	{
		player.inputToProcess("jump -" + FRAME_JUMP);
	}

	public void skipForward()
	{
		player.inputToProcess("jump +" + FRAME_JUMP);
	}

	// The ProcessHandlerThread uses the next two methods to send us stdout
	// and stderr from the process.

	public void outputFromProcess(String outputLine)
	{
		String[] tokenizedResponse = Tokenize.tokenize(outputLine);

		// Because this message occurs so frequently, we have a special
		// method for just sending the updated time to the client.  All
		// other messages from the player require sending the entire
		// set to state information so we handle this one seperately.
		if (tokenizedResponse[0].equals("@F"))
		{
			playState = PS_PLAYING;

			int playedFrames = Integer.parseInt(tokenizedResponse[1]);
			int totalFrames = playedFrames +
				Integer.parseInt(tokenizedResponse[2]);
			double playedSeconds =
				Double.valueOf(tokenizedResponse[3]).doubleValue();
			double totalSeconds = playedSeconds +
				Double.valueOf(tokenizedResponse[4]).doubleValue();

			this.playedFrames = playedFrames;
			this.totalFrames = totalFrames;
			this.playedSeconds = (int) Math.round(playedSeconds);
			this.totalSeconds = (int) Math.round(totalSeconds);

			// Send the updated time to all of the clients
			if (this.playedSeconds != previousPlayedSeconds)
			{
				sendPlayedSeconds(null);
			}

			// If we have a new song length then we need to send all of
			// the info to get this to the clients.
			if (this.totalSeconds != previousTotalSeconds)
			{
				sendSongState(null);
			}

			previousPlayedSeconds = this.playedSeconds;
			previousTotalSeconds = this.totalSeconds;
		}
		else  // All other messages from player
		{
			if (tokenizedResponse[0].equals("@I"))
			{
				playState = PS_PLAYING;

				if (tokenizedResponse[1].length() >=  3 &&
					tokenizedResponse[1].substring(0,3).equals("ID3"))
				{
					// Completely untested
					String currentSong = tokenizedResponse[1].substring(4,34);
					String currentArtist =
						tokenizedResponse[1].substring(35,65);
					String currentAlbum = tokenizedResponse[1].substring(66,96);
					String currentAlbumYear =
						tokenizedResponse[1].substring(97,101);
					String currentSongComment =
						tokenizedResponse[1].substring(102,132);
					String currentSongGenre =
						tokenizedResponse[1].substring(133);

					this.currentArtist = currentArtist;
					this.currentAlbum = currentAlbum;
					this.currentSong = currentSong;
				}
				else  // MP3 file didn't have an ID3 tag
				{
					currentArtist = "";
					currentAlbum = "";
					// The filename might have spaces in it, so we grab the
					// whole line (minus the "@I ") instead of using
					// tokenizedResponse.
					currentSong = outputLine.substring(3);
				}
			}
			else if (tokenizedResponse[0].equals("@S"))
			{
				playState = PS_PLAYING;

				String currentStreamMpegType = tokenizedResponse[1];
				int currentStreamLayer = Integer.parseInt(tokenizedResponse[2]);
				int currentStreamFrequency =
					Integer.parseInt(tokenizedResponse[3]);
				String currentStreamMode = tokenizedResponse[4];
				int currentStreamModeExtension =
					Integer.parseInt(tokenizedResponse[5]);
				int currentStreamFramesize =
					Integer.parseInt(tokenizedResponse[6]);
				int currentStreamStereo =
					Integer.parseInt(tokenizedResponse[7]);
				int currentStreamCopyright =
					Integer.parseInt(tokenizedResponse[8]);
				int currentStreamErrorProtection =
					Integer.parseInt(tokenizedResponse[9]);
				int currentStreamEmphasis =
					Integer.parseInt(tokenizedResponse[10]);
				int currentStreamBitrate =
					Integer.parseInt(tokenizedResponse[11]);
				int currentStreamExtension =
					Integer.parseInt(tokenizedResponse[12]);

				// Make a string out of the interesting bits of info.
				currentSongInfo = "Bitrate:  " + currentStreamBitrate +
					"  Freq:  " + ((float) currentStreamFrequency / 1000) +
					"  Mode:  " + currentStreamMode;
			}
			else if (tokenizedResponse[0].equals("@P"))
			{
				// If the player stops, because it has played all of the
				// current song, and if there are more songs in the playlist,
				// or if we've reached the end of the playlist and looping
				// is enabled, then start it playing the next song.
				if (tokenizedResponse[1].equals("0"))
				{
					// More songs in playlist
					if (playedFrames == totalFrames &&
						currentPlaylistIndex < workingPlaylist.size()-1)
					{
						play(currentPlaylistIndex + 1);
					}
					// End of playlist, but looping is enabled
					else if (playedFrames == totalFrames &&
						currentPlaylistIndex == workingPlaylist.size()-1 &&
						loopEnabled)
					{
						play(0);
					}
					// If it stopped for some other reason (someone hit
					// stop, no more songs in playlist, etc.) then reset the
					// playlist index and stop.
					else
					{
						switchToStoppedPlayState();
					}
				}
				else if (tokenizedResponse[1].equals("1"))
				{
					playState = PS_PAUSED;
				}
				else if (tokenizedResponse[1].equals("2"))
				{
					// "unpaused"
					playState = PS_PLAYING;
				}
			}

			// Send the updated status to each client
			sendSongState(null);
		}
	}

	public void errorFromProcess(String errorLine)
	{
		// Send it to each client
		sendError(errorLine);
		// And log it
		System.err.println("Error from mpg123:  " + errorLine);
	}
}

