/*****************************************************************************
 * $Id$
 *****************************************************************************
 * PalmOS client for NetStereo
 *****************************************************************************
 * $Log$
 *****************************************************************************
 */

/* Includes */
#include <PalmOS.h>
#include <SerialMgrOld.h>  // Use the PalmOS 2.0 serial API
#include "PalmStereo.h"
#include "PalmStereoRsc.h"

/* Constants */
#define DEBUG 1
#define PORT 0
//#define SPEED 9600
//#define SPEED 19200
#define SPEED 57600
#define PS_STOPPED 0  // These three might be better as a enum
#define PS_PLAYING 1
#define PS_PAUSED 2
#define MAX_PLAYLIST_NAME_LENGTH 256  // Don't need?
#define MAX_COMMAND_SIZE 320  // Don't need?
#define MAX_RESPONSE_SIZE 320
#define MAX_RECEIVE_BUFFER 2048
#define MAX_RESPONSE_PARTS 10
#define MAX_INFO_LABEL 40
#define TRIM_PLAYLIST_PATH 1
#define PATH_CHAR '/'

/* Global variables */
UInt16 refNum = sysInvalidRefNum;  // Serial library reference number
Boolean serLibOpened = FALSE;    // State of the serial port
MemHandle serReceiveBufferMemHandle;
/* Current state */
//Char *currentArtist;  // Don't need
//Char *currentAlbum;  // Don't need
//Char *currentSong;  // Don't need
//Char *currentSongInfo;  // Don't need
UInt16 currentPlaylistIndex = 0;
UInt16 playState = PS_STOPPED;
//UInt16 totalSeconds, playedSeconds;  // Don't need
Boolean availablePlaylistsLoaded = false;
Char **availablePlaylists;
//Char *availablePlaylists[128];
UInt16 availablePlaylistIndex = 0;
//Char *tempPlaylist;
//Char *playlistName;  // Don't need
//Boolean shuffleEnabled;  // Don't need
//Boolean loopEnabled;  // Don't need
// Global so the OS doesn't have to allocate it every 0.1 seconds
Char receiveBuffer[MAX_RECEIVE_BUFFER];
Char responseLine[MAX_RESPONSE_SIZE];
UInt16 responseIndex = 0;

