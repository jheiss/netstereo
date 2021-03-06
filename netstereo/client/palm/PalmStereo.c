/*****************************************************************************
 * $Id$
 *****************************************************************************
 * PalmOS client for NetStereo
 *****************************************************************************
 * Copyright (C) 1998-2001  Jason Heiss (jheiss@ofb.net)
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
 * Revision 1.3  2001/03/19 07:50:27  jheiss
 * Added hack to detect if the device was powered off and request
 * updated status from the server if it was.
 *
 * Revision 1.2  2001/03/16 23:15:34  jheiss
 * Removed a bunch of old, commented-out code.
 * Cleaned up/improved a number of comments.
 * Minor bug fixes and improvements.
 *
 * Revision 1.1  2001/03/16 21:50:42  jheiss
 * Initial revision
 *
 *****************************************************************************
 */

/* Includes */
#include <PalmOS.h>
#include <SerialMgrOld.h>  // Use the PalmOS 2.0 serial API
#include "PalmStereo.h"
#include "PalmStereoRsc.h"

/* Constants */
#define DEBUG 0
#define PORT 0
//#define SPEED 9600
//#define SPEED 19200
#define SPEED 57600
#define PS_STOPPED 0  // These three might be better as a enum
#define PS_PLAYING 1
#define PS_PAUSED 2
#define RECEIVE_CHECK_PER_SECOND 10  // # times/sec to check for incoming msg
#define MAX_RESPONSE_SIZE 320
#define MAX_RECEIVE_BUFFER 2048
#define MAX_RESPONSE_PARTS 10
#define MAX_INFO_LABEL 40
#define TRIM_PLAYLIST_PATH 1
#define PATH_CHAR '/'
#define FAVORITE_PLAYLIST "female-nested.m3u"

/* Global variables */
UInt16 refNum = sysInvalidRefNum;  // Serial library reference number
Boolean serLibOpened = FALSE;    // State of the serial port
MemHandle serReceiveBufferMemHandle;  // Handle for our larger receive buffer
// Current state
UInt16 currentPlaylistIndex = 0;
UInt16 playState = PS_STOPPED;
UInt16 oldPlayedSeconds = 65535;
// Available playlists, perhaps ought to be moved to a database someday
Boolean availablePlaylistsLoaded = false;
Char **availablePlaylists;
UInt16 availablePlaylistIndex = 0;
// Global so the OS doesn't have to allocate it every 0.1 seconds
Char receiveBuffer[MAX_RECEIVE_BUFFER];
Char responseLine[MAX_RESPONSE_SIZE];
UInt16 responseIndex = 0;

UInt32 PilotMain(UInt16 launchCode, MemPtr cmdPBP, UInt16 launchFlags)
{
	Err err = 0;

	/* Check for a compatible system version.  We need at least
	 * version 2 for a large number of functions that we use.
	 */
	err = RomVersionCompatible(0x02000000, launchFlags);
	if (err != 0)
	{
		return err;
	}

	if (launchCode == sysAppLaunchCmdNormalLaunch)
	{
		if ((err = StartApplication()) == 0)
		{
			EventLoop();
			StopApplication();
		}
		else
		{
			StopApplication();
		}
	}

	return err;
}

