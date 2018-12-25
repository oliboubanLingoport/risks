package jsouptest.parser;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.*;

public class Analyze10KBackup {

	private final static String SEC_URL="https://www.sec.gov/";
	private final static String EDGAR="edgar";
	private final static String RISK_FACTOR="RISK FACTORS";
	private final static String END_RISK_FACTOR="ITEM"; // All in UPPERCASE, as item or Item can be anywhere in the doc.
	private final static String[] END_RISK_FACTORS={
		"UNRESOLVED STAFF COMMENTS", 
		"PROPERTIES", 
		"LEGAL PROCEEDINGS", 
		"MINE SAFETY PROCEDURE",
		"ITEM",
		"PART II",
		"MARKET FOR REGISTRANT",
		"UNREGISTERED SALES"
	};
	

	private static String removeTillWord(String input, String word) {
		return input.substring(input.indexOf(word));
	}

	/**
	 * takes in a array of string candidates to see when we reach the end of of
	 * section so we can return the string before a string candidate if found.
	 * If none of the string candidates are found, return 'null' and 
	 * let the calling method decide what to do. 
	 * 
	 * @param input
	 * @param words: an array of String's
	 * @return
	 */
	private static String removeAfterWords(String input, String[] words) {
		for (int i = 0; i < words.length; i++) {
			int index = input.indexOf(words[i]);
			if (index != -1) {
				return input.substring(0,  index);
			}
		}

		// If we reached the end of the word candidates and did not find any end
		// we have a problem
		return null;
	}

	private static String removeAfterWord(String input, String word) {
		return input.substring(0, input.indexOf(word));
	}

	// ============================================ Alternative 1 
	private static boolean endRiskFactorsFound(String riskFactors) {
		// If one of the next section if found, return true.
		for (int i = 0; i < END_RISK_FACTORS.length; i++) {
			int index = riskFactors.indexOf(END_RISK_FACTORS[i]);
			if (index != -1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The whole body may be on 
	 * ONE LINE which includes the text "RISK FACTORS" 
	 *   or
	 * MANY LINES
	 *   One of which may contain 'RISK FACTORS'
	 *   The end of which may contain a set of heading words for the next sections!!
	 *   
	 * 1) find a line with 'RISK FACTORS' 
	 * 2) remove anything before 'RISK FACTORS' on that line
	 * 3) starting with that line:
	 *   3.a) Find any of the matching next sections
	 *   3.b) If none on that line, go to the next line, add this line to the overall risk_factor_line variable
	 *   3.c) If some of that line, remove anything after that matching next section including the next section
	 *   3.d) Return that line (very likely a long long line!)
	 *   
	 * Alternative (the above does not cover all the permutations! far from it. Very unstructured)
	 * Make is all one big line: Concat all lines into one.
	 * 1) Start: Find      a name="
	 *   1.a: Is it followed by item or Item or ITEM without an href in between
	 *   1.b: It that followed by Risk Factors or risk factors or RISK FACTORS without an href in between
	 *   => Could be the start
	 *   If not, cut out that piece, continue on to the next 'a name="' 
	 *   go back to 1
	 * 2) End: Find       a name="
	 *   2.a: Is it followed by item or Item or ITEM without an href in between?
	 *   
	 *   
	 * Alternative (if there is an HTML ToC!)
	 * 1) Look for the ToC Risk Factor
	 * 2) Look for the next ToC entry
	 * 2) Extract the risk factors' a name="dfkjljfds" from the ToC ; remove anything before it
	 * 3) Extract the next ToC entry; Look for the a name="lskdjf" from the ToC; Remove anything after it.
	 * 
	 * @param url
	 * @throws Exception
	 */
	private static String getRiskFactorsBackup(URL url) throws Exception {

		BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()));
