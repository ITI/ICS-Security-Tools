import logging
logging.getLogger("scapy.runtime").setLevel(logging.ERROR)
logging.getLogger("scapy").setLevel(1)
import sys
import Global
sys.path.append(Global.mainPath + '/System/')
sys.path.append(Global.mainPath + '/System/Lib/')
sys.path.append(Global.mainPath + '/System/Lib/scapy')
# import scapy
from scapy.all import *
from scapy.layers.inet import *
conf.verb = 0
# Set Scapy log level for getting warnings

import math

# own constant definitions
transId = 1
connection = None
timeout = 5
modport=502


#
# Supported Function Codes
# 	01 (0x01) Read Coils
#	02 (0x02) Read Discrete Inputs
#	03 (0x03) Read Holding Registers
#	04 (0x04) Read Input Registers
#
#	05 (0x05) Write Single Coil
#	06 (0x06) Write Single Holding Register
#
#	07 (0x07) Read Exception Status (Serial Line only)
#
#	15 (0x0F) Write Multiple Coils
#	16 (0x10) Write Multiple Holding Registers
#
#	17 (0x11) Report Slave ID (Serial Line only)
#
#
#
#	Supported function codes:
#   	Modsak supported: [1, 2, 3, 4, 5, 6, 7, 8, 11, 15, 16, 17, 22, 23]
#   	Schneider Factory Cast supported: [1, 2, 3, 4, 5, 6, 15, 16, 22, 43, 90]
#
###

# own imports

# own classes
function_code_name = { 	1 : "Read Coils",
			2 : "Read Discrete Inputs",
			3 : "Read Multiple Holding Registers",
			4 : "Read Input Registers",
			5 : "Write Single Coil",
			6 : "Write Single Holding Register",
			7 : "Read Exception Status",
			8 : "Diagnostic",
			11 : "Get Com Event Counter",
			12 : "Get Com Event Log",
			15 : "Write Multiple Coils",
			16 : "Write Multiple Holding Registers",
			17 : "Report Slave ID",
			20 : "Read File Record",
			21 : "Write File Record",
			22 : "Mask Write Register",
			23 : "Read/Write Multiple Registers",
			24 : "Read FIFO Queue",
			43 : "Read Device Identification"
			}
_modbus_exceptions = {  1: "Illegal function",
						2: "Illegal data address",
						3: "Illegal data value",
						4: "Slave device failure",
						5: "Acknowledge",
						6: "Slave device busy",
						8: "Memory parity error",
						10: "Gateway path unavailable",
						11: "Gateway target device failed to respond"}

# Can be used to replace all Modbus read
class ModbusPDU_Read_Generic(Packet):
	name = "Read Generic"
	fields_desc = [ XByteField("funcCode", 0x01),
			XShortField("startAddr", 0x0000),
			XShortField("quantity", 0x0001)]

# 0x01 - Read Coils
class ModbusPDU01_Read_Coils(Packet):
	name = "Read Coils Request"
	fields_desc = [ XByteField("funcCode", 0x01),
			# 0x0000 to 0xFFFF
			XShortField("startAddr", 0x0000),
			XShortField("quantity", 0x0001)]
class ModbusPDU01_Read_Coils_Answer(Packet):
	name = "Read Coils Answer"
	fields_desc = [ XByteField("funcCode", 0x01),
			BitFieldLenField("byteCount", None, 8, count_of="coilStatus"),
			FieldListField("coilStatus", [0x00], ByteField("",0x00), count_from = lambda pkt: pkt.byteCount) ]
class ModbusPDU01_Read_Coils_Exception(Packet):
	name = "Read Coils Exception"
	fields_desc = [ XByteField("funcCode", 0x81),
			ByteEnumField("exceptCode", 1, _modbus_exceptions)]

# 0x02 - Read Discrete Inputs
class ModbusPDU02_Read_Discrete_Inputs(Packet):
	name = "Read Discrete Inputs"
	fields_desc = [ XByteField("funcCode", 0x02),
			XShortField("startAddr", 0x0000),
			XShortField("quantity", 0x0001)]