Err StartApplication(void)
{
	Err err = 0;
	MemPtr memPtr;
	SerSettingsType serSettings;

	/* Initialize serial communications with the computer */

	// Get the reference number for the serial library
	err = SysLibFind("Serial Library", &refNum); 
	if (err != 0)
	{
		FrmAlert(SerialPortRefNumAlert);
		return err;
	}

	// Open the serial port
	err = OpenSerialPort();
	if (err != 0)
	{
		// The OpenSerialPort function takes care of alerting the user
		// to any errors
		return err;
	}

	/* Install a bigger receive buffer */
	// +32 for OS overhead according to Palm docs
	serReceiveBufferMemHandle = MemHandleNew(MAX_RECEIVE_BUFFER + 32);
	if (serReceiveBufferMemHandle == 0)
	{
		// alert user
		FrmAlert(MemoryAllocationFailedAlert);

		// No need to exit or anything like that, we should be able to
		// continue with the default buffer.  ??
	}
	else
	{
		memPtr = MemHandleLock(serReceiveBufferMemHandle);
		SerSetReceiveBuffer(refNum, memPtr, MAX_RECEIVE_BUFFER + 32);
		// Can't unlock until we restore the original receive buffer on exit
	}

	// Turn on hardware handshaking
	SerGetSettings(refNum, &serSettings);
	serSettings.flags = serSettings.flags | serSettingsFlagRTSAutoM |
		serSettingsFlagCTSAutoM;
	err = SerSetSettings(refNum, &serSettings);
	if (err == serErrBadParam)
	{
	}

	// Authenticate
	//err = SerSendFlush(refNum);
	// For some reason my Pilot send a ^H in front of the first string
	// and nothing seems to get rid of that.  So we send a bogus
	// string first and then continue on our merry way.
	err = SendString("JUNK\r\n", true);
	if (err)
	{
		return err;
	}
	err = SendString("AUTH\tNULL\tNOPLAYLISTS\r\n", true);
	if (err)
	{
		return err;
	}
	// Make sure authentication was successful
	//  ...

	FrmGotoForm(MainForm);

	return err;
}

void StopApplication(void)
{
	Err err = 0;

	/* Close down the serial port */

	// Restore the original receive buffer
	SerSetReceiveBuffer(refNum, NULL, 0);
	MemHandleUnlock(serReceiveBufferMemHandle);
	MemHandleFree(serReceiveBufferMemHandle);

	err = CloseSerialPort();

	CleanupAvailablePlaylists();
}

/* Should take the port and speed as arguments and keep track of which
 * port(s) are open in a more intelligent fashion.
 */
Err OpenSerialPort(void)
{
	Err err;

	//if (serLibOpened)
	//{
		//FrmAlert(SerialPortInUseAlert);
		//return err;
	//}
	
	err = SerOpen(refNum, PORT, SPEED);

	if (err == serErrAlreadyOpen)
	{
		err = SerClose(refNum);  // We don't want to share or disrupt
		FrmAlert(SerialPortInUseAlert);
		return err;
	}
	else if (err != 0)  // Other errors
	{
		FrmAlert(SerialPortOpenAlert);
		return err;
	}

	// Record our open status in a global
	if (err == 0)
	{
		serLibOpened = TRUE;
	}

	return err;
}

Err CloseSerialPort(void)
{
	Err err;

	err = SerClose(refNum);
	serLibOpened = false;

	return err;
}

static void EventLoop(void)
{
	EventType event;
	UInt16 error;

	do
	{
		EvtGetEvent(&event, SysTicksPerSecond()/RECEIVE_CHECK_PER_SECOND);

		// It seems like we ought to do some check here to see if we
		// got a new event or if we just timed out.  Otherwise it seems
		// like we run the risk of processing an event twice.  However, it
		// is not clear how we determine why EvtGetEvent returned.  
		if (1)
		{
			if (! SysHandleEvent(&event))
				if (! MenuHandleEvent(0, &event, &error))
					if (! ApplicationHandleEvent(&event))
						FrmDispatchEvent(&event);
		}

		CheckReceive();
	} while (event.eType != appStopEvent);
}

static Boolean ApplicationHandleEvent(EventType *event)
{
	FormType *frm;
	UInt32 formId;
	Boolean handled = false;

	if (event->eType == frmLoadEvent)
	{
		// Load the form resource specified in the event then activate it
		formId = event->data.frmLoad.formID;
		frm = FrmInitForm(formId);
		FrmSetActiveForm(frm);

		// Set the event handler for the form. The handler of the currently
		// active form is called by FrmDispatchEvent each time it is called
		switch (formId)
		{
			case MainForm:
				FrmSetEventHandler(frm, MainFormHandleEvent);
				break;
			//case AboutForm:
				//FrmSetEventHandler(frm, AboutFormHandleEvent);
				//break;
		}

		handled = true;
	}

	return handled;
}

