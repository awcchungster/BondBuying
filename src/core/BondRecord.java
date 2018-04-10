package core;

import java.math.BigDecimal;

import org.joda.time.LocalDate;

public class BondRecord {
	private String cusip;
	private BigDecimal coupon;
	private LocalDate maturityDate;
	private int availableQuantity;
	private BigDecimal price;
	private BigDecimal yieldToMaturity;

	public BondRecord(String cusip, BigDecimal coupon, LocalDate maturityDate, int availableQuantity, BigDecimal price,
			BigDecimal yieldToMaturity) {
		this.cusip = cusip;
		this.coupon = coupon;
		this.maturityDate = maturityDate;
		this.availableQuantity = availableQuantity;
		this.price = price;
		this.yieldToMaturity = yieldToMaturity;
	}

	public BigDecimal getYieldToMaturity() {
		return yieldToMaturity;
	}

	public int getAvailableQuantity() {
		return availableQuantity;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public String toCsv() {
		return cusip + "," + coupon + "," + maturityDate + "," + availableQuantity + "," + price + ","
				+ yieldToMaturity;
	}

	public String toString() {
		return cusip + " @ $" + price.multiply(BigDecimal.TEN) + " - Exp: " + maturityDate + ", YTM:" + yieldToMaturity
				+ (coupon.compareTo(BigDecimal.ZERO) == 1 ? " (has coupon)" : "");
	}
}
