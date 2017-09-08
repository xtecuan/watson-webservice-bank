package main.java.ie.dcu.cngl.summarizer;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

import main.java.ie.dcu.cngl.tokenizer.IStructurer;
import main.java.ie.dcu.cngl.tokenizer.PageStructure;
import main.java.ie.dcu.cngl.tokenizer.Tokenizer;

/**
 * Provides a sentence extracted summary of provided content.
 * There are three stages in the summarization pipeline - </br>
 * Tokenization: 
 * The summarizer uses a Structurer to not only tokenize the content,
 * but to structure the content by sentences and paragraphs.</br>
 * Weighting: 
 * Using the content structure and chosen features the weighter assigns
 * weights to each sentence.</br>
 * Aggregation: The aggregator combines the weights from each feature.
 * It may decide to completely discount those negatively weighted, combine
 * them linearly, logarithmically, or in any other fashion chosen. The sentences are
 * then ranked. </br>
 * The summarizer finishes by outputting the number of sentences desired
 * according to their score.
 * 
 * @author Shane
 *
 */
public class Summarizer {
	
	private Tokenizer tokenizer;
	private IStructurer structurer;	
	private IWeighter weighter;
	private IAggregator aggregator;
	
	private int numSentences;
	private String title;
	private String query;
	
	private ArrayList<Double[]> weights;
	
	/**
	 * Creates new summarizer with provided components.
	 * @param structurer Extracts content structure
	 * @param weighter Weights sentences and paragraphs
	 * @param aggregator Combines scores and ranks sentences
	 */
	public Summarizer(IStructurer structurer, IWeighter weighter, IAggregator aggregator) {
		this.tokenizer = Tokenizer.getInstance();
		this.weighter = weighter;
		this.aggregator = aggregator;
		this.structurer = structurer;
		this.numSentences = 2;	//Default number of sentences
		this.weights = new ArrayList<Double[]>();
	}
	
	/**
	 * Sets the number sentences to be returned by the summarizer.
	 * @param numSentences
	 */
	public void setNumSentences(int numSentences) {
		this.numSentences = numSentences;
	}
	
	/**
	 * Provides sentence extracted summary of provided content.
	 * @param content to be summarized
	 * @return summary of provided content
	 */
	public String summarize(String content) {
		if(StringUtils.isEmpty(content)) {
			return StringUtils.EMPTY;
		}
		
		PageStructure structure = structurer.getStructure(content);	
		weighter.setStructure(structure);
		weighter.setTitle(StringUtils.isNotEmpty(title) ? tokenizer.tokenize(title) : null);
		weighter.setQuery(StringUtils.isNotEmpty(query) ? tokenizer.tokenize(query) : null);
		aggregator.setSentences(structure.getSentences());
		
		weighter.calculateWeights(weights);
		for(Double [] featureWeight : weights) {	//Correct internal state?
			assert(featureWeight.length == structure.getNumSentences());
		}
		SentenceScore [] scores = aggregator.aggregate(weights);
		
		String summary = StringUtils.EMPTY;
		for(int i = 0; i < scores.length; i++) {
			if(scores[i].getScore() < numSentences)
				summary+=(scores[i].getSentence() + "\n");
		}
		
		return summary;
	}
	
	/**
	 * Set content title
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Set query related to the content
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}
	
	/** 
	 * Provide pre-calculated weights that will be combined with
	 * weights calculated on new run.
	 * @param weights Pre-calculated weights
	 */
	public void setWeights(ArrayList<Double[]> weights) {	
		this.weights = weights;
	}
	
}