static Boolean MainFormHandleEvent(EventPtr event)
{
	Boolean handled = false;
	Char *selectedPlaylist;
	Char playlistIndexString[16];
	FormType *frm;

	switch (event->eType)
	{
		case menuEvent:
			switch (event->data.menu.itemID)
			{
				case PreferencesMenuItem:
					handled = true;
					break;
				case AboutMenuItem:
					//FrmPopupForm(AboutForm);
					//FrmDoDialog(AboutForm);
					FrmAlert(AboutAlert);
					handled = true;
					break;
			}
			break;

		case ctlEnterEvent:
			switch (event->data.ctlSelect.controlID)  // Control btn was pressed
			{
				case PlaylistPopup:
					/* If we were storing the available playlists in a
					 * database then this would be where we would
					 * fill in the Playlist list from the database
					 */
					/* The event hasn't been fully handled yet, we need to
					 * let the pilot display the popup list.
					 */
					//handled = false;
					break;
			}
			break;
			
		case popSelectEvent:
			switch (event->data.ctlSelect.controlID)
			{
				case PlaylistPopup:
					selectedPlaylist = LstGetSelectionText(
						event->data.popSelect.listP,
						event->data.popSelect.selection);
					if (StrLen(selectedPlaylist) != 0)
					{
						frm = FrmGetFormPtr(MainForm);
						// The docs imply this happens automagically, but
						// it doesn't...
						CtlSetLabel(FrmGetObjectPtr(frm,
							FrmGetObjectIndex(frm, PlaylistPopup)),
							selectedPlaylist);

						SendString("PLAYLIST\t", false);
						SendString(selectedPlaylist, false);
						SendString("\r\n", false);
					}
					handled = true;
					break;
			}
			break;

		case ctlSelectEvent: // A control button was pressed and released.
			switch (event->data.ctlSelect.controlID)
			{
				case PreviousSongButton:
					if (currentPlaylistIndex != 0)
					{
						StrIToA(playlistIndexString, currentPlaylistIndex-1);
						SendString("PLAY\t", false);
						SendString(playlistIndexString, false);
						SendString("\r\n", false);
					}
					handled = true;
					break;
				case BackButton:
					SendString("SKIP_BACK\r\n", false);
					handled = true;
					break;
				case StopButton:
					SendString("STOP\r\n", false);
					handled = true;
					break;
				case PlayPauseButton:
					switch (playState)
					{
						case PS_STOPPED:
							StrIToA(playlistIndexString, currentPlaylistIndex);
							SendString("PLAY\t", false);
							SendString(playlistIndexString, false);
							SendString("\r\n", false);
							break;
						case PS_PLAYING:
						case PS_PAUSED:
							SendString("PAUSE\r\n", false);
							break;
					}
					handled = true;
					break;
				case ForwardButton:
					SendString("SKIP_FORWARD\r\n", false);
					handled = true;
					break;
				case NextSongButton:
					// Should check to make sure we don't go off the end
					// of the playlist.  The server checks as well, but
					// better we check it and do something appropriate.
					if (1)
					{
						StrIToA(playlistIndexString, currentPlaylistIndex+1);
						SendString("PLAY\t", false);
						SendString(playlistIndexString, false);
						SendString("\r\n", false);
					}
					handled = true;
					break;
				case ShuffleCheckbox:
					if (CtlGetValue(event->data.ctlSelect.pControl))
					{
						SendString("SET_SHUFFLE\tTRUE\r\n", false);
					}
					else
					{
						SendString("SET_SHUFFLE\tFALSE\r\n", false);
					}
					break;
				case LoopCheckbox:
					if (CtlGetValue(event->data.ctlSelect.pControl))
					{
						SendString("SET_LOOP\tTRUE\r\n", false);
					}
					else
					{
						SendString("SET_LOOP\tFALSE\r\n", false);
					}
					break;
				case LoadPlaylistButton:
					SendString("GET_AVAIL_PLAYLISTS\r\n", false);
					handled = true;
					break;
				case FavoritePlaylistButton:
					SendString("PLAYLIST\t", false);
					SendString(FAVORITE_PLAYLIST, false);
					SendString("\r\n", false);
					handled = true;
					break;
			}
			break;

		case frmOpenEvent:
			FrmDrawForm(FrmGetActiveForm());
			handled = true;
			break;

		default:
			break;
	}

	return handled;
}

