/*****************************************************************************
 * $Id$
 *****************************************************************************
 * An implementation of SongPlayer using the Java Media Framework.
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
 * Revision 1.1  2001/03/20 06:32:32  jheiss
 * Initial revision
 *
 *****************************************************************************
 */

/* Imports */
import java.io.*;
import java.util.*;
import javax.media.*;
import javax.media.format.*;
import javax.media.control.*;
import de.vdheide.mp3.*;  // www.vdheide.de

public class JMFSongPlayer extends SongPlayer
	implements ControllerListener, Runnable
{
	private Player player;
	private int playedFrames, totalFrames;
	private int previousPlayedSeconds;
	private boolean requestedPause;
	private boolean changingTime;
	private File songFile;

	public JMFSongPlayer()
	{
		super();
	}

	public void play(int indexOfSongToPlay)
	{
		// If we already have a player, stop it.  Otherwise it will keep
		// playing right along with our current song until it happens to
		// get garbage collected.  :)
		if (player != null)
		{
			player.stop();
		}

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

		songFile = new File(playlistEntry);

		if (songFile.exists())
		{
			try
			{
				player = Manager.createRealizedPlayer(songFile.toURL());
				player.addControllerListener(this);
				player.start();
			}
			//catch (MalformedURLException e)
			//{
				//sendError("Couldn't make a URL out of " + playlistEntry);
				//switchToStoppedPlayState();
			//}
			catch (NoPlayerException e)
			{
				sendError("Couldn't create a Player");
				switchToStoppedPlayState();
			}
			catch (IOException e)
			{
				sendError("Error accessing " + playlistEntry);
				switchToStoppedPlayState();
			}
			catch (CannotRealizeException e)
			{
				sendError("Error realizing Player");
				switchToStoppedPlayState();
			}
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
		if (playState == PS_PLAYING)
		{
			requestedPause = true;
			player.stop();
		}
		else
		{
			player.start();
			//player.syncStart();
		}
	}

	public void stop()
	{
		requestedPause = false;  // Just in case
		player.stop();
	}

	public void skipBack()
	{
		player.stop();
		player.setMediaTime(new Time(player.getMediaTime().getSeconds() - 5));
		player.start();
	}

	public void skipForward()
	{
		player.stop();
		player.setMediaTime(new Time(player.getMediaTime().getSeconds() + 5));
		player.start();
	}

	public void run()
	{
		while (true)
		{
			if (player != null)
			{
				playedSeconds =
					(int) Math.round(player.getMediaTime().getSeconds());
				//System.out.println("New media time is " + playedSeconds);
				sendPlayedSeconds(null);
			}

			try
			{
				//Thread.sleep(1000);
				Thread.sleep(500);
			}
			catch (InterruptedException e)
			{
				// Blah
			}
		}
	}

	// This is called anytime an event occurs with our player
	public void controllerUpdate(ControllerEvent ce)
	{
		// This is all possible events as of JMF 2.1.1 Beta 2

		//System.out.println("ControllerEvent:  " + ce.toString());

		// Parent:  javax.media.ControllerEvent
		if (ce instanceof AudioDeviceUnavailableEvent)
		{
		}
		// Parent:  javax.media.ControllerEvent
		else if (ce instanceof CachingControlEvent)
		{
		}
		// Parent:  javax.media.ControllerEvent
		else if (ce instanceof ControllerClosedEvent)
		{
			// Parent:  javax.media.ControllerEvent.ControllerClosedEvent
			if (ce instanceof ControllerErrorEvent)
			{
				// Parent: javax.media.ControllerEvent.ControllerClosedEvent.
				//    ControllerErrorEvent
				if (ce instanceof ConnectionErrorEvent)
				{
				}
				// Parent:  javax.media.ControllerEvent.ControllerClosedEvent.
				//    ControllerErrorEvent
				else if (ce instanceof InternalErrorEvent)
				{
				}
				// Parent: javax.media.ControllerEvent.ControllerClosedEvent.
				//    ControllerErrorEvent
				else if (ce instanceof ResourceUnavailableEvent)
				{
				}
			}
			// Parent:  javax.media.ControllerEvent.ControllerClosedEvent
			else if (ce instanceof DataLostErrorEvent)
			{
			}
		}
		// Parent:  javax.media.ControllerEvent
		else if (ce instanceof DurationUpdateEvent)
		{
			totalSeconds = (int) Math.round(player.getDuration().getSeconds());
			System.out.println("New media duration is " + totalSeconds);
			sendSongState(null);
		}
		// Parent:  javax.media.ControllerEvent
		else if (ce instanceof FormatChangeEvent)
		{
			//System.out.println("Old format: " +
				//((FormatChangeEvent) ce).getOldFormat.toString());
			//System.out.println("New format: " +
				//((FormatChangeEvent) ce).getNewFormat.toString());

			// Parent:  javax.media.ControllerEvent.FormatChangeEvent
			if (ce instanceof SizeChangeEvent)
			{
				// Indicates that a video media changed size
			}
		}
		// Parent:  javax.media.ControllerEvent
		else if (ce instanceof MediaTimeSetEvent)
		{
			playedSeconds =
				(int) Math.round(player.getMediaTime().getSeconds());
			System.out.println("New media time is " + playedSeconds);
			sendPlayedSeconds(null);
		}
		// Parent:  javax.media.ControllerEvent
		else if (ce instanceof RateChangeEvent)
		{
		}
		// Parent:  javax.media.ControllerEvent
		else if (ce instanceof StopTimeChangeEvent)
		{
		}
		// Parent:  javax.media.ControllerEvent
		else if (ce instanceof TransitionEvent)
		{
			/* Straight TransitionEvents (as opposed to the subevents
			 * handled below) are things like realized->prefetching, etc.
			 * and can be safely ignored in our case.
			 */

			// Parent:  javax.media.ControllerEvent.TransitionEvent
			if (ce instanceof ConfigureCompleteEvent)
			{
			}
			// Parent:  javax.media.ControllerEvent.TransitionEvent
			else if (ce instanceof PrefetchCompleteEvent)
			{
			}
			// Parent:  javax.media.ControllerEvent.TransitionEvent
			else if (ce instanceof RealizeCompleteEvent)
			{
			}
			// Parent:  javax.media.ControllerEvent.TransitionEvent
			else if (ce instanceof StartEvent)
			{
				System.out.println("Player has started");
				playState = PS_PLAYING;

				// Need to retreive all of the media information
				//BitRateControl brc = (BitRateControl)
					//player.getControl("javax.media.control.BitRateControl");
				//System.out.println("Bitrate is " + brc.getBitRate());

				//BufferControl bc = (BufferControl)
					//player.getControl("javax.media.control.BufferControl");
				//System.out.println("Buffer is " + bc.getBufferLength() +
					//" milliseconds");

				totalSeconds =
					(int) Math.round(player.getDuration().getSeconds());
				System.out.println("New media duration is " + totalSeconds);

				FormatControl fc = (FormatControl)
					player.getControl("javax.media.control.FormatControl");
				Format format = fc.getFormat();
				System.out.println("Format is " + format);
				String encoding = format.getEncoding();
				System.out.println("  Encoding is " + encoding);
				if (format instanceof AudioFormat)
				{
					System.out.println("  Format is an AudioFormat");
					String mp3EncodingRate = new String();
					if (encoding.equals("mpeglayer3"))
					{
						// Sure wish I knew what the units from getFrameRate
						// are supposed to be...
						mp3EncodingRate =
							new Double(((AudioFormat) format).getFrameRate()
							* 8 / 1000).toString();
						System.out.println("    MP3 encoding Rate is " +
							mp3EncodingRate + " kbps");
					}
					String sampleRate =
						new Double(((AudioFormat) format).getSampleRate()).
							toString();
					System.out.println("    Sample Rate is " + sampleRate);
					String sampleSize =
						new Integer(((AudioFormat) format).
						getSampleSizeInBits()).toString();
					System.out.println("    Sample Size is " + sampleSize);
					String numberOfChannels =
						new Integer(((AudioFormat) format).getChannels()).
							toString();
					System.out.println("    # of channels is " +
						numberOfChannels);

					currentSongInfo = "Bitrate: " + mp3EncodingRate +
						"  Freq: " + sampleRate +
						"  Channels: " + numberOfChannels;
				}
				else
				{
					System.out.println("  Format is not an AudioFormat");
				}

				if (encoding.equals("mpeglayer3"))
				{
					ID3 id3 = new ID3(songFile);
					try
					{
						currentArtist = id3.getArtist();
						currentAlbum = id3.getAlbum();
						currentSong = id3.getTitle();
					}
					catch (NoID3TagException e)
					{
						currentArtist = "";
						currentAlbum = "";
						currentSong = songFile.getName();
					}

					/*catch (IOException e)
					{
						sendError("Error accessing " +
							songFile.getAbsolutePath());
						switchToStoppedPlayState();
					}*/
				}
				sendSongState(null);
			}
			// Parent:  javax.media.ControllerEvent.TransitionEvent
			else if (ce instanceof StopEvent)
			{
				// Parent:  javax.media.ControllerEvent.TransitionEvent.
				//    StopEvent
				if (ce instanceof DataStarvedEvent)
				{
					// It should start back up on its own, eh?
					System.out.println("Player is starved for data");
					//switchToStoppedPlayState();
				}
				// Parent:  javax.media.ControllerEvent.TransitionEvent.
				//    StopEvent
				else if (ce instanceof DeallocateEvent)
				{
					System.out.println("Player is being deallocated");
					switchToStoppedPlayState();
				}
				// Parent:  javax.media.ControllerEvent.TransitionEvent.
				//    StopEvent
				else if (ce instanceof EndOfMediaEvent)
				{
					System.out.println("End of song reached, player stopped");

					// If there are more songs in the playlist, or if we've
					// reached the end of the playlist and looping
					// is enabled, then start it playing the next song.

					// More songs in playlist
					if (currentPlaylistIndex < workingPlaylist.size()-1)
					{
						System.out.println("  Starting next song in playlist");
						play(currentPlaylistIndex + 1);
					}
					// End of playlist, but looping is enabled
					else if (currentPlaylistIndex == workingPlaylist.size()-1 &&
						loopEnabled)
					{
						System.out.println("  Looping back to first song in " +
							"playlist");
						play(0);
					}
				}
				// Parent:  javax.media.ControllerEvent.TransitionEvent.
				//    StopEvent
				else if (ce instanceof RestartingEvent)
				{
					// This shouldn't happen in our case, no?
					System.out.println("Player is restarting");
				}
				// Parent:  javax.media.ControllerEvent.TransitionEvent.
				//    StopEvent
				else if (ce instanceof StopAtTimeEvent)
				{
					// Shouldn't happen in our case, but in case it does...
					System.out.println("Player stopped due to a " +
						"StopAtTimeEvent");
					switchToStoppedPlayState();
					sendSongState(null);
				}
				// Parent:  javax.media.ControllerEvent.TransitionEvent.
				//    StopEvent
				else if (ce instanceof StopByRequestEvent)
				{
					System.out.println("Player stopped on request");
					if (requestedPause)
					{
						System.out.println("  Player paused");
						playState = PS_PAUSED;
						requestedPause = false;
					}
					else
					{
						System.out.println("  Player stopped");
						switchToStoppedPlayState();
					}
					sendSongState(null);
				}
				else
				{
					System.out.println("Player stopped for an unknown reason");
					switchToStoppedPlayState();
					sendSongState(null);
				}
			}
		}
		else
		{
			// Hmm, some unknown event
			System.out.println("Unknown ControllerEvent:  " + ce.toString());
			//sendError(errorLine);
		}

		// Send the updated status to each client
		//sendSongState(null);
	}
}

