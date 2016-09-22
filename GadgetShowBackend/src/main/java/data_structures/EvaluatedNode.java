package data_structures;

import java.util.ArrayList;

import analysis.Analyser;
import analysis.KeywordMagic;
import edu.stanford.nlp.ie.util.RelationTriple;

/**this class will implement the evaluate method from the BaseNode class
 * this will just make it a bit easier to account for other team members current work
 * @author Ian Temple/Charlie Street
 *
 */
public abstract class EvaluatedNode extends BaseNode {

	private Analyser analyser;
	private final double SENTIMENT_WEIGHT = 1;
	private final double INFO_WEIGHT = 1;
	private final double KEYWORD_WEIGHT = 2;
	private final double RELATION_WEIGHT = 3;
	private final boolean LOGGING_ON = false;
	
	/**constructor same as superclass
	 * 
	 * @param message message for node
	 * @param keywords the keywords of said message
	 * @param analyser the object for nlp
	 */
	public EvaluatedNode(String message, String[] keywords, Analyser analyser) {
		super(message, keywords);
		this.analyser = analyser;
	}
	
	/**constructor same as superclass
	 * 
	 * @param message the message for the node
	 * @param keywords the keywords of said message
	 * @param neighbours the neighbours on the graph
	 * @param analyser the object for nlp
	 */
	public EvaluatedNode(String message, String[] keywords, ArrayList<BaseNode> neighbours, Analyser analyser) {
		super(message, keywords, neighbours);
		this.analyser = analyser;
	}
	
	/** 
	 * @author TauOmicronMu
	 * @param score the similarity score to be scaled.
	 * @return Scaled score based on Tom's lovely method (just trust him :( )
	 */
	private double scale(double score) {
		final double TOMS_MAGIC_NUMBER = 10;
		
		double squared = Math.pow(score, 2); //Weight the scale towards the "perfect" end
		double scaledBack = squared/TOMS_MAGIC_NUMBER;
		
		return scaledBack;
	}
	
	/**implemented from BaseNode class
	 * see that class for reference about what this should do
	 */
	public double evaluate(String incMessage) {
		
		double totalScore = 0.0;
		
		int sentScore = analyser.compareSentiment(this.getMessage().toLowerCase(), incMessage.toLowerCase());
		totalScore += (SENTIMENT_WEIGHT * sentScore);
		
		if(LOGGING_ON) System.out.println("Sent score: " + sentScore);
		
		double keywordScore = KeywordMagic.correctKeyWords(this.getKeywords(), incMessage.toLowerCase());
		totalScore += (KEYWORD_WEIGHT * keywordScore);
		
		if(LOGGING_ON) System.out.println("Keyword score: " + keywordScore);
		
		RelationTriple dataRelation = analyser.getRelations(getMessage());
		RelationTriple incRelation = analyser.getRelations(incMessage);
		double information = analyser.compareInformation(dataRelation, incRelation);
		
		if(LOGGING_ON) System.out.println("Information: " + information);
		
		totalScore += (INFO_WEIGHT * information);
		
		try {
			String relation = incRelation.objectLemmaGloss();
			int relationScore = 0;
			for(int i = 0; i < this.getKeywords().length; i++) {
				if(relation.toLowerCase().equals(this.getKeywords()[i].toLowerCase())) {
					relationScore = 1;
					break;
				}
			}
			totalScore += (RELATION_WEIGHT * relationScore);
			
			if(LOGGING_ON) System.out.println("Relation score: " + relationScore);
		} catch(Exception e) {}
		
		return scale(totalScore);
	}
	
}