/*static Boolean AboutFormHandleEvent(EventPtr event)
{
	Boolean handled = false;
	switch (event->eType)
	{
		case ctlSelectEvent: // A control button was pressed and released.
			switch (event->data.ctlSelect.controlID)
			{
				case AboutOKButton:
					//FrmReturnToForm(0);
					//handled = true;
					break;
			}
			break;
		default:
			break;
	}

	return handled;
}*/

Err SendString(Char *msg, Boolean wait)
{
	UInt32 bytesSent;
	Err err;

	bytesSent = SerSend(refNum, msg, StrLen (msg), &err);
	if (err == serErrTimeOut)
	{
		// Handshake timeout
		FrmAlert(SerialHandshakeTimeoutAlert);
	}

	if (wait)
	{
		err = SerSendWait(refNum, -1);
	}
	if (err == serErrTimeOut)
	{
		// Handshake timeout
		FrmAlert(SerialHandshakeTimeoutAlert);
	}

	return err;
}

//Err CheckReceive(Boolean wait)
Err CheckReceive(void)
{
	Err err;
	UInt32 availableCount, receivedCount;
	Char *c;
	//Char *d;
	UInt16 i;
	#if DEBUG
	Char byteCountString[32];
	#endif

	err = SerReceiveCheck(refNum, &availableCount);
	if (err == serErrLineErr)
	{
		FrmAlert(SerialLineErrorAlert);
		SerClearErr(refNum);
		return err;
	}

	if (availableCount > 0)
	{
		// Don't overflow our receive buffer if there is a lot of data
		// waiting.
		if (availableCount > MAX_RECEIVE_BUFFER)
		{
			availableCount = MAX_RECEIVE_BUFFER;
		}

		receivedCount =
			SerReceive(refNum, receiveBuffer, availableCount, 0, &err);

		//StrIToA(byteCountString, receivedCount);
		//SendString("JUNK\tReceived bytes: ", false);
		//SendString(byteCountString, false);
		//SendString("\r\n", false);

		if (err == serErrLineErr)
		{
			FrmAlert(SerialLineErrorAlert);
			SerClearErr(refNum);
			return err;
		}
		else if (err == serErrTimeOut)
		{
			// Shouldn't happen since we specified no timeout?
			FrmAlert(SerialHandshakeTimeoutAlert);
			return err;
		}

		if (receivedCount != availableCount)
		{
			// Does this ever happen?
		}

		// All the commented out bits here were to do the parsing using
		// pointer arithmetic instead of array indicies.  Should be a
		// little faster but it's got a bug and doesn't work.
		//for (c=receiveBuffer, d=responseLine+responseIndex, i=0 ;
		for (c=receiveBuffer, i=0 ;
			i<receivedCount ;
			//c++, d++, i++)
			c++, i++)
		{
			// A newline or carriage return indicates a complete response
			// from the server.
			if (*c != '\n' && *c != '\r')
			{
				// -1 to allow for trailing null to be added
				if (responseIndex < MAX_RESPONSE_SIZE-1)
				{
					responseLine[responseIndex] = *c;
					//*d = *c;
					responseIndex++;
				}
				else
				{
					// Probably ought to alert the user
				}
			}
			else
			{
				if (responseIndex != 0)
				{
					responseLine[responseIndex] = '\0';
					//*d = '\0';
					ParseResponse(responseLine);
				}
				responseIndex = 0;
				//d = responseLine;
			}
		}
	}

	return err;
}

