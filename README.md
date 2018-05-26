# BondBuying
An algorithm to enable savvy investors to achieve highest yield on short-term debt (treasury bill ladder)

Interest Rates in checking, saving, and CD accounts at most credible financial insitutions are extremely low relative to the treasury yield that th US Government provides on it's short-term debt. Treasury bills, government bonds that matury in less than a year, are one of the safest instruments in the world. These bills can be bought/sold at any time, but allowing the bonds to mature is the best way to ensure full accumulation of the yield. For the individual, that means laddering the bonds to a schedule that fits the cash liquidity needs.

The following script is designed for individuals to ladder their portfolio programattically, and there are 3 inputs: target allocations (Ex. allocations1.xlsx), existing portfolio (Ex. portfolio1.csv), and a CSV file of available treasuy bills with pricing. 

Trade execution is not included in this tool, but a script can be written to integrate. 

## Setup

### Get a list of bonds available

To find the list of current treasury bills for sale, you'll need extract a CSV file from your broker's data. The format should be as follows

    "cusip, coupon, maturity date, available quantity (in thousands), price (on the 100),
                YTM (yield to maturity)"

If you have a Schwab account, this tool already has that completed. You can save the treasury search results to feed to this algorithm. Visit the `https://client.schwab.com/Areas/Trade/FixedIncomeSearch/FISearch.aspx/Treasuries` after logging in, and select the maturity date from this month to one year from now. Save the page html. Repeat for each page of bonds. Run `java -jar bond_ladder.jar parseSchwabBondsPage file1.html file2.html`. Additional files can be appended in the same format. 

### Download your portfolio

If you have bonds already in your portfolio, the algorithm can take that into account to prevent overweighing. the CSV file downloaded from your broker should be formatted as follows. 

    "Symbol","Description","Quantity","Price","Price Change $","Price Change %","Market Value" 

Cash should have a description with the title "cash" and the total amount in the 7th column. Bonds need to have the CUSIP identitifer and description that includes the phrase "us treasury" and "DUE 04/26/18". Quantity is in total principal value.

    "912796PC7","US TREASURY BILL18 U S T BILL DUE 04/26/18","5,000","$99.75733","N/A","N/A"
    "Cash & Money Market","--","--","--","--","--","$1000000.00","$0.00","0%","--","--","--","--"

### Set your allocations

Your bond buying preferences are set in allocation.xlsx. There are 2 categories to note: special events, and general allocation.

Special events allow you to set certain funds that have to mature on a specific date, and general allocations are how you want the remaining funds distributed by week and percentage. If you have none, that can be removed by emptying the columns.

The remaining allocation block is distributed based on preferences set in Column B. As a general note, longer maturities bills have higher yield, but may decrease in value as interest rates go up.

If you had $1,000,000 to invest, the current file `allocations.xlx` file included would allocate $75,000 for maturity on May 18th, 2018, and of the remainder: keep $92,500 (10%) in pure cash, $138,750 (15%) in bills that mature in 4 weeks, 8 weeks, and onwards until 24 weeks out. 

`java -jar bond_ladder.jar calculateLadder portfolio.csv allocations.xlsx bonds_available.csv`
