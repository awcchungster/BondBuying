package core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.joda.time.LocalDate;

public class Main {
	public static String[] parameters;
	public static int weeksInThisYear;
	public static LocalDate today;

	public static void main(String[] args) {
		today = new LocalDate();

		// get number of weeks in this year
		// determines if we should do 52 or 53
		LocalDate startCalc = today.withMonthOfYear(12).withDayOfMonth(24);
		for (int j = 1; j <= 7; j++) {
			weeksInThisYear = Math.max(startCalc.plusDays(j).getWeekOfWeekyear(), weeksInThisYear);
		}

		try {
			parameters = args;

			if (args[0].equals("parseSchwabBondsPage")) {
				if (args.length > 1) {
					PrintWriter writer = new PrintWriter("bonds_available.csv");
					writer.print("");
					writer.close();

					for (int i = 1; i < args.length; i++) {
						// "schwabtreasuries1.html schwabtreasuries2.html"
						Parser.parseSchwabBondPage(args[i]);
					}

					System.out.println("Completed");
				} else {
					System.out.println("Missing Parameters");
				}
			} else if (args[0].equals("calculateLadder")) {
				long lStartTime = System.currentTimeMillis();

				// "portfolio.csv"
				String portfolioFile = args[1];

				// "allocations.xlsx"
				String allocationsFile = args[2];

				// get cash
				BigDecimal cash = Parser.cashInPortfolio(portfolioFile);
				System.out.println("Cash: $" + cash);

				// get maturities of existing bonds
				HashMap<Integer, BigDecimal> maturingAmounts = new HashMap<Integer, BigDecimal>();
				maturingAmounts = Parser.bondsInPortfolio(portfolioFile, maturingAmounts);

				System.out.println("List bonds in portfolio (week/amount)");
				for (int name : maturingAmounts.keySet()) {
					System.out.println(name + " " + maturingAmounts.get(name).toString());
				}

				System.out.println("Listing bonds available");
				// hashmap from int week to list of bonds available for that week
				HashMap<Integer, ArrayList<BondRecord>> bondsAvailable = Parser.parseBondAvailability(args[3]);

				Set<Integer> keys = new TreeSet<Integer>(bondsAvailable.keySet());
				for (int i : keys) {
					System.out.println(i);
					ArrayList<BondRecord> iter = bondsAvailable.get(i);
					for (int j = 0; j < iter.size(); j++)
						System.out.println("\t" + iter.get(j));
				}

				HashMap<Integer, BigDecimal> purchaseNeeded = UserInputs.getAllocations(allocationsFile, cash,
						maturingAmounts);

				String toPrint = "Based on client prescribed needs in " + allocationsFile + "\n\n";
				for (int week : purchaseNeeded.keySet()) {
					toPrint += "Client desires $" + Misc.getCurrFormat(purchaseNeeded.get(week))
							+ " worth of bonds that expire in " + week + " weeks\n";

					BigDecimal bondsToBuy = purchaseNeeded.get(week).divide(new BigDecimal(1000));
					// System.out.println(bondsToBuy.intValue());
					int weekIter = week;
					while (true) {
						// if week is 0, move on
						if (weekIter == 0)
							break;
						// else if there are bonds, take the highest yield
						else if (bondsAvailable.containsKey(weekIter)) {
							BondRecord br = bondsAvailable.get(weekIter).get(0);
							toPrint += "BUY: " + bondsToBuy.intValue() + " bonds of " + br + "\n\n";
							break;
						}
						// else look for bonds the week before
						else {
							weekIter--;
						}
					}
				}

				BufferedWriter writer = new BufferedWriter(new FileWriter("log.txt", false));
				writer.write(toPrint.substring(0, toPrint.length() - 4));
				writer.close();
				System.out.println(toPrint.substring(0, toPrint.length() - 4));
				System.out.println("TO PURCHASE FILE GENERATED in log.txt");
				BigDecimal timeRounded = new BigDecimal((System.currentTimeMillis() - lStartTime) * .001);
				Misc.echo("Time Elapsed: " + timeRounded.divide(new BigDecimal("1.0"), 3, BigDecimal.ROUND_HALF_UP));
			} else {
				System.out.println("Invalid start parameters");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}