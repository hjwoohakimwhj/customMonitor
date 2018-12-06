package core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class MonitorRepository {
	//Map<nsTypeId, Map<vnfNodeId,Map<monitorTargetName, MonitorFormat>>>
	private Map<String, Map<String,Map<String, MonitorFormat>>>  monitorRepo = Collections
			.synchronizedMap(new HashMap<String,Map<String,Map<String, MonitorFormat>>>());
	
	//use nsTypeId vnfNodeId vnfcNodeId monitorConfigId to identify a montorConfig
	private Map<String,MonitorConfigItem> quickConfig = new HashMap<String,MonitorConfigItem>();

	public MonitorRepository() {
		System.out.println("!!!!!! monitor repository initiates");
	}
	
	public Set<String> getVnf(String nsTypeId){
		return this.monitorRepo.get(nsTypeId).keySet();
	}
	public MonitorConfigItem getMonitorConfig(String id) {
		return this.quickConfig.get(id) ;
	}
	
	public void putMonitorFormat(MonitorFormat format) {
		String nsTypeId = format.getNsTypeId();
		String vnfNodeId = format.getVnfNodeId();
		String monitorTargetId = format.getMonitorTarget();
		if(!this.monitorRepo.containsKey(nsTypeId)){
			Map<String, MonitorFormat> monitorTarget = new HashMap<String, MonitorFormat>();
			monitorTarget.put(monitorTargetId, format);
			Map<String,Map<String, MonitorFormat>> monitorVNF = new HashMap<String,
					Map<String, MonitorFormat>>();
			monitorVNF.put(vnfNodeId, monitorTarget);
			this.monitorRepo.put(nsTypeId, monitorVNF);
		}else if(!this.monitorRepo.get(nsTypeId).containsKey(vnfNodeId)) {
			Map<String, MonitorFormat> monitorTarget = new HashMap<String, MonitorFormat>();
			monitorTarget.put(monitorTargetId, format);
			this.monitorRepo.get(nsTypeId).put(vnfNodeId, monitorTarget);
		}else {
			this.monitorRepo.get(nsTypeId).get(vnfNodeId).put(monitorTargetId, format);
		}
		for(String itemConfigId : format.getItemMap().keySet()) {
			this.quickConfig.put(itemConfigId, format.getItemMap().get(itemConfigId));
		}
	}
	
	public JSONObject getNsMonitor(String nsTypeId) {
		JSONObject nsChain = new JSONObject();
		for(String vnf : this.monitorRepo.get(nsTypeId).keySet()) {
			JSONArray vnfChain = new JSONArray();
			for(String monitorTarget : this.monitorRepo.get(nsTypeId).get(vnf).keySet()) {
				vnfChain.add(monitorTarget);
			}
			nsChain.put(vnf, vnfChain);
		}
		return nsChain;
	}
	public JSONArray getVnfMonitor(String nsTypeId, String vnf) {
		JSONArray vnfChain = new JSONArray();
		for(String monitorTarget : this.monitorRepo.get(nsTypeId).get(vnf).keySet()) {
			vnfChain.add(monitorTarget);
		}
		return vnfChain;
	}

}
