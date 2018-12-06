package core;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import net.sf.json.JSONObject;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;


public class MonitorFormat {
	public static final ScriptEngine jse = new ScriptEngineManager().getEngineByName("JavaScript");
	
	//monitorConfigId
	private Map<String, MonitorConfigItem> itemMap = new HashMap<String, MonitorConfigItem>();

	private String format;
	private String monitorTarget;
	private String vnfNodeId;
	private String nsTypeId ;
	private String interval;
	public static int count =0;

	public MonitorFormat(Object format,Object monitorTarget,String vnfNodeId, String nsTypeId,
			String interval) {
		System.out.println("!!!!!! monitor formate initiates, monitor target name is " + monitorTarget + "\n");
		this.format = String.valueOf(format);
		this.monitorTarget =String.valueOf(monitorTarget);
		this.vnfNodeId = vnfNodeId;
		this.nsTypeId = nsTypeId;
		this.interval = interval;
	}

	public Map<String, MonitorConfigItem>  getItemMap(){
			return this.itemMap;
	}
	
	public void mapItemId(HashMap<String,String> map, Map<String,String> configToTarget
			, String nsTypeId, String vnfNodeId, Map<String,JSONObject> configToParams) {
		Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
		Matcher m = pattern.matcher(this.format);
		while(m.find()) {
			String itemId = map.get(m.group(1));
			MonitorConfigItem configItem = new MonitorConfigItem(m.group(1), itemId,
					configToTarget.get(m.group(1)), nsTypeId, vnfNodeId, configToParams.get(m.group(1)));
			itemMap.put(configItem.getConfigId(), configItem);
			configItem.setMonitorFormat(this);
			this.format = this.format.replace(m.group(1), configItem.getConfigId());
		}
	}
	
	public String getMonitorTarget() {
		return this.monitorTarget;
	}
	public String getInterval() {
		return this.interval;
	}
	
	public String getNsTypeId() {
		return this.nsTypeId;
	}
	
	public String getVnfNodeId() {
		return this.vnfNodeId;
	}
	
	public void setTimer(MonitorTimer monitorTimer) {
		try {
			monitorTimer.addTimer(this);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
/*	public String request(ZabbixDriver zabbixDriver,ServiceMgr serviceMgr,String monConfigId) {
		String itemId = "null";
		for(MonitorConfigItem item : this.itemIdList) {
			if(item.getConfigId() ==  monConfigId) {
				itemId = item.getItemId();
				break;
			}
		}
		HashMap<String,String> resultMap = new HashMap<String,String>();
		resultMap.put(itemId, "null");
		JSONObject params = new JSONObject();
		params.put("itemids", itemId);
		params.put("limit", 1);
		serviceMgr.handler(params, zabbixDriver, resultMap, 2);
		while(true) {
			if(resultMap.get(itemId) != "null") {
				break;
			}
		}
		return resultMap.get(itemId);
	}*/
	
	public String request(MonitorThreads monitorThreads) {
		HashMap<String,String> resultMap = new HashMap<String,String>();
		for(MonitorConfigItem configId : itemMap.values()) {
			if(configId.getItemIds().size() >= 2) {
				for(String item : configId.getItemIds()) {
					resultMap.put(item,"null");
					JSONObject params = new JSONObject();
					params.put("itemids", item);
					params.put("limit", 1);
					//System.out.print("in the request\n");
					//System.out.print(params);
					monitorThreads.handler(params, resultMap, 2);
				}
				continue;
			}
			resultMap.put(configId.getItemIds().get(0),"null" );
			JSONObject params = new JSONObject();
			params.put("itemids", configId.getItemIds().get(0));
			params.put("limit", 1);
			System.out.print("in the request\n");
			System.out.print(params);
			monitorThreads.handler(params, resultMap, 2);
		}
		while(true) {
			int count = 0;
			for(String key : resultMap.keySet()) {
				if(resultMap.get(key) == "null") {
					break;
				}
				count++;
			}
			if(count == resultMap.size()) {
				break;
			}
		}
		Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
		Matcher m = pattern.matcher(this.format);
		String returnValue = this.format;
		while(m.find()) {
			count++;
			if(this.itemMap.get(m.group(1)).size() > 1) {
				List<String> middleResult = new ArrayList<String>();
				for(String itemId : this.itemMap.get(m.group(1)).getItemIds()) {
					middleResult.add(resultMap.get(itemId));
				}
				String unit = "{{" + m.group(1) + "}}" ;
				returnValue = returnValue.replace(unit, StringUtils.join(middleResult,"+"));
				continue;
			}
			String itemValue = resultMap.get(this.itemMap.get(m.group(1)).getConfigId());
			String unit = "{{" + m.group(1) + "}}" ;
			System.out.println("unit is " + unit);
			returnValue = returnValue.replace(unit, itemValue);
		}	
		try {
			returnValue = String.valueOf(jse.eval(returnValue));
		}catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("return value is " + returnValue);
		System.out.println(count);
		return returnValue;
	}
}
