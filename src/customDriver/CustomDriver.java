package customDriver;

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
 * Servlet implementation class CustomDriver
 */
@WebServlet("/CustomDriver")
public class CustomDriver extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private CustomRepo customRepo;
	private CustomTimer customTimer;
       
    public CustomDriver() {
        super();
        customRepo = new CustomRepo();
        customTimer = new CustomTimer(20*1000);
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String result = "null";
		System.out.println(request.getParameter("itemId"));
		if(request.getParameter("itemId") != null) {
			String monitorItemId = request.getParameter("itemId");
			try {
				result = this.customRepo.getItemValue(monitorItemId);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		response.getWriter().write(result);
	}

	//serverId filePack fileType command ip monitorTarget monitorConfig
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
			String filePath = monitorObject.getString("filePath");
			String ip = monitorObject.getString("ip");
			String monitorTargetId = monitorObject.getString("monitorTargetId");
			String monitorConfigId = monitorObject.getString("monitorConfigId");
			String command = monitorObject.getString("command");
			String itemId = this.customRepo.register(serverId, monitorTargetId
					, monitorConfigId, filePath, ip, command);
			String updateTime = monitorObject.getString("updateTime");
			this.customTimer.addTimer(itemId, customRepo, updateTime);
			JSONObject itemJSON = new JSONObject();
			itemJSON.put("itemId", itemId);
			response.getWriter().write(itemJSON.toString());
		}catch(Exception e){
			e.printStackTrace();
			response.getWriter().write("sad");
		}
	}
}
