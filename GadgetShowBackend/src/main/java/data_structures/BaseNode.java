package data_structures;


import java.util.ArrayList;

/**this class represents a single node of a conversation graph that 
 * we will be using to respond to messages
 * This class will be fairly basic due to the time of writing
 * so extending this may be a useful idea for the future
 * i am making the assumption that keywords will still be useful regardless of tactic
 * @author Charlie Street
 *
 */
public abstract class BaseNode {

	private String response;//if we stop at this node then we can give a response
	private String[] keyWords;//a list of keywords to look out for in the message
	private ArrayList<BaseNode> neighbours;//turning these nodes into a graph (we don't know how many children we will need)
	private boolean visited;//has the node been visited in traversal? Should improve efficiency
	private boolean changeTopic; //certain nodes may include responses which justify a change in topic, this should aid this difficult task a bit
	
	/**constructor where we can't add the children yet
	 * 
	 * @param response the response to make if we stop here
	 * @param keyWords the list of keywords to help judge the likelihood of a message
	 * @param changeTopic should this node cause a change in topic if visited?
	 */
	public BaseNode(String response, String[] keyWords, boolean changeTopic) {
		this.response = response;
		this.keyWords = keyWords;
		this.neighbours = new ArrayList<BaseNode>();
		this.visited = false;//a node isn't visited when first created
		this.changeTopic = changeTopic;
	}
	
	/**second constructor for the case we know the neighbours of the node
	 * 
	 * @param response the response if we stop here
	 * @param keyWords the keywords to look out for
	 * @param neighbours the neighbours of the node in the topic graph
	 * @param changeTopic should this node cause a change in topic if visited?
	 */
	public BaseNode(String response, String[] keyWords, boolean changeTopic, ArrayList<BaseNode> neighbours) {
		this.response = response;
		this.keyWords = keyWords;
		this.neighbours = neighbours;
		this.visited = false;//see previous constructor
		this.changeTopic = changeTopic;
	}
	
	/**this is the crucial method of the class
	 * it will take a message and explore the likelihood of a response being suitable
	 * @param message the subjects entered message
	 * @return a number between 0 and 1, representing the likelihood of this node being suitable
	 * (0 = no chance) (1 = absolute certainty)
	 */
	public abstract double evaluate(String message);
	
	/**method will return response of node
	 * will be used when we find the appropriate node for a message
	 * @return the response
	 */
	public String getResponse() {
		return this.response;
	}
	
	/**adds a new neighbour to the node on the graph
	 * 
	 * @param neighbour the new neighbour
	 */
	public void addNeighbour(BaseNode neighbour) {
		this.neighbours.add(neighbour);
	}
	
	/**returns us a neighbour of our node
	 * to be used when traversing a graph
	 * @param index the index in the list
	 * @return the appropriate neighbour
	 */
	public BaseNode getNeighbour(int index) {
		return this.neighbours.get(index);
	}
	
	/**returns a keyword
	 * 
	 * @param index self-explanatory
	 * @return the keyword at the specified index
	 */
	public String getKeyword(int index) {
		return this.keyWords[index];
	}
	
	/**trivial set method
	 * 
	 * @param visited new visited status of node
	 */
	public void setVisited(boolean visited) {
		this.visited = visited;
	}
	
	/**trivial get method
	 * 
	 * @return has the node been visited?
	 */
	public boolean isVisited() {
		return this.visited;
	}
	
	/**trivial get method
	 * 
	 * @return should the topic be changed when I visit this node
	 */
	public boolean shouldIChangeTopic() {
		return this.changeTopic;
	}
	
}
