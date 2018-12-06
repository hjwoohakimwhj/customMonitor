package core;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

public class MonitorConfigItem {
	// Map<DriverName, Map<type, List<Item>>>
	public static MonitorDriverType driverRepo = new MonitorDriverType("driverType.yaml");
	private List<String> itemIds = new ArrayList<String>();
	private String monConfigId = "null";
	private String target = null;
	private String vnfNodeId = null;
	private String nsTypeId = null;
	private MonitorFormat monitorForamt = null;
	private String id = null;
	private JSONObject params = null;
	private String driverType = null;
	public MonitorConfigItem(String configId, String itemId, String target, String nsTypeId
			, String vnfNodeId, JSONObject params) {
		this.itemIds.add(itemId);
		this.monConfigId = configId;
		this.target = target;
		this.nsTypeId = nsTypeId;
		this.vnfNodeId = vnfNodeId;
		this.params = params;
		this.genConfigId();
		this.setDriverType();
	}

	private void genConfigId() {
		this.id = this.nsTypeId + "%" + this.vnfNodeId + "%" + this.target + "%" + this.monConfigId;
	}
	
	public String getId() {
		return this.id;
	}
	public void setMonitorFormat(MonitorFormat format) {
		this.monitorForamt = format;
	}
	
	private void setDriverType() {
		JSONObject monitorInfo = JSONObject.fromObject(this.params.get("monitorInfo"));
		String type = monitorInfo.getString("type");
		String item = monitorInfo.getString("item");
		if (MonitorConfigItem.searchDriver(type, item)) {
			this.driverType = "OpenStack";
			return;
		}
		this.driverType = "zabbix";
	}
	
	public static boolean searchDriver(String type, String item) {
		return MonitorConfigItem.searchDriver(type, item);
	}
	
	public String getDriverType() {
		return this.driverType;
	}
	
	public void setMonConfigId(String monConfigId) {
		this.monConfigId = monConfigId;
	}
	
	public MonitorFormat getMonitorFormat() {
		return this.monitorForamt;
	}
	
	public void addItemId(String itemId) {
		this.itemIds.add(itemId);
	}
	
	public int size() {
		return this.itemIds.size();
	}
	
	public JSONObject getParams() {
		return this.params;
	}
	
	public List<String> getItemIds(){
		return this.itemIds;
	}
	
	public String getConfigId() {
		return monConfigId;
	}
	
	public String getTarget() {
		return this.target;
	}
}