Err ParseResponse(Char *response)
{
	Err err;
	Char *c;
	UInt16 i;
	UInt16 numResponseParts;
	Char *responseParts[MAX_RESPONSE_PARTS];
	FormType *frm;
	UInt16 seconds;
	Char formattedTime[16];
	#if DEBUG
	Char playlistCountString[16];
	#endif

	//SendString("JUNK\tLINE: '", false);
	//SendString(response, false);
	//SendString("'\r\n", false);

	/* Chop the response (a tab seperated string) into an array of
	 * strings.
	 */
	responseParts[0] = response;
	i = 1;
	for (c=response; *c!='\0' ; c++)
	{
		if (*c == '\t')
		{
			if (i < MAX_RESPONSE_PARTS)
			{
				responseParts[i] = c+1;
				i++;
				*c = '\0';
			}
			else
			{
				// Probably ought to alert user
				//err = ;
			}
		}
	}
	numResponseParts = i;

	// PLAYEDSECONDS goes first since we expect to get it much more
	// often than anything else.
	if (StrCompare(responseParts[0], "PLAYEDSECONDS") == 0)
	{
		//SendString("JUNK\tPS '", false);
		//SendString(responseParts[1], false);
		//SendString("'\r\n", false);

		if (numResponseParts == 2)
		{
			seconds = StrAToI(responseParts[1]);
			FormatTime(formattedTime, seconds);
			frm = FrmGetFormPtr(MainForm);
			FrmHideObject(frm, FrmGetObjectIndex(frm, PlayedTimeTextLabel));
			FrmCopyLabel(frm, PlayedTimeTextLabel, formattedTime);
			FrmShowObject(frm, FrmGetObjectIndex(frm, PlayedTimeTextLabel));

			/* If the time is more than 10 seconds off, assume the Palm
			 * was powered off for a bit and request a status update.
			 * This should be replaced by something that uses the
			 * notification feature and request notifications for the
			 * sysNotifyLateWakeupEvent, but that stuff looked complicated
			 * so I wimped out and put in this hack.
			 */
			/*SendString("JUNK\tSECS:", false);
			SendString(responseParts[1], false);
			StrIToA(formattedTime, oldPlayedSeconds);
			SendString("  OPS:", false);
			SendString(formattedTime, false);
			SendString("\r\n", false);
			
			if (seconds > oldPlayedSeconds &&
				seconds - oldPlayedSeconds > 10)
			{
				SendString("JUNK\tGT\r\n", false);
			}
			if (seconds < oldPlayedSeconds &&
				oldPlayedSeconds - seconds > 10)
			{
				SendString("JUNK\tLT\r\n", false);
			}*/

			if (oldPlayedSeconds != 65535 &&
				((seconds > oldPlayedSeconds &&
				seconds - oldPlayedSeconds > 10) ||
				(seconds < oldPlayedSeconds &&
				oldPlayedSeconds - seconds > 10)))
			{
				SendString("STATUS\r\n", false);
			}
			oldPlayedSeconds = seconds;
		}
	}
	else if (StrCompare(responseParts[0], "ARTIST") == 0)
	{
		if (numResponseParts == 2)
		{
			StringChop(responseParts[1], MAX_INFO_LABEL);
			frm = FrmGetFormPtr(MainForm);
			FrmHideObject(frm, FrmGetObjectIndex(frm, ArtistTextLabel));
			FrmCopyLabel(frm, ArtistTextLabel, responseParts[1]);
			FrmShowObject(frm, FrmGetObjectIndex(frm, ArtistTextLabel));
		}
	}
	else if (StrCompare(responseParts[0], "ALBUM") == 0)
	{
		if (numResponseParts == 2)
		{
			StringChop(responseParts[1], MAX_INFO_LABEL);
			frm = FrmGetFormPtr(MainForm);
			FrmHideObject(frm, FrmGetObjectIndex(frm, AlbumTextLabel));
			FrmCopyLabel(frm, AlbumTextLabel, responseParts[1]);
			FrmShowObject(frm, FrmGetObjectIndex(frm, AlbumTextLabel));
		}
	}
	else if (StrCompare(responseParts[0], "SONG") == 0)
	{
		if (numResponseParts == 2)
		{
			StringChop(responseParts[1], MAX_INFO_LABEL);
			frm = FrmGetFormPtr(MainForm);
			FrmHideObject(frm, FrmGetObjectIndex(frm, SongTextLabel));
			FrmCopyLabel(frm, SongTextLabel, responseParts[1]);
			FrmShowObject(frm, FrmGetObjectIndex(frm, SongTextLabel));
		}
	}
	else if (StrCompare(responseParts[0], "SONGINFO") == 0)
	{
		if (numResponseParts == 2)
		{
			StringChop(responseParts[1], MAX_INFO_LABEL);
			frm = FrmGetFormPtr(MainForm);
			FrmHideObject(frm, FrmGetObjectIndex(frm, InfoTextLabel));
			FrmCopyLabel(frm, InfoTextLabel, responseParts[1]);
			FrmShowObject(frm, FrmGetObjectIndex(frm, InfoTextLabel));
		}
	}
	else if (StrCompare(responseParts[0], "TOTALSECONDS") == 0)
	{
		if (numResponseParts == 2)
		{
			seconds = StrAToI(responseParts[1]);
			FormatTime(formattedTime, seconds);
			frm = FrmGetFormPtr(MainForm);
			FrmHideObject(frm, FrmGetObjectIndex(frm, TotalTimeTextLabel));
			FrmCopyLabel(frm, TotalTimeTextLabel, formattedTime);
			FrmShowObject(frm, FrmGetObjectIndex(frm, TotalTimeTextLabel));
		}
	}
	else if (StrCompare(responseParts[0], "CURRENTPLAYLISTINDEX") == 0)
	{
		if (numResponseParts == 2)
		{
			currentPlaylistIndex = StrAToI(responseParts[1]);
		}
	}
	else if (StrCompare(responseParts[0], "PLAYSTATE") == 0)
	{
		if (numResponseParts == 2)
		{
			playState = StrAToI(responseParts[1]);
			switch (playState)
			{
				case PS_STOPPED:
					frm = FrmGetFormPtr(MainForm);
					CtlSetLabel(FrmGetObjectPtr(frm,
						FrmGetObjectIndex(frm, PlayPauseButton)),
						"Play");
					break;
				case PS_PLAYING:
					frm = FrmGetFormPtr(MainForm);
					CtlSetLabel(FrmGetObjectPtr(frm,
						FrmGetObjectIndex(frm, PlayPauseButton)),
						"Pause");
					break;
				case PS_PAUSED:
					frm = FrmGetFormPtr(MainForm);
					CtlSetLabel(FrmGetObjectPtr(frm,
						FrmGetObjectIndex(frm, PlayPauseButton)),
						"Unpause");
					break;
			}
		}
	}
	else if (StrCompare(responseParts[0], "SHUFFLEENABLED") == 0)
	{
		if (numResponseParts == 2)
		{
			frm = FrmGetFormPtr(MainForm);
			if (StrCompare(responseParts[1], "true") == 0)
			{
				FrmSetControlValue(frm,
					FrmGetObjectIndex(frm, ShuffleCheckbox), 1);
			}
			else
			{
				FrmSetControlValue(frm,
					FrmGetObjectIndex(frm, ShuffleCheckbox), 0);
			}
		}
	}
	else if (StrCompare(responseParts[0], "LOOPENABLED") == 0)
	{
		if (numResponseParts == 2)
		{
			frm = FrmGetFormPtr(MainForm);
			if (StrCompare(responseParts[1], "true") == 0)
			{
				FrmSetControlValue(frm,
					FrmGetObjectIndex(frm, LoopCheckbox), 1);
			}
			else
			{
				FrmSetControlValue(frm,
					FrmGetObjectIndex(frm, LoopCheckbox), 0);
			}
		}
	}
	else if (StrCompare(responseParts[0], "PLAYLIST") == 0)
	{
		if (StrLen(responseParts[1]) == 0)
		{
			return 0;
		}

		// Set selected entry in PlaylistList
		if (availablePlaylistsLoaded)
		{
			// TODO
		}
		else
		{
			/* Playlists haven't been loaded from server yet, so
			 * create a fake list with just this playlist.
			 */
			CleanupAvailablePlaylists();
			CreateAvailablePlaylistArray(1);
			AddToAvailablePlaylists(responseParts[1]);
			UpdatePlaylistDisplay();
			frm = FrmGetFormPtr(MainForm);
			CtlSetLabel(FrmGetObjectPtr(frm,
				FrmGetObjectIndex(frm, PlaylistPopup)),
				responseParts[1]);
		}
	}
	else if (StrCompare(responseParts[0], "END_PLAYLIST") == 0)
	{
		// Nothing to do, since we don't get the contents of the
		// playlists.
	}
	else if (StrCompare(responseParts[0], "AVAIL_PLAYLISTS") == 0)
	{
		/* Clear existing UI list so we can free the associated memory */
		// It seems like we ought to do this, but the Pilot crashes if
		// we do...  Maybe we shouldn't pass a NULL to LstSetListChoices.
		//listPtr = FrmGetObjectPtr(frm, PlaylistList);
		//LstSetListChoices(listPtr, NULL, 0);

		/* Free up the memory used by the old list */
		CleanupAvailablePlaylists();

		/* Create a new array to hold the new list of playlists */
		CreateAvailablePlaylistArray(StrAToI(responseParts[1]));
	}
	else if (StrCompare(responseParts[0], "AVAIL_PLAYLIST") == 0)
	{
		//StrIToA(playlistCountString, availablePlaylistIndex);
		//SendString("JUNK\tAVP: '", false);
		//SendString(playlistCountString, false);
		//SendString("_", false);
		//SendString(responseParts[1], false);
		//SendString("'\r\n", false);

		AddToAvailablePlaylists(responseParts[1]);
	}
	else if (StrCompare(responseParts[0], "END_AVAIL_PLAYLISTS") == 0)
	{
		#if DEBUG
		StrIToA(playlistCountString, availablePlaylistIndex);
		SendString("JUNK\tAVP count: ", false);
		SendString(playlistCountString, false);
		SendString("\r\n", false);
		#endif

		UpdatePlaylistDisplay();
		availablePlaylistsLoaded = true;
	}
	else if (StrCompare(responseParts[0], "ERROR") == 0)
	{
	}
	else
	{
		// Probably ought to alert user
		//err = ;
	}

	return err;
}

