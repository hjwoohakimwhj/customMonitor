package OpenStackDriver;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import net.sf.json.JSONObject;

public class NewtonHttpClient {
	private URI authUri;
	private String novaBaseUrl = "http://192.168.0.21:8774/v2.1/";
	//private String heatBaseUrl = "http://192.168.0.21:8004/v1/";
	private String tenantId;
	private String password;
	private String userName;
	private CloseableHttpClient httpClient = HttpClients.custom().build();

	private String tokenId;
	private long expireTime;
	private String tenantName;
	
	public NewtonHttpClient() {
		try {
			this.authUri = new URI(new String("http://192.168.0.21:35357/v2.0/tokens").trim());
			this.userName = "demo";
			this.password = "demo";
			this.tenantName = "demo";
		}catch(URISyntaxException e) {			
		}
	}
	
	public void auth() {
		String response = this.getAuthResponse();
		JSONObject responseJSON = JSONObject.fromObject(response);
		JSONObject access = JSONObject.fromObject(responseJSON.get("access"));
		this.tokenId = JSONObject.fromObject(access.get("token")).getString("id");
		this.expireTime = System.currentTimeMillis() + 10*60*1000;
		JSONObject tenant = JSONObject.fromObject(access.get("token")).getJSONObject("tenant");
		this.tenantId = tenant.getString("id");
	}
	
	public String getAuthResponse() {
		JSONObject authBody = new JSONObject();
		JSONObject passwordCredentials = new JSONObject();
		passwordCredentials.put("username", this.userName);
		passwordCredentials.put("password", this.password);
		authBody.put("tenantName", this.tenantName);
		authBody.put("passwordCredentials", passwordCredentials);
		JSONObject auth = new JSONObject();
		auth.put("auth", authBody);
		String response = this.doPost(auth.toString());
		return response;
	}
	
