package main.java.ie.dcu.cngl.summarizer.main;

import java.io.File;

import org.apache.commons.io.FileUtils;

import main.java.ie.dcu.cngl.summarizer.Aggregator;
import main.java.ie.dcu.cngl.summarizer.Summarizer;
import main.java.ie.dcu.cngl.summarizer.Weighter;
import main.java.ie.dcu.cngl.tokenizer.Structurer;


public class Test {
	public static void main(String [] args) throws Exception {
		String text = FileUtils.readFileToString(new File("C:\\Users\\Shane\\Desktop\\long.txt"), "UTF-8");
		Structurer structurer = new Structurer();
		Weighter weighter = new Weighter();
		Aggregator aggregator = new Aggregator();		
		Summarizer summarizer = new Summarizer(structurer, weighter, aggregator);
		summarizer.setNumSentences(10);
		String summary = summarizer.summarize(text);
		System.out.println(summary);
	}
}
