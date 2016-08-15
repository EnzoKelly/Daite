package networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;

import networking.chat.ChatMessage;

/*
 * This Java source file was auto generated by running 'gradle buildInit --type java-library'
 * by 'andreas' at '8/12/16 4:39 AM' with Gradle 2.6
 *
 * @author andreas, @date 8/12/16 4:39 AM
 */
public class Server {

    public static void main(String[] args) {
    	Logger logger = LoggerFactory.getLogger(Server.class);
    	logger.info("Test");
    	
    	Configuration config = new Configuration();
    	config.setPort(6969);
    	SocketConfig socketConfig = new SocketConfig();
    	socketConfig.setReuseAddress(true);
    	config.setSocketConfig(socketConfig);
    	
    	SocketIOServer server = new SocketIOServer(config);
    	
    	SocketIONamespace chatNamespace = server.addNamespace("/chat");
    	
    	logger.info(String.format("Created namespace: %s", chatNamespace.getName()));
    	
    	chatNamespace.addConnectListener(new ConnectListener() {
			
			@Override
			public void onConnect(SocketIOClient client) {
				logger.info(String.format("Connection from %s to %s", client.getRemoteAddress(), chatNamespace.getName()));
			}
			
		});
    	
    	chatNamespace.addEventListener("message", ChatMessage.class, new DataListener<ChatMessage>() {

			@Override
			public void onData(SocketIOClient client, ChatMessage data, AckRequest ackSender) throws Exception {
				logger.info(String.format("Recieved message from %s: %s", data.getUsername(), data.getMessage()));
				chatNamespace.getBroadcastOperations().sendEvent("message", data);	
			}
    		
		});
    	server.start();
  
	}
}
