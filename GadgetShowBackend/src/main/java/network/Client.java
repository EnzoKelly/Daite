package network;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import logger.ConvoLogger;
import psychology.NameMatcher;
import structure_building.BuildHashOfGraphs;
import structure_building.BuildWrapper;
import traversal.Conversation;

import org.json.JSONObject;
import org.json.JSONException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

/**class contains networking code for ai client
 * as well as the code for making the bot run
 * @author Charlie Street - making bot run, Vlad Rotea - networking client
 *
 */
public class Client {
    private Socket socket;

    private static final Logger log = LoggerFactory.getLogger(Client.class);
    private static final ConvoLogger logger = new ConvoLogger();

    //Match details
    private boolean botMatched = false;
    private String matchName, matchGender;
    private int matchAge;

    //Bot details
    // we need to keep the name assigned in order to ignore/check if the server received the
    // message bot send
    private String botName = "testBOT";
    

    public Client(String domain, String port, String namespace) {
        log.info("Bot connecting to: {}:{}/{}", domain, port, namespace);
        logger.logMessage("Bot connecting to: " + domain + ":" + port + "/" + namespace);
        try {
            socket = IO.socket(domain + ":" + port + "/" + namespace);
        }
        catch (URISyntaxException e) {

        }
        
        //initialize bot stuff here
        try {
        	//TODO INITIALISE NLP STUFF HERE
            // build data structures
        	BuildWrapper dataStructures = BuildHashOfGraphs.build(Client.logger);
            //initialise conversation 
        	Conversation convo = new Conversation(dataStructures.getGraphs(),true,dataStructures.getQuestionList());
        	logger.logMessage("Conversation Ready");
        	//now connect
            connect();
         }catch (Exception e) {
        	 //TODO what to do here?
         }
        
        /*
         * Server listeners
         */
        // receiving details of the user which bot was matched with
        socket.on("bot-matched", onMatch);
        // user typing events
        socket.on("isTyping", onTyping);
        // receiving messages
        socket.on("message", onMessage);
    }
   
    /*
     * Connect to server
     */
    private void connect() {
        socket.connect();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.emit("bot-available");
                log.info("Bot connected to server");
                logger.logMessage("Bot connected to server");
            }
        });
    }

    /*
     * Send computed bot details to server 
     */
    public void sendDetails(String name, int age, String gender) {
        JSONObject details = new JSONObject();

        try {
            details.put("name", name);
            details.put("age", age);
            details.put("gender", gender);
        }
        catch(JSONException e) {
            log.error(e.getMessage());
            return;
        }

        log.info("Sending bot details to server: name: {}, age: {}, gender: {}", name, age, gender);
        logger.logMessage("Sending bot details to server: name: " + name + ", age: " + age + ", gender: " + gender);
        socket.emit("register", details);
    }

    /*
     * Send message
     */
    public void sendMessage(String message) {
        if(botName == null) {
            return;
        }
        
        JSONObject msg  = new JSONObject();
        try {
            msg.put("username", botName);
            msg.put("message", message);
        }
        catch (JSONException e) {
            log.error(e.getMessage());
        }

        log.info("Sending message: {}");
        socket.emit("message", msg);
        logger.logMessage("Bot:> "+ msg);//log on our graphical logger
    }

    /*
     * Stop typing indicator
     */
    public void stopTyping() {
        socket.emit("isTyping", false); 
    }

    /*
     * Trigger typing indicator
     */
    public void startTyping() {
        socket.emit("isTyping", true);
    }
    
    /*
     * Parse the match details received from the server
     */
    private Emitter.Listener onMatch = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject matchDetails = (JSONObject) args[0];
            try {
                matchName = matchDetails.getString("name");
                matchAge = matchDetails.getInt("age");
                matchGender = matchDetails.getString("gender");
            }
            catch(JSONException e) {
                log.error(e.getMessage());
            }

            log.info("Received match details. name: {}, gender: {}, age: {}", matchName, matchGender, matchAge);
            botMatched = true;
            
            NameMatcher nameChooser = new NameMatcher();//respond with our own names and stuff
            if(matchGender == "male") {
            	sendDetails(nameChooser.pickName(matchName, true),20,"female");
            } else if(matchGender == "female") {
            	sendDetails(nameChooser.pickName(matchName, false),20,"male");
            } else { //be female by default
            	sendDetails(nameChooser.pickName(matchName, false),20,"female");
            }
        }
    };

    /*
     * On typing listener
     */
    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            boolean isTyping = (boolean) args[0];
            if (isTyping) {
                log.info("Match started typing");
                // TODO: match started typing
            }
            else {
                log.info("Match stopped typing");
                // TODO: match stoped typing
            }
        }
    };
    
    /* 
     * Incoming messages
     */
    private Emitter.Listener onMessage = new Emitter.Listener() {
        String username, message; 
        
        @Override
        public void call(Object... args) {
            JSONObject msg = (JSONObject) args[0];
            try {
                username = msg.getString("username");
                message = msg.getString("message");
            }
            catch(JSONException e){
                log.error(e.getMessage());
                return;
            }

            if(username == botName) {
                // ignore for now the messages bot sent
            }
            else {
                if(message != null){
                    log.info("Received message from match ({}): {}", username, message);
                        
                    logger.logMessage(username+":> " + message);//log to fancy logger what the user has said
                    //TODO crazy stuff here
                }
            }
        }
    };

    /**main method for entire chat bot, will run it as a network client
     * 
     * @param args standard java shiz
     */
    public static void main (String[] args) {
        // Setup connection
        Client client = new Client("http://localhost", "6969" , "chat");//this should start the bot running
        //TODO make sure we deal with disconnecting
    }
}

