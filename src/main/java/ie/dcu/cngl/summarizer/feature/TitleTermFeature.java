package main.java.ie.dcu.cngl.summarizer.feature;

import java.io.IOException;
import java.util.ArrayList;

import main.java.ie.dcu.cngl.summarizer.SummarizerUtils;
import main.java.ie.dcu.cngl.tokenizer.TokenInfo;

/**
 * The title of an article often reveals the major subject of that document. Sentences
 * containing terms from the title are likely to be good summarization candidates.</br>
 * sentence score = number of title terms found in sentence/total number of title terms
 * @author Shane
 *
 */
public class TitleTermFeature extends Feature {

	private ArrayList<TokenInfo> titleTokens;

	public TitleTermFeature(ArrayList<TokenInfo> titleTokens) throws IOException {
		this.titleTokens = titleTokens;
	}

	@Override
	public Double[] calculateRawWeights(Double[] weights) {
		final double numTitleTerms = numberOfTerms(titleTokens);
		int sentenceNumber = 0;
		ArrayList<TokenInfo> tokenHolder;
		for(ArrayList<ArrayList<TokenInfo>> paragraph : structure.getStructure()) {
			for(ArrayList<TokenInfo> sentence : paragraph) {
				double numOccurences = 0;
				for(TokenInfo titleToken : titleTokens) {
					tokenHolder = new ArrayList<TokenInfo>();
					tokenHolder.add(titleToken);
					numOccurences+=getNumOccurrences(tokenHolder, sentence);
				}
				weights[sentenceNumber++] = numOccurences/numTitleTerms;
			}
		}
		return weights;
	}

	@Override
	public double getMultiplier() {
		return SummarizerUtils.titleTermMultiplier;
	}

}
