package jsouptest.parser;

import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.jsoup.Jsoup;

public class HTMLUtils {
  private HTMLUtils() {}

  public static String extractText(Reader reader) throws IOException {
    StringBuilder sb = new StringBuilder();
    BufferedReader br = new BufferedReader(reader);
    String line;
    while ( (line=br.readLine()) != null) {
      sb.append(line);
    }
    String textOnly = Jsoup.parse(sb.toString()).text();
    return textOnly;
  }

  public final static String extractRiskFactors(String ticker, int index) throws Exception{
	  final String RISK_FACTORS="item 1a. risk factors".toLowerCase();
	  final String[] ITEMS= {
	  "Item 1B. Unresolved Staff Comments".toLowerCase(),
	  "item 2. unregistered sales of equity securities".toLowerCase(),
	  "Item 2. Properties".toLowerCase(),
	  "Item 3. Defaults Upon Senior Securities".toLowerCase(),
	  "Item 3. Legal Proceedings".toLowerCase(),
	  "Item 4. Mine Safety Disclosures".toLowerCase(),
	  "Item 5. Other Information".toLowerCase(),
	  "Item 5. Market for Registrant’s Common Equity".toLowerCase(),
	  "Item 6. Exhibits".toLowerCase(),
	  "Item 6. Selected Financial Data".toLowerCase(),
	  "Item 7. Management’s Discussion and Analysis".toLowerCase(),
	  "Item 7A. Quantitative and Qualitative Disclosures about Market Risk".toLowerCase(),
	  "Item 8. Financial Statements and Supplementary Data".toLowerCase(),
	  "Item 9. Changes in and Disagreements with Accountants on Accounting and Financial Disclosure".toLowerCase(),
	  "Item 9A. Controls and Procedures".toLowerCase(),
	  "Item 9B. Other Information".toLowerCase(),
	  "Item 10. Directors, Executive Officers and Corporate Governance".toLowerCase(),
	  "Item 11. Executive Compensation".toLowerCase(),
	  "Item 12. Security Ownership of Certain".toLowerCase(), 
	  "Item 13. Certain Relationships and Related Transactions, and Director Independence".toLowerCase(),
	  "Item 14. Principal Accountant Fees and Services".toLowerCase(),
	  "Item 15. Exhibits and Financial Statement Schedules".toLowerCase()
	  };
	  

	String local10K = StringUtil.getLocal10KFileName(ticker, index);
    FileReader reader = new FileReader(local10K);
    String extractedText = HTMLUtils.extractText(reader);
    BufferedWriter writer = new BufferedWriter(new FileWriter("c:/SEC/10-K/tmp.txt"));
    writer.write(extractedText);
    writer.close();

    String riskFactorsText = extractedText.toLowerCase();
    int rfIndex = riskFactorsText.lastIndexOf(RISK_FACTORS);
    if (rfIndex != -1) {
//        System.out.println("[OK] Risk Factor Section Index = " + rfIndex);
    	riskFactorsText = riskFactorsText.substring(rfIndex);
    } else {
    	System.out.println("[***] The Item 1A. Risk Factors section was not found");
    	return null;
 //   	System.exit(-1);
    }
    
    // Then find the next possible Item in order they could appear
    int itemIndex =  -1;
    int loopIndex = 0;
    for (loopIndex=0; itemIndex ==-1 && loopIndex<ITEMS.length; loopIndex++ ) {
    	itemIndex = riskFactorsText.indexOf(ITEMS[loopIndex]);
    }
    
    if (itemIndex == -1) {
//    	System.out.println("[***] The end of the Risk Factors section was not found");
 //   	System.out.println("[***] The file may contain non risk factors past the risk factors section");
    } else {
    	loopIndex--;
//        System.out.println("[OK] End Risk Factor Section Index = " + itemIndex);
//        System.out.println("[OK] Loop Index = " + loopIndex);
//        System.out.println("[OK] Next Section = " + ITEMS[loopIndex]);
    	riskFactorsText = riskFactorsText.substring(0, itemIndex + ITEMS[loopIndex].length());
//    	System.out.println("[OK] The Risk Factors section was found");
    }
    
	String localRiskFactors = StringUtil.getLocalRiskFactorFileName(ticker, index);
    BufferedWriter riskWriter = new BufferedWriter(new FileWriter(localRiskFactors));
    riskWriter.write(riskFactorsText);
    riskWriter.close();
    
    // Return the actual string to the caller. The file was for tracking purposes.
    return riskFactorsText;
  }
  
  public final static void main(String[] args) throws Exception{

    extractRiskFactors("MMM", 8);
  }
}