package core;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import common.CommonHttpClient;

/**
 * Servlet implementation class Alarm
 */
@WebServlet("/Alarm")
public class Alarm extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String name  = "AlarmModule";
	private AlarmRepository alarmRepository;
	private CommonHttpClient httpToMonitor;

    public Alarm() {
        super();
		System.out.println("!!!!!! alarm initiates");
		this.alarmRepository = new AlarmRepository(); 
        this.httpToMonitor = new CommonHttpClient("http://192.168.0.20:8080/ServiceManager/monitor");
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("!!!!!! in the alarm function : doGet");
		request.setCharacterEncoding("UTF-8");
		response.getWriter().write("welcome to the alarm");
/*		String nsTypeId = request.getParameter("nsTypeId");
		String alarmId = request.getParameter("alarmId");
		String result = "false";
		if(this.getAlarmStatus(nsTypeId, alarmId)) {
			result = "true";
		}
		response.getWriter().write(result);*/
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("!!!!!! in the alarm function : doPost");
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

			JSONObject alarmObject  = JSONObject.fromObject(acceptJSON);
			if(!alarmObject.getBoolean("nsTypeId")) {
				throw new Exception("error ! the request body lacks nsTypeId");
			}

			if(!alarmObject.getBoolean("alarmInfo")) {
				throw new Exception("error ! the request body lacks alarmInfo");
			}

			this.addAlarmInfo(alarmObject);
			response.getWriter().write("happy");
		}catch(Exception e){
			e.printStackTrace();
			response.getWriter().write("sad");
		}
	}
	
	public String getName() {
		return this.name;
	}

	/*
	 * alarmBody contains the nsTypeId
	 */
	private void addAlarmInfo(JSONObject alarmBody) throws URISyntaxException {
		String nsTypeId = alarmBody.getString("nsTypeId");
		JSONObject request = new JSONObject();
		request.put("range", "ns");
		request.put("name", nsTypeId);
		String response = this.httpToMonitor.doGet(request);
		JSONObject result = JSONObject.fromObject(response);
		Map<String, List<String>> targetsToVnf =  new HashMap<String,List<String>>();
		for(Object vnf : result.keySet()) {
			JSONArray monitorTargets = JSONArray.fromObject(result.get(vnf));
			List<String> targetList = new ArrayList<String>();
			for(Object monitorTarget : monitorTargets) {
				targetList.add(String.valueOf(monitorTarget));
			}
			targetsToVnf.put(String.valueOf(vnf), targetList);
		}
		this.alarmRepository.addAlarmInfo(alarmBody, targetsToVnf);
	}

	private Boolean getAlarmStatus(String nsTypeId, String alarmId) {
		return this.alarmRepository.getAlarmStatus(nsTypeId, alarmId);
	}
}
