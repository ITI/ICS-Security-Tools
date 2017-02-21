/* packet-s7comm_plus.c
 *
 * Author:      Thomas Wiens, 2014 <th.wiens@gmx.de>
 * Description: Wireshark dissector for S7 Communication plus
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

#ifdef HAVE_CONFIG_H
# include "config.h"
#endif

#include <stdio.h>
#include <glib.h>
#include <epan/packet.h>
#include <epan/reassemble.h>
#include <epan/conversation.h>
#include <string.h>
#include <time.h>

/* #include <epan/dissectors/packet-wap.h>  Für variable length */
//#define USE_INTERNALS
/* #define DEBUG_REASSEMBLING */

 /*******************************************************
 * It's only possible to use this plugin for dissection of hexdumps (user-link-layer),
 * but only when the dissector isn't registered as heuristic dissector.
 * See how to use this:
 * http://wiki.wireshark.org/HowToDissectAnything
 * https://www.wireshark.org/docs/man-pages/text2pcap.html
 */
//#define DONT_ADD_AS_HEURISTIC_DISSECTOR

#include "packet-s7comm_plus.h"

#define PROTO_TAG_S7COMM_PLUS                   "S7COMM-PLUS"

/* Min. telegram length for heuristic check */
#define S7COMMP_MIN_TELEGRAM_LENGTH             4

/* Protocol identifier */
#define S7COMM_PLUS_PROT_ID                     0x72

/* Max number of array values displays on Item-Value tree. */
#define S7COMMP_ITEMVAL_ARR_MAX_DISPLAY         10

/* Wireshark ID of the S7COMM_PLUS protocol */
static int proto_s7commp = -1;

/* Forward declaration */
static gboolean dissect_s7commp(tvbuff_t *tvb, packet_info *pinfo, proto_tree *tree, void *data _U_);

/**************************************************************************
 * PDU types
 */
#define S7COMMP_PDUTYPE_CONNECT                 0x01
#define S7COMMP_PDUTYPE_DATA                    0x02
#define S7COMMP_PDUTYPE_DATAFW1_5               0x03
#define S7COMMP_PDUTYPE_KEEPALIVE               0xff

static const value_string pdutype_names[] = {
    { S7COMMP_PDUTYPE_CONNECT,                  "Connect" },
    { S7COMMP_PDUTYPE_DATA,                     "Data" },
    { S7COMMP_PDUTYPE_DATAFW1_5,                "DataFW1_5" },
    { S7COMMP_PDUTYPE_KEEPALIVE,                "Keep Alive" },
    { 0,                                        NULL }
};

/**************************************************************************
 * Opcodes in data part
 */
#define S7COMMP_OPCODE_REQ                      0x31
#define S7COMMP_OPCODE_RES                      0x32
#define S7COMMP_OPCODE_NOTIFICATION             0x33
#define S7COMMP_OPCODE_RES2                     0x02    /* V13 HMI bei zyklischen Daten, dann ist in dem Request Typ2=0x74 anstatt 0x34 */

static const value_string opcode_names[] = {
    { S7COMMP_OPCODE_REQ,                       "Request" },
    { S7COMMP_OPCODE_RES,                       "Response" },
    { S7COMMP_OPCODE_NOTIFICATION,              "Notification" },
    { S7COMMP_OPCODE_RES2,                      "Response2" },
    { 0,                                        NULL }
};

/**************************************************************************
 * Function codes in data part.
 */
#define S7COMMP_FUNCTIONCODE_EXPLORE            0x04bb
#define S7COMMP_FUNCTIONCODE_CREATEOBJECT       0x04ca
#define S7COMMP_FUNCTIONCODE_DELETEOBJECT       0x04d4
#define S7COMMP_FUNCTIONCODE_SETVARIABLE        0x04f2
#define S7COMMP_FUNCTIONCODE_GETLINK            0x0524
#define S7COMMP_FUNCTIONCODE_SETMULTIVAR        0x0542
#define S7COMMP_FUNCTIONCODE_GETMULTIVAR        0x054c
#define S7COMMP_FUNCTIONCODE_BEGINSEQUENCE      0x0556
#define S7COMMP_FUNCTIONCODE_ENDSEQUENCE        0x0560
#define S7COMMP_FUNCTIONCODE_INVOKE             0x056b
#define S7COMMP_FUNCTIONCODE_GETVARSUBSTR       0x0586

static const value_string data_functioncode_names[] = {
    { S7COMMP_FUNCTIONCODE_EXPLORE,             "Explore" },
    { S7COMMP_FUNCTIONCODE_CREATEOBJECT,        "CreateObject" },
    { S7COMMP_FUNCTIONCODE_DELETEOBJECT,        "DeleteObject" },
    { S7COMMP_FUNCTIONCODE_SETVARIABLE,         "SetVariable" },
    { S7COMMP_FUNCTIONCODE_GETLINK,             "GetLink" },
    { S7COMMP_FUNCTIONCODE_SETMULTIVAR,         "SetMultiVariables" },
    { S7COMMP_FUNCTIONCODE_GETMULTIVAR,         "GetMultiVariables" },
    { S7COMMP_FUNCTIONCODE_BEGINSEQUENCE,       "BeginSequence" },
    { S7COMMP_FUNCTIONCODE_ENDSEQUENCE,         "EndSequence" },
    { S7COMMP_FUNCTIONCODE_INVOKE,              "Invoke" },
    { S7COMMP_FUNCTIONCODE_GETVARSUBSTR,        "GetVarSubStreamed" },
    { 0,                                        NULL }
};
/**************************************************************************
 * Data types
 */
#define S7COMMP_ITEM_DATATYPE_NULL              0x00
#define S7COMMP_ITEM_DATATYPE_BOOL              0x01        /* BOOL: fix 1 Byte */
#define S7COMMP_ITEM_DATATYPE_USINT             0x02        /* USINT, CHAR: fix 1 Byte */
#define S7COMMP_ITEM_DATATYPE_UINT              0x03        /* UINT, DATE: fix 2 Bytes */
#define S7COMMP_ITEM_DATATYPE_UDINT             0x04        /* UDint: varuint32 */
#define S7COMMP_ITEM_DATATYPE_ULINT             0x05        /* ULInt: varuint64 */
#define S7COMMP_ITEM_DATATYPE_SINT              0x06        /* SINT: fix 1 Bytes */
#define S7COMMP_ITEM_DATATYPE_INT               0x07        /* INT: fix 2 Bytes */
#define S7COMMP_ITEM_DATATYPE_DINT              0x08        /* DINT, TIME: varint32 */
#define S7COMMP_ITEM_DATATYPE_LINT              0x09        /* LInt: varint64 */
#define S7COMMP_ITEM_DATATYPE_BYTE              0x0a        /* BYTE: fix 1 Byte */
#define S7COMMP_ITEM_DATATYPE_WORD              0x0b        /* WORD: fix 2 Bytes */
#define S7COMMP_ITEM_DATATYPE_DWORD             0x0c        /* DWORD: fix 4 Bytes */
#define S7COMMP_ITEM_DATATYPE_LWORD             0x0d        /* LWORD: fix 8 Bytes */
#define S7COMMP_ITEM_DATATYPE_REAL              0x0e        /* REAL: fix 4 Bytes */
#define S7COMMP_ITEM_DATATYPE_LREAL             0x0f        /* LREAL: fix 8 Bytes */
#define S7COMMP_ITEM_DATATYPE_TIMESTAMP         0x10        /* TIMESTAMP: e.g reading CPU from TIA portal, fix 8 Bytes */
#define S7COMMP_ITEM_DATATYPE_TIMESPAN          0x11        /* TIMESPAN: e.g. reading cycle time from TIA portal, varuint64 */
#define S7COMMP_ITEM_DATATYPE_RID               0x12        /* RID: fix 4 Bytes */
#define S7COMMP_ITEM_DATATYPE_AID               0x13        /* AID: varuint32*/
#define S7COMMP_ITEM_DATATYPE_BLOB              0x14
#define S7COMMP_ITEM_DATATYPE_WSTRING           0x15        /* Wide string with length header, UTF8 encoded */
#define S7COMMP_ITEM_DATATYPE_VARIANT           0x16
#define S7COMMP_ITEM_DATATYPE_STRUCT            0x17
/* 0x18 ?? */
#define S7COMMP_ITEM_DATATYPE_S7STRING          0x19        /* S7 String with maximum length of 254 characters, only for tag-description */

static const value_string item_datatype_names[] = {
    { S7COMMP_ITEM_DATATYPE_NULL,               "Null" },
    { S7COMMP_ITEM_DATATYPE_BOOL,               "Bool" },
    { S7COMMP_ITEM_DATATYPE_USINT,              "USInt" },
    { S7COMMP_ITEM_DATATYPE_UINT,               "UInt" },
    { S7COMMP_ITEM_DATATYPE_UDINT,              "UDInt" },
    { S7COMMP_ITEM_DATATYPE_ULINT,              "ULInt" },
    { S7COMMP_ITEM_DATATYPE_SINT,               "SInt" },
    { S7COMMP_ITEM_DATATYPE_INT,                "Int" },
    { S7COMMP_ITEM_DATATYPE_DINT,               "DInt" },
    { S7COMMP_ITEM_DATATYPE_LINT,               "LInt" },
    { S7COMMP_ITEM_DATATYPE_BYTE,               "Byte" },
    { S7COMMP_ITEM_DATATYPE_WORD,               "Word" },
    { S7COMMP_ITEM_DATATYPE_DWORD,              "DWord" },
    { S7COMMP_ITEM_DATATYPE_LWORD,              "LWord" },
    { S7COMMP_ITEM_DATATYPE_REAL,               "Real" },
    { S7COMMP_ITEM_DATATYPE_LREAL,              "LReal" },
    { S7COMMP_ITEM_DATATYPE_TIMESTAMP,          "Timestamp" },
    { S7COMMP_ITEM_DATATYPE_TIMESPAN,           "Timespan" },
    { S7COMMP_ITEM_DATATYPE_RID,                "RID" },
    { S7COMMP_ITEM_DATATYPE_AID,                "AID" },
    { S7COMMP_ITEM_DATATYPE_BLOB,               "Blob" },
    { S7COMMP_ITEM_DATATYPE_WSTRING,            "WString" },
    { S7COMMP_ITEM_DATATYPE_VARIANT,            "Variant" },
    { S7COMMP_ITEM_DATATYPE_STRUCT,             "Struct" },
    { S7COMMP_ITEM_DATATYPE_S7STRING,           "S7String" },
    { 0,                                        NULL }
};

/* Datatype flags */
#define S7COMMP_DATATYPE_FLAG_ARRAY             0x10
#define S7COMMP_DATATYPE_FLAG_ADDRESS_ARRAY     0x20
#define S7COMMP_DATATYPE_FLAG_SPARSEARRAY       0x40

/**************************************************************************
 * Element-IDs
 */
#define S7COMMP_ITEMVAL_ELEMENTID_STARTOBJECT   0xa1
#define S7COMMP_ITEMVAL_ELEMENTID_TERMOBJECT    0xa2
#define S7COMMP_ITEMVAL_ELEMENTID_ATTRIBUTE     0xa3
#define S7COMMP_ITEMVAL_ELEMENTID_RELATION      0xa4
#define S7COMMP_ITEMVAL_ELEMENTID_STARTTAGDESC  0xa7
#define S7COMMP_ITEMVAL_ELEMENTID_TERMTAGDESC   0xa8
#define S7COMMP_ITEMVAL_ELEMENTID_BLOCK0xAB     0xab
#define S7COMMP_ITEMVAL_ELEMENTID_BLOCK0xAC     0xac

static const value_string itemval_elementid_names[] = {
    { S7COMMP_ITEMVAL_ELEMENTID_STARTOBJECT,    "Start of Object" },
    { S7COMMP_ITEMVAL_ELEMENTID_TERMOBJECT,     "Terminating Object" },
    { S7COMMP_ITEMVAL_ELEMENTID_ATTRIBUTE,      "Attribute" },
    { S7COMMP_ITEMVAL_ELEMENTID_RELATION,       "Relation" },
    { S7COMMP_ITEMVAL_ELEMENTID_STARTTAGDESC,   "Start of Tag-Description" },
    { S7COMMP_ITEMVAL_ELEMENTID_TERMTAGDESC,    "Terminating Tag-Description" },
    { S7COMMP_ITEMVAL_ELEMENTID_BLOCK0xAB,      "Block0xAB" },
    { S7COMMP_ITEMVAL_ELEMENTID_BLOCK0xAC,      "Block0xAC" },
    { 0,                                        NULL }
};

/**************************************************************************
 * There are IDs which values can be read or be written to.
 * This is some kind of operating system data/function for the plc.
 * The IDs seem to be unique for all telegrams in which they occur.
 * Add the datatype for this value in parentheses.
 */
 #ifdef USE_INTERNALS
    #include "internals/packet-s7comm_plus-aid-names.h"
#else
static const value_string id_number_names[] = {
    { 233,                                      "Subscription name (String)" },
    { 537,                                      "Object OMS Type-Info-Container" },
    { 1048,                                     "Cyclic variables update set of addresses (UDInt, Addressarray)" },
    { 1049,                                     "Cyclic variables update rate (UDInt, in milliseconds)" },
    { 1051,                                     "Unsubscribe" },
    { 1053,                                     "Cyclic variables number of automatic sent telegrams, -1 means unlimited (Int)" },

    { 1256,                                     "Object Qualifier" },
    { 1257,                                     "Parent RID" },
    { 1258,                                     "Composition AID" },
    { 1259,                                     "Key Qualifier" },

    { 2421,                                     "Set CPU clock" },
    { 2449,                                     "Ident ES" },
    { 2450,                                     "Designators" },
    { 2451,                                     "Working Memory Size" },
    { 2453,                                     "Last modified" },
    { 2454,                                     "Load Memory Size" },

    { 2521,                                     "Block Number" },
    { 2522,                                     "Auto Numbering" },
    { 2523,                                     "Block Language" },
    { 2524,                                     "Knowhow Protected" },
    { 2527,                                     "Unlinked" },
    { 2529,                                     "Runtime Modified" },
    { 2532,                                     "CRC" },
    { 2533,                                     "Body Description" },
    { 2537,                                     "Optimize Info" },

    { 2543,                                     "Interface Modified" },
    { 2544,                                     "Interface Description" },
    { 2545,                                     "Compiler Swiches" },
    { 2546,                                     "Line Comments" },
    { 2580,                                     "Code block" },
    { 2581,                                     "Parameter modified" },
    { 2582,                                     "External Ref Data" },
    { 2583,                                     "Internal Ref Data" },
    { 2584,                                     "Network Comment" },
    { 2585,                                     "Network Title" },
    { 2586,                                     "Callee List" },
    { 2587,                                     "Interface Signature" },
    { 2588,                                     "Display Info" },
    { 2589,                                     "Debug Info" },
    { 2590,                                     "Local Error Handling" },
    { 2591,                                     "Long Constants" },
    { 2607,                                     "Start Info Type" },

    { 3151,                                     "Binding" },
    { 3448,                                     "Knowhow Protection Mode" },
    { 3449,                                     "Knowhow Protection Password" },
    { 3619,                                     "TO Block Set Number" },
    { 3634,                                     "Change Counter Copy" },

    { 4287,                                     "Title" },
    { 4288,                                     "Comment" },
    { 4294,                                     "Instance DB" },
    { 4560,                                     "PIP" },
    { 4578,                                     "Type Info" },
    { 4615,                                     "Latest Runtime" },
    { 4616,                                     "Min Runtime" },
    { 4617,                                     "Max Runtime" },
    { 4618,                                     "Call Frequency" },
    { 4619,                                     "Runtime Ratio" },

    { 0,                                        NULL }
};
#endif
static value_string_ext id_number_names_ext = VALUE_STRING_EXT_INIT(id_number_names);

/* Error codes */
#ifdef USE_INTERNALS
    #include "internals/packet-s7comm_plus-errorcodes.h"
#else
static const value_string errorcode_names[] = {
    { 0,                                        "OK" },
    { 17,                                       "Message Session Pre-Legitimated" },
    { 19,                                       "Warning Service Executed With Partial Error" },
    { 22,                                       "Service Session Delegitimated" },
    { -12,                                      "Object not found" },
    { -17,                                      "Invalid CRC" },
    { -134,                                     "Service Multi-ES Not Supported" },
    { -255,                                     "Invalid LID" },
    { 0,                                        NULL }
};
#endif

/* Item access area */
#define S7COMMP_VAR_ITEM_AREA1_DB               0x8a0e              /* Reading DB, 2 byte DB-Number following */
#define S7COMMP_VAR_ITEM_AREA1_IQMCT            0x0000              /* Reading I/Q/M/C/T, 2 Byte detail area following */

static const value_string var_item_area1_names[] = {
    { S7COMMP_VAR_ITEM_AREA1_DB,                "DB" },
    { S7COMMP_VAR_ITEM_AREA1_IQMCT,             "IQMCT" },
    { 0,                                        NULL }
};

#define S7COMMP_VAR_ITEM_AREA2_DB               0x8a0e
#define S7COMMP_VAR_ITEM_AREA2_I                0x50
#define S7COMMP_VAR_ITEM_AREA2_Q                0x51
#define S7COMMP_VAR_ITEM_AREA2_M                0x52
#define S7COMMP_VAR_ITEM_AREA2_C                0x53
#define S7COMMP_VAR_ITEM_AREA2_T                0x54

static const value_string var_item_area2_names[] = {
    { S7COMMP_VAR_ITEM_AREA2_I,                 "Inputs (I)" },
    { S7COMMP_VAR_ITEM_AREA2_Q,                 "Outputs (Q)" },
    { S7COMMP_VAR_ITEM_AREA2_M,                 "Flags (M)" },
    { S7COMMP_VAR_ITEM_AREA2_C,                 "Counter (C)" },
    { S7COMMP_VAR_ITEM_AREA2_T,                 "Timer (T)" },
    { S7COMMP_VAR_ITEM_AREA2_DB,                "Datablock (DB)" },
    { 0,                                        NULL }
};

static const value_string var_item_area2_names_short[] = {
    { S7COMMP_VAR_ITEM_AREA2_I,                 "I" },
    { S7COMMP_VAR_ITEM_AREA2_Q,                 "Q" },
    { S7COMMP_VAR_ITEM_AREA2_M,                 "M" },
    { S7COMMP_VAR_ITEM_AREA2_C,                 "C" },
    { S7COMMP_VAR_ITEM_AREA2_T,                 "T" },
    { S7COMMP_VAR_ITEM_AREA2_DB,                "DB" },
    { 0,                                        NULL }
};

#define S7COMMP_VAR_ITEM_BASE_AREA_IQMCT        0x0e98
#define S7COMMP_VAR_ITEM_BASE_AREA_DB           0x09f6
static const value_string var_item_base_area_names[] = {
    { S7COMMP_VAR_ITEM_BASE_AREA_IQMCT,         "IQMCT" },
    { S7COMMP_VAR_ITEM_BASE_AREA_DB,            "DB" },
    { 0,                                        NULL }
};

/* Explore areas */
#define S7COMMP_EXPLORE_CLASS_IQMCT             0x90
#define S7COMMP_EXPLORE_CLASS_UDT               0x91
#define S7COMMP_EXPLORE_CLASS_DB                0x92
#define S7COMMP_EXPLORE_CLASS_FB                0x93
#define S7COMMP_EXPLORE_CLASS_FC                0x94
#define S7COMMP_EXPLORE_CLASS_OB                0x95
#define S7COMMP_EXPLORE_CLASS_FBT               0x96
#define S7COMMP_EXPLORE_CLASS_LIB               0x02
static const value_string explore_class_names[] = {
    { S7COMMP_EXPLORE_CLASS_IQMCT,              "IQMCT" },
    { S7COMMP_EXPLORE_CLASS_UDT,                "UDT" },
    { S7COMMP_EXPLORE_CLASS_DB,                 "DB" },
    { S7COMMP_EXPLORE_CLASS_FB,                 "FB" },
    { S7COMMP_EXPLORE_CLASS_FC,                 "FC" },
    { S7COMMP_EXPLORE_CLASS_OB,                 "OB" },
    { S7COMMP_EXPLORE_CLASS_FBT,                "FBT" },
    { S7COMMP_EXPLORE_CLASS_LIB,                "LIB" },
    { 0,                                        NULL }
};
#define S7COMMP_EXPLORE_CLASS_IQMCT_INPUT       0x01
#define S7COMMP_EXPLORE_CLASS_IQMCT_OUTPUT      0x02
#define S7COMMP_EXPLORE_CLASS_IQMCT_BITMEM      0x03
#define S7COMMP_EXPLORE_CLASS_IQMCT_04          0x04
#define S7COMMP_EXPLORE_CLASS_IQMCT_TIMER       0x05
#define S7COMMP_EXPLORE_CLASS_IQMCT_COUNTER     0x06
static const value_string explore_class_iqmct_names[] = {
    { S7COMMP_EXPLORE_CLASS_IQMCT_INPUT,        "Input area" },
    { S7COMMP_EXPLORE_CLASS_IQMCT_OUTPUT,       "Output area" },
    { S7COMMP_EXPLORE_CLASS_IQMCT_BITMEM,       "M Bit memory" },
    { S7COMMP_EXPLORE_CLASS_IQMCT_04,           "Unknown area 04" },
    { S7COMMP_EXPLORE_CLASS_IQMCT_TIMER,        "S7-Timer" },
    { S7COMMP_EXPLORE_CLASS_IQMCT_COUNTER,      "S7-Counter" },
    { 0,                                        NULL }
};

#define S7COMMP_EXPLORE_CLASS_LIB_STYPE         0x00
#define S7COMMP_EXPLORE_CLASS_LIB_STYPEARR      0x01
#define S7COMMP_EXPLORE_CLASS_LIB_SFC           0x02
#define S7COMMP_EXPLORE_CLASS_LIB_SFB           0x03
#define S7COMMP_EXPLORE_CLASS_LIB_FBT           0x04
#define S7COMMP_EXPLORE_CLASS_LIB_FB            0x05
#define S7COMMP_EXPLORE_CLASS_LIB_FC            0x06
#define S7COMMP_EXPLORE_CLASS_LIB_FCT           0x07
#define S7COMMP_EXPLORE_CLASS_LIB_UDT           0x08
#define S7COMMP_EXPLORE_CLASS_LIB_STRUCT        0x09
static const value_string explore_class_lib_names[] = {
    { S7COMMP_EXPLORE_CLASS_LIB_STYPE,          "SimpleType" },
    { S7COMMP_EXPLORE_CLASS_LIB_STYPEARR,       "SimpleTypeArray" },
    { S7COMMP_EXPLORE_CLASS_LIB_SFC,            "SFC" },
    { S7COMMP_EXPLORE_CLASS_LIB_SFB,            "SFB" },
    { S7COMMP_EXPLORE_CLASS_LIB_FBT,            "FBT" },
    { S7COMMP_EXPLORE_CLASS_LIB_FB,             "FB" },
    { S7COMMP_EXPLORE_CLASS_LIB_FC,             "FC" },
    { S7COMMP_EXPLORE_CLASS_LIB_FCT,            "FCT" },
    { S7COMMP_EXPLORE_CLASS_LIB_UDT,            "UDT" },
    { S7COMMP_EXPLORE_CLASS_LIB_STRUCT,         "STRUCT" },
    { 0,                                        NULL }
};


static const char mon_names[][4] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

