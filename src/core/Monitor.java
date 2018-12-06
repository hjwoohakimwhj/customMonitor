package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import core.MonitorFormat;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import common.CommonHttpClient;

/**
 * Servlet implementation class Monitor
 */
@WebServlet("/Monitor")
public class Monitor extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private MonitorRepository monitorRepository;
	private MonitorThreads monitorThreads;
	private MonitorTimer monitorTimer;
	private CommonHttpClient httpToZabbix; 
	private CommonHttpClient httpToAlarm; 
	public static JSONObject map2JSON(Map<String,String> map) {
		JSONObject obj = new JSONObject();
		for(String key : map.keySet()) {
			obj.put(key, map.get(key));
		}
		return obj;
	}
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Monitor() {
        super();
        System.out.println("!!!!!! monitor initiates");
        this.monitorRepository = new MonitorRepository();

        this.httpToAlarm = new CommonHttpClient("http://192.168.0.20:8080/ServiceManager/alarm");
        this.httpToZabbix = new CommonHttpClient("http://192.168.0.20:8080/ServiceManager/zabbixDriver");

		int threadNumber = 5;
		int queueSize = 5;
        this.monitorThreads = new MonitorThreads(threadNumber, queueSize,this.httpToZabbix);
        this.monitorTimer = new MonitorTimer(this.monitorThreads);
    }

	/**
	 *get the ns monitorTargets: request must be "range" = "ns" "name" = nsTypeId 
	 *get the vnf monitorTargets: request must be "range" = "vnf " "name" = vnfNodeId "nsTypeId" = nsTypeId
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		System.out.println("!!!!!! in the monitor function: doGet");
		String result = null;
		if(request.getParameter("range") != null && request.getParameter("name") != null) {
			String range = request.getParameter("range");
			String name = request.getParameter("name");
			switch(range) {
				case "ns" :
					result = this.monitorRepository.getNsMonitor(name).toString();
					break;
				case "vnf" :
					result = this.monitorRepository.getVnfMonitor(name, request
							.getParameter("nsTypeId")).toString();
					break;
			}
		}
		response.getWriter().write(result);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("!!!!!! in the monitor function: doPost");
		response.setContentType("text/html;charset=UTF-8");
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader((ServletInputStream)request.getInputStream(),"utf-8"));
			StringBuffer stringBuffer = new StringBuffer();
			String tmp;
			while((tmp = br.readLine()) != null) {
				stringBuffer.append(tmp);
			}
			br.close();
			String acceptJSON = stringBuffer.toString();
			JSONObject monitorObject  = JSONObject.fromObject(acceptJSON);
			if(monitorObject.getBoolean("action")) {
				switch(monitorObject.getString("action")) {
					case "add" :
						this.addVnfcMonitor(monitorObject);
						break;
					case "delete" :
						this.deleteVnfcMonitor(monitorObject);
						break;
				}
				this.addVnfcMonitor(monitorObject);
			}else {
				this.copy(monitorObject);
			}
			response.getWriter().write("happy");
		}catch(Exception e){
			e.printStackTrace();
			response.getWriter().write("sad");
		}
	}
	private void addVnfcMonitor(JSONObject monitorObject) {
		String nsTypeId = monitorObject.getString("nsTypeId");
		String vnfNodeId = monitorObject.getString("vnfNodeId");
		String vnfcNodeId = monitorObject.getString("vnfcNodeId");
		String ip = monitorObject.getString("ip");
		JSONArray monitorConfigs = monitorObject.getJSONArray("monitorConfigs");
		String hostId = "null";
		HashMap<String ,String > monitorToItem = new HashMap<String, String>();
		for(Object monitorConfig : monitorConfigs) {
			JSONObject monitorConfigBody = JSONObject.fromObject(monitorConfig);
			String configId = monitorConfigBody.getString("id");
			String id = nsTypeId + "%" + vnfNodeId + "%" + vnfcNodeId + "%" + configId;
			MonitorConfigItem monitorConfigItem =  this.monitorRepository.getMonitorConfig(id);
			if(hostId.equals("null")) {
				//send request to the zabbix to get the group id and the proxyid
				String defaultItemId = monitorConfigItem.getItemIds().get(0);
				JSONObject body = new JSONObject();
				body.put("itemId", defaultItemId);
	  			JSONObject request = new JSONObject();
	   			request.put("type","getItem");
	   			request.put("body", body);
	     		String response  = this.httpToZabbix.doPost(request.toString());
	     		JSONObject responseJSON = JSONObject.fromObject(response);
	     		String groupId = responseJSON.getString("groupId");
	     		String proxyId = responseJSON.getString("proxyId");
				hostId = this.vnfcNodeHandler(groupId, proxyId, vnfcNodeId, ip);
			}
			JSONObject params = monitorConfigItem.getParams();
			params.put("hostId", (Object)hostId);
			params.put("itemName",id);
			monitorToItem.put(id, "null");
			this.monitorThreads.handler(params, monitorToItem,1);
		}
		while(true) {
			int count = 0;
			for(String key :  monitorToItem.keySet()) {
				if(monitorToItem.get(key) == "null") {
					break;
				}
				count++;
			}
			if(count == monitorToItem.size()) {
				break;
			}
		}
		this.refreshMonitorConfig(monitorToItem);
	}

	private void refreshMonitorConfig(HashMap<String ,String > monitorToItem) {
		for(String configId : monitorToItem.keySet()) {
			MonitorConfigItem configItem = this.monitorRepository.getMonitorConfig(configId);
			configItem.addItemId(monitorToItem.get(configId));
		}
	}

	private void deleteVnfcMonitor(JSONObject monitorObject) {
		
	}
	private void copy(JSONObject monitorInfo) {
		try {
			if(!monitorInfo.getBoolean("nsTypeId")) {
				throw new Exception("nsTypeId is missing");
			}
			String nsTypeId = monitorInfo.getString("nsTypeId");
			JSONObject monitorObject = monitorInfo.getJSONObject("monitorInfo");
			if(monitorObject.get("Info") == null) {
				throw new Exception("Info is missing");
			}
			String[] hostGroup = infoHandler(monitorObject.get("Info"));
			String hostGroupId = hostGroup[0];
			String vnfNodeId = hostGroup[1];

			if(monitorObject.get("MgmtNode") == null || hostGroupId == "null") {
				throw new Exception("MgmtNode is missing");
			}
			String proxyId = mgmtNodeHandler(monitorObject.get("MgmtNode"));

			if(monitorObject.get("VnfcNodes") == null) {
				throw new Exception("VnfcNodesId is missing");
			}
			HashMap<String,String>  hostsInfo  = new HashMap<String,String>();
			vnfcNodesHandler(monitorObject.get("VnfcNodes"),
					hostGroupId,proxyId,hostsInfo);

			if(monitorObject.get("MonitorOptions") == null) {
				throw new Exception("MonitorOptions is missing");
			}
			HashMap<String,String>  monitorToItem  = new HashMap<String,String>();
			monitorOptionsHandler(monitorObject.get("MonitorOptions")
					, hostsInfo,monitorToItem,vnfNodeId,nsTypeId);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	/*
	 * zabbix hostgroup register
	 */
	private String[] infoHandler(Object vnfNodeId) throws Exception {
		JSONObject vnfId = JSONObject.fromObject(vnfNodeId);
		String vnfGroupId = "null";
		try {
     		if(vnfId.get("vnfNodeId") == null) {
     			throw new Exception("vnfNodeId is missing");
     		}
  			JSONObject request = new JSONObject();
   			request.put("type","registerHostGroup");
   			request.put("body", vnfId);
     		vnfGroupId = this.httpToZabbix.doPost(request.toString());//use vnfGroupId to create a host group for this vnf
		}catch(Exception e) {
			e.printStackTrace();
		}
		String[] vnfGroup = {vnfGroupId, String.valueOf(vnfId.get("vnfNodeId"))};
		return vnfGroup;
	}
	
	private String vnfcNodeHandler(String groupId, String proxyId, String vnfcNodeId, String ip) {
		HashMap<String,String> vnfcToHostId = new HashMap<String, String>();
		JSONObject hostInfo = new JSONObject();
		hostInfo.put("vnfcNodeId", vnfcNodeId);
		hostInfo.put("ip",ip);
		hostInfo.put("hostGroupId", groupId);
		hostInfo.put("proxyId", proxyId);
		vnfcToHostId.put(vnfcNodeId, "null");
		this.monitorThreads.handler(hostInfo,vnfcToHostId,0);
		while(true) {
			int count = 0;
			for(String key :  vnfcToHostId.keySet()) {
				if(vnfcToHostId.get(key) == "null") {
					break;
				}
				count++;
			}
			if(count == vnfcToHostId.size()) {
				break;
			}
		}
		//return hostId
		return vnfcToHostId.get(vnfcNodeId);
	}

	/*
	 * zabbix hosts register
	 */
	private void vnfcNodesHandler(Object vnfcNodes,String hostGroupId
			, String proxyId,HashMap<String,String> vnfcToHostId) throws Exception{
		JSONArray vnfcNodesArray = JSONArray.fromObject(vnfcNodes);
		Iterator<Object> iterator = vnfcNodesArray.iterator();
		while(iterator.hasNext()) {
			JSONObject hostInfo = JSONObject.fromObject(iterator.next());
			try {
     			if(hostInfo.get("vnfcNodeId") == null || hostInfo.get("ip") == null) {
     				throw new Exception("vnfcNodeId or ip is missing");
     			}
     			hostInfo.put("hostGroupId",hostGroupId);
     			hostInfo.put("proxyId", proxyId);
     			vnfcToHostId.put(String.valueOf(hostInfo.get("vnfcNodeId")), "null");
     			this.monitorThreads.handler(hostInfo,vnfcToHostId,0);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		while(true) {
			int count = 0;
			for(String key :  vnfcToHostId.keySet()) {
				if(vnfcToHostId.get(key) == "null") {
					break;
				}
				count++;
			}
			if(count == vnfcToHostId.size()) {
				break;
			}
		}
	}
	
	/*
	 * zabbix proxy register
	 */
	private String mgmtNodeHandler(Object mgmtNode) throws Exception{
		JSONObject mgmtNodeInfo = JSONObject.fromObject(mgmtNode);
		String returnValue = "null";
		try {
   			if(mgmtNodeInfo.get("vnfcNodeId") == null || mgmtNodeInfo.get("ip") == null) {
   				throw new Exception("vnfcNodeId or ip is missing");
   			}
   			JSONObject request = new JSONObject();
   			request.put("type","registerProxy");
   			request.put("body", mgmtNodeInfo);
   			returnValue = this.httpToZabbix.doPost(request.toString());//use vnfGroupId to create a host group for this vnf
		}catch(Exception e) {
			e.printStackTrace();
		}
		return returnValue;
	}
	
	private void monitorOptionsHandler(Object monitorOptions,HashMap<String,String> vnfcToHostId,
			HashMap<String,String> monitorToItem,String vnfNodeId,String nsTypeId){
		JSONArray monitorOptionsArray = JSONArray.fromObject(monitorOptions);
		Iterator<Object> iterator = monitorOptionsArray.iterator();
		List<String> vnfMonitorTargetsChain = new ArrayList<String>();
		while(iterator.hasNext()) {
			JSONObject monitorTarget = JSONObject.fromObject(iterator.next());
			String monitorTargetString = "null";
			for(Object monitorOne :  monitorTarget.keySet()) {
				monitorTargetString = String.valueOf(monitorOne);
				break;
			}
			if(monitorTargetString == "null") {
				break;
			}
			vnfMonitorTargetsChain.add(monitorTargetString);
			JSONObject monitorTargetBody = JSONObject.fromObject(monitorTarget.get(monitorTargetString));
			String monitorInterval = monitorTargetBody.getString("interval");
			JSONArray parameters = JSONArray.fromObject(monitorTargetBody.get("parameters"));
			MonitorFormat format = new MonitorFormat(monitorTargetBody.get("format"),
					 monitorTargetString,vnfNodeId,nsTypeId, monitorInterval);
			Iterator<Object> iteratorParam = parameters.iterator();
			//monitorConfig, target
			Map<String, String> configToTarget = new HashMap<String,String>();
			Map<String, JSONObject> configToParams = new HashMap<String, JSONObject>();
			while(iteratorParam.hasNext()) {
				JSONObject monConfigId = JSONObject.fromObject(iteratorParam.next());
				String monitorConfigIdKey = "null";
				for(Object monitorOne :  monConfigId.keySet()) {
					monitorConfigIdKey = String.valueOf(monitorOne);
					break;
				}
				System.out.println(monitorConfigIdKey);
				JSONObject monitorConfigBody = JSONObject.fromObject(monConfigId.get(monitorConfigIdKey));
				String hostId = vnfcToHostId.get(String.valueOf(monitorConfigBody.get("target")));
				Object updateTime = monitorConfigBody.get("updateTime");			
				configToTarget.put(monitorConfigIdKey, String.valueOf(monitorConfigBody.get("target")));
				JSONObject params = new JSONObject();
				params.put("hostId", hostId);
				params.put("updateTime", updateTime);
				params.put("itemName", monitorConfigIdKey);
				//lack interface id
				String url = String.valueOf(JSONObject.fromObject(monitorConfigBody.get("script")).get("url"));
				JSONObject monitorInfo = parseUrl(url);
				params.put("monitorInfo", monitorInfo);
				monitorToItem.put(monitorConfigIdKey, "null");
				configToParams.put(monitorConfigIdKey, params);
				this.monitorThreads.handler(params, monitorToItem,1);
			}
			while(true) {
				int count = 0;
				for(String key :  monitorToItem.keySet()) {
					if(monitorToItem.get(key) == "null") {
						break;
					}
					count++;
				}
				if(count == monitorToItem.size()) {
					break;
				}
			}
			format.mapItemId(monitorToItem,configToTarget, nsTypeId, vnfNodeId, configToParams);
			this.monitorRepository.putMonitorFormat(format);
			format.setTimer(this.monitorTimer);
		}
	}
	
	private JSONObject parseUrl(String url) {
		JSONObject returnUrl = new JSONObject();
		String newString = url.substring(21);
		String[] strs = newString.split("/");
		returnUrl.put("type", strs[0]);
		if(strs[1].contains("%")) {
			String[] strList = strs[1].split("%");
			String param = strList[1];
			String item = "null";
			if(strs[0] == "cpu") {
				item = strList[0] + "%Minites%";
			}
			if(strs[0] == "network") {
				item = strList[0] + "%Ethx%";
			}
			returnUrl.put("item", item);
			returnUrl.put("param",param);
		}else {
			returnUrl.put("item", strs[1]);
		}
		return returnUrl;
	}
}
