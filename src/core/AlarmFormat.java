package core;
import net.sf.json.JSONArray;
import java.util.HashMap;
import java.util.HashSet;

import net.sf.json.JSONObject;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;

public class AlarmFormat {
	public final static String alarmRootPath = "/tmp/";
	private String alarmId;
	private String csarFilePath;//absolute
	private String fileName;

	//private String packType = null;
	private String statFormat;
	//private String relPath = null;
	//private String outputEnv = null;
	private String comparison;
	private String threshold;
	//private String description = null;
	private String alarmStatus = "false"; //false or true
	//private String tmpStatFormat;
	private String newestValue;
	
	//the time which the shell run at
	private String interval;
	private String nsTypeId;

	//String->monitorTargetName		<String->"value" ,String->"vnf" >
	private Map<String, Map<String,String>> involveMonitorTargets = new HashMap<String, Map<String,String>>();
	//String->VNF related to this alarm
	private Set<String> vnfSet = new HashSet<String>();
	public static final ScriptEngine jse = new ScriptEngineManager().getEngineByName("JavaScript");
	//compressType -> the format of the monitor file which user uploads

	public static final List<String> compressType = new ArrayList<String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			add(".tar.gz");
			add(".tar");
			add(".gz");
			add(".tgz");
		}
	};
	public static final Map<String, String> compareType = new HashMap<String,String>(){
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			put("lt","<");
			put("gt",">");
			put("eq","==");
		}
	};
	public AlarmFormat(String alarmId, String nsTypeId,
			JSONObject alarmInfo,Map<String, List<String>> targetToVnf) {
		System.out.println("!!!!!! alarmFormat initiates, the alarm id is " + alarmId + "\n");
		this.alarmId = alarmId;
		filePathCopy(alarmInfo.getString("csarFilePath"));
		this.comparison = alarmInfo.getString("comparison");
		//this.outputEnv = alarmInfo.getString("outputEnv");
		//this.packType  = alarmInfo.getString("");
		this.statFormat = alarmInfo.getString("statFormat");
		//this.relPath = alarmInfo.getString("relPath");
		this.threshold = alarmInfo.getString("threshold");
		//this.description = alarmInfo.getString("description");
		JSONArray monitorTargets = JSONArray.fromObject(alarmInfo.get("involveMetrics"));
		
		//attention!!!!!! add the interval for the monitor shell to run
		this.interval = alarmInfo.getString("interval");
		this.nsTypeId = nsTypeId;

		for(Object obj : monitorTargets) {
			String targetName = String.valueOf(obj);
			String vnf = this.searchVnf(targetToVnf,targetName);
			Map<String,String> targetMap = new HashMap<String,String>();
			targetMap.put("value", "null");
			targetMap.put("vnf", vnf);
			this.vnfSet.add(vnf);
			involveMonitorTargets.put(targetName, targetMap);
		}
	}
	
	public Set<String> getVnfSet(){
		return this.vnfSet;
	}
	
	private String searchVnf(Map<String, List<String>> targetToVnf,String target) {
		for(String key : targetToVnf.keySet()) {
			if(targetToVnf.get(key).contains(target)) {
				return key;
			}
		}
		return "null";
	}

	private void filePathCopy(String filePath) {
		this.csarFilePath = AlarmFormat.alarmRootPath + filePath;
		String[] filePathArray  = filePath.split("/");
		this.fileName = filePathArray[filePathArray.length-1];
	}
	
	private void setAlarmStatus(String status) {
		this.alarmStatus = status;
	}

	public  synchronized String getAlarmStatus() {
		return this.alarmStatus;
	}
	
	public String getAlarmId() {
		return this.alarmId;
	}
	
	public String getInterval() {
		return this.interval;
	}
	public String getNsTypeId() {
		return this.nsTypeId;
	}
	
	public void setTimer(AlarmTimer alarmTimer) {
		try {
			alarmTimer.addTimer(this);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void copy(JSONObject vnfsMonitor) {
		this.refreshMonitorTargets(vnfsMonitor);
		String tmpStatFormat = new String(this.statFormat);
		String alarmExpression = this.refreshTempStat(tmpStatFormat);
		this.refreshLatestValue(alarmExpression);
		this.refreshStatus();

		//if the status is true means alarm occurs ,so need something to tell the user

	}
	
	private void refreshMonitorTargets(JSONObject vnfsMonitor) {
		for(String monitorTarget : this.involveMonitorTargets.keySet()) {
			String vnf = this.involveMonitorTargets.get(monitorTarget).get("vnf");
			this.involveMonitorTargets.get(monitorTarget).put("value",
					JSONObject.fromObject(vnfsMonitor.get(vnf)).getString(monitorTarget));
		}
	}
	/*
	 * before running this function , we must run the refresh monitorTarget first
	 * result is the expression which remove all the "{{" and "}}"
	 */
	private String refreshTempStat(String tmpStatFormat) {
		for(String key : this.involveMonitorTargets.keySet()) {
			String newKey = "{{" + key + "}}";
			tmpStatFormat = tmpStatFormat.replace(newKey, this.involveMonitorTargets.get(key).get("value"));
		}
		return tmpStatFormat;
	}
	
	private void refreshLatestValue(String expression) {
		try {
			//something like /tmp/
			String dirString = this.extractDir();
			File dir = new File(dirString);
			Process ps = Runtime.getRuntime().exec(expression, null, dir);
			ps.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			StringBuffer stringBr = new StringBuffer();
			String line;
			while((line = br.readLine()) != null) {
				stringBr.append(line);
			}
			this.newestValue = stringBr.toString();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	private String extractDir() {
		String cmd = null;
		String dir = StringUtils.substringBefore(this.csarFilePath, this.fileName);
		if(this.csarFilePath.endsWith(".tar")) {
			cmd = "tar -xvf " + this.csarFilePath + " -C " + dir;
		}
		if(this.csarFilePath.endsWith(".tar.gz") || this.csarFilePath.endsWith(".tgz")) {
			cmd = "tar -xzvf " + this.csarFilePath + " -C " + dir;
		}
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			if(p.exitValue() != 0) {
				System.out.println("tar error");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return dir;
	}
	
	private void refreshStatus() {
		String compare = AlarmFormat.compareType.get(this.comparison);
		String expression = this.newestValue + compare + this.threshold;
		try {
			this.setAlarmStatus(String.valueOf(jse.eval(expression)));
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
