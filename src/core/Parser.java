package core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.opencsv.CSVReader;

public class Parser {
	public static BigDecimal cashInPortfolio(String path) throws IOException, ParseException {
		@SuppressWarnings("resource")
		CSVReader csvReader = new CSVReader(new FileReader(path));
		String[] column = null;
		csvReader.readNext();
		DecimalFormat df = new DecimalFormat("#,##0.00;(#,##0.00)");
		BigDecimal cash = BigDecimal.ZERO;

		while ((column = csvReader.readNext()) != null)
			// if the row is for cash
			if (column[0].toLowerCase().contains("cash"))
				cash = BigDecimal.valueOf(df.parse(column[6].replace("$", "")).doubleValue()).abs();

		return cash;
	}

	public static HashMap<Integer, BigDecimal> bondsInPortfolio(String path,
			HashMap<Integer, BigDecimal> maturingAmounts) throws IOException, ParseException {

		System.out.println("Parsing " + path);
		HashMap<Integer, BigDecimal> maturingAmountsIter = maturingAmounts;
		@SuppressWarnings("resource")
		CSVReader csvReader = new CSVReader(new FileReader(path));
		String[] column = null;
		csvReader.readNext();
		DecimalFormat df = new DecimalFormat("#,##0.00;(#,##0.00)");

		while ((column = csvReader.readNext()) != null) {
			if (column.length > 1)
				// if the security name has "US TREASURY"
				if (column[1].toLowerCase().contains("us treasury")) {
					// System.out.println("Treasury Found: " + column);

					String toParse = column[1].substring(column[1].indexOf("DUE") + 4, column[1].length());
					LocalDate date = LocalDate.parse(toParse, DateTimeFormat.forPattern("MM/dd/YY"));
					// BigDecimal amount =
					// BigDecimal.valueOf(df.parse(column[1]).doubleValue()).abs();
					BigDecimal amount = BigDecimal.valueOf(df.parse(column[2]).doubleValue()).abs();
					// System.out.println(date + " " + amount);

					// PEGS THE KEY TO CALENDAR WEEKS FROM TODAY
					int weeksFromToday = (date.getWeekOfWeekyear() - Main.today.getWeekOfWeekyear()
							+ Main.weeksInThisYear) % 52;

					// String maturingKey = Integer.toString(date.getYear()) +
					// Integer.toString(date.getWeekOfWeekyear());

					// if key is not in the hash, add new value
					if (!maturingAmounts.containsKey(weeksFromToday))
						maturingAmounts.put(weeksFromToday, amount);
					// else increment the existing value in hash
					else
						maturingAmounts.put(weeksFromToday, maturingAmounts.get(weeksFromToday).add(amount));
				}
		}

		return maturingAmountsIter;
	}

	public static HashMap<Integer, ArrayList<BondRecord>> parseBondAvailability(String path) throws IOException {

		HashMap<Integer, ArrayList<BondRecord>> bondsAvailableIter = new HashMap<Integer, ArrayList<BondRecord>>();

		@SuppressWarnings("resource")
		CSVReader csvReader = new CSVReader(new FileReader(path));
		String[] column = null;
		csvReader.readNext();

		while ((column = csvReader.readNext()) != null) {
//			System.out.println(column[0]+ ","+column[1]);
			String cusip = column[0];
			BigDecimal coupon = new BigDecimal(column[1]);
			LocalDate maturityDate = new LocalDate(column[2]);
			int availableQuantity = Integer.parseInt(column[3]);
			BigDecimal price = new BigDecimal(column[4]);
			BigDecimal yieldToMaturity = new BigDecimal(column[5]);

			// ignore bonds greater than 1 year away
			if (Days.daysBetween(Main.today, maturityDate).getDays() > 366)
				continue;

			// System.out.println(br);
			int weeksFromToday = (maturityDate.getWeekOfWeekyear() - Main.today.getWeekOfWeekyear()
					+ Main.weeksInThisYear) % 52;

			// ignore bonds that are maturing this week
			if (weeksFromToday < 1)
				continue;

			BondRecord br = new BondRecord(cusip, coupon, maturityDate, availableQuantity, price, yieldToMaturity);
			if (!bondsAvailableIter.containsKey(weeksFromToday))
				bondsAvailableIter.put(weeksFromToday, new ArrayList<BondRecord>());

			bondsAvailableIter.get(weeksFromToday).add(br);

			// sort in descending order by yield the week that was changed
			Collections.sort(bondsAvailableIter.get(weeksFromToday),
					(br1, br2) -> br2.getYieldToMaturity().compareTo(br1.getYieldToMaturity()));
		}

		return bondsAvailableIter;
	}

	public static void parseSchwabBondPage(String path) throws IOException {
		System.out.println("Parsing " + path);
		FileOutputStream fs = new FileOutputStream(new File("bonds_available.csv"));

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fs));

		String fileString = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
		Document doc = Jsoup.parse(fileString);

		Elements allTags = doc.select(".table-results").select("tbody").first().getElementsByTag("tr");
		for (int i = 0, len = allTags.size(); i < len; i++) {

			Element iter = allTags.get(i);

			// get CUSIP
			String cusipLine = iter.getElementsByTag("th").text();
			int cusipIndex = cusipLine.toLowerCase().indexOf("cusip") + 6;
			String cusip = cusipLine.substring(cusipIndex);

			Elements bondData = iter.getElementsByTag("td");
			// bondData[2] = coupon
			BigDecimal coupon = new BigDecimal(bondData.get(2).text());
			// bondData[3] = maturity
			LocalDate maturityDate = LocalDate.parse(bondData.get(3).text(), DateTimeFormat.forPattern("MM/dd/YY"));
			// bondData[4] = type
			// boolean isAsk = bondData.get(4).text().toLowerCase().contains("ask");
			// bondData[5] = quantity
			int availableQuantity = Integer.parseInt(bondData.get(5).text());

			// bondData[6] = price
			BigDecimal price = new BigDecimal(bondData.get(6).text());

			// bondData[7] = min
			// int minOrder = Integer.parseInt(bondData.get(7).text());

			// bondData[8] = max
			// int maxOrder = Integer.parseInt(bondData.get(8).text());

			// bondData[9] = YTM
			BigDecimal yieldToMaturity;

			try {
				yieldToMaturity = new BigDecimal(bondData.get(9).text());
			} catch (NumberFormatException e) {
				// ignore
				yieldToMaturity = new BigDecimal(-1);
				continue;
			}

			BondRecord br = new BondRecord(cusip, coupon, maturityDate, availableQuantity, price, yieldToMaturity);
			bw.write(br.toCsv());
			bw.newLine();

		}
		bw.close();
		fs.close();

	}
}
