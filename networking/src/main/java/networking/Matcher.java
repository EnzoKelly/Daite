package networking;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.SocketIOClient;

import networking.chat.ChatUser;
import networking.chat.MatchEvent;
import networking.Match;

/**
 * Class to match clients with other clients or AI(?).
 * @author andreas
 *
 */
public class Matcher {
	private Logger log = LoggerFactory.getLogger(Matcher.class);
	private BlockingQueue<SocketIOClient> matchQueue;
    private Hashtable<String, Match> matches = new Hashtable<String, Match>();

    private BlockingQueue<SocketIOClient> availableBots;

	public Matcher() {
		matchQueue = new LinkedBlockingDeque<>();
        availableBots = new LinkedBlockingDeque<>();
	}

    /*
     * Where the actual matching is done
     */
	public void match(SocketIOClient user) throws InterruptedException {
		//Matching with the ai
		//Check if a bot is available
		if(availableBots.size() > 0) {

			SocketIOClient bot;
			// get a bot
			synchronized(availableBots)
			{
				bot = availableBots.take();
			}

			//Trigger the bot with user details
			triggerBot(bot, user.get("userData"));

			// Create a new match and store it in the match list
			// At this point, the conversation is NOT ready to start, cause we need to wait for
			// the bot to reply with detials
			Match newMatch = new Match(user, bot);
			matches.put(newMatch.getRoomID(), newMatch);
		}
		else
		{
			//Check if user will be matched with a real client or with a bot
			// Here is done the matching between two real users
			// Store the connecting client
			synchronized(matchQueue)
			{
				matchQueue.put(user);
			}

			//If we have another client available, do the match
			if(matchQueue.size() >= 2)
			{
				SocketIOClient matchedClient;
				synchronized(matchQueue)
				{
					matchedClient = matchQueue.take();
					matchQueue.remove(user);
				}

				// Create a new match
				Match newMatch = new Match(matchedClient, user);
				matches.put(newMatch.getRoomID(), newMatch);
				listAllMatches();
			}
		}
	}

    /*
     * When bot sent details, chat is ready to start 
     */
    public void startBotChat(String room){
        Match match = getMatch(room);
        if(match != null) {
            match.setBotDetails();
        }
        else {
            log.error("Could not find bot match");
        }
    }

    /*
     * Remove a client from the matching queue, in case it disconnects
     */
    public boolean removeFromQueue(SocketIOClient user) {
        synchronized(matchQueue) { 
            return matchQueue.remove(user);
        }
    }

    public boolean removeAvailableBot(SocketIOClient bot) {
        synchronized(availableBots) {
            return availableBots.remove(bot);
        }
    }

    public boolean removeMatch(String room) {
        Enumeration roomIDs = matches.keys();
        while(roomIDs.hasMoreElements()) {
            if(room.equals(roomIDs.nextElement())){
                matches.remove(room);
                log.info("Removed match with room ID: {}", room);
                return true;
            }   
        }
        return false;
    }
    
    /*
     * Room is a valid room
     */
    public boolean isCurrentRoom(String id) {
        Enumeration roomIDs = matches.keys();
        while(roomIDs.hasMoreElements()) {
            if(id.equals((String) roomIDs.nextElement())){
                return true;
            } 
        }
        return false;
    }

    /*
     * Returns paired client
     */
    public SocketIOClient getMatchedClient (SocketIOClient client) { 
        //TODO catch empty room ID
        String clientRoomID = client.getAllRooms().iterator().next();
        Match match = getMatch(clientRoomID);
        if(match != null) {
            ChatUser chatuser = client.get("userData");
            return match.getPairedClient(client.getSessionId());
        }
        else
            return null;

    }

    public Match getMatch(String roomID) {
        Enumeration roomIDs = matches.keys();
        while(roomIDs.hasMoreElements()) {
            if(roomID.equals((String) roomIDs.nextElement())) {
                return matches.get(roomID);
            }
        }
        return null;
    }

    public String getRoomFromClient(SocketIOClient client) {
        List<String> allRoomsList = new ArrayList<>(client.getAllRooms());
        if(allRoomsList.size() == 1) {
            return allRoomsList.get(0);
        }
        else {
            return null;
        }
    }

    /* Add a new bot available */
    public void addAvailableBot(SocketIOClient bot) throws InterruptedException {
        synchronized (availableBots) {
            availableBots.put(bot);
            log.info("Tracking bot at: {}", bot.getRemoteAddress());
        }
    }

    public void triggerBot(SocketIOClient bot, ChatUser matchUser) {
        bot.sendEvent("bot-matched", matchUser);
    }


    // DEBUGING
    private void listAllMatches() {
        log.info("Rooms in use: ----------");
        Enumeration roomIDs = matches.keys();
        while (roomIDs.hasMoreElements()){
            String roomID = (String) roomIDs.nextElement();
            Match match = matches.get(roomID);
            log.info("  Room ID: {}, clients: {}, {}", roomID, match.getChatClient1().getName(), match.getChatClient2().getName());
        }
    }

}



