package OpenStackDriver;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import net.sf.json.JSONObject;

public class DriverRepo {
	//serverId: disk cpu vmInfo
	private Map<String, Map<String, Map<String, String>>> serversInfo;
	
	//serverId publicIp
	private Map<String , String> serverIp;
	
	//extend type, item ,command
	private Map<String, Map<String,String>> itemCommand;
	
	public DriverRepo() {
		this.serversInfo = new HashMap<String, Map<String, Map<String, String>>>();
		this.serverIp = new HashMap<String, String>();
		this.itemCommand = new HashMap<String, Map<String, String>>();
		this.extendType();
	}
	
	private void extendType(){
		String path = "/home/whj/eclipse-workspace/ServiceManager" + "/src/OpenStackDriver/ExtendType.yaml";
		Yaml yaml = new Yaml();
		JSONObject result = null;
		try {
			result =JSONObject.fromObject(yaml.load(new FileInputStream(path)));
		}catch(Exception e) {
			e.printStackTrace();
		}
		for(Object obj : result.keySet()) {
			String type = obj.toString();
			JSONObject items = JSONObject.fromObject(result.get(obj));
			Map<String, String> itemsMap = new HashMap<String, String>();
			for(Object item : items.keySet()) {
				String command = String.valueOf(items.get(item));
				String itemStr = item.toString();
				System.out.println(type);
				System.out.println(itemStr);
				System.out.println(command);
				itemsMap.put(itemStr, command);
			}
			this.itemCommand.put(type, itemsMap);
		}
	}
	
	public boolean containServer(String server) {
		return this.serversInfo.containsKey(server);
	}
	
	public String getServerIp(String server) {
		return this.serverIp.get(server);
	}
	
	public Map<String, Map<String, String>> getItemMap(){
		return this.itemCommand;
	}
	
	public void registerServer(String server, String ip) {
		System.out.println("register server");
		Map<String,Map<String, String>> serverObj = this.initiateServerObj(); 
		this.serversInfo.put(server, serverObj);
		this.serverIp.put(server, ip);
		String testValue = this.serversInfo.get(server).get("vmInfo").get("host");
		System.out.println("test value is" + testValue);
	}
	
	//for doGet
	public String getItemValue(String itemId) {
		String[] itemList = itemId.split("_");
		if(itemList.length != 3) {
			return "not exist ";
		}
		System.out.println("server is" + itemList[0]);
		System.out.println("type is" + itemList[1]);
		System.out.println("item is" + itemList[2]);
		String serverId = itemList[0];
		String type = itemList[1];
		String item = itemList[2];
		//String itemValue = this.serversInfo.get(serverId).getJSONObject(type).getString(item);
		System.out.println("contain server or not is " + this.serversInfo.containsKey(serverId));
		try {
			System.out.println("contain type or not is " + this.serversInfo.get(serverId).containsKey(type));
			System.out.println("contain item or not is " + this.serversInfo.get(serverId).get(type).containsKey(item));
		}catch(Exception e) {
			e.printStackTrace();
		}
		String itemValue = this.serversInfo.get(serverId).get(type).get(item);
		System.out.println("itemValue is " + itemValue);
		return itemValue;
	}
	
	//for check
	public String getItemValue(String serverId, String type, String item) {
		return this.serversInfo.get(serverId).get(type).get(item);
	}
	
	public String createItemId(String serverId, String type, String item) {
		String itemId = serverId + "_" + type + "_" +  item;
		return itemId;
	}
	
	public void refresh(String serverId, Map<String, Map<String, String>> result) {
		this.serversInfo.put(serverId, result);
	}

	private Map<String, Map<String, String>> initiateServerObj() {
		Map<String, Map<String,String>> serverInfo = new HashMap<String, Map<String, String>>();
		Map<String, String> vmInfo = new HashMap<String, String>();
		vmInfo.put("vmState", "null");
		vmInfo.put("powerState", "null");
		vmInfo.put("launchTime", "null");
		vmInfo.put("host", "null");
		vmInfo.put("vmId", "null");
		
		serverInfo.put("vmInfo", vmInfo);
		
		Map<String,String> disk = new HashMap<String, String>();
		disk.put("totalDisk", "null");
		disk.put("freeDisk", "null");

		serverInfo.put("disk", disk);
		System.out.println("initiate the serverObj");
		return serverInfo;
	}
}