class ModbusPDU02_Read_Discrete_Inputs_Answer(Packet):
	name = "Read Discrete Inputs Answer"
	fields_desc = [ XByteField("funcCode", 0x02),
			BitFieldLenField("byteCount", None, 8, count_of="inputStatus"),
			FieldListField("inputStatus", [0x00], ByteField("",0x00), count_from = lambda pkt: pkt.byteCount) ]
class ModbusPDU02_Read_Discrete_Inputs_Exception(Packet):
	name = "Read Discrete Inputs Exception"
	fields_desc = [ XByteField("funcCode", 0x82),
			ByteEnumField("exceptCode", 1, _modbus_exceptions)]

# 0x03 - Read Holding Registers
class ModbusPDU03_Read_Holding_Registers(Packet):
	name = "Read Holding Registers"
	fields_desc = [ XByteField("funcCode", 0x03),
			XShortField("startAddr", 0x0001),
			XShortField("quantity", 0x0002)]
class ModbusPDU03_Read_Holding_Registers_Answer(Packet):
	name = "Read Holding Registers Answer"
	fields_desc = [ XByteField("funcCode", 0x03),
			BitFieldLenField("byteCount", None, 8, count_of="registerVal"),
			FieldListField("registerVal", [0x00], ByteField("",0x00), count_from = lambda pkt: pkt.byteCount)]
class ModbusPDU03_Read_Holding_Registers_Exception(Packet):
	name = "Read Holding Registers Exception"
	fields_desc = [ XByteField("funcCode", 0x83),
			ByteEnumField("exceptCode", 1, _modbus_exceptions)]

# 0x04 - Read Input Registers
class ModbusPDU04_Read_Input_Registers(Packet):
	name = "Read Input Registers"
	fields_desc = [ XByteField("funcCode", 0x04),
			XShortField("startAddr", 0x0000),
			XShortField("quantity", 0x0001)]
class ModbusPDU04_Read_Input_Registers_Answer(Packet):
	name = "Read Input Registers Answer"
	fields_desc = [ XByteField("funcCode", 0x04),
			BitFieldLenField("byteCount", None, 8, count_of="registerVal"),
			FieldListField("registerVal", [0x00], ByteField("",0x00), count_from = lambda pkt: pkt.byteCount)]
class ModbusPDU04_Read_Input_Registers_Exception(Packet):
	name = "Read Input Registers Exception"
	fields_desc = [ XByteField("funcCode", 0x84),
			ByteEnumField("exceptCode", 1, _modbus_exceptions)]

# 0x05 - Write Single Coil
class ModbusPDU05_Write_Single_Coil(Packet):
	name = "Write Single Coil"
	fields_desc = [ XByteField("funcCode", 0x05),
			XShortField("outputAddr", 0x0000), # from 0x0000 to 0xFFFF
			XShortField("outputValue", 0x0000)]# 0x0000 == Off, 0xFF00 == On
class ModbusPDU05_Write_Single_Coil_Answer(Packet): # The answer is the same as the request if successful
	name = "Write Single Coil"
	fields_desc = [ XByteField("funcCode", 0x05),
			XShortField("outputAddr", 0x0000), # from 0x0000 to 0xFFFF
			XShortField("outputValue", 0x0000)]# 0x0000 == Off, 0xFF00 == On
class ModbusPDU05_Write_Single_Coil_Exception(Packet):
	name = "Write Single Coil Exception"
	fields_desc = [ XByteField("funcCode", 0x85),
			ByteEnumField("exceptCode", 1, _modbus_exceptions)]

# 0x06 - Write Single Register
class ModbusPDU06_Write_Single_Register(Packet):
	name = "Write Single Register"
	fields_desc = [ XByteField("funcCode", 0x06),
			XShortField("registerAddr", 0x0000), 
			XShortField("registerValue", 0x0000)]
class ModbusPDU06_Write_Single_Register_Answer(Packet):
	name = "Write Single Register Answer"
	fields_desc = [ XByteField("funcCode", 0x06),
			XShortField("registerAddr", 0x0000), 
			XShortField("registerValue", 0x0000)]
class ModbusPDU06_Write_Single_Register_Exception(Packet):
	name = "Write Single Register Exception"
	fields_desc = [ XByteField("funcCode", 0x86),
			ByteEnumField("exceptCode", 1, _modbus_exceptions)]

