package customDriver;

import java.util.HashMap;
import java.util.Map;

import com.jcraft.jsch.ChannelSftp;

import common.SFTPHelper;

public class CustomRepo {
	//serverId monitorTargetId monitorConfigId filePack
	private Map<String, Map<String, Map<String, String>>> serversInfo;
	
	//serverId publicIp
	private Map<String , String> serverIp;
	
	//itemId value
	private Map<String, Map<String,String>> quickCache;

	public CustomRepo() {
		this.serverIp = new HashMap<String,String>();
		this.quickCache = new HashMap<String,Map<String,String>>();
		this.serversInfo = new HashMap<String, Map<String, Map<String,String>>>();
	}

	public String register(String server, String monitorTarget, String monitorConfig,
			String filePath, String ip, String command) {
		this.serverIp.put(server, ip);
		if(!this.serversInfo.containsKey(server)) {
			Map<String, String> monitorTargetMap = new HashMap<String, String>();
			monitorTargetMap.put(monitorConfig, filePath);
			Map<String, Map<String, String>> serverMap = new HashMap<String, Map<String, String>>();
			serverMap.put(monitorTarget, monitorTargetMap);
			this.serversInfo.put(server, serverMap);
		}else if( !this.serversInfo.get(server).containsKey(monitorTarget)) {
			Map<String, String> monitorTargetMap = new HashMap<String, String>();
			monitorTargetMap.put(monitorConfig, filePath);
			this.serversInfo.get(server).put(monitorTarget, monitorTargetMap);
		}else {
			this.serversInfo.get(server).get(monitorTarget).put(monitorConfig, filePath);
		}
		String itemId =  this.createItemId(server, monitorTarget, monitorConfig, command, filePath);
		this.transport(ip, filePath);
		return itemId;
	}
	
	private String createItemId(String server, String monitorTarget, String monitorConfig
			, String command, String filePath) {
		String itemId = server + "_" + monitorTarget + "_" + monitorConfig;
		Map<String, String> itemInfo = new HashMap<String, String>();
		itemInfo.put("value", "null");
		itemInfo.put("command", command);
		String[] fileDir = filePath.split("/");
		String fileName = fileDir[fileDir.length-1];
		System.out.println("fileName is " + fileName);
		itemInfo.put("fileName", fileName);
		this.quickCache.put(itemId, itemInfo);
		return itemId;
	}
	
	private void transport(String ip, String filePath) {
    	SFTPHelper sftpTransfer = new SFTPHelper(ip);
        String src = filePath; // 本地文件名
        String dst = "/tmp/"; // 目标文件名
        try {
        	ChannelSftp chSftp = sftpTransfer.getChannel(60000);
        	chSftp.put(src, dst, ChannelSftp.OVERWRITE); // 代码段2
        	chSftp.quit();
        	sftpTransfer.closeChannel();
        }catch(Exception e) {
        	System.out.println("transport file error");
        	e.printStackTrace();
        }
    }
	
	public String getItemValue(String itemId) {
		return this.quickCache.get(itemId).get("value");
	}
	
	public Map<String, String> getItemInfo(String itemId) {
		return this.quickCache.get(itemId);
	}
	
	public String getIp(String itemId) {
		String[] itemIdArray = itemId.split("_");
		String serverId = itemIdArray[0];
		return this.serverIp.get(serverId);
	}
	
	public void updateItemValue(String itemId, String value) {
		this.quickCache.get(itemId).put("value", value);
	}
}
