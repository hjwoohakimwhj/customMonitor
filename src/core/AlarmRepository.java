package core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import core.AlarmFormat;
import net.sf.json.JSONObject;

public class AlarmRepository {
	//String->nsTypeId		AlarmFormat->alarm
	private Map<String,List<AlarmFormat>> alarmInfoMap = Collections.synchronizedMap(new HashMap<String, 
			List<AlarmFormat>>());
	
	private AlarmTimer alarmTimer;

	public AlarmRepository() {
		System.out.println("!!!!!! alarmRepository initiates");
		this.alarmTimer = new AlarmTimer();
	}

	/*
	 * each slice will use only once
	 */
	public void addAlarmInfo(JSONObject alarm,Map<String, List<String>> targetToVnf) {
		String nsTypeId = alarm.getString("nsTypeId");
		JSONObject alarmInfo = JSONObject.fromObject(alarm.get("alarmInfo"));

		Iterator<?> alarmKey = alarmInfo.keys();
		while(alarmKey.hasNext()) {
			String alarmId = String.valueOf(alarmKey.next());
			JSONObject alarmEntry = alarmInfo.getJSONObject(alarmId);
			AlarmFormat alarmFormat = new AlarmFormat(alarmId,nsTypeId, alarmEntry,targetToVnf);
			this.alarmInfoMap.get(nsTypeId).add(alarmFormat);
			alarmFormat.setTimer(this.alarmTimer);
		}
	}
	
	public boolean getAlarmStatus(String nsTypeId, String alarmId) {
		boolean status = false;
		for(AlarmFormat alarmFormat : this.alarmInfoMap.get(nsTypeId)) {
			if (alarmFormat.getAlarmId().equals(alarmId)) {
				status =Boolean.valueOf(alarmFormat.getAlarmStatus());
				break;
			}
		}
		return status;
	}

}
