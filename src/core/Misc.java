package core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Locale;

public class Misc {
	/**
	 * Used in place of system.out.printlns to record logs to database
	 * 
	 * @param s
	 */
	public static void echo(String s) {
//		try {
//			if (Main.parameters.length > 1 && Main.parameters[1].equals("server"))
//				DbModuleC.output(Main.parameters[0], s, 1);
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
		System.out.println(getTimestamp() + s);
	}

	/**
	 * Returns System.printout formatting
	 * 
	 * @return
	 */
	public static String getTimestamp() {
		return new Timestamp((new java.util.Date()).getTime()).toString().substring(0, 21) + ": ";
	}
	
	public static String getCurrFormat(BigDecimal amount) {
		return String.format("%,.2f", amount.setScale(2, RoundingMode.DOWN));
	}

	public static String getThousandsFormat(int number) {
		return NumberFormat.getNumberInstance(Locale.US).format(number);
	}
	
	public static BigDecimal dollarify(BigDecimal d) {
		return d.divide(new BigDecimal("1.0"), 2, BigDecimal.ROUND_HALF_UP);
	}
}
