/* packet-s7comm_plus.h
 * Wireshark dissector for S7-Communication plus
 *
 * $Id$
 *
 * Wireshark - Network traffic analyzer
 * By Gerald Combs <gerald@wireshark.org>
 * Copyright 1998 Gerald Combs
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
#ifndef __PACKET_S7COMM_PLUS_H__
#define __PACKET_S7COMM_PLUS_H__

void proto_reg_handoff_s7commp(void);
void proto_register_s7commp(void);

static gboolean dissect_s7commp(tvbuff_t *tvb, packet_info *pinfo, proto_tree *tree, void *data _U_);

#endif
