import MyVisitors.CodeGeneratorVisitor;
import MyVisitors.CreateVtableVisitor;
import MyVisitors.Visitor1;
import MyVisitors.Visitor2;
import SymbolTable.SymbolTable;
import VTable.VTable;
import syntaxtree.Goal;

import java.io.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length == 0){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }

        FileInputStream fis = null;
        Writer writer=null;
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
                String outputFile;
                if(fileArg.contains(".java")){
                    outputFile = fileArg.replace(".java",".ll");
                }else{
                    outputFile = fileArg+".ll";
                }
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "utf-8"));
                CreateVtableVisitor vis = new CreateVtableVisitor(writer);
                root.accept(vis, null);
                CodeGeneratorVisitor vis2 = new CodeGeneratorVisitor(writer);
                root.accept(vis2, null);
                System.out.println("\n-----------------------------------------------------");
            } catch (ParseException ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace();
            } finally {
                SymbolTable.deleteData();
                VTable.deleteData();
                try {
                    if (fis != null) fis.close();
		            if (writer != null) writer.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}