UInt32 PilotMain(UInt16 launchCode, MemPtr cmdPBP, UInt16 launchFlags)
{
	Err err = 0;

	/* Check for a compatible system version.  We need at least
	 * version 2 for some of the serial functions we use.
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
	err = SendString("JUNK\r\n", true);
	err = SendString("AUTH\tNULL\tNOPLAYLISTS\r\n", true);
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
		//EvtGetEvent(&event, evtWaitForever);
		EvtGetEvent(&event, SysTicksPerSecond()/10);

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
					// Fill in the Playlist list from the database
					// The event hasn't been fully handled yet, we need to
					// let the pilot display the popup list.
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
					frm = FrmGetFormPtr(MainForm);
					CtlSetLabel(FrmGetObjectPtr(frm,
						FrmGetObjectIndex(frm, PlaylistPopup)),
						selectedPlaylist);

					SendString("PLAYLIST\t", false);
					SendString(selectedPlaylist, false);
					SendString("\r\n", false);
					handled = true;
					break;
			}
			break;

		case ctlSelectEvent: // A control button was pressed and released.
			switch (event->data.ctlSelect.controlID)
			{
				case SkipBackButton:
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
				case SkipForwardButton:
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
	Char *c, *d;
	UInt16 i;
	Char byteCountString[32];  // For debugging

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
	Char *c, *d;
	UInt16 i = 1;
	UInt16 numResponseParts = 0;
	Char *responseParts[MAX_RESPONSE_PARTS];
	FormType *frm;
	//MemHandle memHandle;
	//MemPtr memPtr;
	//ListType *listPtr;
	//ListPtr listPtr;
	Char playlistCountString[16];  // For debugging only
	UInt16 seconds;
	Char formattedTime[16];

	//SendString("JUNK\tLINE: '", false);
	//SendString(response, false);
	//SendString("'\r\n", false);

	responseParts[0] = response;

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
			return err;
		}

		// Set selected entry in PlaylistList
		if (availablePlaylistsLoaded)
		{
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
		//listPtr = FrmGetObjectPtr(frm, PlaylistList);
		//LstSetListChoices(listPtr, NULL, 0);

		/* Free up the memory used by the old list */
		CleanupAvailablePlaylists();

		/* Create a new array to hold the new list of playlists */
		CreateAvailablePlaylistArray(StrAToI(responseParts[1]));
		/*i = StrAToI(responseParts[1]);
		memHandle = MemHandleNew(sizeof(Char *) * i);
		if (memHandle == 0)
		{
			// alert user
			FrmAlert(MemoryAllocationFailedAlert);

			return err;
		}
		memPtr = MemHandleLock(memHandle);
		availablePlaylists = memPtr;*/
	}
	else if (StrCompare(responseParts[0], "AVAIL_PLAYLIST") == 0)
	{
		//StrIToA(playlistCountString, availablePlaylistIndex);
		//SendString("JUNK\tAVP: '", false);
		//SendString(playlistCountString, false);
		//SendString("_", false);
		//SendString(responseParts[1], false);
		//SendString("'\r\n", false);

		/*if (availablePlaylists == NULL)
		{
			return;
		}*/

		/*if (TRIM_PLAYLIST_PATH)
		{
			for (c=responseParts[1], d=c ; *c!='\0' ; c++)
			{
				if (*c == PATH_CHAR)
				{
					d = c + 1;
				}
			}

			responseParts[1] = d;
		}*/

		// +1 for the trailing null character
		/*memHandle = MemHandleNew(StrLen(responseParts[1])+1);
		if (memHandle == 0)
		{
			// alert user
			FrmAlert(MemoryAllocationFailedAlert);

			return err;
		}
		memPtr = MemHandleLock(memHandle);*/
		/*memPtr = AllocateAndLockMemory(StrLen(responseParts[1])+1);
		StrCopy(memPtr, responseParts[1]);
		availablePlaylists[availablePlaylistIndex] = memPtr;
		availablePlaylistIndex++;*/
		AddToAvailablePlaylists(responseParts[1]);
	}
	else if (StrCompare(responseParts[0], "END_AVAIL_PLAYLISTS") == 0)
	{
		#ifdef DEBUG
		StrIToA(playlistCountString, availablePlaylistIndex);
		SendString("JUNK\tAVP count: ", false);
		SendString(playlistCountString, false);
		SendString("\r\n", false);
		#endif

		/*for (i=0 ; i<availablePlaylistIndex ; i++)
		{
			SendString("JUNK\tAVPS: '", false);
			SendString(availablePlaylists[i], false);
			SendString("'\r\n", false);
		}*/

		/*if (availablePlaylists == NULL)
		{
			return;
		}*/

		/*frm = FrmGetFormPtr(MainForm);
		//listPtr = FrmGetObjectPtr(frm, PlaylistList);
		listPtr = FrmGetObjectPtr(frm, FrmGetObjectIndex(frm, PlaylistList));
		if (listPtr != NULL)
		{
			LstSetListChoices(listPtr, availablePlaylists,
				availablePlaylistIndex);
			LstSetHeight(listPtr, availablePlaylistIndex);
			//LstDrawList(listPtr);
		}*/
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

	/*MemHandle memHandle = MemHandleNew(sizeof(Char *) * numberOfPlaylists);
	if (memHandle == 0)
	{
		// alert user
		FrmAlert(MemoryAllocationFailedAlert);
		availablePlaylists = NULL;
	}
	memPtr = MemHandleLock(memHandle);
	availablePlaylists = memPtr;*/
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

	// Free old list
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

// Ensure 'string' in no longer than 'size' by inserting a null character
// at position 'size - 1' in the Char array.  Note that 'string' may have
// a null character earlier in the array, this function does not affect
// that.
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

