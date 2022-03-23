all: compile

compile:
	java -jar jtb132di.jar -te minijava.jj
	java -jar javacc5.jar minijava-jtb.jj
	javac MyVisitors/*.java
	javac SymbolTable/*.java
	javac VTable/*.java
	javac Main.java
	
run:
	java Main

clean:
	rm -f *.class *~
	rm -f ParseException.java Token.java TokenMgrError.java MiniJavaParser.java MiniJavaParserConstants.java MiniJavaParserTokenManager.java JavaCharStream.java minijava-jtb.jj
	rm -f MyVisitors/*.class
	rm -f SymbolTable/*.class
	rm -f VTable/*.class  
	rm -rf visitor
	rm -rf syntaxtree