//		System.out.println("URL = " + url);

		String riskLine = null;
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			//            System.out.println(inputLine);
			if ( inputLine.indexOf(RISK_FACTOR) != -1 ) {
				riskLine = inputLine;
				break; 
			}
		}

		if (riskLine == null) {
			System.out.println(url + "This document did not contain any RISK FACTORS");
			System.out.println("Exiting without any handling");
			System.exit(1);
		}

		// At this point, we found the RISK_FACTOR String

		String riskFactors = removeTillWord(riskLine, RISK_FACTOR);
		//        System.out.println("-----------------------");
		//        System.out.println(riskFactors);

		// And now, we start riskLine with RISK_FACTOR; Whatever was before, one or many lines
		// has been removed.

		// Let's continue to build the entire line: It can be all on one line or lines after lines of 
		// risk factors followed by lines of other sections
		if (endRiskFactorsFound(riskFactors)) {
			riskFactors = removeAfterWords(riskFactors, END_RISK_FACTORS);
			in.close();
			return riskFactors;
		}

		// Let's continue reading in lines from where we stopped, including the current riskFactors;
		while ((inputLine = in.readLine()) != null) {
			riskFactors = riskFactors + " " + inputLine;
			if (endRiskFactorsFound(riskFactors)) {
				riskFactors = removeAfterWords(riskFactors, END_RISK_FACTORS);
				break;
			}
		}

		//        System.out.println("-----------------------");
		//        System.out.println(riskFactors);
		//        System.out.println("-----------------------");
		in.close();
		return riskFactors;
	}

	// ============================================ Alternative 2 
	/**
	 *   
	 * Alternative (if there is an HTML ToC!)
	 * 1) Look for the ToC Risk Factor
	 * 2) Look for the next ToC entry
	 * 2) Extract the risk factors' a name="dfkjljfds" from the ToC ; remove anything before it
	 * 3) Extract the next ToC entry; Look for the a name="lskdjf" from the ToC; Remove anything after it.
	 * 
	 * @param url
	 * @throws Exception
	 */
	private static String getRiskFactorsFromAnchorsBackup(URL url) throws Exception {

		String riskFactors = null;

		// Since some 10-K's are on one line, others on hundred of lines, 
		// let's make it all one line to reduce the handling complexity
		BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()));
		String inputLine;
		String riskLine = null;
		while ((inputLine = in.readLine()) != null) {
			riskLine = riskLine + " " + inputLine.toLowerCase();
		}
		in.close();

		//System.out.println("Length of riskLine = " + riskLine.length() + "== " + riskLine.substring(0, 500));
		// AND sometimes risk factors is written risk&nbps;factors, even after lower case! SOB!
		riskLine = riskLine.replaceAll("risk&nbsp;factors", "risk factors");

		// Look for a name="
		// If there is not href between 'a name' and 'risk factors', we should be at the right location
		int indexAName = riskLine.indexOf("a name=\"");
		int indexRiskFactors = 0;
		int indexHRef = 0;

		// As long as there is another risk factor in the string
		while (indexAName != -1) {
			// Cut everything before a name="
			riskLine = riskLine.substring(indexAName);

			//System.out.println("Length of riskLine = " + riskLine.length() + "== " + riskLine.substring(0, 500));

			// From the location of a name=, look for href and risk factors.
			indexRiskFactors = riskLine.indexOf("risk factors");
			indexHRef = riskLine.indexOf("href");

			// No 'risk factors' section: get out with error
			if (indexRiskFactors == -1) {
				System.out.println("An indexAName, but not indexRiskFactors: Risk Factors not found. Exiting");
				System.exit(1);
			}

			// index 'href' between 'a name' and 'risk factors': not the risk factors section
			// and hopefully risk factors is 'close enough' to a name (less than what? 500??)
			if (indexHRef < indexRiskFactors || indexRiskFactors > 500) {
				// Remove anything before the href index: The risk factor may still have another a name before it
				riskLine = riskLine.substring("a name".length());
				indexAName = riskLine.indexOf("a name=\"");
				continue;
			} else {
				// No href between a name and risk factors!
				break;
			}
		}

		if (indexRiskFactors == -1) {
			System.out.println("No Risk Factors section found: Exiting");
			System.exit(1);
		}

		// At this point, we found the start of risk factors. Let's find the next a name= hoping this is
		// the beginning of the next section in the 10-K as per the ToC!
		riskLine = riskLine.substring("a name".length()); // jump over the current a name for 'Risk Factors'
		indexAName = riskLine.indexOf("a name=\"");  // and find the next one.
		if (indexAName == -1) {
			System.out.println("No Next Section Found: Exiting without Risk Factors");
			System.exit(1);
		}

		// We found at least an a name=, which we HOPE is the next section. We can't know FOR SURE
		// though with this messy and extremely variable 10-K form!
		riskLine = riskLine.substring(0, indexAName);
		// System.out.println("Risk Factors section: " + riskLine.length() + "  ==> " + riskLine.substring(0, 500));

		return riskLine;
	}


	/**
	 * Looks up in SEC DB for the last five 10-K filing details for a given company's ticker symbol
	 * The filing details has a link to the actual 10-K
	 * Typically, the Archive/edgar htm is the URL to the 10-K, but
	 * SOMETIMES it is NOT! (Frustrating!). 
	 * 
	 * Sometimes, it is preceded by a 10-K/A amendment, which makes the whole thing different.
	 *  So First make sure we did not see a 10-K/A string before as opposed to a 10-K string before
	 *  and THEN take the htm URL
	 * 
	 * @param tickerSymbol like GILD
	 * @param the number of the filingDetail to return: from 0 to 9 for 10 or the last one
	 * 
	 * @return a string with the filing details, like /Archives/edgar/data/882095/000088209517000006/0000882095-17-000006-index
	 * @throws IOException 
	 */
	private static String findFilingDetails(String tickerSymbol, int filingDetailNumber) throws IOException {

		URL tickerURL=new URL("https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=" + tickerSymbol +"&type=10-K&dateb=&owner=exclude&count=5");

//		System.out.println("URL = " + tickerURL);
		BufferedReader in = new BufferedReader( new InputStreamReader(tickerURL.openStream()));

		String mainLine = null;
		String filingDetail = null;
		String inputLine;
		int filingIndex =0;

		boolean is10Ksection = true;

		while ((inputLine = in.readLine()) != null) {

			System.out.print("-");
			// If the line contains a "10-K/A", we start the amendment section
			if ( inputLine.indexOf("10-K/A") != -1) {
				is10Ksection = false;
			}

			// If the line contains "10-K" but does not contain "10-K/A", it is a 10-K section
			if (inputLine.indexOf("10-K") != -1 && inputLine.indexOf("10-K/A") == -1 ) {
				is10Ksection = true;
			}
			//            System.out.println(inputLine);
			// If it is a 10-K section and we have a URL, it is the 10-K URL
			if ( inputLine.indexOf(EDGAR) != -1 && is10Ksection) {
				if (inputLine.indexOf("documentsbutton") != -1) {
					mainLine = inputLine;

					String removedIntro = removeTillWord(mainLine, "/Archives");
					//                  System.out.println("-----------------------");
					//                  System.out.println(removedIntro);

					filingDetail = removeAfterWord(removedIntro, ".htm");
//					System.out.println("-----------------------");
					System.out.println(filingDetail);
					if (filingIndex == filingDetailNumber) {
						in.close();
						return filingDetail + ".htm";
					}
					filingIndex++;
					System.out.println("filing index = " + filingIndex);
					System.out.println("=======================");
				}
			}
		}
		in.close();
//		System.out.println("-----------------------");
//		System.out.println(filingDetail + ".htm");
		System.out.print(".");

		return filingDetail + ".htm";
	}

	/**
	 * A filing detail has lots of peripheral info and in the midst of it a link to the 10-K
	 * html file. We look for that link.  
	 * 
	 * Possibly: Look for the first             <td scope="row">1</td>
	 * as in:
	 *       <td scope="row">1</td>
	 *       <td scope="row">FORM 10-K</td>
	 *       <td scope="row"><a href="/Archives/edgar/data/882095/000119312508040255/d10k.htm">d10k.htm</a></td>
	 * and then look for the first "Archives/edgar": Row 1 "should" be a 10-K
	 * What other info can we rely on???
	 * 
	 * All *VERY* brittle!
	 * 
	 * @param filingDetail for instance: /Archives/edgar/data/882095/000088209517000006/0000882095-17-000006-index
	 * @return the actual 10-K link for instance: https://www.sec.gov/Archives/edgar/data/882095/000088209517000006/a2016form10-k.htm
	 * @throws IOException 
	 */
	private static String findTenK(String filingDetail) throws IOException {

		String secFilingDetail = SEC_URL + filingDetail;
		URL filingDetailURL= new URL(secFilingDetail);

//		System.out.println("SEC Filing Detail URL = " + secFilingDetail);
		BufferedReader in = new BufferedReader( new InputStreamReader(filingDetailURL.openStream()));

		String mainLine = null;
		String inputLine;
		int tenKFound = 0;
		// The hope is that every time a 10-K htm URL is indicated, it is preceded by 
		// a line in the form of:
		//                   <td scope="row">FORM 10-K</td>
		while ((inputLine = in.readLine()) != null) {
//			 System.out.println(inputLine);

			// The current line should have a 10-K but not some exceptions, such as 10K/A
			if ( inputLine.indexOf("10-K") != -1 && inputLine.indexOf("10-K/A") == -1) {
				mainLine = inputLine;
//				System.out.println(" >>> with 10-K in it: " + mainLine);
				tenKFound = 2;
			} 

			if (inputLine.indexOf("Archive") !=-1 && tenKFound>0) {
				mainLine = inputLine;
//				System.out.println("URL for 10-K is: " + mainLine);

				String removedIntro = removeTillWord(mainLine, "Archive");
				//   System.out.println("-----------------------");
				//   System.out.println(removedIntro);

				String tenK = removeAfterWord(removedIntro, ".htm");
//				System.out.println("-----------------------");
				System.out.println(tenK);
//				System.out.println("-----------------------");
         		in.close();
				return "https://www.sec.gov/" + tenK + ".htm";
			}
			tenKFound --;
		}
		in.close();
		System.out.println("NOT FOUND ************************************");
		return null;
	}
	
	private static String getTenKURL(String ticker, int index) throws Exception {
		String FilingDetail = findFilingDetails(ticker, index);
		String tenKURL = findTenK(FilingDetail);
		return tenKURL;
	}

	private static String getTenKRisks(String tenKURL, String ticker, int index) throws Exception {

		// If the 10-K URL is null, we did not find anymore, exit return null
		if (tenKURL == null) {
			return null;
		}

		// Get the local file name from StringUtil
		String diskFileName = StringUtil.getLocal10KFileName(ticker, index);
		InputStream in = new URL(tenKURL).openStream();
		Files.copy(in, Paths.get(diskFileName), StandardCopyOption.REPLACE_EXISTING);
//		System.out.println(" Ten-K HTML written out to : " + diskFileName);
//		System.out.println(" ======>  ");

		// Get the risk factors from that file
		return HTMLUtils.extractRiskFactors(ticker, index);
	}

	/**
	 * Extract an array of ticker symbol from the 'symbolList' file
	 * There should be one ticker symbol per line on the 'symbolList' file
	 * @param symbolList
	 * @return
	 * @throws IOException
	 */
	private static String[] getTickerList(String symbolList) throws IOException {
		// Open the file
		System.out.println("Running with Ticker Symbols found in: " + symbolList); 
		FileInputStream fstream = new FileInputStream(symbolList);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		ArrayList<String> candidateSymbols = new ArrayList<String>();
		String strLine;

		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {
		  // Print the content on the console
	      StringTokenizer st1 = new StringTokenizer(strLine);

   	      for (int i = 1; st1.hasMoreTokens(); i++) {
   	    	  candidateSymbols.add(st1.nextToken());
		   }
		}

		//Close the input stream
		br.close();
		
		String[] symbolArray = candidateSymbols.toArray(new String[candidateSymbols.size()]);
		for (int i=0; i<symbolArray.length; i++) {
			System.out.println(symbolArray[i]);
		}
		return symbolArray;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		// Get the list of symbols from the file passed in as the first arg
		// We expect one ticker symbol per line in that file
		if (args == null || args.length < 1) {
			System.out.println("Please pass a file with ticker symbols as the first argument");
			System.exit(1);
		}

		String[] tickers = getTickerList(args[0]);

		final int MAX_YEARS=12; // At most three years worth of 10-K
		int maxTickers = tickers.length;
		System.out.println("Max Tickers = " + maxTickers);

		ArrayList<TenKMetrics> tenKList = new ArrayList<TenKMetrics>();

		// Build a set of risk factor differences for each ticker symbol passed in
		for (int indexTicker=0; indexTicker<maxTickers; indexTicker++) {
			
			System.out.println(" Searching SEC for 10-K of " + tickers[indexTicker]);
			
			// Get up to MAX_YEARS 10-K's
			int yearIndex = 0; // 0 is the most recent year
			int riskDelta = 0;
			int absoluteLength = 0;
			int previousAbsoluteLength = 0;
			double percentDiff = 0.0;

			String tenKURL = getTenKURL(tickers[indexTicker], yearIndex);
			String tenKRisk = getTenKRisks(tenKURL, tickers[indexTicker], yearIndex);

			while (yearIndex < MAX_YEARS && tenKRisk != null) {
				absoluteLength = tenKRisk.length();

				if (yearIndex > 1) {
					riskDelta = Math.abs(previousAbsoluteLength - absoluteLength);
					percentDiff = (100* riskDelta / absoluteLength);
				    tenKList.add(new TenKMetrics(tickers[indexTicker], 
				    					yearIndex, 
				    					absoluteLength, 
				    					riskDelta, 
				    					percentDiff, 
				    					tenKURL));
				}
				System.out.println(">>" + tickers[indexTicker] + " Shortened Risk:" + tenKRisk.substring(0, 80));
				
				// Re-initialize for the loop condition
				// especially the previous AbsoluteLength
				yearIndex++;
				previousAbsoluteLength = absoluteLength;
				tenKURL = getTenKURL(tickers[indexTicker], yearIndex);
				tenKRisk = getTenKRisks(tenKURL, tickers[indexTicker], yearIndex);
			}
			
		}
		
		// Print out all the entire List Array of 10-K Metrics
		// before sorting:
		int MAX_TOP = 10;

		System.out.println(MAX_TOP + " Before Sorting:");
		for (int i =0; i< MAX_TOP; i++) {
			System.out.println(tenKList.get(i));
		}

		tenKList.sort(TenKMetrics.percentComparator);
		System.out.println("Top " + MAX_TOP + " After Sorting on %:");
		
		// After sorting
		for (int i =0; i< MAX_TOP; i++) {
			System.out.println(tenKList.get(i));
		}

		tenKList.sort(TenKMetrics.absoluteComparator);
		System.out.println("Top " + MAX_TOP + " After Sorting on actual length of diff:");
		
		// After sorting
		for (int i =0; i< MAX_TOP; i++) {
			System.out.println(tenKList.get(i));
		}


	}


}