# 0x07 - Read Exception Status (Serial Line Only)
class ModbusPDU07_Read_Exception_Status(Packet):
	name = "Read Exception Status"
	fields_desc = [ XByteField("funcCode", 0x07)]
class ModbusPDU07_Read_Exception_Status_Answer(Packet):
	name = "Read Exception Status Answer"
	fields_desc = [ XByteField("funcCode", 0x07),
			XByteField("startingAddr", 0x00)]
class ModbusPDU07_Read_Exception_Status_Exception(Packet):
	name = "Read Exception Status Exception"
	fields_desc = [ XByteField("funcCode", 0x87),
			ByteEnumField("exceptCode", 1, _modbus_exceptions)]

# 0x0F - Write Multiple Coils
class ModbusPDU0F_Write_Multiple_Coils(Packet):
	name = "Write Multiple Coils"
	fields_desc = [ XByteField("funcCode", 0x0F),
			XShortField("startingAddr", 0x0000),
			XShortField("quantityOutput", 0x0001),
			BitFieldLenField("byteCount", None, 8, count_of="outputsValue", adjust=lambda pkt,x:x),
			FieldListField("outputsValue", [0x00], XByteField("", 0x00), count_from = lambda pkt: pkt.byteCount)]
class ModbusPDU0F_Write_Multiple_Coils_Answer(Packet):
	name = "Write Multiple Coils Answer"
	fields_desc = [ XByteField("funcCode", 0x0F),
			XShortField("startingAddr", 0x0000),
			XShortField("quantityOutput", 0x0001)]
class ModbusPDU0F_Write_Multiple_Coils_Exception(Packet):
	name = "Write Multiple Coils Exception"
	fields_desc = [ XByteField("funcCode", 0x8F),
			ByteEnumField("exceptCode", 1, _modbus_exceptions)]

# 0x10 - Write Multiple Registers
class ModbusPDU10_Write_Multiple_Registers(Packet):
	name = "Write Multiple Registers"
	fields_desc = [ XByteField("funcCode", 0x10),
			XShortField("startingAddr", 0x0000),
			XShortField("quantityRegisters", 0x0001),
			BitFieldLenField("byteCount", None, 8, count_of="outputsValue", adjust=lambda pkt,x:x),
			FieldListField("outputsValue", [0x00], XByteField("", 0x00), count_from = lambda pkt: pkt.byteCount)]
class ModbusPDU10_Write_Multiple_Registers_Answer(Packet):
	name = "Write Multiple Registers Answer"
	fields_desc = [ XByteField("funcCode", 0x10),
			XShortField("startingAddr", 0x0000),
			XShortField("quantityRegisters", 0x0001)]
class ModbusPDU10_Write_Multiple_Registers_Exception(Packet):
	name = "Write Multiple Registers Exception"
	fields_desc = [ XByteField("funcCode", 0x90),
			ByteEnumField("exceptCode", 1, _modbus_exceptions)]

# 0x11 - Report Slave Id
class ModbusPDU11_Report_Slave_Id(Packet):
	name = "Report Slave Id"
	fields_desc = [ XByteField("funcCode", 0x11)]
class ModbusPDU11_Report_Slave_Id_Answer(Packet):
	name = "Report Slave Id Answer"
	fields_desc = [ XByteField("funcCode", 0x11),
			BitFieldLenField("byteCount", None, 8, length_of="slaveId"),
			ConditionalField(StrLenField("slaveId", "", length_from = lambda pkt: pkt.byteCount), lambda pkt: pkt.byteCount>0),
			ConditionalField(XByteField("runIdicatorStatus", 0x00), lambda pkt: pkt.byteCount>0)]
class ModbusPDU11_Report_Slave_Id_Exception(Packet):
	name = "Report Slave Id Exception"
	fields_desc = [ XByteField("funcCode", 0x91),
			ByteEnumField("exceptCode", 1, _modbus_exceptions)]