void CreateAvailablePlaylistArray(UInt16 numberOfPlaylists)
{
	availablePlaylists =
		AllocateAndLockMemory(sizeof(Char *) * numberOfPlaylists);
	if (availablePlaylists == NULL)
	{
		// alert user
		FrmAlert(MemoryAllocationFailedAlert);
		return;
	}
}

MemPtr AllocateAndLockMemory(UInt16 amountOfMemoryToAllocate)
{
	MemHandle memHandle;

	memHandle = MemHandleNew(amountOfMemoryToAllocate);
	if (memHandle == 0)
	{
		// alert user
		FrmAlert(MemoryAllocationFailedAlert);
		return NULL;
	}
	return MemHandleLock(memHandle);
}

void AddToAvailablePlaylists(Char *playlistName)
{
	Char *c, *d;
	MemPtr memPtr;

	if (availablePlaylists == NULL)
	{
		return;
	}

	if (TRIM_PLAYLIST_PATH)
	{
		for (c=playlistName, d=c ; *c!='\0' ; c++)
		{
			if (*c == PATH_CHAR)
			{
				d = c + 1;
			}
		}
		playlistName = d;
	}

	memPtr = AllocateAndLockMemory(StrLen(playlistName)+1);
	if (memPtr != NULL)
	{
		StrCopy(memPtr, playlistName);
		availablePlaylists[availablePlaylistIndex] = memPtr;
		availablePlaylistIndex++;
	}
}

