package core;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class MonitorDriverType {
	//Map<OpenStack, Map<type,List<item>>>
	public Map<String, Map<String, List<String>>> driverInfo = new HashMap<String
			, Map<String, List<String>>>();
	private Yaml yaml;
	private JSONObject result;
	public MonitorDriverType(String filePath){
		this.readConfig(filePath);
	}
	
	private void readConfig(String filePath){
		String path = "/home/whj/eclipse-workspace/ServiceManager" + "/src/core/" + filePath;
		yaml = new Yaml();
		try {
			result =JSONObject.fromObject(yaml.load(new FileInputStream(path)));
		}catch(Exception e) {
			e.printStackTrace();
		}
		JSONArray arrayList = JSONArray.fromObject(result.get("driverTypes"));
		//right now we only use the first array->OpenStack
		JSONObject driverType = JSONObject.fromObject(arrayList.get(0));
		String driverName = driverType.getString("driverType");
		JSONArray monitorInfo = JSONArray.fromObject(driverType.get("monitorType"));
		Map<String, List<String>> driverMap = new HashMap<String, List<String>>();
		for(Object key : monitorInfo) {
			JSONObject keyEntry = JSONObject.fromObject(key);
			String keyName = keyEntry.getString("key");
			JSONArray itemList = JSONArray.fromObject(keyEntry.get("item"));
			List<String> itemArray = new ArrayList<String>();
			for(Object item : itemList) {
				JSONObject itemJSON = JSONObject.fromObject(item);
				itemJSON.getString("itemName");
				itemArray.add(itemJSON.getString("itemName"));
			}
			driverMap.put(keyName, itemArray);
		}
		this.driverInfo.put(driverName, driverMap);
	}
	
	public List<String> getList(String driverName, String typeName){
		return this.driverInfo.get(driverName).get(typeName);
	}
	
	public boolean contain(String type, String item) {
		if(!this.driverInfo.get("OpenStack").containsKey(type)) {
			return false;
		}
		if(!this.driverInfo.get("OpenStack").get(type).contains(item)) {
			return false;
		}
		return true;
	}
	
	
/*	public static void main(String[] args) throws FileNotFoundException {
		MonitorDriverType driver = new MonitorDriverType("driverType.yaml");
		System.out.println(driver.getList("OpenStack", "CPU"));
	}*/
}