class ModbusADU(Packet):
	name = "ModbusADU"
	fields_desc = [ 
			XShortField("transId", 0x0001), # needs to be unique
			XShortField("protoId", 0x0000), # needs to be zero (Modbus)
			XShortField("len", None), 		# is calculated with payload
			XByteField("unitId", 0x00)] 	# 0xFF or 0x00 should be used for Modbus over TCP/IP
	# Dissects packets
	def guess_payload_class(self, payload):
		funcCode = int(payload[0].encode("hex"),16)

		if funcCode == 0x01:
			return ModbusPDU01_Read_Coils
		elif funcCode == 0x81:
			return ModbusPDU01_Read_Coils_Exception

		elif funcCode == 0x02:
			return ModbusPDU02_Read_Discrete_Inputs
		elif funcCode == 0x82:
			return ModbusPDU02_Read_Discrete_Inputs_Exception

		elif funcCode == 0x03:
			return ModbusPDU03_Read_Holding_Registers
		elif funcCode == 0x83:
			return ModbusPDU03_Read_Holding_Registers_Exception

		elif funcCode == 0x04:
			return ModbusPDU04_Read_Input_Registers
		elif funcCode == 0x84:
			return ModbusPDU04_Read_Input_Registers_Exception

		elif funcCode == 0x05:
			return ModbusPDU05_Write_Single_Coil
		elif funcCode == 0x85:
			return ModbusPDU05_Write_Single_Coil_Exception

		elif funcCode == 0x06:
			return ModbusPDU06_Write_Single_Register
		elif funcCode == 0x86:
			return ModbusPDU06_Write_Single_Register_Exception

		elif funcCode == 0x07:
			return ModbusPDU07_Read_Exception_Status
		elif funcCode == 0x87:
			return ModbusPDU07_Read_Exception_Status_Exception

		elif funcCode == 0x0F:
			return ModbusPDU0F_Write_Multiple_Coils
		elif funcCode == 0x8F:
			return ModbusPDU0F_Write_Multiple_Coils_Exception

		elif funcCode == 0x10:
			return ModbusPDU10_Write_Multiple_Registers
		elif funcCode == 0x90:
			return ModbusPDU10_Write_Multiple_Registers_Exception

		elif funcCode == 0x11:
			return ModbusPDU11_Report_Slave_Id
		elif funcCode == 0x91:
			return ModbusPDU11_Report_Slave_Id_Exception

		else:
			return Packet.guess_payload_class(self, payload)

	def post_build(self, p, pay):
		if self.len is None:
			l = len(pay)+1 #+len(p)
			p = p[:4]+struct.pack("!H", l)+p[6:]
		return p+pay


# If we know the packet is an Modbus answer, we can dissect it with
# ModbusADU_Answer(str(pkt))
# Scapy will dissect it on it's own if the TCP stream is available
class ModbusADU_Answer(Packet):
	name = "ModbusADU"
	fields_desc = [ 
			XShortField("transId", 0x0001), # needs to be unique
			XShortField("protoId", 0x0000), # needs to be zero (Modbus)
			XShortField("len", None), 		# is calculated with payload
			XByteField("unitId", 0x01)] 	# 0xFF or 0x00 should be used for Modbus over TCP/IP
	# Dissects packets
	def guess_payload_class(self, payload):
		funcCode = int(payload[0].encode("hex"),16)

		if funcCode == 0x01:
			return ModbusPDU01_Read_Coils_Answer
		elif funcCode == 0x81:
			return ModbusPDU01_Read_Coils_Exception

		elif funcCode == 0x02:
			return ModbusPDU02_Read_Discrete_Inputs_Answer
		elif funcCode == 0x82:
			return ModbusPDU02_Read_Discrete_Inputs_Exception

		elif funcCode == 0x03:
			return ModbusPDU03_Read_Holding_Registers_Answer
		elif funcCode == 0x83:
			return ModbusPDU03_Read_Holding_Registers_Exception

		elif funcCode == 0x04:
			return ModbusPDU04_Read_Input_Registers_Answer
		elif funcCode == 0x84:
			return ModbusPDU04_Read_Input_Registers_Exception

		elif funcCode == 0x05:
			return ModbusPDU05_Write_Single_Coil_Answer
		elif funcCode == 0x85:
			return ModbusPDU05_Write_Single_Coil_Exception

		elif funcCode == 0x06:
			return ModbusPDU06_Write_Single_Register_Answer
		elif funcCode == 0x86:
			return ModbusPDU06_Write_Single_Register_Exception

		elif funcCode == 0x07:
			return ModbusPDU07_Read_Exception_Status_Answer
		elif funcCode == 0x87:
			return ModbusPDU07_Read_Exception_Status_Exception

		elif funcCode == 0x0F:
			return ModbusPDU0F_Write_Multiple_Coils_Answer
		elif funcCode == 0x8F:
			return ModbusPDU0F_Write_Multiple_Coils_Exception

		elif funcCode == 0x10:
			return ModbusPDU10_Write_Multiple_Registers_Answer
		elif funcCode == 0x90:
			return ModbusPDU10_Write_Multiple_Registers_Exception

		elif funcCode == 0x11:
			return ModbusPDU11_Report_Slave_Id_Answer
		elif funcCode == 0x91:
			return ModbusPDU11_Report_Slave_Id_Exception

		else:
			return Packet.guess_payload_class(self, payload)