	public  Map<String, Map<String, String>> doGet(String serverId) {
		try {
			long nowTime = System.currentTimeMillis();
			if(nowTime > this.expireTime) {
				this.auth();
			}
			String urlParam = this.novaBaseUrl + this.tenantId + "/servers/" + serverId;
			System.out.println("urlParam is" + urlParam);
			URI uri = new URI(new String(urlParam).trim());
			HttpUriRequest httpRequest = RequestBuilder.get().setUri(uri)
					.addHeader("Content-Type", "application/json")
					.addHeader("X-auth-Token", this.tokenId).build();
			CloseableHttpResponse response = httpClient.execute(httpRequest);
			HttpEntity entity = response.getEntity();
			byte[] data = EntityUtils.toByteArray(entity);
			String dataResponse = new String(data);
			System.out.println("response data is" + dataResponse);
			Map<String, Map<String, String>> responseMap = this.copyServerResponse(dataResponse);
			return responseMap;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String doPost(String body) {
		try {
			HttpUriRequest httpRequest = RequestBuilder.post().setUri(this.authUri)
					.addHeader("Content-Type", "application/json")
					.setEntity(new StringEntity(body,ContentType.APPLICATION_JSON)).build();
			CloseableHttpResponse response = httpClient.execute(httpRequest);
			HttpEntity entity = response.getEntity();
			byte[] data = EntityUtils.toByteArray(entity);
			String dataResponse = new String(data);
			System.out.println("openstack return base is" + dataResponse);
			return dataResponse;
		}catch(IOException e) {
			e.printStackTrace();
			return "error";
		}
	}
	
	public Map<String, Map<String, String>> copyServerResponse(String response) {
		JSONObject server = JSONObject.fromObject(response);
		JSONObject serverInfo = server.getJSONObject("server");
		//JSONObject flavor = server.getJSONObject("server").getJSONObject("flavor");
/*		JSONObject disk = new JSONObject();
		disk.put("totalDisk", flavor.getString("disk"));
		disk.put("swapDisk", flavor.getString("swap"));
		disk.put("ephemeralDisk", flavor.getString("ephemeral"));
		JSONObject cpu = new JSONObject();
		cpu.put("vCPUs", flavor.getString("vcpus"));*/
		Map<String, String> vmInfo = new HashMap<String, String>();
		vmInfo.put("vmState", serverInfo.getString("OS-EXT-STS:vm_state"));
		vmInfo.put("powerState", serverInfo.getString("OS-EXT-STS:power_state"));
		vmInfo.put("launchTime", serverInfo.getString("OS-SRV-USG:launched_at"));
		vmInfo.put("host", serverInfo.getString("hostId"));
		vmInfo.put("vmId", serverInfo.getString("id"));
		
		Map<String, Map<String, String>> returnMap = new HashMap<String, Map<String,String>>();
/*		returnJSON.put("disk", disk);
		returnJSON.put("CPU", cpu);*/
		returnMap.put("vmInfo", vmInfo);
		return returnMap;
	}
	
/*	public static void main(String[] args) {
		TestHttp httpTest = new TestHttp();
		
		String method = "user.login";	
		JSONObject params = new JSONObject();
		params.put("user", "Admin");
		params.put("password", "zabbix");
		
		UserRequestBody body = new UserRequestBody(method,params);
		String response = httpTest.doPost(body.getBodyString());	
		String auth = body.getAuth(response);
		System.out.println("Token is " + auth);*/
		
/*		String hostMethod = "host.create";
		Map<String,Object> paramsHost = new HashMap<String,Object>();
		paramsHost.put("ip", "192.168.0.58");
		paramsHost.put("macAddress", "00:50:56:20:ba:74");
		paramsHost.put("hostName", "monitorOne");
		HostRequestBody bodyHost = new HostRequestBody(hostMethod,paramsHost,auth);
		String responseHost = httpTest.doPost(bodyHost.getBodyString());
		System.out.println(responseHost);*/
		
/*		
 * 		String hostMethod = "host.delete";
		Map<String,Object> paramsHost = new HashMap<String,Object>();
		paramsHost.put("hostId", "10106");
		HostRequestBody bodyHost = new HostRequestBody(hostMethod,paramsHost,auth);
		String responseHost = httpTest.doPost(bodyHost.getBodyString());
		System.out.println(responseHost);	*/
		
/*		interfaceId =3
 * 		Map<String,Object> paramsInterface = new HashMap<String,Object>();
		paramsInterface.put("hostId", "10107");
		InterfaceBody interfaceBody = new InterfaceBody("hostinterface.get",paramsInterface,auth);
		String responseInterface = httpTest.doPost(interfaceBody.getBodyString());
		JSONArray interfaceInfo = interfaceBody.getInterfaceInfo(responseInterface);
		System.out.println(interfaceInfo);*/
		
/*		String itemMethod = "item.create";
		JSONObject paramsItem = new JSONObject();
		paramsItem.put("itemName", "memory free");
		paramsItem.put("hostId", "10107");
		paramsItem.put("interfaceId", "3");
		paramsItem.put("updateTime", 30);
		JSONObject monitorInfo = new JSONObject();
		monitorInfo.put("type", "memory");
		monitorInfo.put("item", "availableMemory");
		paramsItem.put("monitorInfo", monitorInfo);
		ItemRequestBody itemBody = new ItemRequestBody(itemMethod,paramsItem,auth);
		System.out.println(itemBody.getBodyString());
		String responseItem = httpTest.doPost(itemBody.getBodyString());
		System.out.println(responseItem);*/
		
/*		String triggerMethod = "trigger.create";
		JSONObject paramsItem = new JSONObject();
		paramsItem.put("hostName","monitorOne");
		paramsItem.put("itemName","vm.memory.size[available]");
		paramsItem.put("comparison",1);
		paramsItem.put("triggerName","memory trigger");
		paramsItem.put("valueCompare","500");
		paramsItem.put("compareUnit","M");
		paramsItem.put("function",4);
		paramsItem.put("unit","now");	
		TriggerRequestBody itemBody = new TriggerRequestBody(triggerMethod,paramsItem,auth);
		System.out.println(itemBody.getBodyString());
		String responseItem = httpTest.doPost(itemBody.getBodyString());
		System.out.println(responseItem);*/
		
/*		String mediaMethod = "mediatype.create";
		JSONObject paramsItem = new JSONObject();
		paramsItem.put("mediaName", "mediaOne");
		paramsItem.put("execPath", "mail.sh");	
		MediaType mediaType = new MediaType(mediaMethod,paramsItem,auth);
		System.out.println(mediaType.getBodyString());
		String responseItem = httpTest.doPost(mediaType.getBodyString());
		System.out.println(responseItem);*/
		
/*		String mediaAdd = "user.addmedia";
		JSONObject paramsItem = new JSONObject();
		paramsItem.put("userId", "1");
		paramsItem.put("mediaTypeId", "4");
		paramsItem.put("period", "1-7,00:00-24:00");//周一到周日全天
		paramsItem.put("sendTo", "1227116218@qq.com");	
		UserRequestBody mediaType = new UserRequestBody(mediaAdd,paramsItem,auth);
		System.out.println(mediaType.getBodyString());
		String responseItem = httpTest.doPost(mediaType.getBodyString());
		System.out.println(responseItem);*/
		
/*		String methodAction = "action.create";
		JSONObject paramsItem = new JSONObject();
		paramsItem.put("actionName","actionOne");
		paramsItem.put("hostId","10107");
		paramsItem.put("triggerName", "memory trigger");
		JSONArray operations = new JSONArray();
		JSONObject operation = new JSONObject();
		operation.put("type", "mail");
		operation.put("userId", "1");
		operation.put("mediaId","4");
		operations.add(operation);
		paramsItem.put("operations", operations);
		ActionType actionType = new ActionType(methodAction,paramsItem,auth);
		System.out.println(actionType.getBodyString());
		String responseItem = httpTest.doPost(actionType.getBodyString());
		System.out.println(responseItem);
	}*/
	
}