void CleanupAvailablePlaylists(void)
{
	MemHandle memHandle;
	UInt16 i;

	/* Free the memory for each playlist entry */
	for(i=0 ; i<availablePlaylistIndex ; i++)
	{
		if (availablePlaylists != NULL && availablePlaylists[i] != NULL)
		{
			memHandle = MemPtrRecoverHandle(availablePlaylists[i]);
			if (memHandle != 0)
			{
				MemHandleUnlock(memHandle);
				MemHandleFree(memHandle);
			}
		}
	}
	/* Free the memory for the array */
	if (availablePlaylists != NULL)
	{
		memHandle = MemPtrRecoverHandle(availablePlaylists);
		if (memHandle != 0)
		{
			MemHandleUnlock(memHandle);
			MemHandleFree(memHandle);
		}
	}

	availablePlaylistIndex = 0;
}

void UpdatePlaylistDisplay(void)
{
	FormType *frm;
	ListType *list;

	if (availablePlaylists == NULL)
	{
		return;
	}

	frm = FrmGetFormPtr(MainForm);
	list = FrmGetObjectPtr(frm, FrmGetObjectIndex(frm, PlaylistList));
	if (list != NULL)
	{
		LstSetListChoices(list, availablePlaylists, availablePlaylistIndex);
		LstSetHeight(list, availablePlaylistIndex);
	}
	CtlSetLabel(FrmGetObjectPtr(frm, FrmGetObjectIndex(frm, PlaylistPopup)),
		"Select Playlist");
}

