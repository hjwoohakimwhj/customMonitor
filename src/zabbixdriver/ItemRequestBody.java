package zabbixdriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
public class ItemRequestBody extends RequestBody{
	private static List<String> methodList = Collections.synchronizedList(
			new ArrayList<String>(){{add("item.update"); add("item.create");add("item.delete");add("item.get");}});
	
	//upload the custom script to the zabbix server execPath
	private static final String execPath = "/home/customeScript/";
	
	private static final ItemType itemType = new ItemType();
	
	//value_type:0 - numeric float 	1 - character 2 - log	3 - numeric unsigned	4 - text.

	
	public ItemRequestBody(String method, JSONObject params,String auth) {
		super(method,params,auth);
	}
	
	public boolean checkMethod(String method) {
		if(methodList.contains(method)) {
			return true;
		}
		return false;
	}
	
	public static synchronized void addUserMethod(String method) {
		methodList.add(method);
	}
	
	public void setParams(String method, JSONObject params) {
		switch(method) {
			case "item.create" :
				setItemCreate(params);
				break;
			case "item.delete" :
				setItemDelete(params);
				break;
			case "item.update" :
				setItemUpdate(params);
			case "item.get" :
				setItemGet(params);
			default :
				break;
		}
	}
	
	//itemName hostId interfaceId monitorInfo
	public void setItemCreate(JSONObject params) {
		JSONObject parameters = new JSONObject();
		parameters.put("name",params.get("itemName"));
		parameters.put("hostid",params.get("hostId"));
		parameters.put("interfaceid", params.get("interfaceId"));
		parameters.put("delay", params.get("updateTime"));
		monitorItem(parameters,(JSONObject)params.get("monitorInfo"));
	}
	
	//we only delete one item each time,but we still use the list to adapt to the zabbix
	public void setItemDelete(JSONObject params) {
		List<String> parameters = new ArrayList<String>();
		parameters.add(String.valueOf(params.get("itemId")));
		body.put("params", parameters);		
	}
	
	public void setItemGet(JSONObject params) {
		JSONObject parameters = new JSONObject();
		JSONArray itemList = new JSONArray();
		itemList.add(String.valueOf(params.get("itemId")));
		parameters.put("itemids",itemList);
		body.put("params", parameters);
	}
	
	
	//just use to enable item or disable it
	public void setItemUpdate(JSONObject params) {
		JSONObject parameters = new JSONObject();
		parameters.put("itemid",String.valueOf(params.get("itemId")));
		parameters.put("status",(int)params.get("status"));
		body.put("params", parameters);
	}
	
	/**
	 * monitor is must contain the type updateTime item
	 * type:cpu ... 
	 * item: avaiableMemory ...
	 * if need parameter,param is required
	 * (localPath)
	 * (returnValueType)
	 */
	public void monitorItem(JSONObject parameters,JSONObject monitorJSON) {	
		HashMap<String,Object> item = new HashMap<String,Object>();
		if(!(monitorJSON.containsKey("type")&&monitorJSON.containsKey("item"))){
			return ;
		}
		switch(String.valueOf(monitorJSON.get("type"))) {
			case "cpu" :
				//item is like availableMemory
				item = itemType.getCpu().get(monitorJSON.get("item"));
				break;
			case "memory" :
				item = itemType.getMemory().get(monitorJSON.get("item"));
				break;
			case "service" :
				item = itemType.getService().get(monitorJSON.get("item"));
				break;
			case "network" :
				item = itemType.getNetwork().get(monitorJSON.get("item"));
				break;
			case "os" :
				item = itemType.getOs().get(monitorJSON.get("item"));
				break;
			case "process" :
				item = itemType.getProcess().get(monitorJSON.get("item"));
				break;
			case "security" :
				item = itemType.getSecurity().get(monitorJSON.get("item"));
				break;
		}
		if(String.valueOf(item.get("key_")).indexOf("%")!= -1) {
			String keyReplace = JSONHandler.replaceItemChar(String.valueOf(item.get("key_")),
					String.valueOf(monitorJSON.get("param")));
			parameters.put("key_",keyReplace);
		}else {
			parameters.put("key_", item.get("key_"));
		}
		parameters.put("type", item.get("type"));
		parameters.put("value_type", item.get("value_type"));
		body.put("params", parameters);
	}
	
	public String getItemId(String response) {
		return JSONHandler.getId(response, "itemids");
	}
	
	public JSONObject getItemHostProxy(String response) {
		return JSONHandler.getHostProxy(response);
	}
}