/* Attribute flags in tag description */
#define S7COMMP_TAGDESCR_ATTRIBUTE_HOSTRELEVANT       0x8000000
#define S7COMMP_TAGDESCR_ATTRIBUTE_PLAINMEMBERRETAIN  0x2000000
#define S7COMMP_TAGDESCR_ATTRIBUTE_PLAINMEMBERCLASSIC 0x1000000
#define S7COMMP_TAGDESCR_ATTRIBUTE_HMIVISIBLE         0x0800000
#define S7COMMP_TAGDESCR_ATTRIBUTE_HMIREADONLY        0x0400000
#define S7COMMP_TAGDESCR_ATTRIBUTE_HMICACHED          0x0200000
#define S7COMMP_TAGDESCR_ATTRIBUTE_HMIACCESSIBLE      0x0100000
#define S7COMMP_TAGDESCR_ATTRIBUTE_ISQUALIFIER        0x0040000
#define S7COMMP_TAGDESCR_ATTRIBUTE_NORMALACCESS       0x0008000
#define S7COMMP_TAGDESCR_ATTRIBUTE_NEEDSLEGITIMIZATION 0x0004000
#define S7COMMP_TAGDESCR_ATTRIBUTE_CHANGEBLEINRUN     0x0002000
#define S7COMMP_TAGDESCR_ATTRIBUTE_SERVERONLY         0x0000800
#define S7COMMP_TAGDESCR_ATTRIBUTE_CLIENTREADRONLY    0x0000400
#define S7COMMP_TAGDESCR_ATTRIBUTE_SEPLOADMEMFA       0x0000200
#define S7COMMP_TAGDESCR_ATTRIBUTE_ASEVALREQ          0x0000100
#define S7COMMP_TAGDESCR_ATTRIBUTE_BL                 0x0000040
#define S7COMMP_TAGDESCR_ATTRIBUTE_PERSISTENT         0x0000020
#define S7COMMP_TAGDESCR_ATTRIBUTE_CORE               0x0000010
#define S7COMMP_TAGDESCR_ATTRIBUTE_ISOUT              0x0000008
#define S7COMMP_TAGDESCR_ATTRIBUTE_ISIN               0x0000004
#define S7COMMP_TAGDESCR_ATTRIBUTE_APPWRITEABLE       0x0000002
#define S7COMMP_TAGDESCR_ATTRIBUTE_APPREADABLE        0x0000001

/* Offsetinfo type for tag description */
#define S7COMMP_TAGDESCR_OFFSETINFOTYPE_LIBELEMENT      0x00
#define S7COMMP_TAGDESCR_OFFSETINFOTYPE_BOOLINUDT       0x01
#define S7COMMP_TAGDESCR_OFFSETINFOTYPE_ARRAYINSTRUCT   0x02
#define S7COMMP_TAGDESCR_OFFSETINFOTYPE_PLAINSTATIC     0x04
#define S7COMMP_TAGDESCR_OFFSETINFOTYPE_BOOL            0x05
#define S7COMMP_TAGDESCR_OFFSETINFOTYPE_ARRAY           0x06
#define S7COMMP_TAGDESCR_OFFSETINFOTYPE_MULTIDIMARRAY   0x07
static const value_string tagdescr_offsetinfotype_names[] = {
    { S7COMMP_TAGDESCR_OFFSETINFOTYPE_LIBELEMENT,       "LibraryElement" },
    { S7COMMP_TAGDESCR_OFFSETINFOTYPE_BOOLINUDT,        "BoolInUdt" },
    { S7COMMP_TAGDESCR_OFFSETINFOTYPE_ARRAYINSTRUCT,    "ArrayInStruct" },
    { S7COMMP_TAGDESCR_OFFSETINFOTYPE_PLAINSTATIC,      "Plain/Static" },
    { S7COMMP_TAGDESCR_OFFSETINFOTYPE_BOOL,             "Bool" },
    { S7COMMP_TAGDESCR_OFFSETINFOTYPE_ARRAY,            "Array" },
    { S7COMMP_TAGDESCR_OFFSETINFOTYPE_MULTIDIMARRAY,    "Multidimensional Array" },
    { 0,                                                NULL }
};

static const value_string tagdescr_section_names[] = {
    { 0x00,                                     "Undefined" },
    { 0x01,                                     "Input" },
    { 0x02,                                     "Output" },
    { 0x03,                                     "InOut" },
    { 0x04,                                     "Static" },
    { 0x05,                                     "Dynamic" },
    { 0x06,                                     "Retval" },
    { 0x07,                                     "Operand" },
    { 0,                                        NULL }
};

static const value_string tagdescr_softdatatype_names[] = {
    { 0,        "Void" },
    { 1,        "Bool" },
    { 2,        "Byte" },
    { 3,        "Char" },
    { 4,        "Word" },
    { 5,        "Int" },
    { 6,        "DWord" },
    { 7,        "DInt" },
    { 8,        "Real" },
    { 9,        "Date" },
    { 10,       "Time_Of_Day" },
    { 11,       "Time" },
    { 12,       "S5Time" },
    { 13,       "S5Count" },
    { 14,       "Date_And_Time" },
    { 15,       "Internet_Time" },
    { 16,       "Array" },
    { 17,       "Struct" },
    { 18,       "Endstruct" },
    { 19,       "String" },
    { 20,       "Pointer" },
    { 21,       "Multi_FB" },
    { 22,       "Any" },
    { 23,       "Block_FB" },
    { 24,       "Block_FC" },
    { 25,       "Block_DB" },
    { 26,       "Block_SDB" },
    { 27,       "Multi_SFB" },
    { 28,       "Counter" },
    { 29,       "Timer" },
    { 30,       "IEC_Counter" },
    { 31,       "IEC_Timer" },
    { 32,       "Block_SFB" },
    { 33,       "Block_SFC" },
    { 34,       "Block_CB" },
    { 35,       "Block_SCB" },
    { 36,       "Block_OB" },
    { 37,       "Block_UDT" },
    { 38,       "Offset" },
    { 39,       "Block_SDT" },
    { 40,       "BBOOL" },
    { 41,       "BLOCK_EXT" },
    { 48,       "LReal" },
    { 49,       "ULInt" },
    { 50,       "LInt" },
    { 51,       "LWord" },
    { 52,       "USInt" },
    { 53,       "UInt" },
    { 54,       "UDInt" },
    { 55,       "SInt" },
    { 56,       "Bcd8" },
    { 57,       "Bcd16" },
    { 58,       "Bcd32" },
    { 59,       "Bcd64" },
    { 60,       "ARef" },
    { 61,       "WChar" },
    { 62,       "WString" },
    { 63,       "Variant" },
    { 64,       "LTime" },
    { 65,       "LTOD" },
    { 66,       "LDT" },
    { 67,       "DTL" },
    { 68,       "IEC_LTimer" },
    { 69,       "SCounter" },
    { 70,       "DCounter" },
    { 71,       "LCounter" },
    { 72,       "UCounter" },
    { 73,       "USCounter" },
    { 74,       "UDCounter" },
    { 75,       "ULCounter" },
    { 96,       "REMOTE" },
    { 97,       "Error_Struct" },
    { 98,       "NREF" },
    { 99,       "VREF" },
    { 100,      "FBTREF" },
    { 101,      "CREF" },
    { 102,      "VAREF" },
    { 128,      "AOM_IDENT" },
    { 129,      "EVENT_ANY" },
    { 130,      "EVENT_ATT" },
    { 131,      "EVENT_HWINT" },
    { 132,      "FOLDER" },
    { 133,      "AOM_AID" },
    { 134,      "AOM_LINK" },
    { 144,      "HW_ANY" },
    { 145,      "HW_IOSYSTEM" },
    { 146,      "HW_DPMASTER" },
    { 147,      "HW_DEVICE" },
    { 148,      "HW_DPSLAVE" },
    { 149,      "HW_IO" },
    { 150,      "HW_MODULE" },
    { 151,      "HW_SUBMODULE" },
    { 152,      "HW_HSC" },
    { 153,      "HW_PWM" },
    { 154,      "HW_PTO" },
    { 155,      "HW_INTERFACE" },
    { 160,      "OB_ANY" },
    { 161,      "OB_DELAY" },
    { 162,      "OB_TOD" },
    { 163,      "OB_CYCLIC" },
    { 164,      "OB_ATT" },
    { 168,      "CONN_ANY" },
    { 169,      "CONN_PRG" },
    { 170,      "CONN_OUC" },
    { 172,      "HW_NR" },
    { 173,      "PORT" },
    { 174,      "RTM" },
    { 176,      "C_ALARM" },
    { 177,      "C_ALARM_S" },
    { 178,      "C_ALARM_8" },
    { 179,      "C_ALARM_8P" },
    { 180,      "C_ALARM_T" },
    { 181,      "C_AR_SEND" },
    { 182,      "C_NOTIFY" },
    { 183,      "C_NOTIFY_8P" },
    { 192,      "OB_PCYCLE" },
    { 193,      "OB_HWINT" },
    { 194,      "OB_COMM" },
    { 195,      "OB_DIAG" },
    { 196,      "OB_TIMEERROR" },
    { 197,      "OB_STARTUP" },
    { 253,      "Para" },
    { 254,      "Label" },
    { 255,      "Undefined" },
    { 256,      "NotChosen" },
    { 0,         NULL }
};
static value_string_ext tagdescr_softdatatype_names_ext = VALUE_STRING_EXT_INIT(tagdescr_softdatatype_names);

static const value_string tagdescr_accessability_names[] = {
    { 0,                                        "Public" },
    { 1,                                        "ReadOnly" },
    { 2,                                        "Internal" },
    { 3,                                        "InternalReadOnly" },
    { 4,                                        "Protected" },
    { 5,                                        "ProtectedReadOnly" },
    { 6,                                        "Constant" },
    { 7,                                        "ConstantReadOnly" },
    { 0,                                        NULL }
};

/**************************************************************************
 **************************************************************************/
/* Header Block */
static gint hf_s7commp_header = -1;
static gint hf_s7commp_header_protid = -1;              /* Header Byte  0 */
static gint hf_s7commp_header_pdutype = -1;             /* Header Bytes 1 */
static gint hf_s7commp_header_datlg = -1;               /* Header Bytes 2, 3*/
static gint hf_s7commp_header_keepaliveseqnum = -1;     /* Sequence number in keep alive telegrams */

static gint hf_s7commp_data = -1;
static gint hf_s7commp_data_item_address = -1;
static gint hf_s7commp_data_item_value = -1;
static gint hf_s7commp_data_data = -1;
static gint hf_s7commp_data_opcode = -1;
static gint hf_s7commp_data_reserved1 = -1;
static gint hf_s7commp_data_reserved2 = -1;
static gint hf_s7commp_data_unknown1 = -1;
static gint hf_s7commp_data_function = -1;
static gint hf_s7commp_data_sessionid = -1;
static gint hf_s7commp_data_seqnum = -1;
static gint hf_s7commp_objectqualifier = -1;

static gint hf_s7commp_valuelist = -1;
static gint hf_s7commp_errorvaluelist = -1;
static gint hf_s7commp_addresslist = -1;
static gint ett_s7commp_valuelist = -1;
static gint ett_s7commp_errorvaluelist = -1;
static gint ett_s7commp_addresslist = -1;

static gint hf_s7commp_trailer = -1;
static gint hf_s7commp_trailer_protid = -1;
static gint hf_s7commp_trailer_pdutype = -1;
static gint hf_s7commp_trailer_datlg = -1;

/* Read Response */
static gint hf_s7commp_data_req_set = -1;
static gint hf_s7commp_data_res_set = -1;

static gint hf_s7commp_data_id_number = -1;

static gint hf_s7commp_notification_set = -1;

/* Fields for object traversion */
static gint hf_s7commp_element_object = -1;
static gint hf_s7commp_element_attribute = -1;
static gint hf_s7commp_element_relation = -1;
static gint hf_s7commp_element_tagdescription = -1;
static gint hf_s7commp_element_block = -1;
static gint ett_s7commp_element_object = -1;
static gint ett_s7commp_element_attribute = -1;
static gint ett_s7commp_element_relation = -1;
static gint ett_s7commp_element_tagdescription = -1;
static gint ett_s7commp_element_block = -1;

/* Error value and subfields */
static gint hf_s7commp_data_returnvalue = -1;
static gint hf_s7commp_data_errorcode = -1;
static gint hf_s7commp_data_omsline = -1;
static gint hf_s7commp_data_errorsource = -1;
static gint hf_s7commp_data_genericerrorcode = -1;
static gint hf_s7commp_data_servererror = -1;
static gint hf_s7commp_data_debuginfo = -1;
static gint hf_s7commp_data_errorextension = -1;
static const int *s7commp_data_returnvalue_fields[] = {
    &hf_s7commp_data_errorcode,
    &hf_s7commp_data_omsline,
    &hf_s7commp_data_errorsource,
    &hf_s7commp_data_genericerrorcode,
    &hf_s7commp_data_servererror,
    &hf_s7commp_data_debuginfo,
    NULL
};

/* These are the ids of the subtrees that we are creating */
static gint ett_s7commp = -1;                           /* S7 communication tree, parent of all other subtree */
static gint ett_s7commp_header = -1;                    /* Subtree for header block */
static gint ett_s7commp_data = -1;                      /* Subtree for data block */
static gint ett_s7commp_data_returnvalue = -1;          /* Subtree for returnvalue */
static gint ett_s7commp_data_item = -1;                 /* Subtree for an item in data block */
static gint ett_s7commp_trailer = -1;                   /* Subtree for trailer block */

static gint ett_s7commp_data_req_set = -1;              /* Subtree for data request set*/
static gint ett_s7commp_data_res_set = -1;              /* Subtree for data response set*/
static gint ett_s7commp_notification_set = -1;          /* Subtree for notification data set */

static gint ett_s7commp_itemaddr_area = -1;             /* Subtree for item address area */
static gint ett_s7commp_itemval_array = -1;             /* Subtree if item value is an array */
static gint ett_s7commp_objectqualifier = -1;           /* Subtree for object qualifier data */
static gint ett_s7commp_integrity = -1;                 /* Subtree for integrity block */

/* Item Address */
static gint hf_s7commp_item_count = -1;
static gint hf_s7commp_item_no_of_fields = -1;
static gint hf_s7commp_itemaddr_crc = -1;
static gint hf_s7commp_itemaddr_area = -1;
static gint hf_s7commp_itemaddr_area1 = -1;
static gint hf_s7commp_itemaddr_area2 = -1;
static gint hf_s7commp_itemaddr_dbnumber = -1;
static gint hf_s7commp_itemaddr_lid_nesting_depth = -1;
static gint hf_s7commp_itemaddr_base_area = -1;
static gint hf_s7commp_itemaddr_lid_value = -1;

/* Item Value */
static gint hf_s7commp_itemval_itemnumber = -1;
static gint hf_s7commp_itemval_elementid = -1;
static gint hf_s7commp_itemval_datatype_flags = -1;
static gint hf_s7commp_itemval_datatype_flags_array = -1;               /* 0x10 for array */
static gint hf_s7commp_itemval_datatype_flags_address_array = -1;       /* 0x20 for address-array */
static gint hf_s7commp_itemval_datatype_flags_sparsearray = -1;         /* 0x40 for nullterminated array with key/value */
static gint hf_s7commp_itemval_datatype_flags_0x80unkn = -1;            /* 0x80 unknown, seen in S7-1500 */
static gint ett_s7commp_itemval_datatype_flags = -1;
static const int *s7commp_itemval_datatype_flags_fields[] = {
    &hf_s7commp_itemval_datatype_flags_array,
    &hf_s7commp_itemval_datatype_flags_address_array,
    &hf_s7commp_itemval_datatype_flags_sparsearray,
    &hf_s7commp_itemval_datatype_flags_0x80unkn,
    NULL
};
static gint hf_s7commp_itemval_datatype = -1;
static gint hf_s7commp_itemval_arraysize = -1;
static gint hf_s7commp_itemval_value = -1;

static gint ett_s7commp_explore_req_area = -1;
static gint hf_s7commp_explore_req_area = -1;
static gint hf_s7commp_explore_req_area_class = -1;
static gint hf_s7commp_explore_req_area_class_iqmct = -1;
static gint hf_s7commp_explore_req_area_class_lib = -1;
static gint hf_s7commp_explore_req_area_structindex = -1;
static gint hf_s7commp_explore_req_area_section = -1;

/* Explore result, variable (tag) description */
static gint hf_s7commp_tagdescr_offsetinfo = -1;
static gint ett_s7commp_tagdescr_offsetinfo = -1;
static gint hf_s7commp_tagdescr_offsetinfotype = -1;
static gint hf_s7commp_tagdescr_namelength = -1;
static gint hf_s7commp_tagdescr_name = -1;
static gint hf_s7commp_tagdescr_unknown2 = -1;
static gint hf_s7commp_tagdescr_datatype = -1;
static gint hf_s7commp_tagdescr_softdatatype = -1;
static gint hf_s7commp_tagdescr_accessability = -1;
static gint hf_s7commp_tagdescr_section = -1;

static gint hf_s7commp_tagdescr_attributeflags = -1;
static gint hf_s7commp_tagdescr_attributeflags_hostrelevant = -1;
static gint hf_s7commp_tagdescr_attributeflags_retain = -1;
static gint hf_s7commp_tagdescr_attributeflags_classic = -1;
static gint hf_s7commp_tagdescr_attributeflags_hmivisible = -1;
static gint hf_s7commp_tagdescr_attributeflags_hmireadonly = -1;
static gint hf_s7commp_tagdescr_attributeflags_hmicached = -1;
static gint hf_s7commp_tagdescr_attributeflags_hmiaccessible = -1;
static gint hf_s7commp_tagdescr_attributeflags_isqualifier = -1;
static gint hf_s7commp_tagdescr_attributeflags_normalaccess = -1;
static gint hf_s7commp_tagdescr_attributeflags_needslegitimization = -1;
static gint hf_s7commp_tagdescr_attributeflags_changeableinrun = -1;
static gint hf_s7commp_tagdescr_attributeflags_serveronly = -1;
static gint hf_s7commp_tagdescr_attributeflags_clientreadonly = -1;
static gint hf_s7commp_tagdescr_attributeflags_seploadmemfa = -1;
static gint hf_s7commp_tagdescr_attributeflags_asevaluationrequired = -1;
static gint hf_s7commp_tagdescr_attributeflags_bl = -1;
static gint hf_s7commp_tagdescr_attributeflags_persistent = -1;
static gint hf_s7commp_tagdescr_attributeflags_core = -1;
static gint hf_s7commp_tagdescr_attributeflags_isout = -1;
static gint hf_s7commp_tagdescr_attributeflags_isin = -1;
static gint hf_s7commp_tagdescr_attributeflags_appwriteable = -1;
static gint hf_s7commp_tagdescr_attributeflags_appreadable = -1;
static gint ett_s7commp_tagdescr_attributeflags = -1;
static const int *s7commp_tagdescr_attributeflags_fields[] = {
    &hf_s7commp_tagdescr_attributeflags_hostrelevant,
    &hf_s7commp_tagdescr_attributeflags_retain,
    &hf_s7commp_tagdescr_attributeflags_classic,
    &hf_s7commp_tagdescr_attributeflags_hmivisible,
    &hf_s7commp_tagdescr_attributeflags_hmireadonly,
    &hf_s7commp_tagdescr_attributeflags_hmicached,
    &hf_s7commp_tagdescr_attributeflags_hmiaccessible,
    &hf_s7commp_tagdescr_attributeflags_isqualifier,
    &hf_s7commp_tagdescr_attributeflags_normalaccess,
    &hf_s7commp_tagdescr_attributeflags_needslegitimization,
    &hf_s7commp_tagdescr_attributeflags_changeableinrun,
    &hf_s7commp_tagdescr_attributeflags_serveronly,
    &hf_s7commp_tagdescr_attributeflags_clientreadonly,
    &hf_s7commp_tagdescr_attributeflags_seploadmemfa,
    &hf_s7commp_tagdescr_attributeflags_asevaluationrequired,
    &hf_s7commp_tagdescr_attributeflags_bl,
    &hf_s7commp_tagdescr_attributeflags_persistent,
    &hf_s7commp_tagdescr_attributeflags_core,
    &hf_s7commp_tagdescr_attributeflags_isout,
    &hf_s7commp_tagdescr_attributeflags_isin,
    &hf_s7commp_tagdescr_attributeflags_appwriteable,
    &hf_s7commp_tagdescr_attributeflags_appreadable,
    NULL
};

static gint hf_s7commp_tagdescr_unknown4 = -1;
static gint hf_s7commp_tagdescr_unknown5 = -1;
static gint hf_s7commp_tagdescr_lid = -1;

/* Integrity part, for 1500 */
static gint hf_s7commp_integrity = -1;
static gint hf_s7commp_integrity_id = -1;
static gint hf_s7commp_integrity_digestlen = -1;
static gint hf_s7commp_integrity_digest = -1;

/* These fields used when reassembling S7COMMP fragments */
static gint hf_s7commp_fragments = -1;
static gint hf_s7commp_fragment = -1;
static gint hf_s7commp_fragment_overlap = -1;
static gint hf_s7commp_fragment_overlap_conflict = -1;
static gint hf_s7commp_fragment_multiple_tails = -1;
static gint hf_s7commp_fragment_too_long_fragment = -1;
static gint hf_s7commp_fragment_error = -1;
static gint hf_s7commp_fragment_count = -1;
static gint hf_s7commp_reassembled_in = -1;
static gint hf_s7commp_reassembled_length = -1;
static gint ett_s7commp_fragment = -1;
static gint ett_s7commp_fragments = -1;

static const fragment_items s7commp_frag_items = {
    /* Fragment subtrees */
    &ett_s7commp_fragment,
    &ett_s7commp_fragments,
    /* Fragment fields */
    &hf_s7commp_fragments,
    &hf_s7commp_fragment,
    &hf_s7commp_fragment_overlap,
    &hf_s7commp_fragment_overlap_conflict,
    &hf_s7commp_fragment_multiple_tails,
    &hf_s7commp_fragment_too_long_fragment,
    &hf_s7commp_fragment_error,
    &hf_s7commp_fragment_count,
    /* Reassembled in field */
    &hf_s7commp_reassembled_in,
    /* Reassembled length field */
    &hf_s7commp_reassembled_length,
    /* Reassembled data field */
    NULL,
    /* Tag */
    "S7COMM-PLUS fragments"
};

typedef struct {
    gboolean first_fragment;
    gboolean inner_fragment;
    gboolean last_fragment;
    guint32 start_frame;
} frame_state_t;

#define CONV_STATE_NEW         -1
#define CONV_STATE_NOFRAG      0
#define CONV_STATE_FIRST       1
#define CONV_STATE_INNER       2
#define CONV_STATE_LAST        3
typedef struct {
    int state;
    guint32 start_frame;
} conv_state_t;

/*
 * reassembly of S7COMMP
 */
static reassembly_table s7commp_reassembly_table;

static void
s7commp_defragment_init(void)
{
    reassembly_table_init(&s7commp_reassembly_table,
                          &addresses_reassembly_table_functions);
}


/* Register this protocol */
void
proto_reg_handoff_s7commp(void)
{
    static gboolean initialized = FALSE;
    if (!initialized) {
        #ifdef DONT_ADD_AS_HEURISTIC_DISSECTOR
            new_register_dissector("dlt", dissect_s7commp, proto_s7commp);
        #else
            heur_dissector_add("cotp", dissect_s7commp, proto_s7commp);
        #endif
        initialized = TRUE;
    }
}

