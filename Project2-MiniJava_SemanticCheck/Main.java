import syntaxtree.*;
import visitor.*;
import MyVisitors.Visitor1;
import MyVisitors.Visitor2;
import SymbolTable.SymbolTable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length == 0){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }

        FileInputStream fis = null;
        for(String fileArg: args) {
            try {
                fis = new FileInputStream(fileArg);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();

                System.out.println("\n-----------------------------------------------------\n");
                System.out.println("File: "+fileArg);
                Visitor1 eval = new Visitor1();
                root.accept(eval, null);
                Visitor2 v2 = new Visitor2();
                root.accept(v2, null);
                System.out.println("Program parsed successfully.");
                System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
                System.out.println("Offsets:");
                SymbolTable.printOffsets();
                System.out.println("\n-----------------------------------------------------");
                SymbolTable.deleteData();
            } catch (ParseException ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null) fis.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}



