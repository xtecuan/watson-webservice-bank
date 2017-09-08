package main.java.ie.dcu.cngl.summarizer.feature;

import java.io.IOException;
import java.util.ArrayList;

import main.java.ie.dcu.cngl.summarizer.SummarizerUtils;
import main.java.ie.dcu.cngl.tokenizer.TokenInfo;

/**
 * Bias factor to score sentences containing query terms more highly.</br>
 * sentence score = (number of query terms)^2/number of terms in query
 * @author Shane
 *
 */
public class QueryBiasFeature extends Feature {
	
	private ArrayList<TokenInfo> query;

	public QueryBiasFeature(ArrayList<TokenInfo> query) throws IOException {
		this.query = query;
	}
	
	@Override
	public Double[] calculateRawWeights(Double[] weights) {
		final double numQueryTerms = numberOfTerms(query);
		int sentenceNumber = 0;
		ArrayList<TokenInfo> tokenHolder;
		for(ArrayList<ArrayList<TokenInfo>> paragraph : structure.getStructure()) {
			for(ArrayList<TokenInfo> sentence : paragraph) {
				double numOccurences = 0;
				for(TokenInfo queryToken : query) {
					tokenHolder = new ArrayList<TokenInfo>();
					tokenHolder.add(queryToken);
					numOccurences+=getNumOccurrences(tokenHolder, sentence);
				}
				weights[sentenceNumber++] = Math.pow(numOccurences, 2)/numQueryTerms;
			}
		}
		return weights;
	}

	@Override
	public double getMultiplier() {
		return SummarizerUtils.queryBiasMultiplier;
	}

}
