/*****************************************************************************
 * $Id$
 *****************************************************************************
 * This interface indicates that the class represents a connection to
 * a client and thus ProtocolHandler can request traffic to and from
 * that client via this interface.
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
 * Revision 1.1  2001/03/20 06:32:09  jheiss
 * Initial revision
 *
 *****************************************************************************
 */

/* Imports */

interface ClientConnection
{
	public String readLine();
	public void println(String line);
}

