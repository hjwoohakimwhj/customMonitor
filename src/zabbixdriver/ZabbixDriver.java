package zabbixdriver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Servlet implementation class ZabbixDriver
 */
@WebServlet("/ZabbixDriver")
public class ZabbixDriver extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public String auth;
	public String hostId;
	public String interfaceId;
	public String itemId;
	public String hostName;
	public String triggerId;
	public String mediaTypeId;
	public String mediaId;
	public String actionId;
	public String itemKey;
	public final static String userId = "1";
	ZabbixHttpClient httpClient = new ZabbixHttpClient();
	
	private String userName;
	private String password;
       
    public ZabbixDriver() {
        super();
        System.out.println("!!!!!! zabbix driver initiates");
		this.userName = "Admin";
		this.password = "zabbix";
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		System.out.println("!!!!!! in the zabbix function : doGet");
		System.out.println(request);
		response.getWriter().write("welcome to the zabbix");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("!!!!!! in the zabbix function : doPost");
		response.setContentType("text/html;charset=UTF-8");
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader((ServletInputStream)request.getInputStream(),"utf-8"));
			StringBuffer stringBuffer = new StringBuffer();
			String tmp;
			while((tmp = br.readLine()) != null) {
				stringBuffer.append(tmp);
			}
			br.close();
			String accept = stringBuffer.toString();
			JSONObject acceptJSON  = JSONObject.fromObject(accept);
			String requestType = acceptJSON.getString("type");
			JSONObject requestBody = acceptJSON.getJSONObject("body");
			String result = null;
			switch(requestType) {
				case "registerHostGroup" :
					result = this.registerHostGroup(requestBody);
					break;
				case "registerProxy" :
					result = this.registerProxy(requestBody);
					break;
				case "registerHost" :
					result = this.registerHost(requestBody);
					break;
				case "createItem" :
					result = this.createItem(requestBody);
					break;
				case "getHistory" :
					result = this.getHistory(requestBody);
					break;
				case "getItem" :
					result = this.getItem(requestBody);
					break;
			}
			response.getWriter().write(result);
		}catch(Exception e){
			e.printStackTrace();
			response.getWriter().write("sad");
		}
	}
	
	private String getAuth() {
		JSONObject params = new JSONObject();
		params.put("user", this.userName);
		params.put("password", this.password);
		String method = "user.login";
		UserRequestBody body = new UserRequestBody(method, params);
		String response = httpClient.doPost(body.getBodyString());	
		return body.getAuth(response);
	}
	
	private String registerHostGroup(JSONObject vnfJSON) {
		System.out.println("============");
		System.out.println(vnfJSON);
		String auth = getAuth();
		String method = "hostgroup.create";
		HostGroupRequestBody body = new HostGroupRequestBody(method, vnfJSON,auth);
		String response = httpClient.doPost(body.getBodyString());	
		return body.getHostGroupId(response);
	}
	
	private String registerProxy(JSONObject mgmtNode) {
		String auth = getAuth();
		String method = "proxy.create";
		ProxyRequestBody body = new ProxyRequestBody(method, mgmtNode,auth);
		String response = httpClient.doPost(body.getBodyString());	
		return body.getProxyId(response);
	}

	private String registerHost(JSONObject hostInfo) {
		String auth = getAuth();

		JSONObject params = new JSONObject();
		params.put("ip", hostInfo.get("ip"));
		params.put("host",hostInfo.get("vnfcNodeId") );
		params.put("proxy_hostid", hostInfo.get("proxyId"));
		

		JSONArray groupIds = new JSONArray();
		JSONObject groupId = new JSONObject();
		groupId.put("groupid", hostInfo.get("hostGroupId"));
		groupIds.add(groupId);
		params.put("groups", groupIds);

		String method = "host.create";
		HostRequestBody body = new HostRequestBody(method, params,auth);
		String response = httpClient.doPost(body.getBodyString());	
		System.out.println(response);
		return body.getHostId(response);
	}
	
	private String getInterfaceId(String hostId,String auth) {
		String method = "hostinterface.get";
		JSONObject params = new JSONObject();
		params.put("hostId", hostId);
		InterfaceBody body = new InterfaceBody(method,params,auth);
		String response = httpClient.doPost(body.getBodyString());	
		return body.getInterfaceInfo(response);
	}
	
	private String createItem(JSONObject monitorConfig) {
		String auth = getAuth();
		String method = "item.create";
		String hostId = String.valueOf(monitorConfig.get("hostId"));
		String interfaceId = getInterfaceId(hostId,auth);
		monitorConfig.put("interfaceId",interfaceId);
		ItemRequestBody body = new ItemRequestBody(method,monitorConfig,auth);
		String response = httpClient.doPost(body.getBodyString());	
		return body.getItemId(response);
	}
	
	//this itemInfo must contain the "itemId"
	private String getItem(JSONObject itemInfo) {
		String auth = getAuth();
		String method = "item.get";
		ItemRequestBody body = new ItemRequestBody(method,itemInfo,auth);
		String response = httpClient.doPost(body.getBodyString());	
		return body.getItemHostProxy(response).toString();
	}
	
	private String getHistory(JSONObject params) {
		String auth = getAuth();
		String method = "history.get";
		HistoryRequestBody body = new HistoryRequestBody(method,params,auth);
		String response = "";
		boolean blank = true;
		do {
			response = httpClient.doPost(body.getBodyString());
			blank = body.getResult(response);
			try {
				Thread.sleep(500);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}while(blank);
		return body.getValue(response);
	}
}