# Binds TCP port 502 to Modbus/TCP
bind_layers( TCP, ModbusADU_Answer, sport=502 )
bind_layers( TCP, ModbusADU, dport=502 )

def isAlive():
	ans = None;
	if connection:
		ans = connection.sr1(ModbusADU(transId=getTransId())/ModbusPDU01_Read_Coils(),timeout=timeout)
		if ans:
			return True #Alive
		else:
			return False #Dead

# Generates an unique transaction ID
def getTransId():
	global transId
	transId = transId + 1
	if transId > 65535:
		transId = 1
	return transId

# Connects to a target via TCP socket
def connectToTarget(IP="127.0.0.1",port=modport):
	try:
		global connection	
		s = socket.socket()
		s.connect((IP,int(port))) # encapsulate into try/catch
		connection = StreamSocket(s,Raw)
		return connection
	except Exception,e:
		print "Connection unsuccessful due to the following error :"
		print e.message
		return None
def closeConnectionToTarget(c):
	global connection
	connection = c
	connection.close()
	connection = None

# Verifies which function codes are supported by a Modbus Server
# Returns a list with accepted function codes
def getSupportedFunctionCodes(c):
	connection = c
	supportedFuncCodes = []
	if connection == None:
		return "Connection needs to be established first."

	print "Looking for supported function codes.."
	for i in range(0,256): # Total of 127 (legal) function codes
		ans = connection.sr1(ModbusADU(transId=getTransId())/ModbusPDU_Read_Generic(funcCode=i),timeout=timeout, verbose=0)

		# We are using the raw data format, because not all function
		# codes are supported out by this library.
		if ans:
			data = str(ans)
			data2 = data.encode('hex')
			returnCode = int(data2[14:16],16)
			exceptionCode = int(data2[17:18],16)

			if returnCode > 127 and exceptionCode == 0x01:
				# If return function code is > 128 --> error code
				#print "Function Code "+str(i)+" not supported."
				a=1
			else:
				supportedFuncCodes.append(i)
				if(function_code_name.get(i) != None):
					print "Function Code "+str(i)+"("+function_code_name.get(i)+") is supported."
				else:
					print "Function Code "+str(i)+" is supported."
		else:
			print "Function Code "+str(i)+" probably supported."
			supportedFuncCodes.append(i)

	return supportedFuncCodes


def getSupportedDiagnostics(c):
	connection = c
	supportedDiagnostics = []
	if connection == None:
		return "Connection needs to be established first."

	print "Looking for supported diagnostics codes.."
	for i in range(0,65535): # Total of 65535, function code 8, sub-function code is 2 bytes long
		ans = connection.sr1(ModbusADU(transId=getTransId())/Raw("\x08")/struct.pack(">H",i)/Raw("\x00\x00"),timeout=timeout, verbose=0)

		# We are using the raw data format, because not all function
		# codes are supported by this library.
		if ans:
			data = str(ans)
			data2 = data.encode('hex')
			returnCode = int(data2[14:16],16)
			exceptionCode = int(data2[17:18],16)

			if returnCode > 127 and exceptionCode == 0x01:
				# If return function code is > 128 --> error code
				#print "Function Code "+str(i)+" not supported."
				a=1
			else:
				supportedDiagnostics.append(i)
				print "Diagnostics Code "+str(i)+" is supported."
		else:
			print "Diagnostics Code "+str(i)+" probably supported."
			supportedDiagnostics.append(i)

	return supportedDiagnostics



