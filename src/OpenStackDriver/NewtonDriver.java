package OpenStackDriver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;
/**
 * Servlet implementation class NewtonDriver
 */
@WebServlet("/NewtonDriver")
public class NewtonDriver extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private DriverRepo driverRepo; 
    private NewtonHttpClient newtonHttpClient;
    private NewtonTimer newtonTimer;

    public NewtonDriver() {
        super();
        System.out.println("newton Driver initiates");
        driverRepo = new DriverRepo(); 
        this.newtonHttpClient = new NewtonHttpClient();
        
        long gap = 30000;
        this.newtonTimer = new NewtonTimer(gap);
    }

    //we need the itemId as the parameter
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String result = "null";
		System.out.println(request.getParameter("itemId"));
		if(request.getParameter("itemId") != null) {
			String monitorItemId = request.getParameter("itemId");
			try {
				result = this.driverRepo.getItemValue(monitorItemId);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		response.getWriter().write(result);
	}

	//we need the request like this:  serverId publicIp type item
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			String serverId = monitorObject.getString("serverId");
			String type = monitorObject.getString("type");
			String item = monitorObject.getString("item");
			String ip = monitorObject.getString("item");
			String itemId = this.register(serverId, type, item, ip);
			JSONObject itemJSON = new JSONObject();
			itemJSON.put("itemId", itemId);
			response.getWriter().write(itemJSON.toString());
		}catch(Exception e){
			e.printStackTrace();
			response.getWriter().write("sad");
		}
	}

	//return itemId
	public String register(String serverId, String type, String item, String ip) {
		if(!this.driverRepo.containServer(serverId)) {
			System.out.println("not contain the server");
			this.driverRepo.registerServer(serverId, ip);
		}
		if(this.driverRepo.getItemValue(serverId, type, item).equals("null")) {
			//means the timer is not set
			this.newtonTimer.addTimer(serverId, this.newtonHttpClient, this.driverRepo);
		}
		return this.driverRepo.createItemId(serverId, type, item);
	}
}
