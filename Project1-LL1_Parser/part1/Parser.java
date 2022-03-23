import java.io.IOException;
import java.io.InputStream;

/*		GRAMMAR

#1  expr -> term expr2
#2  expr2 -> + term expr2
#3       |  - term expr2
#4       |  ε
#5  term -> factor term2
#6  term2 -> ** term
#7        |  ε
#8  factor -> num
#9         |  ( expr )

*/


public class Parser {
	private InputStream input;	/* input from user	*/
	private int lookAhead;	/* the lookAhead token of the LL(1) parser	*/

	public Parser(InputStream input) throws IOException {	/* create and initialize the parser */
		this.input=input;
		this.lookAhead=input.read();	/* read the first character/token of the input */
	}

	public static void main(String[] args) {
		try {
			Parser parser = new Parser(System.in);	/* create a new parser that "reads" from the standard input */
			System.out.println(parser.evaluate());	/* parse the input and print the result */

		}catch (ParseException | IOException e) {
			System.out.println(e.getMessage());
		}
	}

	private int evaluate() throws IOException, ParseException {	/* start the parsing and the evaluation of the given input */
		int result=expr();	/* call the "goal" non terminal, after the parsing/evaluation the result is returned */
		if(outOfBounds() || matchCurrent('\n'))	/* the input must always end with new line or EOF */
			return result;
		else
			throw new ParseException();
	}

	private int expr() throws IOException, ParseException {
		/* expr -> term expr2	*/
		/* first call term() in order to parse the term and give the result of term as input to the expr2 in order to continue parsing */
		return expr2(term());
	}

	private int expr2(int term) throws IOException, ParseException {
		/* expr2 -> + term expr2
		 * 		 |	- term expr2
		 * 		 |  e
		 * */
		if(outOfBounds() || matchCurrent(')','\n') ) {	/* if the lookAhead is on the set FOLLOW(expr2) */
			return term;	/* we chose the 3rd rule (e) so we just return the result that we "took" as argument */
		}
		if(matchCurrent('+')) {	/* if the lookAhead token is '+' (1rst rule) */
			getChar('+');	/* consume the token and read the next one */
			/* because addition is left-associative we first calculate the sum of the already parsed term and the following term */
			return expr2(term + term());	/* then, this sum is given as input to expr2 in order to continue parsing following the rules of the grammar */
		}else if(matchCurrent('-')) {/* if the lookAhead token is '-' (2nd rule) */
			/* same logic as the addition */
			getChar('-');
			return expr2(term - term());
		}else {
			throw new ParseException();
		}
	}

	private int term() throws IOException, ParseException {
		/* term -> factor term		*/
		/* first call factor() in order to parse the factor and give the result of factor as input to the term2 in order to continue parsing */
		return term2(factor());
	}

	private int term2(int factor) throws IOException, ParseException {
		/* term2 -> ** term
		 * 		 |  e
		 * */
		if(outOfBounds() || matchCurrent('+','-',')','\n') ) {	/* if the lookAhead is on the set FOLLOW(term2) */
			return factor;	/* we chose the 2nd rule (e) so we just return the result that we "took" as argument */
		}
		if(matchCurrent('*')) {	/* if the lookAhead token is '*' (1st rule) */
			getChar('*');	/* consume the token and read the next one */
			if(matchCurrent('*')) {	/* the next token must also be '*' */
				getChar('*');	/* consume it */
				/* because exponentiation is right-associative we continue parsing (by calling term()) in order to find any following exponentiations*/
				/* if term() find another exponentiation as the next token, the second exponentiation is first computed and then we compute the first one */
				/* --->  factor ** term() , where term() is the result of any following exponentiations*/
				return pow(factor, term());
			}
		}
		throw new ParseException();
	}

	private int factor() throws IOException, ParseException {
		/* factor -> num
		 * 		  |  (expr)
		 * */
		if(matchCurrent('(')) {	/* if the lookAhead token is '*' (2nd rule) */
			getChar('(');	/* consume the token and read the next one */
			int parenthesisValue = expr();	/* recursively call expr in order to calculate the value of the expression inside the parenthesis */
			getChar(')');	/* the next token, after parsing the expression, must be ')' . If it is not, ParseException is thrown */
			return parenthesisValue;
		}else if(currentIsDigit()) {	/* if the lookAhead token is a number (1st rule) */
			return parseNumber();	/* call parseNumber() in order to parse the multi digit number */
		}else {
			throw new ParseException();
		}
	}

	private int parseNumber() throws IOException, ParseException {	/* function that parses a number that can be multidigit	*/
		if(lookAhead == '0'){   /* check if the number starts with 0*/
           		 getChar('0');   /* consume 0 */
            		if(currentIsDigit()){   /* the number starts with 0 followed by other numbers ex. 07 */
                		throw new ParseException();
            		}else{  /* the number token is 0 */
                		return 0;
            		}
        	}
		int num=0;
		while(currentIsDigit()) {	/* while the lookAhead token is a number */
			/* consume the token, get it's numeric value and create the new number */
			num= num*10 + Character.getNumericValue(getChar());
		}
		return num;
	}

	private void getChar(char c) throws IOException, ParseException {
		/* function that consumes read the character from the input only if the current lookAhead token matches the given one */
		if(!outOfBounds() && (c == lookAhead)) {	/* if we can read one more character, and the current lookAhead token matches the given*/
			/* read the next character and assing it as the new lookAhead token */
			lookAhead = input.read();
		}else {
			throw new ParseException();
		}

	}

	private int getChar() throws IOException, ParseException {
		/* funtion that returns the current lookAhead token and reads the next one */
		/* this function is only called by the parseNumber function, that we already know that the current lookAhead is a number before calling this function */
		if(!outOfBounds()) {
			int current = lookAhead;	/* store the current lookAhead token */
			lookAhead = input.read();	/* read the next one, and assing it as the new lookAhead token */
			return current;	/* return the old lookAhead token */
		}else {
			throw new ParseException();
		}

	}

	private boolean matchCurrent(char... validOperators ) {
		/* takes as argument a "list" of characters/operators and checks if the current lookAhead token matched to either of them */
		if(outOfBounds())
			return false;
		for(char op:validOperators) {	/* for each of the given characters */
			if(op == lookAhead) {	/* if it matched with the current lookAhead */
				return true;
			}
		}
		return false;
	}

	private boolean outOfBounds() {	/* checks if we have reached at the "end" of the input */
		return lookAhead==-1;
	}

	private boolean currentIsDigit() {	/* checks if the current lookAhead token is a number between 0 and 9 */
		if(outOfBounds())
			return false;
		return (lookAhead>='0') && (lookAhead<='9');
	}

	private static int pow(int base, int exponent) {
	        if (exponent < 0)
	            return 0;
	        if (exponent == 0)
	            return 1;
	        if (exponent == 1)
	            return base;

	        if (exponent % 2 == 0) //even exp -> b ^ exp = (b^2)^(exp/2)
	            return pow(base * base, exponent/2);
	        else                   //odd exp -> b ^ exp = b * (b^2)^(exp/2)
	            return base * pow(base * base, exponent/2);
	   }

}
