package core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.joda.time.LocalDate;

public class UserInputs {
	public static HashMap<Integer, BigDecimal> getAllocations(String file, BigDecimal cash,
			HashMap<Integer, BigDecimal> maturingAmounts) throws ValidationException, FileNotFoundException,
			IOException, EncryptedDocumentException, InvalidFormatException {

		Workbook workbook = WorkbookFactory.create(new File(file));
		Sheet sheet = workbook.getSheetAt(0);
		Iterator<Row> rows = sheet.rowIterator();

		HashMap<Integer, BigDecimal> desiredAllocation = new HashMap<Integer, BigDecimal>();
		HashMap<LocalDate, BigDecimal> specialEvent = new HashMap<LocalDate, BigDecimal>();
		while (rows.hasNext()) {
			XSSFRow row = (XSSFRow) rows.next();

			// check regular allocations [cell 1/2]
			if (row.getPhysicalNumberOfCells() >= 2 && row.getCell(0).getCellTypeEnum() == CellType.NUMERIC
					&& row.getCell(1).getCellTypeEnum() == CellType.NUMERIC) {
				BigDecimal allocation = new BigDecimal(row.getCell(1).getRawValue()).setScale(4, RoundingMode.CEILING);

				desiredAllocation.put(Integer.parseInt(row.getCell(0).getRawValue()), allocation);
			}

			// check special events [cell 3/4]
			if (row.getPhysicalNumberOfCells() > 2 && row.getCell(3).getCellTypeEnum() == CellType.NUMERIC
					&& HSSFDateUtil.isCellDateFormatted(row.getCell(3))) {

				specialEvent.put(new LocalDate(row.getCell(3).getDateCellValue()),
						new BigDecimal(row.getCell(4).getRawValue()).setScale(4, RoundingMode.CEILING));
			}

		}

		// validation for desired allocations
		BigDecimal total = BigDecimal.ZERO;
		int desiredAllocationMaxWeek = Integer.MIN_VALUE;
		int desiredAllocationMinWeek = Integer.MAX_VALUE;
		for (int key : desiredAllocation.keySet()) {
			// store minimum and maximum values for processing later
			desiredAllocationMaxWeek = Math.max(key, desiredAllocationMaxWeek);
			desiredAllocationMinWeek = Math.min(key, desiredAllocationMinWeek);

			// sum desired allocations for validation check
			total = total.add(desiredAllocation.get(key));
		}
		System.out.println("Total Allocation Sum:" + total);
		System.out.println("Min: " + desiredAllocationMinWeek + " Max: " + desiredAllocationMaxWeek);

		int compare = total.compareTo(BigDecimal.ONE);
		if (compare != 0) {
			System.out.println("FAILED VALIDATION: ALLOCATIONS DID NOT SUM TO 1");
			throw new ValidationException("ALLOCATIONS DID NOT SUM TO 1");
		} else
			System.out.println("PASSED VALIDATION");

		// generate purchase hash
		HashMap<Integer, BigDecimal> purchaseNeeded = new HashMap<Integer, BigDecimal>();

		// SUBTRACT OUT SPECIAL EVENTS
		BigDecimal cashLeft = cash;
		for (LocalDate eventDate : specialEvent.keySet()) {

			BigDecimal amount = specialEvent.get(eventDate);

			// find which week this is for
			int week = (eventDate.getWeekOfWeekyear() - Main.today.getWeekOfWeekyear() + Main.weeksInThisYear) % 52;
			// adds to hash
			if (purchaseNeeded.containsKey(week))
				purchaseNeeded.put(week, Misc.dollarify(purchaseNeeded.get(week).add(amount)));
			else
				purchaseNeeded.put(week, amount);

			cashLeft = cashLeft.subtract(amount);
		}

		for (int i = desiredAllocationMinWeek; i <= desiredAllocationMaxWeek; i++) {

			// retrieves desired amount based on cash in account
			BigDecimal iter = desiredAllocation.get(i).multiply(cashLeft);

			// removes already purchased allocation
			if (maturingAmounts.containsKey(i))
				iter = iter.subtract(maturingAmounts.get(i));

			// places in hash
			if (purchaseNeeded.containsKey(i))
				purchaseNeeded.put(i, Misc.dollarify(purchaseNeeded.get(i).add(iter)));
			else
				purchaseNeeded.put(i, iter);
		}

		return purchaseNeeded;
	}
}
