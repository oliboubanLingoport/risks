package jsouptest.parser;

import java.util.ArrayList;
import java.util.Comparator;

public class TenKMetrics {

	private String tickerSymbol;
	private int yearIndex;
	private int riskLength;
	private int riskFactorDiff;
	private double percentDiff;
	private String URL;

	public TenKMetrics(String tickerSymbol, int yearIndex, int riskLength, int riskDelta, double percentDiff, String URL) {
		this.tickerSymbol = tickerSymbol;
		this.yearIndex = yearIndex;
		this.riskLength=riskLength;
		this.riskFactorDiff = riskDelta;
		this.percentDiff = percentDiff;
		this.setURL(URL);
	}
	
	@Override
    //this is overridden to print the user-friendly information about the Employee
    public String toString() {
        return "[Symbol=" + this.tickerSymbol + 
                ", year =" + this.yearIndex + 
                ", risk length=" + this.riskLength + 
                ", risk diff =" + this.riskFactorDiff + 
                ", % diff = " + this.percentDiff +
                ", " + this.URL + "]";
    }
	

	public String getTickerSymbol() {
		return tickerSymbol;
	}
	public void setTickerSymbol(String tickerSymbol) {
		this.tickerSymbol = tickerSymbol;
	}
	public int getYearIndex() {
		return yearIndex;
	}
	public void setYearIndex(int yearIndex) {
		this.yearIndex = yearIndex;
	}
	public int getRiskLength() {
		return riskLength;
	}
	public void setRiskLength(int riskLength) {
		this.riskLength = riskLength;
	}
	public int getRiskFactorDiff() {
		return riskFactorDiff;
	}
	public void setRiskFactorDiff(int riskFactorDiff) {
		this.riskFactorDiff = riskFactorDiff;
	}
	public double getPercentDiff() {
		return percentDiff;
	}
	public void setPercentDiff(double percentDiff) {
		this.percentDiff = percentDiff;
	}

    public static Comparator<TenKMetrics> percentComparator = new Comparator<TenKMetrics>() {

	     @Override
	     public int compare(TenKMetrics metrics1, TenKMetrics metrics2) {
	         return (int) (100 * (metrics2.getPercentDiff() - metrics1.getPercentDiff()));
	     }
	};

    public static Comparator<TenKMetrics> absoluteComparator = new Comparator<TenKMetrics>() {

	     @Override
	     public int compare(TenKMetrics metrics1, TenKMetrics metrics2) {
	         return (int) (100 * (metrics2.getRiskFactorDiff() - metrics1.getRiskFactorDiff()));
	     }
	};

	public static void main(String[] tickers) throws Exception {
		
		TenKMetrics metrics1 = new TenKMetrics("GILD", 1, 1, 540, (double)2.456, "Some GIL URL");
		TenKMetrics metrics2 = new TenKMetrics("GILD", 2, 2, 780, (double)1.456, "Some Other GIL URL");
		TenKMetrics metrics3 = new TenKMetrics("AAPL", 3, 14500, 230, (double)5, "Some AAPL URL");
		
		ArrayList<TenKMetrics> tenKList = new ArrayList<TenKMetrics>();
		tenKList.add(metrics1);
		tenKList.add(metrics2);
		tenKList.add(metrics3);


		// before sorting:
		System.out.println("Before Sorting:");
		for (TenKMetrics metrics:tenKList) {
			System.out.println(metrics);	
		}

		tenKList.sort(percentComparator);
		System.out.println("After Sorting on %:");
		
		// After sorting
		for (TenKMetrics m:tenKList) {
			System.out.println(m);	
		}

		tenKList.sort(absoluteComparator);
		System.out.println("After Sorting on actual length of diff:");
		
		// After sorting
		for (TenKMetrics m:tenKList) {
			System.out.println(m);	
		}

	
	}

	public String getURL() {
		return URL;
	}

	public void setURL(String uRL) {
		URL = uRL;
	}
}
