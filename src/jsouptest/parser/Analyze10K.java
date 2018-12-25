package jsouptest.parser;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.*;
//import difflib.*;

public class Analyze10K {

	private final static String SEC_URL="https://www.sec.gov/";
	private final static String EDGAR="edgar";
	

	private static String removeTillWord(String input, String word) {
		return input.substring(input.indexOf(word));
	}


	private static String removeAfterWord(String input, String word) {
		return input.substring(0, input.indexOf(word));
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
						System.out.println("Found at filing index = " + filingIndex);
						System.out.println("=======================");
						return filingDetail + ".htm";
					}
					filingIndex++;
					System.out.println("filing index = " + filingIndex);
				}
			}
		}
		in.close();
//		System.out.println("-----------------------");
//		System.out.println(filingDetail + ".htm");
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
			String tenKURL = null;
			String tenKRisk = null;

			try {
				tenKURL = getTenKURL(tickers[indexTicker], yearIndex);
				tenKRisk = getTenKRisks(tenKURL, tickers[indexTicker], yearIndex); 
			} catch (Exception e) {
				System.out.println(" START EXCEPTION: " + e.getMessage());
			}

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
				int summaryLength = Math.min(80, tenKRisk.length())-1;
				System.out.println(">>" + tickers[indexTicker] + " Shortened Risk:" + tenKRisk.substring(0, summaryLength));
				
				// Re-initialize for the loop condition
				// especially the previous AbsoluteLength
				yearIndex++;
				previousAbsoluteLength = absoluteLength;
				try {
					tenKURL = getTenKURL(tickers[indexTicker], yearIndex);
					tenKRisk = getTenKRisks(tenKURL, tickers[indexTicker], yearIndex);
				} catch (Exception e) {
					System.out.println(" EXCEPTION: " + e.getMessage());
				}
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
