package NetOp
class DownloadXml {
	static void main(args) {
	def smbinstance =new au.com.suncorp.smb.NetFile("u391812","Xumin\$06") 
	smbinstance.connect()
	smbinstance.retrieveFile(args[0],args[1])
 }
}
