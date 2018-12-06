package core;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import net.sf.json.JSONObject;

public class Mongo {
	private MongoClient mongoClient;
	private MongoClientOptions.Builder options = new MongoClientOptions.Builder();
	private MongoClientOptions mongoOptions;

	public Mongo(String ip) {
		this.options.socketTimeout(0);
		this.options.connectTimeout(30000);
		this.options.maxWaitTime(5000);
		this.mongoOptions = options.build();
		this.mongoClient = new MongoClient(ip,this.mongoOptions);
	}

	public Mongo() {
		this("192.168.0.20");
		System.out.println("!!!!!! mongo initiates");
	}
	
	/*
	 * alarm module use this function to get the monitorTarget value
	 */
	public JSONObject getVnfMonitorTarget(String nsTypeId, String vnf) {
		MongoDatabase	mongoDatabase = this.mongoClient.getDatabase(nsTypeId);
		JSONObject vnfMonitor = new JSONObject();
		MongoCollection<Document> collection = mongoDatabase.getCollection(vnf);
		FindIterable<Document> iterator = collection.find();
		MongoCursor<Document> mongoCursor = iterator.iterator();
		while(mongoCursor.hasNext()) {
			Document doc = mongoCursor.next();
			String monitorTarget = String.valueOf(doc.get("monitortarget"));
			List<Object> value = (ArrayList<Object>) doc.get("data");
			JSONObject valueLatest = JSONObject.fromObject(value.get(value.size()-1));
			String valueStr = String.valueOf(valueLatest.get("value"));
			vnfMonitor.put(monitorTarget, valueStr);
		}
		return vnfMonitor;
	 }
	
	public void putMonitorTarget(String nsTypeId, String vnf, String monitorTarget, String value) {
		MongoDatabase	mongoDatabase = this.mongoClient.getDatabase(nsTypeId);
		JSONObject vnfMonitor = new JSONObject();
		MongoCollection<Document> collection = mongoDatabase.getCollection(vnf);
		FindIterable<Document> iterator = collection.find();
		MongoCursor<Document> mongoCursor = iterator.iterator();
		while(mongoCursor.hasNext()) {
			Document doc = mongoCursor.next();
			String monitorTargetName = String.valueOf(doc.get("monitortarget"));
			if(monitorTargetName.equals(monitorTarget)) {
				List<Object> valueList = (ArrayList<Object>) doc.get("data");
				JSONObject element = new JSONObject();
				element.put("value", value);
				element.put("time", System.currentTimeMillis());
				valueList.add(element);
				collection.updateOne(Filters.eq("monitortarget", monitorTarget), new Document("$set"
						, new Document("data",valueList)));
			}
		}
	}
}
