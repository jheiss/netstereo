/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Header file for NetStereo Palm client
 *****************************************************************************
 * $Log$
 *****************************************************************************
 */

/* Constants */
#define TRUE 1
#define FALSE 0

/* Function protos */
Err StartApplication (void);
void StopApplication (void);
Err OpenSerialPort (void);
Err CloseSerialPort (void);
static void EventLoop (void);
static Boolean ApplicationHandleEvent(EventPtr event);
static Boolean MainFormHandleEvent(EventPtr event);
//static Boolean AboutFormHandleEvent(EventPtr event);
Err SendString (Char *msg, Boolean wait);
Err CheckReceive (void);
Err ParseResponse(char *response);
void CreateAvailablePlaylistArray(UInt16 numberOfPlaylists);
MemPtr AllocateAndLockMemory(UInt16 amountOfMemoryToAllocate);
void CleanupAvailablePlaylists(void);
void AddToAvailablePlaylists(Char *playlistName);
void UpdatePlaylistDisplay(void);
void StringChop(Char *string, UInt16 size);
Char *FormatTime(Char *formattedTime, UInt16 seconds);
static Err RomVersionCompatible (UInt32 requiredVersion, UInt16 launchFlags);