void
proto_register_s7commp (void)
{
    static hf_register_info hf[] = {
        /*** Header fields ***/
        { &hf_s7commp_header,
          { "Header", "s7comm-plus.header", FT_NONE, BASE_NONE, NULL, 0x0,
            "This is the header of S7 communication plus", HFILL }},
        { &hf_s7commp_header_protid,
          { "Protocol Id", "s7comm-plus.header.protid", FT_UINT8, BASE_HEX, NULL, 0x0,
            "Protocol Identification", HFILL }},
        { &hf_s7commp_header_pdutype,
          { "PDU-Type", "s7comm-plus.header.pdutype", FT_UINT8, BASE_HEX, VALS(pdutype_names), 0x0,
            "Type of packet", HFILL }},
        { &hf_s7commp_header_datlg,
          { "Data length", "s7comm-plus.header.datlg", FT_UINT16, BASE_DEC, NULL, 0x0,
            "Specifies the entire length of the data block in bytes", HFILL }},
        { &hf_s7commp_header_keepaliveseqnum,
          { "Keep alive sequence number", "s7comm-plus.header.keepalive_seqnum", FT_UINT16, BASE_DEC, NULL, 0x0,
            "Sequence number in keep alive telegrams", HFILL }},

        /*** Fields in data part ***/
        { &hf_s7commp_data,
          { "Data", "s7comm-plus.data", FT_NONE, BASE_NONE, NULL, 0x0,
            "This is the data part of S7 communication plus", HFILL }},

        { &hf_s7commp_data_returnvalue,
          { "Return value", "s7comm-plus.returnvalue", FT_UINT64, BASE_HEX, NULL, 0x0,
            "varuint64: Return value", HFILL }},
        /* The extension for 64 bit Bitmasks was implemented on Oct 2014, so don't use it yet to support older Wireshark versions */
        { &hf_s7commp_data_errorcode,
          { "Bitmask 0x000000000000ffff - Error code", "s7comm-plus.errorcode", FT_INT16, BASE_DEC, VALS(errorcode_names), 0x0, /* 0x000000000000ffff */
            NULL, HFILL }},
        { &hf_s7commp_data_omsline,
          { "Bitmask 0x00000000ffff0000 - OMS line", "s7comm-plus.omsline", FT_UINT16, BASE_DEC, NULL, 0x0, /*0x00000000ffff0000 */
            NULL, HFILL }},
        { &hf_s7commp_data_errorsource,
          { "Bitmask 0x000000ff00000000 - Error source", "s7comm-plus.errorsource", FT_UINT8, BASE_HEX, NULL, 0x0, /* 0x000000ff00000000 */
            NULL, HFILL }},
        { &hf_s7commp_data_genericerrorcode,
          { "Bitmask 0x0000ef0000000000 - Generic error code", "s7comm-plus.genericerrorcode", FT_UINT8, BASE_HEX, NULL, 0x0, /* 0x0000ef0000000000 */
            NULL, HFILL }},
        { &hf_s7commp_data_servererror,
          { "Bitmask 0x0000800000000000 - Server error", "s7comm-plus.servererror", FT_BOOLEAN, BASE_NONE, NULL, 0x0, /* 0x0000800000000000 */
            NULL, HFILL }},
        { &hf_s7commp_data_debuginfo,
          { "Bitmask 0x3fff000000000000 - Debug info", "s7comm-plus.debuginfo", FT_UINT16, BASE_DEC, NULL, 0x0, /* 0x3fff000000000000 */
            NULL, HFILL }},
        { &hf_s7commp_data_errorextension,
          { "Bitmask 0x4000000000000000 - Error extension", "s7comm-plus.errorextension", FT_BOOLEAN, BASE_NONE, NULL, 0x0, /* 0x4000000000000000 */
            NULL, HFILL }},

        { &hf_s7commp_data_opcode,
          { "Opcode", "s7comm-plus.data.opcode", FT_UINT8, BASE_HEX, VALS(opcode_names), 0x0,
            NULL, HFILL }},
        { &hf_s7commp_data_reserved1,
          { "Reserved", "s7comm-plus.data.reserved1", FT_UINT16, BASE_HEX, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_data_function,
          { "Function", "s7comm-plus.data.function", FT_UINT16, BASE_HEX, VALS(data_functioncode_names), 0x0,
            NULL, HFILL }},
        { &hf_s7commp_data_reserved2,
          { "Reserved", "s7comm-plus.data.reserved2", FT_UINT16, BASE_HEX, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_data_seqnum,
          { "Sequence number", "s7comm-plus.data.seqnum", FT_UINT16, BASE_DEC, NULL, 0x0,
            "Sequence number (for reference)", HFILL }},
        { &hf_s7commp_data_unknown1,
          { "Unknown 1", "s7comm-plus.data.unknown1", FT_UINT8, BASE_HEX, NULL, 0x0,
            "Unknown 1. Maybe flags or split into nibbles", HFILL }},
        { &hf_s7commp_data_sessionid,
          { "Session Id", "s7comm-plus.data.sessionid", FT_UINT32, BASE_HEX, NULL, 0x0,
            "Session Id, negotiated on session start", HFILL }},

        { &hf_s7commp_data_item_address,
          { "Item Address", "s7comm-plus.data.item_address", FT_NONE, BASE_NONE, NULL, 0x0,
            "Address of one Item", HFILL }},
        { &hf_s7commp_data_item_value,
          { "Item Value", "s7comm-plus.data.item_value", FT_NONE, BASE_NONE, NULL, 0x0,
            "Value of one item", HFILL }},

        { &hf_s7commp_data_data,
          { "Data unknown", "s7comm-plus.data.data", FT_BYTES, BASE_NONE, NULL, 0x0,
            "Data unknown", HFILL }},

        { &hf_s7commp_data_req_set,
          { "Request Set", "s7comm-plus.data.req_set", FT_NONE, BASE_NONE, NULL, 0x0,
            "This is a set of data in a request telegram", HFILL }},
        { &hf_s7commp_data_res_set,
          { "Response Set", "s7comm-plus.data.res_set", FT_NONE, BASE_NONE, NULL, 0x0,
            "This is a set of data in a response telegram", HFILL }},
        { &hf_s7commp_notification_set,
          { "Notification Data Set", "s7comm-plus.notification_dataset", FT_NONE, BASE_NONE, NULL, 0x0,
            "This is a set of data in a notification data telegram", HFILL }},

        { &hf_s7commp_data_id_number,
          { "ID Number", "s7comm-plus.data.id_number", FT_UINT32, BASE_DEC | BASE_EXT_STRING, &id_number_names_ext, 0x0,
            "varuint32: ID Number for function", HFILL }},
        /* Lists */
        { &hf_s7commp_valuelist,
          { "ValueList", "s7comm-plus.valuelist", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_errorvaluelist,
          { "ErrorValueList", "s7comm-plus.errorvaluelist", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_addresslist,
          { "AddressList", "s7comm-plus.addresslist", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
        /* Item Address */
        { &hf_s7commp_item_count,
          { "Item Count", "s7comm-plus.item.count", FT_UINT32, BASE_DEC, NULL, 0x0,
            "varuint32: Number of items following", HFILL }},
        { &hf_s7commp_item_no_of_fields,
          { "Number of fields in complete Item-Dataset", "s7comm-plus.item.no_of_fields", FT_UINT32, BASE_DEC, NULL, 0x0,
            "varuint32: Number of fields in complete Item-Dataset", HFILL }},
        { &hf_s7commp_itemaddr_crc,
          { "Symbol CRC", "s7comm-plus.item.addr.symbol_crc", FT_UINT32, BASE_HEX, NULL, 0x0,
            "CRC generated out of symbolic name with (x^32+x^31+x^30+x^29+x^28+x^26+x^23+x^21+x^19+x^18+x^15+x^14+x^13+x^12+x^9+x^8+x^4+x+1)", HFILL }},
        { &hf_s7commp_itemaddr_area,
          { "Accessing area", "s7comm-plus.item.addr.area", FT_UINT32, BASE_HEX, NULL, 0x0,
            "varuint32: Specifies the area where to read from, DB, Inputs, Outputs, Flags, etc.", HFILL }},
        { &hf_s7commp_itemaddr_area1,
          { "Accessing area", "s7comm-plus.item.addr.area1", FT_UINT16, BASE_HEX, VALS(var_item_area1_names), 0x0,
            "Area from where to read: DB or Inputs, Outputs, etc.", HFILL }},
        { &hf_s7commp_itemaddr_area2,
          { "Accessing area", "s7comm-plus.item.addr.area2", FT_UINT16, BASE_HEX, VALS(var_item_area2_names), 0x0,
            "Specifies the area from where to read", HFILL }},
        { &hf_s7commp_itemaddr_dbnumber,
          { "DB number", "s7comm-plus.item.addr.dbnumber", FT_UINT16, BASE_DEC, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_itemaddr_lid_nesting_depth,
          { "LID Nesting depth", "s7comm-plus.item.addr.lid_nesting_depth", FT_UINT8, BASE_DEC, NULL, 0x0,
            "varuint32: LID Nesting depth", HFILL }},
        { &hf_s7commp_itemaddr_base_area,
          { "LID Access base area (Nesting level 1)", "s7comm-plus.item.addr.base_area", FT_UINT16, BASE_HEX, VALS(var_item_base_area_names), 0x0,
            "This is the base area for all following LIDs", HFILL }},
        { &hf_s7commp_itemaddr_lid_value,
          { "LID Value", "s7comm-plus.item.addr.lid_value", FT_UINT32, BASE_DEC, NULL, 0x0,
            "varuint32: LID Value", HFILL }},

        /*** Item value ***/
        { &hf_s7commp_itemval_itemnumber,
          { "Item Number", "s7comm-plus.item.val.item_number", FT_UINT32, BASE_DEC, NULL, 0x0,
            "varuint32: Item Number", HFILL }},
        { &hf_s7commp_itemval_elementid,
          { "Element Tag-Id", "s7comm-plus.item.val.elementid", FT_UINT8, BASE_HEX, VALS(itemval_elementid_names), 0x0,
            NULL, HFILL }},
        /* Datatype flags */
        { &hf_s7commp_itemval_datatype_flags,
          { "Datatype flags", "s7comm-plus.item.val.datatype_flags", FT_UINT8, BASE_HEX, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_itemval_datatype_flags_array,
          { "Array", "s7comm-plus.item.val.datatype_flags.array", FT_BOOLEAN, 8, NULL, S7COMMP_DATATYPE_FLAG_ARRAY,
            "The data has to be interpreted as an array of values", HFILL }},
        { &hf_s7commp_itemval_datatype_flags_address_array,
          { "Addressarray", "s7comm-plus.item.val.datatype_flags.address_array", FT_BOOLEAN, 8, NULL, S7COMMP_DATATYPE_FLAG_ADDRESS_ARRAY,
            "Array of values for Item Address via CRC and LID", HFILL }},
        { &hf_s7commp_itemval_datatype_flags_sparsearray,
          { "Sparsearray", "s7comm-plus.item.val.datatype_flags.sparsearray", FT_BOOLEAN, 8, NULL, S7COMMP_DATATYPE_FLAG_SPARSEARRAY,
            "Nullterminated Array with key/value for each element", HFILL }},
        { &hf_s7commp_itemval_datatype_flags_0x80unkn,
          { "Unknown-Flag1", "s7comm-plus.item.val.datatype_flags.unknown1", FT_BOOLEAN, 8, NULL, 0x80,
            "Current unknown flag. A S7-1500 sets this flag sometimes", HFILL }},

        { &hf_s7commp_itemval_datatype,
          { "Datatype", "s7comm-plus.item.val.datatype", FT_UINT8, BASE_HEX, VALS(item_datatype_names), 0x0,
            "Type of data following", HFILL }},
        { &hf_s7commp_itemval_arraysize,
          { "Array size", "s7comm-plus.item.val.arraysize", FT_UINT32, BASE_DEC, NULL, 0x0,
            "varuint32: Number of values of the specified datatype following", HFILL }},
        { &hf_s7commp_itemval_value,
          { "Value", "s7comm-plus.item.val.value", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},

        /* Exploring plc */
        { &hf_s7commp_explore_req_area,
          { "Data area to explore", "s7comm-plus.explore.req_area", FT_UINT32, BASE_HEX, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_explore_req_area_class,
          { "Class", "s7comm-plus.explore.req_area.class", FT_UINT32, BASE_HEX, VALS(explore_class_names), 0xff000000,
            NULL, HFILL }},
        { &hf_s7commp_explore_req_area_class_iqmct,
          { "Class IQMCT", "s7comm-plus.explore.req_area.class.iqmct", FT_UINT32, BASE_HEX, VALS(explore_class_iqmct_names), 0x00ff0000,
            NULL, HFILL }},
        { &hf_s7commp_explore_req_area_class_lib,
          { "Class Lib", "s7comm-plus.explore.req_area.class.lib", FT_UINT32, BASE_HEX, VALS(explore_class_lib_names), 0x00ff0000,
            NULL, HFILL }},
        { &hf_s7commp_explore_req_area_section,
          { "Section", "s7comm-plus.explore.req_area.section", FT_UINT32, BASE_DEC, NULL, 0x0000ffff,
            "Section or DB number", HFILL }},
        { &hf_s7commp_explore_req_area_structindex,
          { "Struct index", "s7comm-plus.explore.req_area.structindex", FT_UINT32, BASE_DEC, NULL, 0x00ff0000,
            NULL, HFILL }},

         /* Explore result, variable (tag) description */
        { &hf_s7commp_tagdescr_offsetinfo,
          { "Offset Info", "s7comm-plus.tagdescr.offsetinfo", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_offsetinfotype,
          { "Offsetinfo Type", "s7comm-plus.tagdescr.offsetinfotype", FT_UINT8, BASE_HEX, VALS(tagdescr_offsetinfotype_names), 0x0,
            "Describes how to interpret the last VLQ values", HFILL }},
        { &hf_s7commp_tagdescr_namelength,
          { "Length of name", "s7comm-plus.tagdescr.namelength", FT_UINT8, BASE_DEC, NULL, 0x0,
            "varuint32: Tag description - Length of name", HFILL }},
        { &hf_s7commp_tagdescr_name,
          { "Name", "s7comm-plus.tagdescr.name", FT_STRING, STR_UNICODE, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_unknown2,
          { "Unknown 2", "s7comm-plus.tagdescr.unknown2", FT_UINT8, BASE_HEX, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_datatype,
          { "Datatype", "s7comm-plus.tagdescr.datatype", FT_UINT8, BASE_HEX, VALS(item_datatype_names), 0x0,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_softdatatype,
          { "SoftDataType", "s7comm-plus.tagdescr.softdatatype", FT_UINT32, BASE_DEC | BASE_EXT_STRING, &tagdescr_softdatatype_names_ext, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags,
          { "Attributes", "s7comm-plus.tagdescr.attributeflags", FT_UINT32, BASE_HEX, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_hostrelevant,
          { "Hostrelevant", "s7comm-plus.tagdescr.attributeflags.hostrelevant", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_HOSTRELEVANT,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_retain,
          { "Plainmember-Retain", "s7comm-plus.tagdescr.attributeflags.retain", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_PLAINMEMBERRETAIN,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_classic,
          { "Plainmember-Classic", "s7comm-plus.tagdescr.attributeflags.classic", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_PLAINMEMBERCLASSIC,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_hmivisible,
          { "HMI-Visible", "s7comm-plus.tagdescr.attributeflags.hmivisible", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_HMIVISIBLE,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_hmireadonly,
          { "HMI-Readonly", "s7comm-plus.tagdescr.attributeflags.hmireadonly", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_HMIREADONLY,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_hmicached,
          { "HMI-Cached", "s7comm-plus.tagdescr.attributeflags.hmicached", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_HMICACHED,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_hmiaccessible,
          { "HMI-Accessible", "s7comm-plus.tagdescr.attributeflags.hmiaccessible", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_HMIACCESSIBLE,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_isqualifier,
          { "Is-Qualifier", "s7comm-plus.tagdescr.attributeflags.isqualifier", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_ISQUALIFIER,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_normalaccess,
          { "Normal-Access", "s7comm-plus.tagdescr.attributeflags.normalaccess", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_NORMALACCESS,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_needslegitimization,
          { "Needs-Legitimization", "s7comm-plus.tagdescr.attributeflags.needslegitimization", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_NEEDSLEGITIMIZATION,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_changeableinrun,
          { "Changeable-In-Run", "s7comm-plus.tagdescr.attributeflags.changeableinrun", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_CHANGEBLEINRUN,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_serveronly,
          { "Server-Only", "s7comm-plus.tagdescr.attributeflags.serveronly", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_SERVERONLY,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_clientreadonly,
          { "Client-Read-Only", "s7comm-plus.tagdescr.attributeflags.clientreadonly", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_CLIENTREADRONLY,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_seploadmemfa,
          { "Separate-Load-Memory-File-Allowed", "s7comm-plus.tagdescr.attributeflags.seploadmemfa", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_SEPLOADMEMFA,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_asevaluationrequired,
          { "AS-Evaluation-Required", "s7comm-plus.tagdescr.attributeflags.asevaluationrequired", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_ASEVALREQ,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_bl,
          { "BL", "s7comm-plus.tagdescr.attributeflags.bl", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_BL,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_persistent,
          { "Persistent", "s7comm-plus.tagdescr.attributeflags.persistent", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_PERSISTENT,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_core,
          { "Core", "s7comm-plus.tagdescr.attributeflags.core", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_CORE,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_isout,
          { "Is-Out", "s7comm-plus.tagdescr.attributeflags.isout", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_ISOUT,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_isin,
          { "Is-In", "s7comm-plus.tagdescr.attributeflags.isin", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_ISIN,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_appwriteable,
          { "App-Writeable", "s7comm-plus.tagdescr.attributeflags.appwriteable", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_APPWRITEABLE,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_attributeflags_appreadable,
          { "App-Readable", "s7comm-plus.tagdescr.attributeflags.appreadable", FT_BOOLEAN, 32, NULL, S7COMMP_TAGDESCR_ATTRIBUTE_APPREADABLE,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_unknown4,
          { "Unknown 4", "s7comm-plus.tagdescr.unknown4", FT_UINT8, BASE_HEX, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_unknown5,
          { "Unknown 5", "s7comm-plus.tagdescr.unknown5", FT_UINT8, BASE_HEX, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_lid,
          { "LID", "s7comm-plus.tagdescr.lid", FT_UINT32, BASE_DEC, NULL, 0x0,
            "varuint32: Tag description - LID", HFILL }},
        { &hf_s7commp_tagdescr_accessability,
          { "Accessability", "s7comm-plus.tagdescr.accessability", FT_UINT32, BASE_DEC, VALS(tagdescr_accessability_names), 0x0,
            NULL, HFILL }},
        { &hf_s7commp_tagdescr_section,
          { "Section", "s7comm-plus.tagdescr.section", FT_UINT32, BASE_DEC, VALS(tagdescr_section_names), 0x0,
            NULL, HFILL }},

        /* Fields for object traversion */
        { &hf_s7commp_element_object,
          { "Object", "s7comm-plus.object", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_element_attribute,
          { "Attribute", "s7comm-plus.attribute", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_element_relation,
          { "Relation", "s7comm-plus.relation", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_element_tagdescription,
          { "Tagdescription", "s7comm-plus.tagdescription", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_element_block,
          { "Block", "s7comm-plus.block", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},

        { &hf_s7commp_objectqualifier,
          { "ObjectQualifier", "s7comm-plus.objectqualifier", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
        /* Integrity part for 1500 */
        { &hf_s7commp_integrity,
          { "Integrity part", "s7comm-plus.integrity", FT_NONE, BASE_NONE, NULL, 0x0,
            "Integrity part for 1500", HFILL }},
        { &hf_s7commp_integrity_id,
          { "Integrity Id", "s7comm-plus.integrity.id", FT_UINT32, BASE_DEC, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_integrity_digestlen,
          { "Digest Length", "s7comm-plus.integrity.digestlen", FT_UINT8, BASE_DEC, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_integrity_digest,
          { "Packet Digest", "s7comm-plus.integrity.digest", FT_BYTES, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},

        /*** Trailer fields ***/
        { &hf_s7commp_trailer,
          { "Trailer", "s7comm-plus.trailer", FT_NONE, BASE_NONE, NULL, 0x0,
            "This is the trailer part of S7 communication plus", HFILL }},
        { &hf_s7commp_trailer_protid,
          { "Protocol Id", "s7comm-plus.trailer.protid", FT_UINT8, BASE_HEX, NULL, 0x0,
            "Protocol Identification", HFILL }},
        { &hf_s7commp_trailer_pdutype,
          { "PDU-Type", "s7comm-plus.trailer.pdutype", FT_UINT8, BASE_HEX, VALS(pdutype_names), 0x0,
            "Type of packet", HFILL }},
        { &hf_s7commp_trailer_datlg,
          { "Data length", "s7comm-plus.trailer.datlg", FT_UINT16, BASE_DEC, NULL, 0x0,
            "Specifies the entire length of the data block in bytes", HFILL }},

        /* Fragment fields */
        { &hf_s7commp_fragment_overlap,
          { "Fragment overlap", "s7comm-plus.fragment.overlap", FT_BOOLEAN, BASE_NONE, NULL, 0x0,
            "Fragment overlaps with other fragments", HFILL }},
        { &hf_s7commp_fragment_overlap_conflict,
          { "Conflicting data in fragment overlap", "s7comm-plus.fragment.overlap.conflict", FT_BOOLEAN, BASE_NONE, NULL, 0x0,
            "Overlapping fragments contained conflicting data", HFILL }},
        { &hf_s7commp_fragment_multiple_tails,
          { "Multiple tail fragments found", "s7comm-plus.fragment.multipletails", FT_BOOLEAN, BASE_NONE, NULL, 0x0,
            "Several tails were found when defragmenting the packet", HFILL }},
        { &hf_s7commp_fragment_too_long_fragment,
          { "Fragment too long", "s7comm-plus.fragment.toolongfragment", FT_BOOLEAN, BASE_NONE, NULL, 0x0,
            "Fragment contained data past end of packet", HFILL }},
        { &hf_s7commp_fragment_error,
          { "Defragmentation error", "s7comm-plus.fragment.error", FT_FRAMENUM, BASE_NONE, NULL, 0x0,
            "Defragmentation error due to illegal fragments", HFILL }},
        { &hf_s7commp_fragment_count,
          { "Fragment count", "s7comm-plus.fragment.count", FT_UINT32, BASE_DEC, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_reassembled_in,
          { "Reassembled in", "s7comm-plus.reassembled.in", FT_FRAMENUM, BASE_NONE, NULL, 0x0,
            "S7COMM-PLUS fragments are reassembled in the given packet", HFILL }},
        { &hf_s7commp_reassembled_length,
          { "Reassembled S7COMM-PLUS length", "s7comm-plus.reassembled.length", FT_UINT32, BASE_DEC, NULL, 0x0,
            "The total length of the reassembled payload", HFILL }},
        { &hf_s7commp_fragment,
          { "S7COMM-PLUS Fragment", "s7comm-plus.fragment", FT_FRAMENUM, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
        { &hf_s7commp_fragments,
          { "S7COMM-PLUS Fragments", "s7comm-plus.fragments", FT_NONE, BASE_NONE, NULL, 0x0,
            NULL, HFILL }},
    };

    static gint *ett[] = {
        &ett_s7commp,
        &ett_s7commp_header,
        &ett_s7commp_data,
        &ett_s7commp_data_item,
        &ett_s7commp_data_returnvalue,
        &ett_s7commp_trailer,
        &ett_s7commp_data_req_set,
        &ett_s7commp_data_res_set,
        &ett_s7commp_notification_set,
        &ett_s7commp_itemaddr_area,
        &ett_s7commp_itemval_datatype_flags,
        &ett_s7commp_itemval_array,
        &ett_s7commp_tagdescr_attributeflags,
        &ett_s7commp_tagdescr_offsetinfo,
        &ett_s7commp_explore_req_area,
        &ett_s7commp_element_object,
        &ett_s7commp_element_attribute,
        &ett_s7commp_element_relation,
        &ett_s7commp_element_tagdescription,
        &ett_s7commp_element_block,
        &ett_s7commp_valuelist,
        &ett_s7commp_errorvaluelist,
        &ett_s7commp_addresslist,
        &ett_s7commp_objectqualifier,
        &ett_s7commp_integrity,
        &ett_s7commp_fragments,
        &ett_s7commp_fragment
    };

    proto_s7commp = proto_register_protocol (
        "S7 Communication Plus",            /* name */
        "S7COMM-PLUS",                      /* short name */
        "s7comm-plus"                       /* abbrev */
        );

    proto_register_field_array(proto_s7commp, hf, array_length (hf));

    proto_register_subtree_array(ett, array_length (ett));
    /* Register the init routine. */
    register_init_routine(s7commp_defragment_init);
}
/*******************************************************************************************************
 *
 * Spezial gepacktes Datenformat
 * siehe: http://en.wikipedia.org/wiki/Variable-length_quantity
 *
 * In der Datei packet-wap.c gibt es eine Funktion für unsigned:
 * guint tvb_get_guintvar (tvbuff_t *tvb, guint offset, guint *octetCount)
 * welche aber keine Begrenzung auf eine max-Anzahl hat (5 für int32).
 * Solange das Protokoll noch nicht sicher erkannt wird, ist diese Version hier sicherer.
 *
 *******************************************************************************************************/
guint32
tvb_get_varint32(tvbuff_t *tvb, guint8 *octet_count, guint32 offset)
{
    int counter;
    gint32 val = 0;
    guint8 octet;
    guint8 cont;

    for (counter = 1; counter <= 4+1; counter++) {
        octet = tvb_get_guint8(tvb, offset);
        offset += 1;
        if ((counter == 1) && (octet & 0x40)) {     /* check sign */
            octet &= 0xbf;
            val = 0xffffffc0;                       /* pre-load with one complement, excluding first 6 bits */
        } else {
            val <<= 7;
        }
        cont = (octet & 0x80);
        octet &= 0x7f;
        val += octet;
        if (cont == 0) {
            break;
        }
    }
    *octet_count = counter;
    return val;
}
/*******************************************************************************************************/
guint32
tvb_get_varuint32(tvbuff_t *tvb, guint8 *octet_count, guint32 offset)
{
    int counter;
    gint32 val = 0;
    guint8 octet;
    guint8 cont;
    for (counter = 1; counter <= 4+1; counter++) {        /* große Werte benötigen 5 Bytes: 4*7 bit + 4 bit */
        octet = tvb_get_guint8(tvb, offset);
        offset += 1;
        val <<= 7;
        cont = (octet & 0x80);
        octet &= 0x7f;
        val += octet;
        if (cont == 0) {
            break;
        }
    }
    *octet_count = counter;
    return  val;
}
/*******************************************************************************************************/
guint64
tvb_get_varuint64(tvbuff_t *tvb, guint8 *octet_count, guint32 offset)
{
    int counter;
    guint64 val = 0;
    guint8 octet;
    guint8 cont;
    for (counter = 1; counter <= 8; counter++) {        /* 8*7 bit + 8 bit = 64 bit -> Sonderfall im letzten Octett! */
        octet = tvb_get_guint8(tvb, offset);
        offset += 1;
        val <<= 7;
        cont = (octet & 0x80);
        octet &= 0x7f;
        val += octet;
        if (cont == 0) {
            break;
        }
    }
    *octet_count = counter;
    if(cont) {        /* 8*7 bit + 8 bit = 64 bit -> Sonderfall im letzten Octett! */
        octet = tvb_get_guint8(tvb, offset);
        offset += 1;
        val <<= 8;
        val += octet;
    }
    return  val;
}
/*******************************************************************************************************/
gint64
tvb_get_varint64(tvbuff_t *tvb, guint8 *octet_count, guint32 offset)
{
    int counter;
    gint64 val = 0;
    guint8 octet;
    guint8 cont;
    for (counter = 1; counter <= 8; counter++) {  /* 8*7 bit + 8 bit = 64 bit -> Sonderfall im letzten Octett! */
        octet = tvb_get_guint8(tvb, offset);
        offset += 1;
        if ((counter == 1) && (octet & 0x40)) {   /* check sign */
            octet &= 0xbf;
            val = 0xffffffffffffffc0;             /* pre-load with one complement, excluding first 6 bits */
        } else {
            val <<= 7;
        }
        cont = (octet & 0x80);
        octet &= 0x7f;
        val += octet;
        if (cont == 0) {
            break;
        }
    }
    *octet_count = counter;
    if(cont) {        /* 8*7 bit + 8 bit = 64 bit -> Sonderfall im letzten Octett! */
        octet = tvb_get_guint8(tvb, offset);
        offset += 1;
        val <<= 8;
        val += octet;
    }
    return  val;
}
/*******************************************************************************************************
 *
 * Returns an timestamp as string from a unix-timestamp 64 bit value. Needs a char array of size 34.
 * Format:
 * Jan 31, 2014 23:59:59.999.999.999
 *
 *******************************************************************************************************/
static void
s7commp_get_timestring_from_uint64(guint64 timestamp, char *str, gint max)
{
    guint16 nanosec, microsec, millisec;
    struct tm *mt;
    time_t t;

    nanosec = timestamp % 1000;
    timestamp /= 1000;
    microsec = timestamp % 1000;
    timestamp /= 1000;
    millisec = timestamp % 1000;
    timestamp /= 1000;
    t = timestamp;
    mt = gmtime(&t);
    str[0] = '\0';
    if (mt != NULL) {
        g_snprintf(str, max, "%s %2d, %d %02d:%02d:%02d.%03d.%03d.%03d", mon_names[mt->tm_mon], mt->tm_mday,
            mt->tm_year + 1900, mt->tm_hour, mt->tm_min, mt->tm_sec,
            millisec, microsec, nanosec);
    }
}
/*******************************************************************************************************
 *
 * Decodes a return value, coded as 64 Bit VLQ. Includes an errorcode and some flags.
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_returnvalue(tvbuff_t *tvb,
                           proto_tree *tree,
                           guint32 offset,
                           gint16 *errorcode_out)
{
    guint64 return_value;
    guint8 octet_count = 0;
    gint16 errorcode;
    proto_item *ret_item = NULL;
    proto_tree *ret_tree = NULL;

    return_value = tvb_get_varuint64(tvb, &octet_count, offset);
    errorcode = (gint16)return_value;
    ret_item = proto_tree_add_uint64(tree, hf_s7commp_data_returnvalue, tvb, offset, octet_count, return_value);
    /* add errocdode to main item */
    proto_item_append_text(ret_item, " - Error code: %s (%d)", val_to_str(errorcode, errorcode_names, "%d"), errorcode);
    ret_tree = proto_item_add_subtree(ret_item, ett_s7commp_data_returnvalue);
    proto_tree_add_int(ret_tree, hf_s7commp_data_errorcode, tvb, offset, octet_count, errorcode);
    proto_tree_add_uint(ret_tree, hf_s7commp_data_omsline, tvb, offset, octet_count, (guint16)(return_value >> 16));
    proto_tree_add_uint(ret_tree, hf_s7commp_data_errorsource, tvb, offset, octet_count, (guint8)(return_value >> 32));
    proto_tree_add_uint(ret_tree, hf_s7commp_data_genericerrorcode, tvb, offset, octet_count, (guint8)(return_value >> 40) & 0xef);
    proto_tree_add_boolean(ret_tree, hf_s7commp_data_servererror, tvb, offset, octet_count, (gboolean)(return_value & 0x0000800000000000));
    proto_tree_add_uint(ret_tree, hf_s7commp_data_debuginfo, tvb, offset, octet_count, (guint16)(return_value >> 48) & 0x3fff);
    proto_tree_add_boolean(ret_tree, hf_s7commp_data_errorextension, tvb, offset, octet_count, (gboolean)(return_value & 0x4000000000000000));

    offset += octet_count;
    if (errorcode_out != NULL) {        /* return errorcode if needed outside */
        *errorcode_out = errorcode;
    }

    return offset;
}
/*******************************************************************************************************
 *
 * Decoding of an Address-Array, used to subscribe variables from HMI
 *
 *******************************************************************************************************/
 /* Funktion wird z.Zt. nicht mehr benötigt, da das Adressarray wie ein "normales" Array
  * zerlegt wird. GGf. später die Feldinformationen in anderer Weise hinzufügen.
  * Z.B. Funktion mit (arr_size, arr_index_actual, return_string_with_info).
  */
static guint32
s7commp_decode_udint_address_array(tvbuff_t *tvb,
                                   proto_tree *tree,
                                   guint32 array_size,
                                   guint32 offset)
{
    guint32 value = 0;
    guint8 octet_count = 0;
    guint32 item_count = 0;
    guint32 i = 0;
    guint32 array_size_act = 0;
    guint16 tia_var_area1;
    guint16 tia_var_area2;

    value = tvb_get_varuint32(tvb, &octet_count, offset);
    proto_tree_add_text(tree, tvb, offset, octet_count, "Unknown 1 (ID?): %u", value);
    offset += octet_count;
    array_size_act += 1;

    value = tvb_get_varuint32(tvb, &octet_count, offset);
    proto_tree_add_text(tree, tvb, offset, octet_count, "Unknown 2: %u", value);
    offset += octet_count;
    array_size_act += 1;

    item_count = tvb_get_varuint32(tvb, &octet_count, offset);
    proto_tree_add_text(tree, tvb, offset, octet_count, "Number of addresses following: %u", item_count);
    offset += octet_count;
    array_size_act += 1;

    for (i = 1; i <= item_count; i++) {
        value = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_text(tree, tvb, offset, octet_count, "Address[%u] Unknown 1 (ID?): %u", i, value);
        offset += octet_count;
        array_size_act += 1;

        value = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_text(tree, tvb, offset, octet_count, "Address[%u] Item reference number: %u", i, value);
        offset += octet_count;
        array_size_act += 1;

        value = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_text(tree, tvb, offset, octet_count, "Address[%u] Unknown 2: %u", i, value);
        offset += octet_count;
        array_size_act += 1;

        value = tvb_get_varuint32(tvb, &octet_count, offset);
        /* Area ausmaskieren, ist hier etwas anders codiert als bei einem normalen Read einer Variable */
        tia_var_area1 = (value >> 16);
        tia_var_area2 = (value & 0xffff);
        if (tia_var_area1 == S7COMMP_VAR_ITEM_AREA1_DB) {
            proto_tree_add_text(tree, tvb, offset, octet_count, "Address[%u] Area: %u (Datablock, DB-Number %u)", i, value, tia_var_area2);
        } else {
            proto_tree_add_text(tree, tvb, offset, octet_count, "Address[%u] IQMCT Area: %u (%s)", i, value, val_to_str(value, var_item_area2_names, "Unknown IQMCT Area"));
        }
        offset += octet_count;
        array_size_act += 1;

        value = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_text(tree, tvb, offset, octet_count, "Address[%u] Symbol-CRC: %u (0x%08x)", i, value, value);
        offset += octet_count;
        array_size_act += 1;

        value = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_text(tree, tvb, offset, octet_count, "Address[%u] Base Area: %u (%s)", i, value, val_to_str(value, var_item_base_area_names, "Unknown Base Area"));
        offset += octet_count;
        array_size_act += 1;

        /* When accessing a variable inside a struct / array, the adress has one LID for each struct / array index.
         * There is no header which says how many LIDs are following.
         * When another address follows, the ID of this is always bigger than 2^31. If not, then another LID follows.
         * If this is the last address, check if the number of fields in the array is reached.
         */
        do {
            value = tvb_get_varuint32(tvb, &octet_count, offset);
            if (value < 2147483648lu) {
                proto_tree_add_text(tree, tvb, offset, octet_count, "Address[%u] LID-Value: %u", i, value);
                offset += octet_count;
                array_size_act += 1;
            }
        } while ((value < 2147483648lu) && (array_size_act < array_size));

        /* break decoding if out of array-size range*/
        if (array_size_act >= array_size) {
            break;
        }
    }

    return offset;
}
/*******************************************************************************************************
 *
 * Decoding of a single value with datatype flags, datatype specifier and the value data
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_value(tvbuff_t *tvb,
                     proto_tree *data_item_tree,
                     guint32 offset,
                     int* struct_level)
{
    guint8 octet_count = 0;
    guint8 datatype;
    guint8 datatype_flags;
    gboolean is_array = FALSE;
    gboolean is_address_array = FALSE;
    gboolean is_sparsearray = FALSE;
    gboolean unknown_type_occured = FALSE;
    guint32 array_size = 1;     /* use 1 as default, so non-arrays can dissected in the same manner as arrays */
    guint32 array_index = 0;

    proto_item *array_item = NULL;
    proto_tree *array_item_tree = NULL;
    proto_tree *current_tree = NULL;

    guint64 uint64val = 0;
    guint32 uint32val = 0;
    guint16 uint16val = 0;
    gint16 int16val = 0;
    gint32 int32val = 0;
    guint8 uint8val = 0;
    gint64 int64val = 0;
    gint8 int8val = 0;
    gchar str_val[128];     /* Value of one single item */
    gchar str_arrval[512];  /* Value of array values */
    guint32 sparsearray_key;
    const gchar *str_arr_prefix = "Unknown";

    guint32 start_offset = 0;
    guint32 length_of_value = 0;

    memset(str_val, 0, sizeof(str_val));
    memset(str_arrval, 0, sizeof(str_arrval));

    datatype_flags = tvb_get_guint8(tvb, offset);
    proto_tree_add_bitmask(data_item_tree, tvb, offset, hf_s7commp_itemval_datatype_flags,
        ett_s7commp_itemval_datatype_flags, s7commp_itemval_datatype_flags_fields, ENC_BIG_ENDIAN);
    is_array = (datatype_flags & S7COMMP_DATATYPE_FLAG_ARRAY);
    is_address_array = (datatype_flags & S7COMMP_DATATYPE_FLAG_ADDRESS_ARRAY);
    is_sparsearray = (datatype_flags & S7COMMP_DATATYPE_FLAG_SPARSEARRAY);
    offset += 1;

    datatype = tvb_get_guint8(tvb, offset);
    proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_datatype, tvb, offset, 1, datatype);
    offset += 1;

    if (is_array || is_address_array || is_sparsearray) {
        if (is_sparsearray) {
            /* Bei diesem Array-Typ gibt es keine Angabe über die Anzahl. Das Array ist Null-terminiert.
             * Damit die for-Schleife aber auch hierfür verwendet werden kann, wird die Anzahl auf 999999 gesetzt,
             * und die Schleife bei erreichen der terminierenden Null explizit verlassen.
             */
            array_size = 999999;
        } else {
            array_size = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_arraysize, tvb, offset, octet_count, array_size);
            offset += octet_count;
        }
        /* To Display an array value, build a separate tree for the complete array.
         * Under the array tree the array values are displayed.
         */
        array_item = proto_tree_add_item(data_item_tree, hf_s7commp_itemval_value, tvb, offset, -1, FALSE);
        array_item_tree = proto_item_add_subtree(array_item, ett_s7commp_itemval_array);
        start_offset = offset;
        if (is_array) {
            str_arr_prefix = "Array";
        } else if (is_address_array) {
            str_arr_prefix = "Addressarray";
        } else if (is_sparsearray) {
            str_arr_prefix = "Sparsearray";
        }
        current_tree = array_item_tree;
    } else {
        current_tree = data_item_tree;
    }

    /* Use array loop also for non-arrays */
    for (array_index = 1; array_index <= array_size; array_index++) {
        if (is_sparsearray) {
            sparsearray_key = tvb_get_varuint32(tvb, &octet_count, offset);
            if (sparsearray_key == 0) {
                proto_tree_add_text(current_tree, tvb, offset, octet_count, "Sparsearray key: Terminating Null");
                offset += octet_count;
                break;
            } else {
                if (datatype == S7COMMP_ITEM_DATATYPE_VARIANT) {
                    proto_tree_add_text(current_tree, tvb, offset, octet_count, "Sparsearray Variant Type-ID: %u", sparsearray_key);
                } else {
                    proto_tree_add_text(current_tree, tvb, offset, octet_count, "Sparsearray key: %u", sparsearray_key);
                }
                offset += octet_count;
            }
        }

        switch (datatype) {
            case S7COMMP_ITEM_DATATYPE_NULL:
                /* No value following */
                g_snprintf(str_val, sizeof(str_val), "<NO VALUE>");
                length_of_value = 0;
                break;
            case S7COMMP_ITEM_DATATYPE_BOOL:
                length_of_value = 1;
                g_snprintf(str_val, sizeof(str_val), "0x%02x", tvb_get_guint8(tvb, offset));
                offset += 1;
                break;
            case S7COMMP_ITEM_DATATYPE_USINT:
                length_of_value = 1;
                g_snprintf(str_val, sizeof(str_val), "%u", tvb_get_guint8(tvb, offset));
                offset += 1;
                break;
            case S7COMMP_ITEM_DATATYPE_UINT:
                length_of_value = 2;
                g_snprintf(str_val, sizeof(str_val), "%u", tvb_get_ntohs(tvb, offset));
                offset += 2;
                break;
            case S7COMMP_ITEM_DATATYPE_UDINT:
                uint32val = tvb_get_varuint32(tvb, &octet_count, offset);
                offset += octet_count;
                length_of_value = octet_count;
                g_snprintf(str_val, sizeof(str_val), "%u", uint32val);
                break;
            case S7COMMP_ITEM_DATATYPE_ULINT:
                uint64val = tvb_get_varuint64(tvb, &octet_count, offset);
                offset += octet_count;
                length_of_value = octet_count;
                g_snprintf(str_val, sizeof(str_val), "%llu", uint64val);
                break;
            case S7COMMP_ITEM_DATATYPE_LINT:
                int64val = tvb_get_varint64(tvb, &octet_count, offset);
                offset += octet_count;
                length_of_value = octet_count;
                g_snprintf(str_val, sizeof(str_val), "%lld", int64val);
                break;
            case S7COMMP_ITEM_DATATYPE_SINT:
                uint8val = tvb_get_guint8(tvb, offset);
                memcpy(&int8val, &uint8val, sizeof(int8val));
                length_of_value = 1;
                g_snprintf(str_val, sizeof(str_val), "%d", int8val);
                offset += 1;
                break;
            case S7COMMP_ITEM_DATATYPE_INT:
                uint16val = tvb_get_ntohs(tvb, offset);
                memcpy(&int16val, &uint16val, sizeof(int16val));
                length_of_value = 2;
                g_snprintf(str_val, sizeof(str_val), "%d", int16val);
                offset += 2;
                break;
            case S7COMMP_ITEM_DATATYPE_DINT:
                int32val = tvb_get_varint32(tvb, &octet_count, offset);
                offset += octet_count;
                length_of_value = octet_count;
                g_snprintf(str_val, sizeof(str_val), "%d", int32val);
                break;
            case S7COMMP_ITEM_DATATYPE_BYTE:
                length_of_value = 1;
                g_snprintf(str_val, sizeof(str_val), "0x%02x", tvb_get_guint8(tvb, offset));
                offset += 1;
                break;
            case S7COMMP_ITEM_DATATYPE_WORD:
                length_of_value = 2;
                g_snprintf(str_val, sizeof(str_val), "0x%04x", tvb_get_ntohs(tvb, offset));
                offset += 2;
                break;
            case S7COMMP_ITEM_DATATYPE_STRUCT:
                if (struct_level) *struct_level += 1; /* entering a new structure level */
                length_of_value = 4;
                g_snprintf(str_val, sizeof(str_val), "%u", tvb_get_ntohl(tvb, offset));
                offset += 4;
                break;
            case S7COMMP_ITEM_DATATYPE_DWORD:
                length_of_value = 4;
                g_snprintf(str_val, sizeof(str_val), "0x%08x", tvb_get_ntohl(tvb, offset));
                offset += 4;
                break;
            case S7COMMP_ITEM_DATATYPE_LWORD:
                length_of_value = 8;
                g_snprintf(str_val, sizeof(str_val), "0x%016llx", tvb_get_ntoh64(tvb, offset));
                offset += 8;
                break;
            case S7COMMP_ITEM_DATATYPE_REAL:
                length_of_value = 4;
                g_snprintf(str_val, sizeof(str_val), "%f", tvb_get_ntohieee_float(tvb, offset));
                offset += 4;
                break;
            case S7COMMP_ITEM_DATATYPE_LREAL:
                length_of_value = 8;
                g_snprintf(str_val, sizeof(str_val), "%f", tvb_get_ntohieee_double(tvb, offset));
                offset += 8;
                break;
            case S7COMMP_ITEM_DATATYPE_TIMESTAMP:
                length_of_value = 8;
                uint64val = tvb_get_ntoh64(tvb, offset);
                s7commp_get_timestring_from_uint64(uint64val, str_val, sizeof(str_val));
                offset += 8;
                break;
            case S7COMMP_ITEM_DATATYPE_TIMESPAN:
                uint64val = tvb_get_varuint64(tvb, &octet_count, offset);
                offset += octet_count;
                length_of_value = octet_count;
                g_snprintf(str_val, sizeof(str_val), "%llu ns", uint64val);
                break;
            case S7COMMP_ITEM_DATATYPE_RID:
                length_of_value = 4;
                g_snprintf(str_val, sizeof(str_val), "0x%08x", tvb_get_ntohl(tvb, offset));
                offset += 4;
                break;
            case S7COMMP_ITEM_DATATYPE_AID:
                uint32val = tvb_get_varuint32(tvb, &octet_count, offset);
                offset += octet_count;
                length_of_value = octet_count;
                g_snprintf(str_val, sizeof(str_val), "%u", uint32val);
                break;
            case S7COMMP_ITEM_DATATYPE_WSTRING:
                length_of_value = tvb_get_varuint32(tvb, &octet_count, offset);
                proto_tree_add_text(current_tree, tvb, offset, octet_count, "String actual length: %u", length_of_value);
                offset += octet_count;
                g_snprintf(str_val, sizeof(str_val), "%s",
                       tvb_get_string_enc(wmem_packet_scope(), tvb, offset, length_of_value, ENC_UTF_8|ENC_NA));
                offset += length_of_value;
                break;
            case S7COMMP_ITEM_DATATYPE_VARIANT:
                uint32val = tvb_get_varuint32(tvb, &octet_count, offset);
                offset += octet_count;
                length_of_value = octet_count;
                g_snprintf(str_val, sizeof(str_val), "%u", uint32val);
                break;
            case S7COMMP_ITEM_DATATYPE_BLOB:
                proto_tree_add_text(current_tree, tvb, offset, 1, "Blob Reserved: 0x%02x", tvb_get_guint8(tvb, offset));
                offset += 1;
                length_of_value = tvb_get_varuint32(tvb, &octet_count, offset);
                proto_tree_add_text(current_tree, tvb, offset, octet_count, "Blob size: %u", length_of_value);
                offset += octet_count;
                g_snprintf(str_val, sizeof(str_val), "%s", tvb_bytes_to_ep_str(tvb, offset, length_of_value));
                offset += length_of_value;
                break;
            default:
                unknown_type_occured = TRUE;
                g_strlcpy(str_val, "Unknown Type occured. Could not interpret value!", sizeof(str_val));
                break;
        } /* switch */

        if (unknown_type_occured) {
            break;
        }

        if (is_array || is_address_array || is_sparsearray) {
            /* Build a string of all array values. Maximum number of 10 values */
            if (array_index < S7COMMP_ITEMVAL_ARR_MAX_DISPLAY) {
                if (array_index > 1 && array_size > 1) {
                    g_strlcat(str_arrval, ", ", sizeof(str_arrval));
                }
                g_strlcat(str_arrval, str_val, sizeof(str_arrval));
            } else if (array_index == S7COMMP_ITEMVAL_ARR_MAX_DISPLAY) {
                /* truncate */
                g_strlcat(str_arrval, "...", sizeof(str_arrval));
            }
            if (is_sparsearray) {
                proto_tree_add_text(array_item_tree, tvb, offset - length_of_value, length_of_value, "Value: %s", str_val);
                if (sparsearray_key == 0) {
                    break;
                }
            } else {
                proto_tree_add_text(array_item_tree, tvb, offset - length_of_value, length_of_value, "Value[%u]: %s", array_index, str_val);
            }
        }
    } /* for */

    if (is_array || is_address_array) {
        proto_item_append_text(array_item_tree, " %s[%u] = %s", str_arr_prefix, array_size, str_arrval);
        proto_item_set_len(array_item_tree, offset - start_offset);
        proto_item_append_text(data_item_tree, " (%s) %s[%u] = %s", val_to_str(datatype, item_datatype_names, "Unknown datatype: 0x%02x"), str_arr_prefix, array_size, str_arrval);
    } else if (is_sparsearray) {
        proto_item_append_text(array_item_tree, " %s = %s", str_arr_prefix, str_arrval);
        proto_item_set_len(array_item_tree, offset - start_offset);
        proto_item_append_text(data_item_tree, " (%s) %s = %s", val_to_str(datatype, item_datatype_names, "Unknown datatype: 0x%02x"), str_arr_prefix, str_arrval);
    } else { /* not an array or address array */
        if (length_of_value > 0) {
            proto_tree_add_text(data_item_tree, tvb, offset - length_of_value, length_of_value, "Value: %s", str_val);
        }
        proto_item_append_text(data_item_tree, " (%s) = %s", val_to_str(datatype, item_datatype_names, "Unknown datatype: 0x%02x"), str_val);
    }
    return offset;
}
/*******************************************************************************************************
 *
 * Decodes a list of item-id and a value recursive sub-structs.
 * Builds a tree which represents the data structure.
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_id_value_list(tvbuff_t *tvb,
                             proto_tree *tree,
                             guint32 offset,
                             gboolean looping)
{
    proto_item *data_item = NULL;
    proto_tree *data_item_tree = NULL;
    guint32 id_number;
    guint32 start_offset;
    guint8 octet_count = 0;
    int struct_level;

    do {
        id_number = tvb_get_varuint32(tvb, &octet_count, offset);
        if (id_number == 0) {
            proto_tree_add_text(tree, tvb, offset, octet_count, "Terminating Item/List");
            offset += octet_count;
            return offset;
        } else {
            start_offset = offset;
            data_item = proto_tree_add_item(tree, hf_s7commp_data_item_value, tvb, offset, -1, FALSE);
            data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_data_item);
            proto_tree_add_uint(data_item_tree, hf_s7commp_data_id_number, tvb, offset, octet_count, id_number);
            proto_item_append_text(data_item_tree, " [%u]:", id_number);
            offset += octet_count;
            struct_level = 0;
            offset = s7commp_decode_value(tvb, data_item_tree, offset, &struct_level);
            if (struct_level > 0) { /* A new struct was entered, use recursive struct traversal */
                offset = s7commp_decode_id_value_list(tvb, data_item_tree, offset, TRUE);
            }
            proto_item_set_len(data_item_tree, offset - start_offset);
        }
    } while (looping);
    return offset;
}
/*******************************************************************************************************
 *
 * Calls s7commp_decode_id_value_list an inserts data into a ValueList subtree
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_id_value_list_in_new_tree(tvbuff_t *tvb,
                                                       proto_tree *tree,
                                                       guint32 offset,
                                                       gboolean looping)
{
    proto_item *list_item = NULL;
    proto_tree *list_item_tree = NULL;
    guint32 list_start_offset = offset;
    list_item = proto_tree_add_item(tree, hf_s7commp_valuelist, tvb, offset, -1, FALSE);
    list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_valuelist);
    offset = s7commp_decode_id_value_list(tvb, list_item_tree, offset, looping);
    proto_item_set_len(list_item_tree, offset - list_start_offset);
    return offset;
}
/*******************************************************************************************************
 *
 * Decodes a list of item-number and value. Subvalues (struct members) are decoded as IDs.
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_itemnumber_value_list(tvbuff_t *tvb,
                                     proto_tree *tree,
                                     guint32 offset,
                                     gboolean looping)
{
    proto_item *data_item = NULL;
    proto_tree *data_item_tree = NULL;
    guint32 itemnumber;
    guint32 start_offset;
    guint8 octet_count = 0;
    int struct_level;

    do {
        itemnumber = tvb_get_varuint32(tvb, &octet_count, offset);
        if (itemnumber == 0) {
            proto_tree_add_text(tree, tvb, offset, octet_count, "Terminating Item/List");
            offset += octet_count;
            break;
        } else {
            start_offset = offset;
            data_item = proto_tree_add_item(tree, hf_s7commp_data_item_value, tvb, offset, -1, FALSE);
            data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_data_item);
            proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_itemnumber, tvb, offset, octet_count, itemnumber);
            proto_item_append_text(data_item_tree, " [%u]:", itemnumber);
            offset += octet_count;
            struct_level = 0;
            offset = s7commp_decode_value(tvb, data_item_tree, offset, &struct_level);
            if (struct_level > 0) {
                offset = s7commp_decode_id_value_list(tvb, data_item_tree, offset, TRUE);
            }
            proto_item_set_len(data_item_tree, offset - start_offset);
        }
    } while (looping);
    return offset;
}
/*******************************************************************************************************
 *
 * Calls s7commp_decode_itemnumber_value_list an inserts data into a ValueList subtree
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_itemnumber_value_list_in_new_tree(tvbuff_t *tvb,
                                                       proto_tree *tree,
                                                       guint32 offset,
                                                       gboolean looping)
{
    proto_item *list_item = NULL;
    proto_tree *list_item_tree = NULL;
    guint32 list_start_offset = offset;
    list_item = proto_tree_add_item(tree, hf_s7commp_valuelist, tvb, offset, -1, FALSE);
    list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_valuelist);
    offset = s7commp_decode_itemnumber_value_list(tvb, list_item_tree, offset, looping);
    proto_item_set_len(list_item_tree, offset - list_start_offset);
    return offset;
}
/*******************************************************************************************************
 *
 * Decodes a list of error values, until terminating null and lowest struct level
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_itemnumber_errorvalue_list(tvbuff_t *tvb,
                                          proto_tree *tree,
                                          guint32 offset)
{
    proto_item *list_item = NULL;
    proto_tree *list_item_tree = NULL;
    proto_item *data_item = NULL;
    proto_tree *data_item_tree = NULL;

    guint32 item_number;
    guint8 octet_count = 0;
    gint16 errorcode = 0;

    guint32 start_offset = offset;
    guint32 list_start_offset = offset;

    list_item = proto_tree_add_item(tree, hf_s7commp_errorvaluelist, tvb, offset, -1, FALSE);
    list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_errorvaluelist);

    do {
        item_number = tvb_get_varuint32(tvb, &octet_count, offset);
        if (item_number == 0) {
            proto_tree_add_text(list_item_tree, tvb, offset, octet_count, "Terminating ErrorValueList");
            offset += octet_count;
        } else {
            start_offset = offset;
            data_item = proto_tree_add_item(list_item_tree, hf_s7commp_data_item_value, tvb, offset, -1, FALSE);
            data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_data_item);
            proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_itemnumber, tvb, offset, octet_count, item_number);
            offset += octet_count;
            offset = s7commp_decode_returnvalue(tvb, data_item_tree, offset, &errorcode);
            proto_item_append_text(data_item_tree, " [%u]: Error code: %s (%d)", item_number, val_to_str(errorcode, errorcode_names, "%d"), errorcode);
            proto_item_set_len(data_item_tree, offset - start_offset);
        }
    } while (item_number != 0);
    proto_item_set_len(list_item_tree, offset - list_start_offset);
    return offset;
}
/*******************************************************************************************************
 *
 * Decodes a tag description
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_tagdescription(tvbuff_t *tvb,
                             proto_tree *tree,
                             guint32 offset)
{
    guint32 lid;
    guint32 length_of_value;
    guint32 vlq_value;
    gint32 svlq_value;
    guint8 octet_count = 0;
    guint8 element_id;
    guint8 datatype;
    guint8 offsetinfotype;
    int i;
    proto_item *offsetinfo_item = NULL;
    proto_tree *offsetinfo_tree = NULL;
    guint32 start_offset;
    guint32 number_of_array_dimensions;
    guint32 array_dimension;

    offsetinfotype = tvb_get_guint8(tvb, offset);
    proto_tree_add_uint(tree, hf_s7commp_tagdescr_offsetinfotype, tvb, offset, 1, offsetinfotype);
    offset += 1;

    length_of_value = tvb_get_varuint32(tvb, &octet_count, offset);
    proto_tree_add_uint(tree, hf_s7commp_tagdescr_namelength, tvb, offset, octet_count, length_of_value);
    offset += octet_count;

    proto_tree_add_item(tree, hf_s7commp_tagdescr_name, tvb, offset, length_of_value, ENC_UTF_8|ENC_NA);
    proto_item_append_text(tree, ", for Tag: %s", tvb_get_string_enc(wmem_packet_scope(), tvb, offset, length_of_value, ENC_UTF_8|ENC_NA));
    offset += length_of_value;

    proto_tree_add_uint(tree, hf_s7commp_tagdescr_unknown2, tvb, offset, 1, tvb_get_guint8(tvb, offset));
    offset += 1;

    datatype = tvb_get_guint8(tvb, offset);
    proto_tree_add_uint(tree, hf_s7commp_tagdescr_datatype, tvb, offset, 1, datatype);
    offset += 1;

    vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
    proto_tree_add_uint(tree, hf_s7commp_tagdescr_softdatatype, tvb, offset, octet_count, vlq_value);
    offset += octet_count;

    proto_tree_add_bitmask(tree, tvb, offset, hf_s7commp_tagdescr_attributeflags,
        ett_s7commp_tagdescr_attributeflags, s7commp_tagdescr_attributeflags_fields, ENC_BIG_ENDIAN);
    offset += 4;

    lid = tvb_get_varuint32(tvb, &octet_count, offset);
    proto_tree_add_uint(tree, hf_s7commp_tagdescr_lid, tvb, offset, octet_count, lid);
    offset += octet_count;

    /* Dieser Wert hat je nach Datentyp eine unterschiedliche Funktion.
     * Ist das Element eine Struktur, so kann mir einer folgenden Abfrage die einer Sub-Elements im Datenbaustein anhand der ID
     * die Beziehung zu dem übergeordneten Element hergestellt werden.
     */
    length_of_value = tvb_get_varuint32(tvb, &octet_count, offset);
    if (datatype == S7COMMP_ITEM_DATATYPE_S7STRING) {
        proto_tree_add_text(tree, tvb, offset, octet_count, "Length of S7String: %u", length_of_value);
    } else if (datatype == S7COMMP_ITEM_DATATYPE_STRUCT) {
        proto_tree_add_text(tree, tvb, offset, octet_count, "Relation Id for Struct: %u", length_of_value);
    } else {
        proto_tree_add_text(tree, tvb, offset, octet_count, "Unknown for this datatype: %u", length_of_value);
    }
    offset += octet_count;

    offsetinfo_item = proto_tree_add_item(tree, hf_s7commp_tagdescr_offsetinfo, tvb, offset, -1, FALSE);
    offsetinfo_tree = proto_item_add_subtree(offsetinfo_item, ett_s7commp_tagdescr_offsetinfo);
    start_offset = offset;
    switch (offsetinfotype) {
        case S7COMMP_TAGDESCR_OFFSETINFOTYPE_LIBELEMENT: /* 0x00 */
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType2: %u", vlq_value);
            offset += octet_count;
            break;
        case S7COMMP_TAGDESCR_OFFSETINFOTYPE_BOOLINUDT: /* 0x01 */
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType2: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "BitOffsetType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "BitOffsetType2: %u", vlq_value);
            offset += octet_count;
            break;
        case S7COMMP_TAGDESCR_OFFSETINFOTYPE_ARRAYINSTRUCT: /* 0x02 */
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType2: %u", vlq_value);
            offset += octet_count;
            svlq_value = tvb_get_varint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "Array lower bounds : %d", svlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "Array element count: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "PaddingType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "PaddingType2: %u", vlq_value);
            offset += octet_count;
            break;
        case S7COMMP_TAGDESCR_OFFSETINFOTYPE_PLAINSTATIC: /* 0x04 */
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(offsetinfo_tree, hf_s7commp_tagdescr_accessability, tvb, offset, octet_count, vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(offsetinfo_tree, hf_s7commp_tagdescr_section, tvb, offset, octet_count, vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType2: %u", vlq_value);
            offset += octet_count;
            break;
        case S7COMMP_TAGDESCR_OFFSETINFOTYPE_BOOL: /* 0x05 */
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(offsetinfo_tree, hf_s7commp_tagdescr_accessability, tvb, offset, octet_count, vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(offsetinfo_tree, hf_s7commp_tagdescr_section, tvb, offset, octet_count, vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType2: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "BitOffsetType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "BitOffsetType2: %u", vlq_value);
            offset += octet_count;
            break;
        case S7COMMP_TAGDESCR_OFFSETINFOTYPE_ARRAY: /* 0x06*/
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(offsetinfo_tree, hf_s7commp_tagdescr_accessability, tvb, offset, octet_count, vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(offsetinfo_tree, hf_s7commp_tagdescr_section, tvb, offset, octet_count, vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType2: %u", vlq_value);
            offset += octet_count;
            svlq_value = tvb_get_varint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "Array lower bounds : %d", svlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "Array element count: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "PaddingType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "PaddingType2: %u", vlq_value);
            offset += octet_count;
            break;
        case S7COMMP_TAGDESCR_OFFSETINFOTYPE_MULTIDIMARRAY: /* 0x07 */
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(offsetinfo_tree, hf_s7commp_tagdescr_accessability, tvb, offset, octet_count, vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(offsetinfo_tree, hf_s7commp_tagdescr_section, tvb, offset, octet_count, vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "OffsetType2: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "PaddingType1: %u", vlq_value);
            offset += octet_count;
            vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "PaddingType2: %u", vlq_value);
            offset += octet_count;
            number_of_array_dimensions = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "Number of array dimensions: %u", number_of_array_dimensions);
            offset += octet_count;
            for (array_dimension = 1; array_dimension <= number_of_array_dimensions; array_dimension++) {
                svlq_value = tvb_get_varint32(tvb, &octet_count, offset);
                proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "Array [Dimension %d] lower bounds : %d", array_dimension, svlq_value);
                offset += octet_count;
                vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
                proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "Array [Dimension %d] element count: %u", array_dimension, vlq_value);
                offset += octet_count;
            }
            break;
        default:
            /* use search algorithm for unknown types */
            i = 1;
            element_id = tvb_get_guint8(tvb, offset);
            while (element_id != S7COMMP_ITEMVAL_ELEMENTID_TERMTAGDESC) {
                vlq_value = tvb_get_varuint32(tvb, &octet_count, offset);
                proto_tree_add_text(offsetinfo_tree, tvb, offset, octet_count, "Unknown-VLQ [%d]: %u", i++, vlq_value);
                offset += octet_count;
                element_id = tvb_get_guint8(tvb, offset);
            }
            break;
    }
    proto_item_set_len(offsetinfo_tree, offset - start_offset);
    return offset;
}
/*******************************************************************************************************
 *
 * Decodes a list of following fields per set: Syntax-ID, ID, datatype-flags, datatype, value
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_object(tvbuff_t *tvb,
                      proto_tree *tree,
                      guint32 offset)
{
    proto_item *data_item = NULL;
    proto_tree *data_item_tree = NULL;
    guint32 start_offset;
    guint32 uint32_value;
    guint16 data_len;
    guint8 octet_count = 0;
    guint8 element_id;
    gboolean terminate = FALSE;

    do {
        start_offset = offset;
        element_id = tvb_get_guint8(tvb, offset);
        switch (element_id) {
            case S7COMMP_ITEMVAL_ELEMENTID_STARTOBJECT:              /* 0xa1 */
                data_item = proto_tree_add_item(tree, hf_s7commp_element_object, tvb, offset, -1, FALSE);
                data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_element_object);
                proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_elementid, tvb, offset, 1, element_id);
                offset += 1;
                uint32_value = tvb_get_ntohl(tvb, offset);
                proto_tree_add_text(data_item_tree, tvb, offset, 4, "Relation Id: %s (%u)", val_to_str_ext(uint32_value, &id_number_names_ext, "Unknown"), uint32_value);
                offset += 4;
                uint32_value = tvb_get_varuint32(tvb, &octet_count, offset);
                proto_tree_add_text(data_item_tree, tvb, offset, octet_count, "Class Id: %s (%u)", val_to_str_ext(uint32_value, &id_number_names_ext, "Unknown"), uint32_value);
                offset += octet_count;
                uint32_value = tvb_get_varuint32(tvb, &octet_count, offset);
                proto_tree_add_text(data_item_tree, tvb, offset, octet_count, "Class Id Flags: 0x%08x", uint32_value);
                offset += octet_count;
                uint32_value = tvb_get_varuint32(tvb, &octet_count, offset);
                if (uint32_value != 0) {
                    proto_tree_add_text(data_item_tree, tvb, offset, octet_count, "Attribute Id: %s (%u)", val_to_str_ext(uint32_value, &id_number_names_ext, "Unknown"), uint32_value);
                    offset += octet_count;
                    uint32_value = tvb_get_varuint32(tvb, &octet_count, offset);
                    proto_tree_add_text(data_item_tree, tvb, offset, octet_count, "Attribute Id Flags: 0x%08x", uint32_value);
                    offset += octet_count;
                } else {
                    proto_tree_add_text(data_item_tree, tvb, offset, octet_count, "Attribute Id: None (%u)", uint32_value);
                    offset += octet_count;
                }
                offset = s7commp_decode_object(tvb, data_item_tree, offset);
                proto_item_set_len(data_item_tree, offset - start_offset);
                break;
            case S7COMMP_ITEMVAL_ELEMENTID_TERMOBJECT:               /* 0xa2 */
                proto_tree_add_uint(tree, hf_s7commp_itemval_elementid, tvb, offset, 1, element_id);
                offset += 1;
                terminate = TRUE;
                break;
            case S7COMMP_ITEMVAL_ELEMENTID_RELATION:                 /* 0xa4 */
                data_item = proto_tree_add_item(tree, hf_s7commp_element_relation, tvb, offset, -1, FALSE);
                data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_element_relation);
                proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_elementid, tvb, offset, 1, element_id);
                offset += 1;
                uint32_value = tvb_get_varuint32(tvb, &octet_count, offset);
                proto_tree_add_text(data_item_tree, tvb, offset, octet_count, "Relation Id: %s (%u)", val_to_str_ext(uint32_value, &id_number_names_ext, "Unknown"), uint32_value);
                offset += octet_count;
                proto_tree_add_text(data_item_tree, tvb, offset, 4, "Unknown Value 1: 0x%08x", tvb_get_ntohl(tvb, offset));
                offset += 4;
                proto_item_set_len(data_item_tree, offset - start_offset);
                break;
            case S7COMMP_ITEMVAL_ELEMENTID_STARTTAGDESC:             /* 0xa7 */
                data_item = proto_tree_add_item(tree, hf_s7commp_element_tagdescription, tvb, offset, -1, FALSE);
                data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_element_tagdescription);
                proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_elementid, tvb, offset, 1, element_id);
                offset += 1;
                offset = s7commp_decode_tagdescription(tvb, data_item_tree, offset);
                proto_item_set_len(data_item_tree, offset - start_offset);
                break;
            case S7COMMP_ITEMVAL_ELEMENTID_TERMTAGDESC:              /* 0xa8 */
                proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_elementid, tvb, offset, 1, element_id);
                offset += 1;
                proto_item_set_len(data_item_tree, offset - start_offset);
                break;
            case S7COMMP_ITEMVAL_ELEMENTID_BLOCK0xAB:                /* 0xab */
            case S7COMMP_ITEMVAL_ELEMENTID_BLOCK0xAC:                /* 0xac */
                data_item = proto_tree_add_item(tree, hf_s7commp_element_block, tvb, offset, -1, FALSE);
                data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_element_block);
                proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_elementid, tvb, offset, 1, element_id);
                offset += 1;
                if (element_id == S7COMMP_ITEMVAL_ELEMENTID_BLOCK0xAB) {
                    proto_item_append_text(data_item_tree, ": Block0xAB");
                } else {
                    proto_item_append_text(data_item_tree, ": Block0xAC");
                }
                data_len = tvb_get_ntohs(tvb, offset);
                proto_tree_add_text(data_item_tree, tvb, offset, 2, "Block length: %d", data_len);
                offset += 2;
                proto_tree_add_bytes(data_item_tree, hf_s7commp_data_data, tvb, offset, data_len, tvb_get_ptr(tvb, offset, data_len));
                offset += data_len;
                proto_tree_add_text(data_item_tree, tvb, offset, 2, "Unknown 2 trailing bytes: 0x%04x", tvb_get_ntohs(tvb, offset));
                offset += 2;
                proto_item_set_len(data_item_tree, offset - start_offset);
                break;
            case S7COMMP_ITEMVAL_ELEMENTID_ATTRIBUTE:                /* 0xa3 */
                data_item = proto_tree_add_item(tree, hf_s7commp_element_attribute, tvb, offset, -1, FALSE);
                data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_element_attribute);
                proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_elementid, tvb, offset, 1, element_id);
                offset += 1;
                offset = s7commp_decode_id_value_list(tvb, data_item_tree, offset, FALSE);
                proto_item_set_len(data_item_tree, offset - start_offset);
                break;
            default:
                terminate = TRUE;
        }
    } while (terminate == FALSE);

    return offset;
}

/*******************************************************************************************************
 *
 * Request CreateObject
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_createobject(tvbuff_t *tvb,
                                    proto_tree *tree,
                                    guint32 offset,
                                    guint8 pdutype)
{
    int struct_level = 1;
    guint32 start_offset;
    guint32 id_number;
    proto_item *data_item = NULL;
    proto_tree *data_item_tree = NULL;
    guint8 next_byte;
    guint8 octet_count = 0;
    guint32 value = 0;

    start_offset = offset;
    data_item = proto_tree_add_item(tree, hf_s7commp_data_item_value, tvb, offset, -1, FALSE);
    data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_data_item);
    id_number = tvb_get_ntohl(tvb, offset);
    proto_tree_add_uint(data_item_tree, hf_s7commp_data_id_number, tvb, offset, 4, id_number);
    proto_item_append_text(data_item_tree, " [%u]:", id_number);
    offset += 4;
    offset = s7commp_decode_value(tvb, data_item_tree, offset, &struct_level);
    proto_item_set_len(data_item_tree, offset - start_offset);
    /* es folgen noch 4 Null-Bytes */
    proto_tree_add_text(tree, tvb, offset, 4, "Unknown value 1: 0x%08x", tvb_get_ntohl(tvb, offset));
    offset += 4;

    /* Es gibt keine bekannte Möglichkeit anhand der vorigen Werte festzustellen, ob hier noch ein eingeschobener Wert (VLQ) folgt.
     * Dieser zusätzliche Wert ist so wie es aussieht nur bei einer 1500 vorhanden.
     * Darum wird geprüft, ob der nächste Wert nicht ein Objekt-Anfang darstellt.
     * Das eingeschobene Byte ist aber definitiv nur bei Data-Telegrammen vorhanden.
     */
    next_byte = tvb_get_guint8(tvb, offset);
    if (((pdutype == S7COMMP_PDUTYPE_DATA) || (pdutype == S7COMMP_PDUTYPE_DATAFW1_5)) && next_byte != S7COMMP_ITEMVAL_ELEMENTID_STARTOBJECT) {
        value = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_text(tree, tvb, offset, octet_count, "Unknown VLQ-Value in Data-CreateObject: %u", value);
        offset += octet_count;
    }
    return s7commp_decode_object(tvb, tree, offset);
}
/*******************************************************************************************************
 *
 * Response CreateObject
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_createobject(tvbuff_t *tvb,
                                    proto_tree *tree,
                                    guint32 offset,
                                    guint8 pdutype)
{
    guint8 object_id_count = 0;
    guint8 octet_count = 0;
    guint32 object_id = 0;
    int i;

    offset = s7commp_decode_returnvalue(tvb, tree, offset, NULL);
    object_id_count = tvb_get_guint8(tvb, offset);
    proto_tree_add_text(tree, tvb, offset, 1, "Number of following Object Ids: %d", object_id_count);
    offset += 1;
    for (i = 1; i <= object_id_count; i++) {
        object_id = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_text(tree, tvb, offset, octet_count, "Object Id [%i]: 0x%08x", i, object_id);
        offset += octet_count;
    }
    /* Ein Daten-Objekt gibt es nur beim Connect */
    if (pdutype == S7COMMP_PDUTYPE_CONNECT) {
        offset = s7commp_decode_object(tvb, tree, offset);
    }
    return offset;
}
/*******************************************************************************************************
 *
 * Request DeleteObject
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_deleteobject(tvbuff_t *tvb,
                                    packet_info *pinfo,
                                    proto_tree *tree,
                                    guint32 offset)
{
    guint32 object_id;
    object_id = tvb_get_ntohl(tvb, offset);
    proto_tree_add_text(tree, tvb, offset, 4, "Delete Object Id: 0x%08x", object_id);
    col_append_fstr(pinfo->cinfo, COL_INFO, " ObjId=0x%08x", object_id);
    offset += 4;

    return offset;
}
/*******************************************************************************************************
 *
 * Response DeleteObject
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_deleteobject(tvbuff_t *tvb,
                                     packet_info *pinfo,
                                     proto_tree *tree,
                                     guint32 offset,
                                     gboolean *has_integrity_id)
{
    guint32 object_id;
    offset = s7commp_decode_returnvalue(tvb, tree, offset, NULL);
    object_id = tvb_get_ntohl(tvb, offset);
    proto_tree_add_text(tree, tvb, offset, 4, "Delete Object Id: 0x%08x", object_id);
    col_append_fstr(pinfo->cinfo, COL_INFO, " ObjId=0x%08x", object_id);
    /* If the id is < 0x7000000 there is no integrity-id in integrity dataset at the end (only for 1500) */
    if (object_id > 0x70000000) {
        *has_integrity_id = TRUE;
    } else {
        *has_integrity_id = FALSE;
    }
    offset += 4;
    return offset;
}
/*******************************************************************************************************
 *
 * Decodes a plc address
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_item_address(tvbuff_t *tvb,
                            proto_tree *tree,
                            guint32 *number_of_fields,
                            guint32 offset)
{
    proto_item *adr_item = NULL;
    proto_tree *adr_item_tree = NULL;
    proto_item *area_item = NULL;
    proto_item *area_item_tree = NULL;

    guint8 octet_count = 0;
    guint32 value = 0;
    guint32 crc = 0;
    guint16 tia_var_area1 = 0;
    guint16 tia_var_area2 = 0;
    guint32 tia_lid_nest_depth = 0;
    guint32 tia_lid_cnt = 0;
    guint32 offset_at_start = offset;
    gboolean is_crc_lid_address = TRUE;

    *number_of_fields = 0;

    adr_item = proto_tree_add_item(tree, hf_s7commp_data_item_address, tvb, offset, -1, FALSE);
    adr_item_tree = proto_item_add_subtree(adr_item, ett_s7commp_data_item);

    /**************************************************************
     * CRC als varuint
     *
     * Es gibt mindestens zwei Interpretationsarten:
     * 1) Symbolischer Zugriff vom HMI über CRC und LID
     * 2) Zugriff auf Objekte über AID/RID
     *
     * Vom Aufbau her sind beide Adressen identisch nur die Interpretation ist eine andere. Die Unterscheidung
     * erfolgt anhand des ersten Feldes (crc). Ist dieses Null, dann ist es eine Object ID Interpretation.
     */
    crc = tvb_get_varuint32(tvb, &octet_count, offset);
    if (crc == 0) {
        is_crc_lid_address = FALSE;
    }
    proto_tree_add_uint(adr_item_tree, hf_s7commp_itemaddr_crc, tvb, offset, octet_count, crc);
    offset += octet_count;
    if (is_crc_lid_address) {
        proto_item_append_text(adr_item_tree, ": SYM-CRC=%08x", crc);
    } else {
        proto_item_append_text(adr_item_tree, " by IDs:");
    }

    *number_of_fields += 1;
    /**************************************************************
     * Area 52=Merker usw.
     * when Bytes 2/3 == 0x8a0e then bytes 4/5 are containing the DB number
     */
    value = tvb_get_varuint32(tvb, &octet_count, offset);
    if (is_crc_lid_address) {
        area_item = proto_tree_add_uint(adr_item_tree, hf_s7commp_itemaddr_area, tvb, offset, octet_count, value);
        area_item_tree = proto_item_add_subtree(area_item, ett_s7commp_itemaddr_area);
        tia_var_area1 = (value >> 16);
        tia_var_area2 = (value & 0xffff);
        proto_tree_add_uint(area_item_tree, hf_s7commp_itemaddr_area1, tvb, offset, octet_count, tia_var_area1);
        if (tia_var_area1 == S7COMMP_VAR_ITEM_AREA1_IQMCT) {
            proto_tree_add_uint(area_item_tree, hf_s7commp_itemaddr_area2, tvb, offset, octet_count, tia_var_area2);
            proto_item_append_text(area_item_tree, " (%s)", val_to_str(tia_var_area2, var_item_area2_names, "Unknown IQMCT Area: 0x%04x"));
            proto_item_append_text(adr_item_tree, ", LID=%s", val_to_str(tia_var_area2, var_item_area2_names_short, "Unknown IQMCT Area: 0x%04x"));
        } else if (tia_var_area1 == S7COMMP_VAR_ITEM_AREA1_DB) {
            proto_tree_add_uint(area_item_tree, hf_s7commp_itemaddr_dbnumber, tvb, offset, octet_count, tia_var_area2);
            proto_item_append_text(area_item_tree, " (Datablock, DB-Number: %u)", tia_var_area2);
            proto_item_append_text(adr_item_tree, ", LID=DB%u", tia_var_area2);
        } else {
            proto_tree_add_text(area_item_tree, tvb, offset, octet_count, "Unknown Area: 0x%04x / 0x%04x", tia_var_area1, tia_var_area2);
            proto_item_append_text(adr_item_tree, " Unknown Area 0x%04x / 0x%04x", tia_var_area1, tia_var_area2);
        }
    } else {
        proto_tree_add_uint(adr_item_tree, hf_s7commp_data_id_number, tvb, offset, octet_count, value);
        proto_item_append_text(adr_item_tree, " RID=%u", value);
    }
    offset += octet_count;

    *number_of_fields += 1;

    /**************************************************************
     * LID Nesting Depth
     *
     * 0x01: Merker                 Folgende LIDs: 1
     * 0x02: DB.VAR                 Folgende LIDs: 1
     * 0x03: DB.STRUCT.VAR          Folgende LIDs: 2
     * 0x03: DB.ARRAY[INDEX]        Folgende LIDs: 2
     * 0x04: DB.STRUCT.STRUCT.VAR   Folgende LIDs: 3
     */
    tia_lid_nest_depth = tvb_get_varuint32(tvb, &octet_count, offset);
    if (is_crc_lid_address) {
        proto_tree_add_uint(adr_item_tree, hf_s7commp_itemaddr_lid_nesting_depth, tvb, offset, octet_count, tia_lid_nest_depth);
    } else {
        proto_tree_add_text(adr_item_tree, tvb, offset, octet_count, "Number of following IDs: %u", tia_lid_nest_depth);
    }
    offset += octet_count;
    *number_of_fields += 1;

    /**************************************************************
     * Nochmal Angabe des Speicherbereichs.
     * Bei Merkern scheint hier 0xe98 zu stehen, bei DBs 0x9f6
     * Es gibt noch weitere Bereiche, deren Bedeutung z.Zt. unbekannt ist (Systemdaten? äquivalent zu bisherigen SZL?)
     */
    value = tvb_get_varuint32(tvb, &octet_count, offset);
    if (is_crc_lid_address) {
        proto_tree_add_uint(adr_item_tree, hf_s7commp_itemaddr_base_area, tvb, offset, octet_count, value);
    } else {
        proto_tree_add_uint(adr_item_tree, hf_s7commp_data_id_number, tvb, offset, octet_count, value);
        proto_item_append_text(adr_item_tree, ", ID=%u", value);
    }
    offset += octet_count;

    *number_of_fields += 1;

    /**************************************************************
     * LID pro Nest-Level
     *
     */
    for (tia_lid_cnt = 2; tia_lid_cnt <= tia_lid_nest_depth; tia_lid_cnt++) {
        value = tvb_get_varuint32(tvb, &octet_count, offset);
        if (is_crc_lid_address) {
            proto_tree_add_uint(adr_item_tree, hf_s7commp_itemaddr_lid_value, tvb, offset, octet_count, value);
            proto_item_append_text(adr_item_tree, ".%u", value);
        } else {
            proto_tree_add_uint(adr_item_tree, hf_s7commp_data_id_number, tvb, offset, octet_count, value);
            proto_item_append_text(adr_item_tree, ", ID=%u", value);
        }
        offset += octet_count;
        *number_of_fields += 1;
    }
    proto_item_set_len(adr_item_tree, offset - offset_at_start);
    return offset;
}
/*******************************************************************************************************
 *
 * Request SetMultiVariables
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_setmultivar(tvbuff_t *tvb,
                                   packet_info *pinfo,
                                   proto_tree *tree,
                                   gint16 dlength,
                                   guint32 offset)
{
    guint32 item_count = 0;
    guint32 number_of_fields_in_complete_set = 0;
    guint32 i = 0;
    guint32 number_of_fields = 0;
    guint32 value;
    guint8 octet_count = 0;
    guint32 item_address_count;
    guint32 id_number;
    proto_item *list_item = NULL;
    proto_tree *list_item_tree = NULL;
    guint32 list_start_offset;

    /* Wenn die ersten 4 Bytes 0x00, dann ist es ein 'normaler' Schreib-Befehl
     * Es kann sein dass hier die Session-ID steht, dann ist der Aufbau anders
     */
    value = tvb_get_ntohl(tvb, offset);
    offset += 4;

    if (value == 0) {
        proto_tree_add_text(tree, tvb, offset-4, 4, "Unknown: 0x%08x", value);
        item_count = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_uint(tree, hf_s7commp_item_count, tvb, offset, octet_count, item_count);
        offset += octet_count;

        number_of_fields_in_complete_set = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_uint(tree, hf_s7commp_item_no_of_fields, tvb, offset, octet_count, number_of_fields_in_complete_set);
        offset += octet_count;
        /* Es lassen sich mehrere Variablen mit einem write schreiben.
         * Dann folgen erst die Adressen und dann die Werte.
         */
        list_start_offset = offset;
        list_item = proto_tree_add_item(tree, hf_s7commp_addresslist, tvb, offset, -1, FALSE);
        list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_addresslist);
        for (i = 1; i <= item_count; i++) {
            offset = s7commp_decode_item_address(tvb, list_item_tree, &number_of_fields, offset);
            number_of_fields_in_complete_set -= number_of_fields;
        }
        proto_item_set_len(list_item_tree, offset - list_start_offset);

        list_start_offset = offset;
        list_item = proto_tree_add_item(tree, hf_s7commp_valuelist, tvb, offset, -1, FALSE);
        list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_valuelist);
        for (i = 1; i <= item_count; i++) {
            offset = s7commp_decode_itemnumber_value_list(tvb, list_item_tree, offset, FALSE);
        }
        proto_item_set_len(list_item_tree, offset - list_start_offset);
    } else {
        proto_tree_add_text(tree, tvb, offset-4, 4, "Set Variables in Object Id : 0x%08x", value);
        col_append_fstr(pinfo->cinfo, COL_INFO, " ObjId=0x%08x", value);
        item_count = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_text(tree, tvb, offset, octet_count, "Item count: %u", item_count);
        offset += octet_count;
        item_address_count = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_text(tree, tvb, offset, octet_count, "Item address count: %d", item_address_count);
        offset += octet_count;

        list_start_offset = offset;
        list_item = proto_tree_add_item(tree, hf_s7commp_addresslist, tvb, offset, -1, FALSE);
        list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_addresslist);
        for (i = 1; i <= item_address_count; i++) {
            id_number = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(list_item_tree, hf_s7commp_data_id_number, tvb, offset, octet_count, id_number);
            offset += octet_count;
        }
        proto_item_set_len(list_item_tree, offset - list_start_offset);

        list_start_offset = offset;
        list_item = proto_tree_add_item(tree, hf_s7commp_valuelist, tvb, offset, -1, FALSE);
        list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_valuelist);
        for (i = 1; i <= item_count; i++) {
            offset = s7commp_decode_itemnumber_value_list(tvb, list_item_tree, offset, FALSE);
        }
        proto_item_set_len(list_item_tree, offset - list_start_offset);
    }
    return offset;
}
/*******************************************************************************************************
 *
 * Request GetMultiVariables
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_getmultivar(tvbuff_t *tvb,
                                   proto_tree *tree,
                                   guint32 offset)
{
    guint32 item_count = 0;
    guint32 number_of_fields_in_complete_set = 0;
    guint8 i = 0;
    guint32 number_of_fields = 0;
    guint32 value;
    guint8 octet_count = 0;
    guint32 id_number;
    guint32 item_address_count;
    proto_item *list_item = NULL;
    proto_tree *list_item_tree = NULL;
    guint32 list_start_offset;

    /* Für Variablen-Lesen müssen die ersten 4 Bytes 0 sein. Ansonsten ist es eine Link-Id.
     */
    value = tvb_get_ntohl(tvb, offset);
    proto_tree_add_text(tree, tvb, offset, 4, "%s: 0x%08x", (value == 0) ? "Unknown" : "Link-Id", value);
    offset += 4;
    item_count = tvb_get_varuint32(tvb, &octet_count, offset);
    proto_tree_add_uint(tree, hf_s7commp_item_count, tvb, offset, octet_count, item_count);
    offset += octet_count;
    if (value == 0x0) {
        number_of_fields_in_complete_set = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_uint(tree, hf_s7commp_item_no_of_fields, tvb, offset, octet_count, number_of_fields_in_complete_set);
        offset += octet_count;
        list_start_offset = offset;
        list_item = proto_tree_add_item(tree, hf_s7commp_addresslist, tvb, offset, -1, FALSE);
        list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_addresslist);
        for (i = 1; i <= item_count; i++) {
            offset = s7commp_decode_item_address(tvb, list_item_tree, &number_of_fields, offset);
            number_of_fields_in_complete_set -= number_of_fields;
        }
        proto_item_set_len(list_item_tree, offset - list_start_offset);
    } else {
        item_address_count = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_text(tree, tvb, offset, octet_count, "Item address count: %d", item_address_count);
        offset += octet_count;
        list_start_offset = offset;
        list_item = proto_tree_add_item(tree, hf_s7commp_addresslist, tvb, offset, -1, FALSE);
        list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_addresslist);
        for (i = 1; i <= item_address_count; i++) {
            id_number = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(list_item_tree, hf_s7commp_data_id_number, tvb, offset, octet_count, id_number);
            offset += octet_count;
        }
        proto_item_set_len(list_item_tree, offset - list_start_offset);
    }

    return offset;
}
/*******************************************************************************************************
 *
 * Response GetMultiVariables
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_getmultivar(tvbuff_t *tvb,
                                    proto_tree *tree,
                                    guint32 offset)
{
    offset = s7commp_decode_returnvalue(tvb, tree, offset, NULL);
    offset = s7commp_decode_itemnumber_value_list_in_new_tree(tvb, tree, offset, TRUE);
    offset = s7commp_decode_itemnumber_errorvalue_list(tvb, tree, offset);

    return offset;
}
/*******************************************************************************************************
 *
 * Response SetMultiVariables
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_setmultivar(tvbuff_t *tvb,
                                    proto_tree *tree,
                                    guint32 offset)
{
    /* Der Unterschied zum Read-Response ist, dass man hier sofort im Fehlerbereich ist wenn das erste Byte != 0.
     * Ein erfolgreiches Schreiben einzelner Werte scheint nicht extra bestätigt zu werden.
     */

    offset = s7commp_decode_returnvalue(tvb, tree, offset, NULL);
    offset = s7commp_decode_itemnumber_errorvalue_list(tvb, tree, offset);
    return offset;
}

/*******************************************************************************************************
 *
 * Notification Value List
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_notification_value_list(tvbuff_t *tvb,
                                       proto_tree *tree,
                                       guint32 offset,
                                       gboolean looping)
{
    proto_item *data_item = NULL;
    proto_tree *data_item_tree = NULL;
    guint32 item_number;
    guint32 start_offset;
    guint8 octet_count;
    guint8 item_return_value;
    int struct_level;
    /* Return value: Ist der Wert ungleich 0, dann folgt ein Datensatz mit dem bekannten
     * Aufbau aus den anderen Telegrammen.
     * Liegt ein Adressfehler vor, so werden hier auch Fehlerwerte übertragen. Dann ist Datatype=NULL
     * Folgende Rückgabewerte wurden gesichtet:
     *  0x13 -> Fehler bei einer Adresse (S7-1200)
     *  0x92 -> Erfolg (S7-1200)
     *  0x9c -> Bei Beobachtung mit einer Variablentabelle (S7-1200), Aufbau scheint dann anders zu sein
     *  0x9b -> Bei 1500 gesehen. Es folgt eine ID oder Nummer, dann flag, typ, wert.
     * Danach können noch weitere Daten folgen, deren Aufbau bisher nicht bekannt ist.
     */
    do {
        struct_level = 0;
        item_return_value = tvb_get_guint8(tvb, offset);
        if (item_return_value == 0) {
            proto_tree_add_text(tree, tvb, offset, 1, "Terminating Item/List");
            offset += 1;
            return offset;
        } else {
            start_offset = offset;
            data_item = proto_tree_add_item(tree, hf_s7commp_data_item_value, tvb, offset, -1, FALSE);
            data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_data_item);
            proto_tree_add_text(data_item_tree, tvb, offset, 1, "Return value: 0x%02x", item_return_value);
            offset += 1;
            if (item_return_value == 0x92) {
                /* Item reference number. Is sent to plc on the subscription-telegram for the addresses. */
                item_number = tvb_get_ntohl(tvb, offset);
                proto_tree_add_text(data_item_tree, tvb, offset, 4, "Item reference number: %u", item_number);
                offset += 4;
                proto_item_append_text(data_item_tree, " [%u]:", item_number);
                offset = s7commp_decode_value(tvb, data_item_tree, offset, &struct_level);
            } else if (item_return_value == 0x9b) {
                item_number = tvb_get_varuint32(tvb, &octet_count, offset);
                proto_tree_add_uint(data_item_tree, hf_s7commp_data_id_number, tvb, offset, octet_count, item_number);
                offset += octet_count;
                proto_item_append_text(data_item_tree, " [%u]:", item_number);
                offset = s7commp_decode_value(tvb, data_item_tree, offset, &struct_level);
            } else if (item_return_value == 0x9c) {
                item_number = tvb_get_ntohl(tvb, offset);
                proto_tree_add_text(data_item_tree, tvb, offset, 4, "Unknown value after value 0x9c: 0x%08x", item_number);
                proto_item_append_text(data_item_tree, " Returncode 0x9c, Value: 0x%08x", item_number);
                offset += 4;
            } else if (item_return_value == 0x13) {
                item_number = tvb_get_ntohl(tvb, offset);
                proto_tree_add_text(data_item_tree, tvb, offset, 4, "Item reference number: %u", item_number);
                proto_item_append_text(data_item_tree, " [%u]: Access error", item_number);
                offset += 4;
            } else {
                proto_item_append_text(data_item_tree, " Don't know how to decode the values with return code 0x%02x, stop decoding", item_return_value);
                proto_item_set_len(data_item_tree, offset - start_offset);
                break;
            }
            if (struct_level > 0) {
                offset = s7commp_decode_notification_value_list(tvb, data_item_tree, offset, TRUE);
            }
            proto_item_set_len(data_item_tree, offset - start_offset);
        }
    } while (looping);
    return offset;
}
/*******************************************************************************************************
 *
 * Notification
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_notification(tvbuff_t *tvb,
                            packet_info *pinfo,
                            proto_tree *tree,
                            guint32 offset)
{
    guint16 unknown2;
    guint32 subscr_object_id, subscr_object_id2;
    guint8 credit_tick;
    guint32 seqnum;
    guint8 item_return_value;
    proto_item *list_item = NULL;
    proto_tree *list_item_tree = NULL;
    guint8 octet_count = 0;
    gboolean add_data_info_column = FALSE;
    guint32 list_start_offset;

    /* 4 Bytes Subscription Object Id */
    subscr_object_id = tvb_get_ntohl(tvb, offset);
    proto_tree_add_text(tree, tvb, offset, 4, "Subscription Object Id: 0x%08x", subscr_object_id);
    col_append_fstr(pinfo->cinfo, COL_INFO, " ObjId=0x%08x", subscr_object_id);
    offset += 4;

    /* 6/7: Unbekannt */
    unknown2 = tvb_get_ntohs(tvb, offset);
    proto_tree_add_text(tree, tvb, offset, 2, "Unknown 2: 0x%04x", unknown2);
    offset += 2;

    proto_tree_add_text(tree, tvb, offset, 2, "Unknown 3: 0x%04x", tvb_get_ntohs(tvb, offset));
    offset += 2;

    if (unknown2 == 0x0400) {
        /* Bei V13 und einer 1200 werden hiermit Daten vom HMI zyklisch
         * bei Änderung übermittelt. Daten sind nur enthalten wenn sich etwas ändert.
         * Sonst gibt es ein verkürztes (Status?)-Telegramm.
         */
        proto_tree_add_text(tree, tvb, offset, 2, "Unknown 4: 0x%04x", tvb_get_ntohs(tvb, offset));
        offset += 2;

        /* Es gibt zwei Nummern:
         * 1) Nummerierung für Creditlimit: Wird bei Aufbau der notification-session ein Wert angegeben, so erfolgt die Übertragung
         *                                  bis zur in modifiy-session angegebenen Limit.
         * 2) Sequenznummer: Wurde beim Session-Aufbau -1 angegeben, so ist die Zahl bei 1) Null, und es folgt hier eine aufsteigende Nummer.
         *
         * Bei der Sequenznummer scheint es einen Unterschied zwischen 1200 und 1500 zu geben.
         * Bei der 1200 ist diese immer nur 1 Byte., bei der 1500 ist es ein VLQ!
         * Es scheint abhängig von der ersten ID zu sein. Ist diese größer 0x7000000 dann ist es ein VLQ und zusätzlich
         * gibt es noch drei eingeschobene Bytes am Ende.
         * Es scheint generell so, dass eine 1200 IDs beginnend mit 0x1.. und eine 1500 mit 0x7.. verwendet.
         */
        credit_tick = tvb_get_guint8(tvb, offset);
        proto_tree_add_text(tree, tvb, offset, 1, "Notification Credit tickcount: %u", credit_tick);
        offset += 1;
        if (subscr_object_id > 0x70000000) {
            seqnum = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_text(tree, tvb, offset, octet_count, "Notification sequence number (VLQ): %u", seqnum);
            offset += octet_count;
        } else {
            seqnum = tvb_get_guint8(tvb, offset);
            proto_tree_add_text(tree, tvb, offset, 1, "Notification sequence number: %u", seqnum);
            offset += 1;
        }
        col_append_fstr(pinfo->cinfo, COL_INFO, ", Ctick=%u", credit_tick);
        col_append_fstr(pinfo->cinfo, COL_INFO, ", NSeq=%u", seqnum);

        item_return_value = tvb_get_guint8(tvb, offset);
        if (item_return_value != 0x00 && item_return_value < 0x10) {    /* sehr speziell... */
            proto_tree_add_text(tree, tvb, offset, 1, "Unknown 5: 0x%02x", item_return_value);
            offset += 1;
        }

        list_item = proto_tree_add_item(tree, hf_s7commp_valuelist, tvb, offset, -1, FALSE);
        list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_valuelist);
        list_start_offset = offset;
        offset = s7commp_decode_notification_value_list(tvb, list_item_tree, offset, TRUE);
        proto_item_set_len(list_item_tree, offset - list_start_offset);
        if (offset - list_start_offset > 1) {
            add_data_info_column = TRUE;
        }

        /* Noch ein spezial Datensatz, mit ein paar unbekannten Werten davor und einer Standard Objekt-Datenstruktur. */
        if (tvb_get_guint8(tvb, offset) != 0) {
            subscr_object_id2 = tvb_get_ntohl(tvb, offset);
            if (subscr_object_id2 != 0) {
                proto_tree_add_text(tree, tvb, offset, 4, "Part 2 - Subscription Object Id: 0x%08x", subscr_object_id2);
                offset += 4;
                proto_tree_add_text(tree, tvb, offset, 2, "Part 2 - Unknown 1: 0x%04x", tvb_get_ntohs(tvb, offset));
                offset += 2;
                proto_tree_add_text(tree, tvb, offset, 1, "Part 2 - Unknown 2: 0x%02x", tvb_get_guint8(tvb, offset));
                offset += 1;
                if (tvb_get_guint8(tvb, offset) == S7COMMP_ITEMVAL_ELEMENTID_STARTOBJECT) {
                    offset =  s7commp_decode_object(tvb, tree, offset);
                    add_data_info_column = TRUE;
                }
            }
        }
        /* Nur wenn die id > 0x70000000 dann folgen noch 3 Bytes, die bisher immer null waren. */
        if (subscr_object_id > 0x70000000) {
            proto_tree_add_text(tree, tvb, offset, 3, "Unknown additional 3 bytes, because 1st Object ID > 0x70000000");
            offset += 3;
        }
        if (add_data_info_column) {
            col_append_fstr(pinfo->cinfo, COL_INFO, " <With data>");
        }
    }

    return offset;
}
/*******************************************************************************************************
 *
 * Request SetVariable
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_setvariable(tvbuff_t *tvb,
                                   packet_info *pinfo,
                                   proto_tree *tree,
                                   guint32 offset)
{
    guint32 object_id;
    guint8 octet_count;
    guint32 item_count;
    guint32 i;
    proto_item *list_item = NULL;
    proto_tree *list_item_tree = NULL;
    guint32 list_start_offset;

    object_id = tvb_get_ntohl(tvb, offset);
    proto_tree_add_text(tree, tvb, offset, 4, "In Object Id: 0x%08x", object_id);
    col_append_fstr(pinfo->cinfo, COL_INFO, " ObjId=0x%08x", object_id);
    offset += 4;

    item_count = tvb_get_varuint32(tvb, &octet_count, offset);
    proto_tree_add_text(tree, tvb, offset, octet_count, "Number of items following: %u", octet_count);
    offset += octet_count;
    list_start_offset = offset;
    list_item = proto_tree_add_item(tree, hf_s7commp_valuelist, tvb, offset, -1, FALSE);
    list_item_tree = proto_item_add_subtree(list_item, ett_s7commp_valuelist);
    for (i = 1; i <= item_count; i++) {
        offset = s7commp_decode_id_value_list(tvb, list_item_tree, offset, FALSE);
    }
    proto_item_set_len(list_item_tree, offset - list_start_offset);
    return offset;
}
/*******************************************************************************************************
 *
 * Response SetVariable
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_setvariable(tvbuff_t *tvb,
                                    proto_tree *tree,
                                    guint32 offset)
{
    return s7commp_decode_returnvalue(tvb, tree, offset, NULL);
}
/*******************************************************************************************************
 *
 * Request GetVarSubStreamed
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_getvarsubstr(tvbuff_t *tvb,
                                    proto_tree *tree,
                                    guint32 offset)
{
    proto_item *data_item = NULL;
    proto_tree *data_item_tree = NULL;
    guint32 id_number;
    guint32 start_offset;
    int struct_level = 0;

    do {
        id_number = tvb_get_ntohl(tvb, offset);
        if (id_number == 0) {
            struct_level--;
            proto_tree_add_text(tree, tvb, offset, 1, "Terminating Struct (Lvl:%d <- Lvl:%d)", struct_level, struct_level+1);
            offset += 4;
        } else {
            start_offset = offset;
            data_item = proto_tree_add_item(tree, hf_s7commp_data_item_value, tvb, offset, -1, FALSE);
            data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_data_item);
            proto_tree_add_uint(data_item_tree, hf_s7commp_data_id_number, tvb, offset, 4, id_number);
            proto_item_append_text(data_item_tree, " [%u]:", id_number);
            offset += 4;
            offset = s7commp_decode_value(tvb, data_item_tree, offset, &struct_level);
            proto_item_set_len(data_item_tree, offset - start_offset);
        }
    } while (struct_level > 0);

    return offset;
}
/*******************************************************************************************************
 *
 * Response GetVarSubStreamed
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_getvarsubstr(tvbuff_t *tvb,
                                     proto_tree *tree,
                                     guint32 offset)
{
    proto_item *data_item = NULL;
    proto_tree *data_item_tree = NULL;
    int struct_level = 0;
    guint32 start_offset;
    guint16 errorcode;

    offset = s7commp_decode_returnvalue(tvb, tree, offset, &errorcode);
    proto_tree_add_text(tree, tvb, offset, 1, "Response unknown 1: 0x%02x", tvb_get_guint8(tvb, offset));
    offset += 1;
    data_item = proto_tree_add_item(tree, hf_s7commp_data_item_value, tvb, offset, -1, FALSE);
    data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_data_item);
    start_offset = offset;
    /* This function should be possible to handle a Null-Value */
    offset = s7commp_decode_value(tvb, data_item_tree, offset, &struct_level);
    /* when a struct was entered, then id, flag, type follows until terminating null */
    if (struct_level > 0) {
        offset = s7commp_decode_id_value_list(tvb, data_item_tree, offset, TRUE);
    }
    proto_item_set_len(data_item_tree, offset - start_offset);

    return offset;
}
/*******************************************************************************************************
 *
 * Request GetLink
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_getlink(tvbuff_t *tvb,
                               proto_tree *tree,
                               guint32 offset)
{
    guint8 octet_count = 0;
    guint32 item_number = 0;

    /* Ein Datensatz wurde bisher nur mit 12 Bytes Länge gesichtet. Minus 4 Null-Bytes am Ende bleiben 8 Bytes
     * - 4 Bytes fix
     * - 1 VLQ
     * - 2 Nullbytes?
     */
    proto_tree_add_text(tree, tvb, offset, 4, "Request unknown 1: 0x%08x", tvb_get_ntohl(tvb, offset));
    offset += 4;

    item_number = tvb_get_varuint32(tvb, &octet_count, offset);
    proto_tree_add_uint(tree, hf_s7commp_data_id_number, tvb, offset, octet_count, item_number);
    offset += octet_count;

    proto_tree_add_text(tree, tvb, offset, 2, "Request unknown 2: 0x%04x", tvb_get_ntohs(tvb, offset));
    offset += 2;

    return offset;
}
/*******************************************************************************************************
 *
 * Response GetLink
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_getlink(tvbuff_t *tvb,
                                proto_tree *tree,
                                guint32 offset)
{
    guint16 errorcode;
    guint8 number_of_items;
    int i;

    offset = s7commp_decode_returnvalue(tvb, tree, offset, &errorcode);

    number_of_items = tvb_get_guint8(tvb, offset);
    proto_tree_add_text(tree, tvb, offset, 1, "Number of following Link-Ids: %d", number_of_items);
    offset += 1;

    for (i = 1; i <= number_of_items; i++) {
        /* Es scheint eine Link-Id zu sein, die im folgenden verwendet werden kann (z.B. Vartab als Start-Id für getmultivar */
        proto_tree_add_text(tree, tvb, offset, 4, "Link-Id [%d]: 0x%08x", i, tvb_get_ntohl(tvb, offset));
        offset += 4;
    }
    return offset;
}
/*******************************************************************************************************
 *
 * Request BeginSequence
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_beginsequence(tvbuff_t *tvb,
                                     proto_tree *tree,
                                     gint16 dlength,
                                     guint32 offset)
{
    proto_tree_add_text(tree, tvb, offset, 2, "Request unknown 1: 0x%04x", tvb_get_ntohs(tvb, offset));
    offset += 2;
    proto_tree_add_text(tree, tvb, offset, 2, "Request unknown 2: 0x%04x", tvb_get_ntohs(tvb, offset));
    offset += 2;
    proto_tree_add_text(tree, tvb, offset, 1, "Request unknown 3: 0x%02x", tvb_get_guint8(tvb, offset));
    offset += 1;

    /* Es gibt zwei Formate. Die ersten 5 bytes scheinen zumindest fix zu sein. Wie man daran erkennen kann ob
     * wie die nachfolgenden Bytes interpretiert werden sollen ist unklar. So wird erstmal geprüft ob eine entsprechende
     * Object-Start-ID folgt. Das funktioniert zumindest soweit, dass es für weitere Analysen verwendet werden kann.
     */
    if (tvb_get_guint8(tvb, offset) == S7COMMP_ITEMVAL_ELEMENTID_STARTOBJECT) {
        offset = s7commp_decode_object(tvb, tree, offset);
    } else {
        proto_tree_add_text(tree, tvb, offset, 2, "Request unknown 4: 0x%04x", tvb_get_ntohs(tvb, offset));
        offset += 2;
    }

    return offset;
}
/*******************************************************************************************************
 *
 * Response BeginSequence
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_beginsequence(tvbuff_t *tvb,
                                      proto_tree *tree,
                                      guint32 offset)
{
    guint16 errorcode;

    offset = s7commp_decode_returnvalue(tvb, tree, offset, &errorcode);
    proto_tree_add_text(tree, tvb, offset, 2, "Request unknown 1: 0x%04x", tvb_get_ntohs(tvb, offset));
    offset += 2;
    proto_tree_add_text(tree, tvb, offset, 4, "Request unknown 2: 0x%08x", tvb_get_ntohl(tvb, offset));
    offset += 4;

    return offset;
}
/*******************************************************************************************************
 *
 * Request EndSequence
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_endsequence(tvbuff_t *tvb,
                                   proto_tree *tree,
                                   guint32 offset)
{
    proto_tree_add_text(tree, tvb, offset, 2, "Request unknown 1: 0x%04x", tvb_get_ntohs(tvb, offset));
    offset += 2;

    return offset;
}
/*******************************************************************************************************
 *
 * Response EndSequence
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_endsequence(tvbuff_t *tvb,
                                    proto_tree *tree,
                                    guint32 offset)
{
    guint16 errorcode;

    offset = s7commp_decode_returnvalue(tvb, tree, offset, &errorcode);
    return offset;
}
/*******************************************************************************************************
 *
 * Request Invoke
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_invoke(tvbuff_t *tvb,
                              proto_tree *tree,
                              guint32 offset)
{
    proto_tree_add_text(tree, tvb, offset, 4, "Sub Session Id: 0x%08x", tvb_get_ntohl(tvb, offset));
    offset += 4;
    proto_tree_add_text(tree, tvb, offset, 4, "Request unknown 2: 0x%08x", tvb_get_ntohl(tvb, offset));
    offset += 4;
    offset = s7commp_decode_itemnumber_value_list_in_new_tree(tvb, tree, offset, TRUE);
    proto_tree_add_text(tree, tvb, offset, 1, "Request unknown 3: 0x%02x", tvb_get_guint8(tvb, offset));
    offset += 1;

    return offset;
}
/*******************************************************************************************************
 *
 * Response Invoke
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_invoke(tvbuff_t *tvb,
                               proto_tree *tree,
                               guint32 offset)
{
    guint16 errorcode;

    offset = s7commp_decode_returnvalue(tvb, tree, offset, &errorcode);
    offset = s7commp_decode_returnvalue(tvb, tree, offset, &errorcode);
    offset = s7commp_decode_returnvalue(tvb, tree, offset, &errorcode);
    offset = s7commp_decode_itemnumber_value_list_in_new_tree(tvb, tree, offset, TRUE);
    proto_tree_add_text(tree, tvb, offset, 1, "Response unknown 1: 0x%02x", tvb_get_guint8(tvb, offset));
    offset += 1;
    return offset;
}
/*******************************************************************************************************
 *
 * Exploration areas
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_explore_area(tvbuff_t *tvb,
                            packet_info *pinfo,
                            proto_tree *tree,
                            guint32 offset)
{
    /* Speicherbereich der durchsucht werden soll:
     * Linke 2 (1) Bytes        Rechte 2 (3) Bytes                                                  Antwort Kopf
     * ==============================================================================================================
     *  0000 0003 = Globale DBs (Liste) oder Wurzelknoten bei Programm-Download                     ASRoot / ---
     *  0000 0219 = ?                                                                               ConfiguredTypes
     *  0000 000c = ?                                                                               CommCont
     *  0200 001f = TON Instanz. Unbekannt wie die Zugehörigkeit zu einem DB/IDB hergestellt wird.
     *  9200 mmmm = Global-DB     mmmm = Global-DB-Nummer (bei 1200 maximal Nr. 59999 erlaubt)
     *    nn      = nn = Substrukturelement
     *  9300      = Instanz-DB     Nummer des FBs von dem abgeleitet wurde
     *  9001 0000 = Input area                                                                      IArea
     *  9002 0000 = Output area                                                                     QArea
     *  9003 0000 = M Bit memory                                                                    MArea
     *  9004 0000 = ?
     *  9005 0000 = ?
     *  9006 0000 = ?
     */
    proto_item *item = NULL;
    proto_tree *item_tree = NULL;
    guint32 area;
    guint8 exp_class;
    guint8 sub_class;
    guint16 section;

    area = tvb_get_ntohl(tvb, offset);
    exp_class = tvb_get_guint8(tvb, offset);
    sub_class = tvb_get_guint8(tvb, offset + 1);
    section = tvb_get_ntohs(tvb, offset + 2);

    item = proto_tree_add_uint(tree, hf_s7commp_explore_req_area, tvb, offset, 4, area);
    /* wenn die beiden ersten Bytes 0, dann ist das Format ein komplett anderes */
    if (exp_class == 0) {
        proto_item_append_text(item, " Area:[Objects]");
        if (pinfo != NULL) {
            col_append_fstr(pinfo->cinfo, COL_INFO, " Area:[Objects]");
        }
    } else {
        item_tree = proto_item_add_subtree(item, ett_s7commp_explore_req_area);
        proto_tree_add_uint(item_tree, hf_s7commp_explore_req_area_class, tvb, offset, 4, area);

        switch (exp_class) {
            case S7COMMP_EXPLORE_CLASS_IQMCT:
                proto_tree_add_uint(item_tree, hf_s7commp_explore_req_area_class_iqmct, tvb, offset, 4, area);
                proto_tree_add_uint(item_tree, hf_s7commp_explore_req_area_section, tvb, offset, 4, area);
                proto_item_append_text(item, " Area:[%s %s]", val_to_str(exp_class, explore_class_names, "0x%02x"), val_to_str(sub_class, explore_class_iqmct_names, "%d"));
                if (pinfo != NULL) {
                    col_append_fstr(pinfo->cinfo, COL_INFO, " Area:[%s %s]", val_to_str(exp_class, explore_class_names, "0x%02x"), val_to_str(sub_class, explore_class_iqmct_names, "%d"));
                }
                break;
            case S7COMMP_EXPLORE_CLASS_LIB:
                proto_tree_add_uint(item_tree, hf_s7commp_explore_req_area_class_lib, tvb, offset, 4, area);
                proto_tree_add_uint(item_tree, hf_s7commp_explore_req_area_section, tvb, offset, 4, area);
                proto_item_append_text(item, " Area:[%s %s %d]", val_to_str(exp_class, explore_class_names, "0x%02x"), val_to_str(sub_class, explore_class_lib_names, "%d"), section);
                if (pinfo != NULL) {
                    col_append_fstr(pinfo->cinfo, COL_INFO, " Area:[%s %s %d]", val_to_str(exp_class, explore_class_names, "0x%02x"), val_to_str(sub_class, explore_class_lib_names, "%d"), section);
                }
                break;
            case S7COMMP_EXPLORE_CLASS_DB:
                proto_tree_add_uint(item_tree, hf_s7commp_explore_req_area_structindex, tvb, offset, 4, area);
                proto_tree_add_uint(item_tree, hf_s7commp_explore_req_area_section, tvb, offset, 4, area);
                proto_item_append_text(item, " Area:[%s %d.%d]", val_to_str(exp_class, explore_class_names, "0x%02x"), section, sub_class);
                if (pinfo != NULL) {
                    col_append_fstr(pinfo->cinfo, COL_INFO, " Area:[%s %d.%d]", val_to_str(exp_class, explore_class_names, "0x%02x"), section, sub_class);
                }
                break;
            default:
                proto_tree_add_uint(item_tree, hf_s7commp_explore_req_area_structindex, tvb, offset, 4, area);
                proto_tree_add_uint(item_tree, hf_s7commp_explore_req_area_section, tvb, offset, 4, area);
                proto_item_append_text(item, " Area:[%s %d.%d]", val_to_str(exp_class, explore_class_names, "0x%02x"), section, sub_class);
                if (pinfo != NULL) {
                    col_append_fstr(pinfo->cinfo, COL_INFO, " Area:[%s %d.%d]", val_to_str(exp_class, explore_class_names, "0x%02x"), section, sub_class);
                }
                break;
        }
    }
    offset += 4;
    return offset;
}
/*******************************************************************************************************
 *
 * Exploring the data structure of a plc, request
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_request_explore(tvbuff_t *tvb,
                               packet_info *pinfo,
                               proto_tree *tree,
                               guint32 offset)
{
    int number_of_objects = 0;
    int number_of_ids = 0;
    int i, j;
    guint32 start_offset;
    guint32 id_number = 0;
    guint32 uint32value;
    guint8 octet_count = 0;
    guint8 datatype;
    int id_count = 0;
    proto_item *data_item = NULL;
    proto_tree *data_item_tree = NULL;

    offset = s7commp_decode_explore_area(tvb, pinfo, tree, offset);
    /* 4 oder 5 weitere Bytes unbekannter Funktion
     * wenn die ersten beiden Bytes zu Begin Null sind, dann werden Objekte gelesen.
     */
    uint32value = tvb_get_varuint32(tvb, &octet_count, offset);
    proto_tree_add_text(tree, tvb, offset, octet_count, "Explore request unknown 1 (ID?): %u", uint32value);
    offset += octet_count;
    proto_tree_add_text(tree, tvb, offset, 1, "Explore request unknown 2: 0x%02x", tvb_get_guint8(tvb, offset));
    offset += 1;
    proto_tree_add_text(tree, tvb, offset, 1, "Explore request unknown 3: 0x%02x", tvb_get_guint8(tvb, offset));
    offset += 1;
    proto_tree_add_text(tree, tvb, offset, 1, "Explore request unknown 4: 0x%02x", tvb_get_guint8(tvb, offset));
    offset += 1;
    number_of_objects = tvb_get_guint8(tvb, offset);
    proto_tree_add_text(tree, tvb, offset, 1, "Number of following Objects: %d", number_of_objects);
    offset += 1;
    number_of_ids = tvb_get_guint8(tvb, offset);
    proto_tree_add_text(tree, tvb, offset, 1, "Number of following Addresses (IDs): %d", number_of_ids);
    offset += 1;

    if (number_of_objects > 0) {
        start_offset = offset;
        data_item = proto_tree_add_item(tree, hf_s7commp_data_item_value, tvb, offset, -1, FALSE);
        data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_data_item);
        proto_item_append_text(data_item_tree, " (Objects with (type, value))");
        for (i = 0; i < number_of_objects; i++) {
            /* Hier gibt es nur eine Typ-Kennung und den Wert, ohne Flags. Meistens (immer?) ist es eine Struct */
            datatype = tvb_get_guint8(tvb, offset);
            proto_tree_add_uint(data_item_tree, hf_s7commp_itemval_datatype, tvb, offset, 1, datatype);
            offset += 1;
            if (datatype == S7COMMP_ITEM_DATATYPE_STRUCT) {
                proto_tree_add_text(data_item_tree, tvb, offset, 4, "Value: %d", tvb_get_ntohl(tvb, offset));
                offset += 4;
                offset = s7commp_decode_id_value_list(tvb, data_item_tree, offset, TRUE);
                /* Dann folgt nochmal eine Anzahl an IDs, 2 Bytes */
                id_count = tvb_get_ntohs(tvb, offset);
                proto_tree_add_text(data_item_tree, tvb, offset, 2, "Number of following Sub-Ids: %d", id_count);
                offset += 2;
                for (j = 0; j < id_count; j++) {
                    id_number = tvb_get_varuint32(tvb, &octet_count, offset);
                    proto_tree_add_uint(data_item_tree, hf_s7commp_data_id_number, tvb, offset, octet_count, id_number);
                    offset += octet_count;
                }
            } else {
                proto_tree_add_text(data_item_tree, tvb, offset, 0, "TODO, don't know how to handle this.");
                break;
            }
        }
        proto_item_set_len(data_item_tree, offset - start_offset);
    }

    if (number_of_ids > 0) {
        start_offset = offset;
        data_item = proto_tree_add_item(tree, hf_s7commp_addresslist, tvb, offset, -1, FALSE);
        data_item_tree = proto_item_add_subtree(data_item, ett_s7commp_addresslist);
        proto_item_append_text(data_item_tree, " (ID Numbers)");
        for (i = 0; i < number_of_ids; i++) {
            id_number = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(data_item_tree, hf_s7commp_data_id_number, tvb, offset, octet_count, id_number);
            offset += octet_count;
        }
        proto_item_set_len(data_item_tree, offset - start_offset);
    }

    return offset;
}
/*******************************************************************************************************
 *
 * Exploring the data structure of a plc, response
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_response_explore(tvbuff_t *tvb,
                                proto_tree *tree,
                                guint32 offset)
{
    guint32 id_number;
    gint16 errorcode = 0;
    guint8 octet_count = 0;

    offset = s7commp_decode_returnvalue(tvb, tree, offset, &errorcode);

    id_number = tvb_get_ntohl(tvb, offset);
    proto_tree_add_uint(tree, hf_s7commp_data_id_number, tvb, offset, 4, id_number);
    offset += 4;
    if (errorcode == 0) {    /* alternativ auf id_number > 0 prüfen? */
        /* Es kann sein dass hier noch ein Wert VLQ, wenn nicht ein STARTOBJECT (0xa1) folgt.
         * Welche Logik dahinterstecken mag. Das macht auch nur die 1500.
         */
        if (tvb_get_guint8(tvb, offset) != S7COMMP_ITEMVAL_ELEMENTID_STARTOBJECT) {
            id_number = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(tree, hf_s7commp_data_id_number, tvb, offset, octet_count, id_number);
            offset += octet_count;
        }
        /* Dann nur die Liste durchgehen, wenn auch ein Objekt folgt. Sonst würde ein Null-Byte
         * zur Terminierung der Liste eingefügt werden.
         */
        if (tvb_get_guint8(tvb, offset) == S7COMMP_ITEMVAL_ELEMENTID_STARTOBJECT) {
            offset = s7commp_decode_object(tvb, tree, offset);
        }
    }
    return offset;
}
/*******************************************************************************************************
 *
 * Decode the object qualifier
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_objectqualifier(tvbuff_t *tvb,
                                proto_tree *tree,
                                gint16 dlength,
                                guint32 offset)
{
    guint32 offset_save;
    guint32 offsetmax;
    guint16 id = 0;
    proto_item *objectqualifier_item = NULL;
    proto_tree *objectqualifier_tree = NULL;

    offset_save = offset;
    offsetmax = offset + dlength-2;

    while (offset < offsetmax) {
        id = tvb_get_ntohs(tvb, offset);
        if (id == 0x4e8) {
            /* alles dazwischen mit Dummy-Bytes auffüllen */
            if ((offset+2 - offset_save) > 0) {
                proto_tree_add_bytes(tree, hf_s7commp_data_data, tvb, offset_save, offset - offset_save, tvb_get_ptr(tvb, offset_save, offset - offset_save));
            }
            dlength = dlength - (offset - offset_save);
            offset_save = offset;
            objectqualifier_item = proto_tree_add_item(tree, hf_s7commp_objectqualifier, tvb, offset, -1, FALSE );
            objectqualifier_tree = proto_item_add_subtree(objectqualifier_item, ett_s7commp_objectqualifier);
            proto_tree_add_uint(objectqualifier_tree, hf_s7commp_data_id_number, tvb, offset, 2, id);
            offset += 2;
            offset = s7commp_decode_id_value_list_in_new_tree(tvb, objectqualifier_tree, offset, TRUE);
            proto_item_set_len(objectqualifier_tree, offset - offset_save);
            break;
        }
        offset += 1;
    }
    if (id != 0x4e8) {
        offset = offset_save; /* zurücksetzen wenn nicht gefunden */
    }
    return offset;
}
/*******************************************************************************************************
 *
 * Decode the integrity part
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_integrity(tvbuff_t *tvb,
                         packet_info *pinfo,
                         proto_tree *tree,
                         gboolean has_integrity_id,
                         guint32 offset)
{
    guint32 offset_save;
    guint32 integrity_id = 0;
    guint8 integrity_len = 0;
    guint8 octet_count = 0;

    proto_item *integrity_item = NULL;
    proto_tree *integrity_tree = NULL;

    offset_save = offset;
    integrity_item = proto_tree_add_item(tree, hf_s7commp_integrity, tvb, offset, -1, FALSE );
    integrity_tree = proto_item_add_subtree(integrity_item, ett_s7commp_integrity);
    /* In DeleteObject-Response, the Id is missing if the deleted id is > 0x7000000!
     * This check is done by the decoding function for deleteobject. By default there is an Id.
     *
     * The integrity_id seems to be increased by one in each telegram. The integrity_id in the corresponding
     * response is calculated by adding the sequencenumber to the integrity_id from request.
     */
    if (has_integrity_id) {
        integrity_id = tvb_get_varuint32(tvb, &octet_count, offset);
        proto_tree_add_uint(integrity_tree, hf_s7commp_integrity_id, tvb, offset, octet_count, integrity_id);
        offset += octet_count;
    }

    integrity_len = tvb_get_guint8(tvb, offset);
    proto_tree_add_uint(integrity_tree, hf_s7commp_integrity_digestlen, tvb, offset, 1, integrity_len);
    offset += 1;
    /* Length should always be 32. If not, then the previous decoding was not correct.
     * To prevent malformed packet errors, check this.
     */
    if (integrity_len == 32) {
        proto_tree_add_bytes(integrity_tree, hf_s7commp_integrity_digest, tvb, offset, integrity_len, tvb_get_ptr(tvb, offset, integrity_len));
        offset += integrity_len;
    } else {
        proto_tree_add_text(integrity_tree, tvb, offset-1, 1, "Error in dissector: Integrity Digest length should be 32!");
        col_append_fstr(pinfo->cinfo, COL_INFO, " (DISSECTOR-ERROR)"); /* add info that somthing went wrong */
    }
    proto_item_set_len(integrity_tree, offset - offset_save);
    return offset;
}
/*******************************************************************************************************
 *
 * Decodes the data part
 *
 *******************************************************************************************************/
static guint32
s7commp_decode_data(tvbuff_t *tvb,
                    packet_info *pinfo,
                    proto_tree *tree,
                    gint dlength,
                    guint32 offset,
                    guint8 pdutype)
{
    proto_item *item = NULL;
    proto_tree *item_tree = NULL;

    guint16 seqnum = 0;
    guint16 functioncode = 0;
    guint8 opcode = 0;
    guint32 offset_save = 0;
    guint8 octet_count = 0;
    guint32 integrity_id;
    gboolean has_integrity_id = TRUE;
    gboolean has_objectqualifier = FALSE;

    opcode = tvb_get_guint8(tvb, offset);
    /* 1: Opcode */
    proto_item_append_text(tree, ", Op: %s", val_to_str(opcode, opcode_names, "Unknown Opcode: 0x%02x"));
    proto_tree_add_uint(tree, hf_s7commp_data_opcode, tvb, offset, 1, opcode);
    col_append_fstr(pinfo->cinfo, COL_INFO, " Op: [%s]", val_to_str(opcode, opcode_names, "Unknown Opcode: 0x%02x"));
    offset += 1;
    dlength -= 1;

    if (opcode == S7COMMP_OPCODE_NOTIFICATION) {
        item = proto_tree_add_item(tree, hf_s7commp_notification_set, tvb, offset, -1, FALSE);
        item_tree = proto_item_add_subtree(item, ett_s7commp_notification_set);
        offset_save = offset;
        offset = s7commp_decode_notification(tvb, pinfo, item_tree, offset);
        proto_item_set_len(item_tree, offset - offset_save);
        dlength = dlength - (offset - offset_save);
    } else {
        proto_tree_add_uint(tree, hf_s7commp_data_reserved1, tvb, offset, 2, tvb_get_ntohs(tvb, offset));
        offset += 2;
        dlength -= 2;

        /* 4/5: Functioncode */
        functioncode = tvb_get_ntohs(tvb, offset);
        proto_tree_add_uint(tree, hf_s7commp_data_function, tvb, offset, 2, functioncode);
        col_append_fstr(pinfo->cinfo, COL_INFO, " Function: [%s]",
                        val_to_str(functioncode, data_functioncode_names, "?"));
        offset += 2;
        dlength -= 2;

        proto_tree_add_uint(tree, hf_s7commp_data_reserved2, tvb, offset, 2, tvb_get_ntohs(tvb, offset));
        offset += 2;
        dlength -= 2;

        /* 8/9: Sequence number */
        seqnum = tvb_get_ntohs(tvb, offset);
        proto_tree_add_uint(tree, hf_s7commp_data_seqnum, tvb, offset, 2, seqnum);
        col_append_fstr(pinfo->cinfo, COL_INFO, " Seq=%u", seqnum);
        offset += 2;
        dlength -= 2;

        if (opcode == S7COMMP_OPCODE_REQ) {
            proto_tree_add_uint(tree, hf_s7commp_data_sessionid, tvb, offset, 4, tvb_get_ntohl(tvb, offset));
            offset += 4;
            dlength -= 4;

            proto_tree_add_item(tree, hf_s7commp_data_unknown1, tvb, offset, 1, FALSE);
            offset += 1;
            dlength -= 1;

            item = proto_tree_add_item(tree, hf_s7commp_data_req_set, tvb, offset, -1, FALSE);
            item_tree = proto_item_add_subtree(item, ett_s7commp_data_req_set);
            offset_save = offset;

            switch (functioncode) {
                case S7COMMP_FUNCTIONCODE_GETMULTIVAR:
                    offset = s7commp_decode_request_getmultivar(tvb, item_tree, offset);
                    has_objectqualifier = TRUE;
                    break;
                case S7COMMP_FUNCTIONCODE_SETMULTIVAR:
                    offset = s7commp_decode_request_setmultivar(tvb, pinfo, item_tree, dlength, offset);
                    has_objectqualifier = TRUE;
                    break;
                case S7COMMP_FUNCTIONCODE_SETVARIABLE:
                    offset = s7commp_decode_request_setvariable(tvb, pinfo, item_tree, offset);
                    has_objectqualifier = TRUE;
                    break;
                case S7COMMP_FUNCTIONCODE_CREATEOBJECT:
                    offset = s7commp_decode_request_createobject(tvb, item_tree, offset, pdutype);
                    break;
                case S7COMMP_FUNCTIONCODE_DELETEOBJECT:
                    offset = s7commp_decode_request_deleteobject(tvb, pinfo, item_tree, offset);
                    has_objectqualifier = TRUE;
                    break;
                case S7COMMP_FUNCTIONCODE_GETVARSUBSTR:
                    offset = s7commp_decode_request_getvarsubstr(tvb, item_tree, offset);
                    has_objectqualifier = TRUE;
                    break;
                case S7COMMP_FUNCTIONCODE_EXPLORE:
                    offset = s7commp_decode_request_explore(tvb, pinfo, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_GETLINK:
                    offset = s7commp_decode_request_getlink(tvb, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_BEGINSEQUENCE:
                    offset = s7commp_decode_request_beginsequence(tvb, item_tree, dlength, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_ENDSEQUENCE:
                    offset = s7commp_decode_request_endsequence(tvb, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_INVOKE:
                    offset = s7commp_decode_request_invoke(tvb, item_tree, offset);
                    break;
            }
            proto_item_set_len(item_tree, offset - offset_save);
            dlength = dlength - (offset - offset_save);
        } else if ((opcode == S7COMMP_OPCODE_RES) || (opcode == S7COMMP_OPCODE_RES2)) {
            proto_tree_add_item(tree, hf_s7commp_data_unknown1, tvb, offset, 1, FALSE);
            offset += 1;
            dlength -= 1;

            item = proto_tree_add_item(tree, hf_s7commp_data_res_set, tvb, offset, -1, FALSE);
            item_tree = proto_item_add_subtree(item, ett_s7commp_data_res_set);
            offset_save = offset;

            switch (functioncode) {
                case S7COMMP_FUNCTIONCODE_GETMULTIVAR:
                    offset = s7commp_decode_response_getmultivar(tvb, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_SETMULTIVAR:
                    offset = s7commp_decode_response_setmultivar(tvb, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_SETVARIABLE:
                    offset = s7commp_decode_response_setvariable(tvb, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_CREATEOBJECT:
                    offset = s7commp_decode_response_createobject(tvb, item_tree, offset, pdutype);
                    break;
                case S7COMMP_FUNCTIONCODE_DELETEOBJECT:
                    offset = s7commp_decode_response_deleteobject(tvb, pinfo, item_tree, offset, &has_integrity_id);
                    break;
                case S7COMMP_FUNCTIONCODE_GETVARSUBSTR:
                    offset = s7commp_decode_response_getvarsubstr(tvb, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_EXPLORE:
                    offset = s7commp_decode_response_explore(tvb, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_GETLINK:
                    offset = s7commp_decode_response_getlink(tvb, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_BEGINSEQUENCE:
                    offset = s7commp_decode_response_beginsequence(tvb, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_ENDSEQUENCE:
                    offset = s7commp_decode_response_endsequence(tvb, item_tree, offset);
                    break;
                case S7COMMP_FUNCTIONCODE_INVOKE:
                    offset = s7commp_decode_response_invoke(tvb, item_tree, offset);
                    break;
            }
            proto_item_set_len(item_tree, offset - offset_save);
            dlength = dlength - (offset - offset_save);
        }
    }
    /* Nach Object Qualifier trailer suchen.
     * Der Objectqualifier hat die ID 1256 = 0x04e8. Dieses Objekt hat 3 Member mit jeweils einer ID.
     * Solange wir noch nicht immer direkt auf dieser ID landen, danach suchen.
     */
    if (has_objectqualifier && dlength > 10) {
        offset_save = offset;
        offset = s7commp_decode_objectqualifier(tvb, tree, dlength, offset);
        dlength = dlength - (offset - offset_save);
    }

    /* Request GetVarSubStreamed has two bytes of unknown meaning, request SetVariable session one single byte */
    if (opcode == S7COMMP_OPCODE_REQ) {
        if (functioncode == S7COMMP_FUNCTIONCODE_GETVARSUBSTR) {
            proto_tree_add_text(tree, tvb, offset, 2, "Request GetVarSubStreamed unknown 2 Bytes: 0x%04x", tvb_get_ntohs(tvb, offset));
            offset += 2;
            dlength -= 2;
        } else if (functioncode == S7COMMP_FUNCTIONCODE_SETVARIABLE) {
            proto_tree_add_text(tree, tvb, offset, 1, "Request SetVariable unknown Byte: 0x%02x", tvb_get_guint8(tvb, offset));
            offset += 1;
            dlength -= 1;
        }
    }

    if (pdutype == S7COMMP_PDUTYPE_DATAFW1_5) {
        /* Pakete mit neuerer Firmware haben den Wert / id am Ende, der bei anderen FW vor der Integrität kommt.
         * Dieser ist aber nicht bei jedem Typ vorhanden. Wenn nicht, dann sind 4 Null-Bytes am Ende.
         */
        if ((dlength > 4) && has_integrity_id) {
            integrity_id = tvb_get_varuint32(tvb, &octet_count, offset);
            proto_tree_add_uint(tree, hf_s7commp_integrity_id, tvb, offset, octet_count, integrity_id);
            offset += octet_count;
            dlength -= octet_count;
        }
    } else {
        if (dlength >= 32) {
            offset_save = offset;
            offset = s7commp_decode_integrity(tvb, pinfo, tree, has_integrity_id, offset);
            dlength = dlength - (offset - offset_save);
        }
    }
    /* Show remaining undecoded data as raw bytes */
    if (dlength > 0) {
        proto_tree_add_bytes(tree, hf_s7commp_data_data, tvb, offset, dlength, tvb_get_ptr(tvb, offset, dlength));
        offset += dlength;
    }
    return offset;
}
/*******************************************************************************************************
 *******************************************************************************************************
 *
 * S7-Protocol plus (main tree)
 *
 *******************************************************************************************************
 *******************************************************************************************************/
static gboolean
dissect_s7commp(tvbuff_t *tvb,
                packet_info *pinfo,
                proto_tree *tree,
                void *data _U_)
{
    proto_item *s7commp_item = NULL;
    proto_item *s7commp_sub_item = NULL;
    proto_tree *s7commp_tree = NULL;

    proto_tree *s7commp_header_tree = NULL;
    proto_tree *s7commp_data_tree = NULL;
    proto_tree *s7commp_trailer_tree = NULL;

    guint32 offset = 0;

    guint8 pdutype = 0;
    guint8 hlength = 4;
    gint dlength = 0;
    guint8 keepaliveseqnum = 0;

    gboolean has_trailer = FALSE;
    gboolean save_fragmented;
    guint32 frag_id;
    frame_state_t *packet_state;
    conversation_t *conversation;
    conv_state_t *conversation_state = NULL;
    gboolean no_fragment = FALSE;
    gboolean first_fragment = FALSE;
    gboolean inner_fragment = FALSE;
    gboolean last_fragment = FALSE;
    tvbuff_t* next_tvb = NULL;

    guint packetlength;

    packetlength = tvb_reported_length(tvb);    /* Payload length reported from tpkt/cotp dissector. */
    /*----------------- Heuristic Checks - Begin */
    /* 1) check for minimum length */
    if (packetlength < S7COMMP_MIN_TELEGRAM_LENGTH) {
        return 0;
    }
    /* 2) first byte must be 0x72 */
    if (tvb_get_guint8(tvb, 0) != S7COMM_PLUS_PROT_ID) {
        return 0;
    }
    /*----------------- Heuristic Checks - End */

    col_set_str(pinfo->cinfo, COL_PROTOCOL, PROTO_TAG_S7COMM_PLUS);
    col_clear(pinfo->cinfo, COL_INFO);

    pdutype = tvb_get_guint8(tvb, 1);                       /* Get the type byte */
    hlength = 4;                                            /* Header 4 Bytes */

    /* display some infos in info-column of wireshark */
    col_add_fstr(pinfo->cinfo, COL_INFO, "PDU-Type: [%s]", val_to_str(pdutype, pdutype_names, "PDU-Type: 0x%02x"));

    s7commp_item = proto_tree_add_item(tree, proto_s7commp, tvb, 0, -1, FALSE);
    s7commp_tree = proto_item_add_subtree(s7commp_item, ett_s7commp);

    /******************************************************
     * Header
     ******************************************************/
    s7commp_sub_item = proto_tree_add_item(s7commp_tree, hf_s7commp_header, tvb, offset, hlength, FALSE );
    s7commp_header_tree = proto_item_add_subtree(s7commp_sub_item, ett_s7commp_header);
    proto_item_append_text(s7commp_header_tree, ", PDU-Type: %s", val_to_str(pdutype, pdutype_names, ", PDU-Type: 0x%02x"));
    proto_tree_add_item(s7commp_header_tree, hf_s7commp_header_protid, tvb, offset, 1, FALSE);
    offset += 1;
    proto_tree_add_uint(s7commp_header_tree, hf_s7commp_header_pdutype, tvb, offset, 1, pdutype);
    offset += 1;

    /* Typ FF Pakete scheinen eine Art Keep-Alive Telegramme zu sein. Diese sind nur 4 Bytes lang
     * 1. Protocol-id, 2.PDU Typ und dann 3. eine Art sequenz-Nummer, und das 4. Byte bisher immer 0
     */
    if (pdutype == S7COMMP_PDUTYPE_KEEPALIVE) {
        keepaliveseqnum = tvb_get_guint8(tvb, offset);
        proto_tree_add_uint(s7commp_header_tree, hf_s7commp_header_keepaliveseqnum, tvb, offset, 1, keepaliveseqnum);
        col_append_fstr(pinfo->cinfo, COL_INFO, " KeepAliveSeq=%d", keepaliveseqnum);
        offset += 1;
        /* dann noch ein Byte, noch nicht klar wozu */
        proto_tree_add_text(s7commp_header_tree, tvb, offset, 1, "Reserved? : 0x%02x", tvb_get_guint8(tvb, offset));
        offset += 1;
    } else {
        /* 3/4: Data length */
        dlength = tvb_get_ntohs(tvb, offset);
        proto_tree_add_uint(s7commp_header_tree, hf_s7commp_header_datlg, tvb, offset, 2, dlength);
        offset += 2;

        /* Bei einer 1500 mit Firmware version >= V1.5 wurde der Integritätsteil vom Ende des Datenteils an den Anfang verschoben.
         * Bei fragmentierten Paketen hatte bisher nur das letzte Fragment einen Integritätsteil.
         * Bei >= V1.5 hat nun auch bei fragmentierten Paketen jedes Fragment einen Integritätsteil. Der Integritätsteil
         * zählt aber von der Längenangabe im Kopf zum Datenteil. Bei fragmentierten Paketen muss daher bei dieser Version
         * der Integritätsteil außerhalb der eigentlichen Funktion zum Zerlegen des Datenteils platziert werden, da ansonsten
         * dieser beim Reassemblieren innerhalb der Datenteile liegen würde.
         * Leider wird damit der Zweig nicht unter dem Datenteil, sondern als eigener separater Zweig eingefügt.
         */
        if (pdutype == S7COMMP_PDUTYPE_DATAFW1_5) {
            offset = s7commp_decode_integrity(tvb, pinfo, s7commp_tree, FALSE, offset);
        }

        /* Paket hat einen Trailer, wenn nach der angegebenen Datenlänge noch 4 Bytes übrig bleiben */
        has_trailer = ((signed) packetlength) > (dlength + 4);

        /************************************************** START REASSEMBLING *************************************************************************/
        /*
         * Fragmentation check:
         * Da es keine Kennzeichnungen über die Fragmentierung gibt, muss es in einer Zustandsmaschine abgefragt werden
         *
         * Istzustand   Transition                                      Aktion                                Neuer Zustand
         * state == 0:  Paket hat einen Trailer, keine Fragmentierung   dissect_data                          state = 0
         * state == 0:  Paket hat keinen Trailer, Start Fragmentierung  push data                             state = 1
         * state == 1:  Paket hat keinen Trailer, weiterhin Fragment    push data                             state = 1
         * state == 1:  Paket hat einen trailer, Ende Fragmente         push data, pop, dissect_data          state = 0
         *
         * Die einzige Zugehörigkeit die es gibt, ist die TCP Portnummer. Es müssen dabei BEIDE übereinstimmen.
         *
         * Dabei muss zusätzlich beachtet werden, dass womöglich ein capture inmitten einer solchen Serie gestartet wurde.
         * Das kann aber nicht zuverlässig abgefangen werden, wenn zufällig in den ersten Bytes des Datenteils gültige Daten stehen.
         *
         */

        /* Zustandsdiagramm:
                         NEIN                Konversation    JA
         has_trailer ---------------------mit vorigem Frame-------- Inneres Fragment
              ?                              vorhanden?
              |                                  |
              | JA                               | NEIN
              |                                  |
           Konversation     NEIN        Neue Konversation anlegen
        mit vorigem Frame--------+               |
            vorhanden?           |          Erstes Fragment
              |                  |
              | JA        Nicht fragmentiert
              |
          Letztes Fragment
        */

        if (!pinfo->fd->flags.visited) {        /* first pass */
            #ifdef DEBUG_REASSEMBLING
            printf("Reassembling pass 1: Frame=%3d HasTrailer=%d", pinfo->fd->num, has_trailer);
            #endif
            /* evtl. find_or_create_conversation verwenden?
             * conversation = find_or_create_conversation(pinfo);
             */

            conversation = find_conversation(pinfo->fd->num, &pinfo->dst, &pinfo->src,
                                             pinfo->ptype, pinfo->destport,
                                             0, NO_PORT_B);
            if (conversation == NULL) {
                conversation = conversation_new(pinfo->fd->num, &pinfo->dst, &pinfo->src,
                                                pinfo->ptype, pinfo->destport,
                                                0, NO_PORT2);
                #ifdef DEBUG_REASSEMBLING
                printf(" NewConv" );
                #endif
            }
            conversation_state = (conv_state_t *)conversation_get_proto_data(conversation, proto_s7commp);
            if (conversation_state == NULL) {
                conversation_state = wmem_new(wmem_file_scope(), conv_state_t);
                conversation_state->state = CONV_STATE_NEW;
                conversation_state->start_frame = 0;
                conversation_add_proto_data(conversation, proto_s7commp, conversation_state);
                #ifdef DEBUG_REASSEMBLING
                printf(" NewConvState" );
                #endif
            }
            #ifdef DEBUG_REASSEMBLING
            printf(" ConvState->state=%d", conversation_state->state);
            #endif

            if (has_trailer) {
                if (conversation_state->state == CONV_STATE_NEW) {
                    no_fragment = TRUE;
                    #ifdef DEBUG_REASSEMBLING
                    printf(" no_fragment=1");
                    #endif
                } else {
                    last_fragment = TRUE;
                    /* rücksetzen */
                    #ifdef DEBUG_REASSEMBLING
                    col_append_fstr(pinfo->cinfo, COL_INFO, " (DEBUG: A state=%d)", conversation_state->state);
                    printf(" last_fragment=1, delete_proto_data");
                    #endif
                    conversation_state->state = CONV_STATE_NOFRAG;
                    conversation_delete_proto_data(conversation, proto_s7commp);
                }
            } else {
                if (conversation_state->state == CONV_STATE_NEW) {
                    first_fragment = TRUE;
                    conversation_state->state = CONV_STATE_FIRST;
                    conversation_state->start_frame = pinfo->fd->num;
                    #ifdef DEBUG_REASSEMBLING
                    printf(" first_fragment=1, set state=%d, start_frame=%d", conversation_state->state, conversation_state->start_frame);
                    #endif
                } else {
                    inner_fragment = TRUE;
                    conversation_state->state = CONV_STATE_INNER;
                }
            }
            #ifdef DEBUG_REASSEMBLING
            printf(" => Conv->state=%d", conversation_state->state);
            printf(" => Conv->start_frame=%3d", conversation_state->start_frame);
            printf("\n");
            #endif
        }

        save_fragmented = pinfo->fragmented;
        packet_state = (frame_state_t *)p_get_proto_data(wmem_file_scope(), pinfo, proto_s7commp, 0);
        if (!packet_state) {
            /* First S7COMMP in frame*/
            packet_state = wmem_new(wmem_file_scope(), frame_state_t);
            p_add_proto_data(wmem_file_scope(), pinfo, proto_s7commp, 0, packet_state);
            packet_state->first_fragment = first_fragment;
            packet_state->inner_fragment = inner_fragment;
            packet_state->last_fragment = last_fragment;
            packet_state->start_frame = conversation_state->start_frame;
#ifdef DEBUG_REASSEMBLING
            col_append_fstr(pinfo->cinfo, COL_INFO, " (DEBUG-REASM: INIT-packet_state)");
#endif
        } else {
            first_fragment = packet_state->first_fragment;
            inner_fragment = packet_state->inner_fragment;
            last_fragment = packet_state->last_fragment;
        }

        if (first_fragment || inner_fragment || last_fragment) {
            tvbuff_t* new_tvb = NULL;
            fragment_head *fd_head;
            guint32 frag_data_len;
            /* guint32 frag_number; */
            gboolean more_frags;
#ifdef DEBUG_REASSEMBLING
            col_append_fstr(pinfo->cinfo, COL_INFO, " (DEBUG-REASM: F=%d I=%d L=%d N=%u)", first_fragment, inner_fragment, last_fragment, packet_state->start_frame);
#endif

            frag_id       = packet_state->start_frame;
            frag_data_len = tvb_reported_length_remaining(tvb, offset);     /* Dieses ist der reine Data-Teil, da offset hinter dem Header steht */
            /* frag_number   = pinfo->fd->num; */
            more_frags    = !last_fragment;

            pinfo->fragmented = TRUE;
            /*
             * fragment_add_seq_next() geht davon aus dass die Pakete in der richtigen Reihenfolge reinkommen.
             * Bei fragment_add_seq_check() muss eine Sequenznummer angegeben werden, die gibt es aber nicht im Protokoll.
             */
            fd_head = fragment_add_seq_next(&s7commp_reassembly_table,
                                             tvb, offset, pinfo,
                                             frag_id,               /* ID for fragments belonging together */
                                             NULL,                  /* void *data */
                                             frag_data_len,         /* fragment length - to the end */
                                             more_frags);           /* More fragments? */

            new_tvb = process_reassembled_data(tvb, offset, pinfo,
                                               "Reassembled S7COMM-PLUS", fd_head, &s7commp_frag_items,
                                               NULL, s7commp_tree);

            if (new_tvb) { /* take it all */
                next_tvb = new_tvb;
                offset = 0;
            } else { /* make a new subset */
                next_tvb = tvb_new_subset(tvb, offset, -1, -1);
                offset = 0;
            }
        } else {    /* nicht fragmentiert */
            next_tvb = tvb;
        }
        pinfo->fragmented = save_fragmented;
        /******************************************************* END REASSEMBLING *******************************************************************/
        if (tree) {
            /******************************************************
             * Data
             ******************************************************/
             /* insert data tree */
            s7commp_sub_item = proto_tree_add_item(s7commp_tree, hf_s7commp_data, next_tvb, offset, dlength, FALSE);
            /* insert sub-items in data tree */
            s7commp_data_tree = proto_item_add_subtree(s7commp_sub_item, ett_s7commp_data);
            /* main dissect data function */
            dlength = tvb_reported_length_remaining(next_tvb, offset) - 4;
            if (first_fragment || inner_fragment) {
                col_append_fstr(pinfo->cinfo, COL_INFO, " (S7COMM-PLUS %s fragment)", first_fragment ? "first" : "inner" );
                proto_tree_add_bytes(s7commp_data_tree, hf_s7commp_data_data, next_tvb, offset, dlength, tvb_get_ptr(next_tvb, offset, dlength));
                offset += dlength;
            } else {
                if (last_fragment) {
                    col_append_str(pinfo->cinfo, COL_INFO, " (S7COMM-PLUS reassembled)");
                }
                offset = s7commp_decode_data(next_tvb, pinfo, s7commp_data_tree, dlength, offset, pdutype);
            }
            /******************************************************
             * Trailer
             ******************************************************/
            if (has_trailer) {
                s7commp_sub_item = proto_tree_add_item(s7commp_tree, hf_s7commp_trailer, next_tvb, offset, 4, FALSE);
                s7commp_trailer_tree = proto_item_add_subtree(s7commp_sub_item, ett_s7commp_trailer);
                proto_tree_add_item(s7commp_trailer_tree, hf_s7commp_trailer_protid, next_tvb, offset, 1, FALSE);
                offset += 1;
                proto_tree_add_uint(s7commp_trailer_tree, hf_s7commp_trailer_pdutype, next_tvb, offset, 1, tvb_get_guint8(next_tvb, offset));
                proto_item_append_text(s7commp_trailer_tree, ", PDU-Type: %s", val_to_str(tvb_get_guint8(next_tvb, offset), pdutype_names, ", PDU-Type: 0x%02x"));
                offset += 1;
                proto_tree_add_uint(s7commp_trailer_tree, hf_s7commp_trailer_datlg, next_tvb, offset, 2, tvb_get_ntohs(next_tvb, offset));
                offset += 2;
            }
        }
    }
    return TRUE;
}

/*
 * Editor modelines  -  http://www.wireshark.org/tools/modelines.html
 *
 * Local variables:
 * c-basic-offset: 4
 * tab-width: 8
 * indent-tabs-mode: nil
 * End:
 *
 * vi: set shiftwidth=4 tabstop=8 expandtab:
 * :indentSize=4:tabSize=8:noTabs=true:
 */