/* Ensure 'string' in no longer than 'size' by inserting a null character
 * at position 'size - 1' in the Char array.  Note that 'string' may have
 * a null character earlier in the array, this function does not affect
 * that.
 */
void StringChop(Char *string, UInt16 size)
{
	Char *c;

	c = string + (size - 1);
	*c = '\0';
}

/* Take a number of seconds and return MM:SS */
Char *FormatTime(Char *formattedTime, UInt16 seconds)
{
	UInt16 minutes, remainingSeconds;

	minutes = seconds / 60;
	remainingSeconds = seconds - (minutes * 60);

	if (remainingSeconds < 10)
	{
		StrPrintF(formattedTime, "%d:0%d", minutes, remainingSeconds);
	}
	else
	{
		StrPrintF(formattedTime, "%d:%d", minutes, remainingSeconds);
	}

	return formattedTime;
}

/* Stolen from the O'Reilly Palm Programming book. */
static Err RomVersionCompatible(UInt32 requiredVersion, UInt16 launchFlags)
{
	UInt32 romVersion;

	/* See if we're on a minimum required version of the ROM or later.
	 * The system records the version number in a feature. A feature is a
	 * piece of information that can be looked up by a creator and feature
	 * number.
	 */
	FtrGet(sysFtrCreator, sysFtrNumROMVersion, &romVersion);

	if (romVersion < requiredVersion)
	{
		/* If the user launched the app from the launcher, explain
		 * why the app shouldn't run. If the app was contacted for
		 * something else, like it was asked to find a string by the
		 * system find, then don't bother the user with a warning dialog.
		 * These flags tell how the app was launched to decided if a
		 * warning should be displayed.
		 */
		if ((launchFlags &
		     (sysAppLaunchFlagNewGlobals | sysAppLaunchFlagUIApp))
		    == (sysAppLaunchFlagNewGlobals | sysAppLaunchFlagUIApp))
		{
			FrmAlert(RomIncompatibleAlert);

			/* Pilot 1.0 will continuously relaunch this app unless we switch
			 * to another safe one. The sysFileCDefaultApp is
			 * considered "safe".
			 */
			if (romVersion < 0x02000000)
			{
				AppLaunchWithCommand(sysFileCDefaultApp,
				                     sysAppLaunchCmdNormalLaunch, NULL);
			}
		}
		return sysErrRomIncompatible;
	}
	return 0;
}

