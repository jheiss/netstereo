/*****************************************************************************
 * $Id$
 *****************************************************************************
 * Header file for NetStereo Palm client
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
 * Revision 1.1  2001/03/16 21:51:58  jheiss
 * Initial revision
 *
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

