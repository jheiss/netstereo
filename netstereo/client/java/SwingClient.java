/*****************************************************************************
 * $Id$
 *****************************************************************************
 * NetStereo client using the Swing GUI toolkit.
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
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class SwingClient extends StereoClient
{
	// Class-wide variables
	JFrame frame = null;
	private JMenuBar menuBar = null;
	private JMenu fileMenu = null;
	private JMenuItem preferencesMenuItem = null;
	private JMenuItem exitMenuItem = null;
	private JProgressBar songProgressBar = null;
	private JLabel currentSongLength = null;
	private javax.swing.Timer timer = null;
	private JFileChooser fileChooser = null;
	private SwingClientSongFileFilter songFileFilter = null;
	private SwingClientPlaylistFileFilter playlistFileFilter = null;
	private JLabel artistLabel = null, albumLabel = null, songLabel = null,
		songInfoLabel = null;
	private JButton playPauseButton = null;
	private JCheckBox shuffleCheckBox = null;
	private JCheckBox loopCheckBox = null;
	private JList playlistList = null;
	private DefaultListModel playlistListModel = null;
	private JScrollPane playlistScrollPane = null;
	private boolean playlistUpdated = false;
	private SwingClientItemListener itemListener = null;
	private JDialog playlistSelectDialog = null;
	private JList playlistSelectList = null;
	private JDialog songSelectDialog = null;
	private JList songSelectList = null;

	// These should eventually be user-selected
	private Vector songExtensions = null;
	private Vector playlistExtensions = null;

	public SwingClient()
	{
		super();

		// Create the window
		frame = new JFrame("Net Stereo Client");

		// Handle death with grace
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		// Create lists of extensions for file selection box filters
		songExtensions = new Vector();
		songExtensions.addElement("mp3");
		playlistExtensions = new Vector();
		playlistExtensions.addElement("m3u");

		// Create the panel and add it to the window
		frame.getContentPane().add(createPanel(), BorderLayout.CENTER);

		// Finish up
		frame.pack();
		frame.setVisible(true);

		// Start the IO thread now that the GUI is visible
		ioThread.start();
	}

	public JPanel createPanel()
	{
		// Create the JPanel
		JPanel panel = new JPanel();

		// Create the file chooser
		fileChooser = new JFileChooser();
		//songFileFilter = new SwingClientSongFileFilter();
		//playlistFileFilter = new SwingClientPlaylistFileFilter();
		//fileChooser.addChoosableFileFilter(songFileFilter);
		//fileChooser.addChoosableFileFilter(playlistFileFilter);
		
		// Create the action listener
		SwingClientActionListener actionListener =
			new SwingClientActionListener();

		// Create the menus
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.getAccessibleContext().	
			setAccessibleDescription("File Menu");
		menuBar.add(fileMenu);
		preferencesMenuItem = new JMenuItem("Preferences", KeyEvent.VK_P);
		preferencesMenuItem.getAccessibleContext().	
			setAccessibleDescription("Program preferences configuration");
		preferencesMenuItem.addActionListener(actionListener);
		fileMenu.add(preferencesMenuItem);
		exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
		exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4,
			ActionEvent.ALT_MASK));
		exitMenuItem.getAccessibleContext().	
			setAccessibleDescription("Exit Program");
		exitMenuItem.addActionListener(actionListener);
		fileMenu.add(exitMenuItem);

		// Create the artist, album, song and song info labels
		artistLabel = new JLabel("Artist:");
		albumLabel = new JLabel("Album:");
		songLabel = new JLabel("Song:");
		songInfoLabel = new JLabel("");
		
		// Create the song progress bar
		songProgressBar = new JProgressBar();
		songProgressBar.setString("00:00");
		songProgressBar.setStringPainted(true);
		JLabel initialSongTime = new JLabel("0:00");
		currentSongLength = new JLabel("00:00");

		// Create all of the buttons
		JPanel topButtonPanel = new JPanel();
		JButton previousSongButton = new JButton("Previous Song");
		previousSongButton.addActionListener(actionListener);
		JButton backButton = new JButton("Rewind");
		backButton.addActionListener(actionListener);
		playPauseButton = new JButton("Play");
		playPauseButton.addActionListener(actionListener);
		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(actionListener);
		JButton fastForwardButton = new JButton("Fast Forward");
		fastForwardButton.addActionListener(actionListener);
		JButton nextSongButton = new JButton("Next Song");
		nextSongButton.addActionListener(actionListener);

		JPanel bottomButtonPanel = new JPanel();
		JButton addSongButton = new JButton("Add Song(s)");
		addSongButton.addActionListener(actionListener);
		JButton deleteSongButton = new JButton("Remove Song(s)");
		deleteSongButton.addActionListener(actionListener);
		JButton loadPlaylistButton = new JButton("Load Playlist");
		loadPlaylistButton.addActionListener(actionListener);
		JButton savePlaylistButton = new JButton("Save Playlist");
		savePlaylistButton.addActionListener(actionListener);
		JButton clearPlaylist = new JButton("Clear Playlist");
		clearPlaylist.addActionListener(actionListener);
		//JButton quitButton = new JButton("Quit");
		//quitButton.addActionListener(actionListener);

		// Create the checkboxes
		itemListener = new SwingClientItemListener();
		shuffleCheckBox = new JCheckBox("Shuffle", shuffleEnabled);
		shuffleCheckBox.addItemListener(itemListener);
		loopCheckBox = new JCheckBox("Loop Playlist", loopEnabled);
		loopCheckBox.addItemListener(itemListener);

		// Create all of the components of the playlist
		playlistListModel = new DefaultListModel();
		playlistList = new JList(playlistListModel);
		playlistScrollPane = new JScrollPane(playlistList);

		// Setup the layout manager
		//panel.setLayout(new GridLayout(2,0));
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(gridbag);

		// Add the compenents to our panel
		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 0;
		c.gridheight = 1; c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(artistLabel, c);
		panel.add(artistLabel);

		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 1;
		c.gridheight = 1; c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(albumLabel, c);
		panel.add(albumLabel);

		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 2;
		c.gridheight = 1; c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(songLabel, c);
		panel.add(songLabel);

		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 3;
		c.gridheight = 1; c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(initialSongTime, c);
		panel.add(initialSongTime);

		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 3;
		//c.gridheight = 1; c.gridwidth = GridBagConstraints.RELATIVE;
		c.gridheight = 1; c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(songProgressBar, c);
		panel.add(songProgressBar);

		//c.gridx = 4; c.gridy = 3;
		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 3;
		c.gridheight = 1; c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(currentSongLength, c);
		panel.add(currentSongLength);

		// Code to put in the top button row not in a panel
		// See next section
		//c.gridx = GridBagConstraints.RELATIVE; c.gridy = 4;
		//c.gridheight = 1; c.gridwidth = 1;
		//c.fill = GridBagConstraints.NONE;
		//gridbag.setConstraints(previousSongButton, c);
		//panel.add(previousSongButton);
		//gridbag.setConstraints(backButton, c);
		//panel.add(backButton);
		//gridbag.setConstraints(playPauseButton, c);
		//panel.add(playPauseButton);
		//gridbag.setConstraints(stopButton, c);
		//panel.add(stopButton);
		//gridbag.setConstraints(fastForwardButton, c);
		//panel.add(fastForwardButton);
		//gridbag.setConstraints(nextSongButton, c);
		//panel.add(nextSongButton);

		// Put the top row of button in a seperate panel so that they
		// can squish together
		topButtonPanel.add(previousSongButton);
		topButtonPanel.add(backButton);
		topButtonPanel.add(playPauseButton);
		topButtonPanel.add(stopButton);
		topButtonPanel.add(fastForwardButton);
		topButtonPanel.add(nextSongButton);
		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 4;
		c.gridheight = 1; c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(topButtonPanel, c);
		panel.add(topButtonPanel);

		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 5;
		c.gridheight = 1; c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(shuffleCheckBox, c);
		panel.add(shuffleCheckBox);
		gridbag.setConstraints(loopCheckBox, c);
		panel.add(loopCheckBox);
		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 5;
		c.gridheight = 1; c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(songInfoLabel, c);
		panel.add(songInfoLabel);

		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 6;
		c.gridheight = 4; c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(playlistScrollPane, c);
		panel.add(playlistScrollPane);

		c.gridx = GridBagConstraints.RELATIVE; c.gridy = 10;
		c.gridheight = 1; c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(addSongButton, c);
		panel.add(addSongButton);
		gridbag.setConstraints(deleteSongButton, c);
		panel.add(deleteSongButton);
		gridbag.setConstraints(loadPlaylistButton, c);
		panel.add(loadPlaylistButton);
		gridbag.setConstraints(savePlaylistButton, c);
		panel.add(savePlaylistButton);
		gridbag.setConstraints(clearPlaylist, c);
		panel.add(clearPlaylist);
		//gridbag.setConstraints(quitButton, c);
		//panel.add(quitButton);

		// Experiment in putting the buttom row of buttons in a panel
		// Totally messed up the layout of the app
		//bottomButtonPanel.add(addSongButton);
		//bottomButtonPanel.add(deleteSongButton);
		//bottomButtonPanel.add(loadPlaylistButton);
		//bottomButtonPanel.add(savePlaylistButton);
		//bottomButtonPanel.add(clearPlaylist);
		//c.gridx = GridBagConstraints.RELATIVE; c.gridy = 10;
		//c.gridheight = 1; c.gridwidth = GridBagConstraints.REMAINDER;
		//c.fill = GridBagConstraints.HORIZONTAL;
		//gridbag.setConstraints(bottomButtonPanel, c);
		//panel.add(bottomButtonPanel);

		// Create a timer to update the song info and progress bar
		// every .5 seconds
		timer = new javax.swing.Timer(500, new ActionListener() {
			private int previousPlayState = PS_STOPPED;
			private String previousSong = "";
			public void actionPerformed(ActionEvent e) {

				// Things that must be updated every time
				songProgressBar.setValue(playedSeconds);
				songProgressBar.setString(secToMinSec(playedSeconds));

				if (loopEnabled != loopCheckBox.isSelected())
				{
					// Arrgh, setSelected generated an ItemListener event,
					// which causes us to send the "new" state to the server,
					// which sends it back, which....  So we disable the
					// ItemListener while making the
					System.err.println("loopEnabled: " + loopEnabled +
						"  isSelected: " + loopCheckBox.isSelected());
					loopCheckBox.removeItemListener(itemListener);
					loopCheckBox.setSelected(loopEnabled);
					loopCheckBox.addItemListener(itemListener);
				}

				// Things that only need to be updated if we're playing
				// a new song or our playState changed.
				if (playState != previousPlayState ||
					currentSong != previousSong)
				{
					songProgressBar.setMaximum(totalSeconds);
					currentSongLength.setText(secToMinSec(totalSeconds));
					artistLabel.setText("Artist:  " + currentArtist);
					albumLabel.setText("Album:  " + currentAlbum);
					songLabel.setText("Song:  " + currentSong);
					songInfoLabel.setText(currentSongInfo);
					if (playState == PS_PLAYING)
					{
						playPauseButton.setText("Pause");
					}
					else if (playState == PS_PAUSED)
					{
						playPauseButton.setText("Unpause");
					}
					else
					{
						playPauseButton.setText("Play");
					}
					frame.pack();
				}
				previousPlayState = playState;
				previousSong = currentSong;

				if (playlistUpdated)
				{
					// See the comments above for the reasoning for the
					// ItemListener twiddling.
					System.err.println("Playlist updated");
					shuffleCheckBox.removeItemListener(itemListener);
					shuffleCheckBox.setSelected(shuffleEnabled);
					shuffleCheckBox.addItemListener(itemListener);
					refreshPlaylistList();
					playlistUpdated = false;
				}
			}
		});
		timer.start();

		return panel;
	}

	public JPanel createPreferencesPanel()
	{
		JPanel preferencesPanel = new JPanel();

		return preferencesPanel;
	}

	public static void main(String[] args)
	{
		// Instantiate ourselves to make the event handler stuff happy
		SwingClient ourClient = new SwingClient();
	}

	// Covert from a integer number of seconds to a MM:SS string.
	// Seems like there ought to be a Java method to do this but I
	// couldn't find one.
	public static String secToMinSec(int seconds)
	{
		int minutes = seconds / 60;
		int remSeconds = seconds - (60 * minutes);

		if (remSeconds < 10)
		{
			return new String(minutes + ":0" + remSeconds);
		}
		else
		{
			return new String(minutes + ":" + remSeconds);
		}
	}

	// We need to override this method so that we can update the onscreen
	// playlist.
	public void setPlaylist(String newPlaylistName, Vector newPlaylist)
	{
		System.err.println("New playlist received");
		super.setPlaylist(newPlaylistName, newPlaylist);
		playlistUpdated = true;
	}

	// And this method so we can select the right song in the onscreen
	// playlist.
	public void setCurrentPlaylistIndex(int newCurrentPlaylistIndex)
	{
		super.setCurrentPlaylistIndex(newCurrentPlaylistIndex);
		playlistList.setSelectedIndex(newCurrentPlaylistIndex);
		playlistList.ensureIndexIsVisible(newCurrentPlaylistIndex);
	}

	public void setError(String errorMessage)
	{
		JOptionPane.showMessageDialog(frame, errorMessage, "Error",
			JOptionPane.ERROR_MESSAGE);
	}

	class SwingClientActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			String actionCommand = e.getActionCommand();
			String commandToSend = "";

			if (actionCommand.equals("Previous Song"))
			{
				ioHandler.sendPlay(currentPlaylistIndex - 1);
			}
			else if (actionCommand.equals("Rewind"))
			{
				ioHandler.sendSkipBack();
			}
			else if (actionCommand.equals("Play") ||
				actionCommand.equals("Pause") ||
				actionCommand.equals("Unpause"))
			{
				if (playState == PS_STOPPED)
				{
					ioHandler.sendPlay(currentPlaylistIndex);
				}
				else if (playState == PS_PLAYING)
				{
					ioHandler.sendPause();
				}
				else if (playState == PS_PAUSED)
				{
					ioHandler.sendPause();
				}
			}
			else if (actionCommand.equals("Stop"))
			{
				// playState will be changed when we get the acknowledgement
				// message back from the server.
				ioHandler.sendStop();
			}
			else if (actionCommand.equals("Fast Forward"))
			{
				ioHandler.sendSkipForward();
			}
			else if (actionCommand.equals("Next Song"))
			{
				ioHandler.sendPlay(currentPlaylistIndex + 1);
			}
			else if (actionCommand.equals("Remove Song(s)"))
			{
				Object[] songsToDelete = playlistList.getSelectedValues();
				for (int i=0 ; i<songsToDelete.length ; ++i)
				{
					ioHandler.sendDeleteSongFromPlaylist(
						(String) songsToDelete[i]);
				}
			}
			else if (actionCommand.equals("Add Song(s)"))
			{
				availableSongs.removeAllElements();
				ioHandler.sendGetAvailableSongs();
				// Wait for up to 45 seconds to get a list of songs back
				// from the server
				for (int i=0 ; i<45 && availableSongs.size()==0 ; i++)
				{
					// It would be cool if we popped up some sort of
					// progress dialog here
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException ie)
					{
					}
				}
				if (availableSongs.size() == 0)
				{
					JOptionPane.showMessageDialog(frame,
						"No songs received from server",
						"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				else
				{
					songSelectDialog = new JDialog(frame, "Select Song(s)",
						true);
					songSelectList = new JList(availableSongs);
					//songSelectList.
						//setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					//SongListSelectionHandler listSelectionHandler =
						//new SongListSelectionHandler();
					//songSelectList.
						//addListSelectionListener(listSelectionHandler);
					SongDialogActionHandler actionHandler =
						new SongDialogActionHandler();
					JScrollPane songSelectScrollPane =
						new JScrollPane(songSelectList);
					JButton okButton = new JButton("OK");
					okButton.addActionListener(actionHandler);
					JButton cancelButton = new JButton("Cancel");
					cancelButton.addActionListener(actionHandler);
					JPanel buttonPanel = new JPanel();
					buttonPanel.add(okButton);
					buttonPanel.add(cancelButton);
					songSelectDialog.getContentPane().
						add(songSelectScrollPane, BorderLayout.CENTER);
					songSelectDialog.getContentPane().
						add(buttonPanel, BorderLayout.SOUTH);
					songSelectDialog.pack();
					songSelectDialog.show();
				}
			}
			else if (actionCommand.equals("Load Playlist"))
			{
				availablePlaylists.removeAllElements();
				ioHandler.sendGetAvailablePlaylists();
				// Wait for up to 15 seconds to get a set of playlists back
				// from the server
				for (int i=0 ; i<15 && availablePlaylists.size()==0 ; i++)
				{
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException ie)
					{
					}
				}
				if (availablePlaylists.size() == 0)
				{
					JOptionPane.showMessageDialog(frame,
						"No playlists received from server",
						"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				else
				{
					playlistSelectDialog = new JDialog(frame, "Select Playlist",
						true);
					playlistSelectList = new JList(availablePlaylists);
					playlistSelectList.
						setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					PlaylistListSelectionHandler listSelectionHandler =
						new PlaylistListSelectionHandler();
					playlistSelectList.
						addListSelectionListener(listSelectionHandler);
					JScrollPane playlistSelectScrollPane =
						new JScrollPane(playlistSelectList);
					playlistSelectDialog.getContentPane().
						add(playlistSelectScrollPane);
					playlistSelectDialog.pack();
					playlistSelectDialog.show();
				}
			}
			else if (actionCommand.equals("Save Playlist"))
			{
				fileChooser.setCurrentDirectory(new File(songDirectory));
				fileChooser.rescanCurrentDirectory();
				fileChooser.setFileFilter(playlistFileFilter);
				int returnVal = fileChooser.showSaveDialog(frame);

				if (returnVal == JFileChooser.APPROVE_OPTION)
				{
					PrintWriter playlistWriter = null;

					try
					{
						playlistWriter = new PrintWriter(
							new FileWriter(fileChooser.
								getSelectedFile().getAbsolutePath()));
					}
					catch (IOException savePlaylistIOException)
					{
						JOptionPane.showMessageDialog(frame,
							"Can't write to playlist file " + 
							fileChooser.getSelectedFile().getAbsolutePath(),
							"Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					for (Enumeration savePlaylistEnumeration =
						playlist.elements();
						savePlaylistEnumeration.hasMoreElements();)
					{
						playlistWriter.println(
							savePlaylistEnumeration.nextElement());
					}

					playlistWriter.close();
				}
			}
			else if (actionCommand.equals("Clear Playlist"))
			{
				ioHandler.sendClearPlaylist();
			}
			//else if (actionCommand.equals("Quit"))
			else if (actionCommand.equals("Exit"))
			{
				System.exit(0);
			}
		}
	}

	class SwingClientItemListener implements ItemListener
	{
		public void itemStateChanged(ItemEvent e)
		{
			Object source = e.getItemSelectable();

			if (source == shuffleCheckBox)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
				{
					ioHandler.sendShuffle(true);
				}
				else
				{
					ioHandler.sendShuffle(false);
				}
			}

			if (source == loopCheckBox)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
				{
					ioHandler.sendLoop(true);
				}
				else
				{
					ioHandler.sendLoop(false);
				}
			}
		}
	}

	class SongDialogActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			String actionCommand = e.getActionCommand();

			if (actionCommand.equals("OK"))
			{
				int indices[] = songSelectList.getSelectedIndices();

				for (int i=0 ; i<indices.length ; i++)
				{
					ioHandler.sendAddSongToPlaylist((String)
						availableSongs.elementAt(indices[i]),
						currentPlaylistIndex + 1 + i);
				}
			}

			songSelectDialog.hide();
		}
	}

	class PlaylistListSelectionHandler implements ListSelectionListener
	{
		public void valueChanged (ListSelectionEvent e)
		{
			if (e.getValueIsAdjusting())
			{
				return;
			}

			JList theList = (JList) e.getSource();
			if (theList.isSelectionEmpty())
			{
				return;
			}
			else
			{
				int index = theList.getSelectedIndex();
				ioHandler.sendPlaylist((String)
					availablePlaylists.elementAt(index));
				playlistSelectDialog.hide();
			}
		}
	}

	// Affects GUI, must only be called from within event listeners
	private void refreshPlaylistList()
	{
		playlistListModel.clear();
		currentPlaylistIndex = 0;

		for (Enumeration refreshPlaylistEnumeration = playlist.elements();
			refreshPlaylistEnumeration.hasMoreElements();)
		{
			playlistListModel.addElement(
				refreshPlaylistEnumeration.nextElement());
		}

		playlistList.setSelectedIndex(currentPlaylistIndex);
		playlistList.ensureIndexIsVisible(currentPlaylistIndex);
	}

	class SwingClientSongFileFilter extends javax.swing.filechooser.FileFilter
	{
		public boolean accept(File f)
		{
			if (f.isDirectory())
			{
				return true;
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

		public String getDescription()
		{
			// Vector.toString makes a nice list like:  [one, two, three]
			return "Song files " + songExtensions.toString();
		}
	}

	class SwingClientPlaylistFileFilter
		extends javax.swing.filechooser.FileFilter
	{
		public boolean accept(File f)
		{
			if (f.isDirectory())
			{
				return true;
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

		public String getDescription()
		{
			// Vector.toString makes a nice list like:  [one, two, three]
			return "Playlist files " + playlistExtensions.toString();
		}
	}
}

