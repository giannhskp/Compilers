package MyVisitors;

import SymbolTable.DefinedVariable;
import SymbolTable.OffsetSet;
import SymbolTable.SymbolTable;
import VTable.RegisterSet;
import VTable.VTable;
import syntaxtree.*;
import visitor.GJDepthFirst;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CodeGeneratorVisitor extends GJDepthFirst<RegisterSet, Scope> {
    Writer writer;  // for wtiting the output to the file
    int registerCounter;
    int labelCounter=0;
    boolean isExpression=false; // flag that shows if we an Identifier is inside an expression or a declaration
    String currentExprType; // stores the name of a class when returning from a recursive visit call
    List<RegisterSet> functionCallParams; // stores the parameters of a function call
    Map<String,String> RegisterMap; // stores the name of a local variable and the register that corresponds to it inside a function

    public CodeGeneratorVisitor(Writer wr){ // initialize tge visitor
        writer=wr;
    }

    public void resetRegCounter(){ registerCounter=0; } // reset the reagister count in every function
    public String getRegister(){  // get a new register
        registerCounter++;
        return "%_"+(registerCounter-1);
    }
    public String getLabel(String l){ // get a new label
        labelCounter++;
        return l+(labelCounter-1);
    }
    public int getClassSize(String className){  // get the size of a class object
      int fieldSize = 0;
      while(true){
        List<OffsetSet> variableList = SymbolTable.VariableOffsets.get(className);  // get the offsets of the class fields
        if(variableList!=null && !variableList.isEmpty()){  // if the class has at least one field
            OffsetSet lastField = variableList.get(variableList.size()-1);  // get the ofsset of the last field
            fieldSize =  lastField.getOffset() + lastField.getSize(); // add the size of the variable
            break;
        }else{  // if the class has no fields check the superClass
          String superClass = SymbolTable.ClassInherits.get(className); // get the superClass
          if(superClass.equals(className)){ // if the class does not extend another class
              break;  // then it has 0 fields
          }
          className=superClass; // check the fields of the superClass
        }
      }
      return fieldSize+8; // add 8 to the offset for the pointer to the vtable
    }

    public void emit(String s) throws IOException { // write on the output file
        writer.write(s);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */

    public RegisterSet visit(MainClass n, Scope argu) throws Exception {
        RegisterSet set = n.f1.accept(this, null);
        String classname = set.getId();
        emit("define i32 @main () {\n");
        n.f11.accept(this, null);
        resetRegCounter();  // reset the register counter to 0
        RegisterMap = new LinkedHashMap<>(); // create a map for the local variables
        Scope newScope = new Scope(classname,"main");
        if(n.f14.present()) {
            n.f14.accept(this, newScope); // visit the VarDeclarations
        }
        if(n.f15.present()) {
            n.f15.accept(this, newScope); // visit the Statements
        }
        emit("\n  ret i32 0\n}");

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */

    public RegisterSet visit(ClassDeclaration n, Scope argu) throws Exception {
        RegisterSet set = n.f1.accept(this, null);
        String classname = set.getId(); // get the class name
        if(n.f4.present()) {
            Scope newScope = new Scope(classname,classname);  // check the MethodDeclarations
            n.f4.accept(this, newScope);
        }

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */

    public RegisterSet visit(ClassExtendsDeclaration n, Scope argu) throws Exception {
        RegisterSet set = n.f1.accept(this, null);
        String classname = set.getId(); // get the class name
        if(n.f6.present()) {
            Scope newScope = new Scope(classname,classname);
            n.f6.accept(this, newScope);  // check the MethodDeclarations
        }
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */

    public RegisterSet visit(MethodDeclaration n, Scope argu) throws Exception {
        resetRegCounter();
        RegisterMap = new LinkedHashMap<>();

        RegisterSet set = n.f1.accept(this, null);
        String myType = set.getId();  // get the return type
        String convertedRetType = VTable.typeConvert(myType);  // convert the type to LLVM type
        set = n.f2.accept(this, null);
        String myName = set.getId();  // get the className
        Scope newScope = new Scope(argu.getClassName(),myName);
        emit("\ndefine "+convertedRetType+" @"+argu.getClassName()+"." + myName); // define the function
        emit("(i8* "+getRegister());  // first argument is this
        if(n.f4.present()){
            RegisterSet args = n.f4.accept(this, newScope); // returns a string with all the argument types and registers
            emit(args.getId()); // emit all the argument types/registers
        }
        emit(") {\n");
        for(String arg: RegisterMap.keySet()){  // for every argument
            String reg = getRegister(); // get a new register
            String oldReg = RegisterMap.get(arg); // get the register of the argument
            String type = SymbolTable.ClassVariables.get(new DefinedVariable(arg,argu.getClassName(),myName));  // find the type of the argument
            String convertedType = VTable.typeConvert(type);  // convert the type to LLVM type
            emit("  "+reg+" = alloca "+convertedType+"\n"); // allocate space for the argument
            emit("  store "+convertedType+" "+oldReg+", "+convertedType+"* "+reg+"\n");  // store the value of the argument to the new register
            RegisterMap.put(arg,reg); // replace the old register of the argument with the new one
        }
        emit("\n");
        if(n.f7.present()) {  // visit all the variable declarations
            n.f7.accept(this, newScope);
        }
        if(n.f8.present()) {  // visit all the Statements
            n.f8.accept(this, newScope);
        }
        isExpression=true;
        RegisterSet returnExpr = n.f10.accept(this,newScope); // returns the register that the return result is stored to
        isExpression=false;
        // returnExpr.getId() -> type of the register , returnExpr.getRegister() -> the register that the return result is stored to
        emit("\n  ret "+returnExpr.getId()+" "+returnExpr.getRegister()+"\n");
        emit("}");
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */

    public RegisterSet visit(FormalParameterList n, Scope argu) throws Exception {
        RegisterSet tempSet = n.f0.accept(this, argu);
        String ret = ", "+tempSet.getId()+" "+tempSet.getRegister();  // , type_of_the_argument register_of_the_argument
        if (n.f1 != null) {
            ret += n.f1.accept(this, argu).getId();
        }

        return new RegisterSet(ret,null);
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public RegisterSet visit(FormalParameterTail n, Scope argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            RegisterSet tempSet = node.accept(this, argu);
            ret += ", " + tempSet.getId()+" "+tempSet.getRegister();  // , type_of_the_argument register_of_the_argument
        }

        return new RegisterSet(ret,null);
    }
    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public RegisterSet visit(FormalParameterTerm n, Scope argu) throws Exception {
        return n.f1.accept(this, argu);
    }


    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */

    public RegisterSet visit(FormalParameter n, Scope argu) throws Exception{
        RegisterSet set = n.f0.accept(this, argu);  // the type of the variable <type,LLVM type>
        String name = n.f1.accept(this, null).getId();  // the name of the variable
        RegisterMap.put(name,set.getRegister());  // add the new variable with it's register to the Map
        return set; // <type,LLVM type>
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */

    public RegisterSet visit(VarDeclaration n, Scope argu) throws Exception {
        RegisterSet tempSet = n.f0.accept(this, null);
        String myType = tempSet.getId();  // type of the variable
        String convertedType = VTable.typeConvert(myType);  // convertedType type of the variable
        RegisterSet idSet = n.f1.accept(this, argu);
        String myName = idSet.getId();  // id name
        String register = idSet.getRegister();  // identifier's register
        emit("  "+register+" = alloca "+convertedType+"\n");  // alocate space for the new variable
        emit("  store "+convertedType+" "+VTable.getInitialValues(convertedType)+", "+convertedType+"* "+register+"\n");  // initialize the variable
        RegisterMap.put(myName,register); // add the new variable to the Map
        n.f2.accept(this, argu);

        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public RegisterSet visit(AssignmentStatement n, Scope argu) throws Exception {
        RegisterSet idSet = n.f0.accept(this, null);
        n.f1.accept(this, argu);
        isExpression=true;
        RegisterSet rightExprSet = n.f2.accept(this, argu); // returns the register that the result of the expression is stored to
        isExpression=false;
        n.f3.accept(this, argu);

        String idName = idSet.getId();  // identifier's name
        String idRegister = RegisterMap.get(idName);  //  get identifier's register
        if(idRegister==null){ // if it is a class field
            int offset = SymbolTable.getVariableOffset(argu.getClassName(),idName) + 8; // get the offset of the field
            String tempIdRegister = getRegister();  // get a new register
            emit("  "+tempIdRegister+" = getelementptr i8, i8* %_0, i32 "+offset+"\n"); // get field using the offset
            idRegister = getRegister(); // get a new register
            DefinedVariable varDecl = SymbolTable.getClosestVar(idName,argu.getClassName(),argu.getFunctionName());
            String type = SymbolTable.ClassVariables.get(varDecl);  // get the type of the field
            String convertedType = VTable.typeConvert(type);  // convert the type
            emit("  "+idRegister+" = bitcast i8* "+tempIdRegister+" to "+convertedType+"*\n");  // cast to the right type
        }
        // store the value of the expression (using the register that the visit returned) to the register of the identifier
        emit("  store "+rightExprSet.getId()+" "+rightExprSet.getRegister()+" ,"+rightExprSet.getId()+"* "+idRegister+"\n");
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public RegisterSet visit(ArrayAssignmentStatement n, Scope argu) throws Exception {
        isExpression=true;
        RegisterSet idSet = n.f0.accept(this, argu);
        isExpression=true;
        RegisterSet indexSet = n.f2.accept(this, argu);
        isExpression=true;
        RegisterSet assignExprSet = n.f5.accept(this, argu);
        isExpression=false;

        String regArray = idSet.getRegister();
        String regArraySize = getRegister();
        emit("  "+regArraySize+" = load i32, i32* "+regArray+"\n"); //get array size from the first position of the array
        String regValidIndex = getRegister();
        String givenIndex = indexSet.getRegister();
        emit("  "+regValidIndex+" = icmp ule i32 "+regArraySize+", "+givenIndex+"\n");  // check if the given index is <= than the size of the array (and >=0)
        String label1 = getLabel("oob");  // get new label
        String label2 = getLabel("oob");  // get new label
        emit("  br i1 "+regValidIndex+", label %"+label1+", label %"+label2+"\n");
        emit(label1+":\n"); // label for invalid index
        emit("  call void @throw_oob()\n");
        emit("  br label %"+label2+"\n");
        emit(label2+":\n"); // label for valid index
        String newIndex = getRegister();  // get a new register
        emit("  "+newIndex+" = add i32 "+givenIndex+", 1\n"); //add 1 to the given index (as at the first index the size of the array is stored)
        String regArrayIndexPointer = getRegister();  // get a new register
        emit("  "+regArrayIndexPointer+" = getelementptr i32, i32* "+regArray+", i32 "+newIndex+"\n");  // get pointer to the index of the array
        String regRightExpr = assignExprSet.getRegister();  // get the register that the right hand side expression result is stored to
        emit("  store i32 "+regRightExpr+", i32* "+regArrayIndexPointer+"\n");  // store the result of the expr to the index of the array
        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public RegisterSet visit(IfStatement n, Scope argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        isExpression=true;
        RegisterSet conditionSet = n.f2.accept(this, argu);
        isExpression=false;
        String conditionRegister = conditionSet.getRegister();  // get the register that the result of the condition expression is stored to
        String labelThen = getLabel("if_then");
        String labelElse = getLabel("if_else");
        String labelEnd = getLabel("if_end");
        emit("  br i1 "+conditionRegister+", label %"+labelThen+", label %"+labelElse+"\n");  // check the condition and go to the corresponding label
        emit(labelThen+":\n");  // then label
        n.f4.accept(this, argu);
        emit("  br label %"+labelEnd+"\n"); // go to the end of if Statement
        emit(labelElse+":\n");  // else label
        n.f6.accept(this, argu);
        emit("  br label %"+labelEnd+"\n"); // go to the end of if Statement
        emit(labelEnd+":\n");
        return null;
    }


    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public RegisterSet visit(WhileStatement n, Scope argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String loopStartLabel = getLabel("loop_start");
        String loopBodyLabel = getLabel("loop_body");
        String loopEndLabel = getLabel("loop_end");
        emit("  br label %"+loopStartLabel+"\n");
        emit(loopStartLabel+":\n"); // start of loop
        isExpression=true;
        RegisterSet conditionSet = n.f2.accept(this, argu); // visit the condition expression
        isExpression=false;
        String conditionReg = conditionSet.getRegister(); // get the register that the expression result is stored to
        emit("  br i1 "+conditionReg+", label %"+loopBodyLabel+", label %"+loopEndLabel+"\n");  // depending on the condition go to the corresponding label
        emit(loopBodyLabel+":\n");  // loop body label
        n.f4.accept(this, argu);
        emit("  br label %"+loopStartLabel+"\n"); // go to the start label to check the condition
        emit(loopEndLabel+":\n");   // loop end label
        return null;
    }



    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public RegisterSet visit(PrintStatement n, Scope argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        isExpression=true;
        RegisterSet retSet = n.f2.accept(this, argu); // visit the expression and get the register that the result is stored to
        isExpression=false;
        emit("  call void (i32) @print_int(i32 " + retSet.getRegister() +")\n");  // call print int function
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return null;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public RegisterSet visit(AndExpression n, Scope argu) throws Exception {
        isExpression=true;
        RegisterSet exp1 = n.f0.accept(this, argu); // visit the clause
        n.f1.accept(this, argu);
        isExpression=true;
        RegisterSet exp2 = n.f2.accept(this, argu); // visit the clause
        isExpression=false;
        //create 3 labels
        String label1 = getLabel("andExpr");
        String label2 = getLabel("andExpr");
        String label3 = getLabel("andExpr");
        emit("  br label %"+label1+"\n");
        emit(label1+":\n");
        emit("  br i1 "+exp1.getRegister()+", label %"+label2+", label %"+label3+"\n"); // check the first clause and go to the corresponding label
        emit(label2+":\n"); // if the first clause is true
        emit("  br label %"+label3+"\n");
        emit(label3+":\n");
        String reg = getRegister();// get new register
        // check from which label we got here and compute the clause result
        emit("  "+reg+" = phi i1 [0, %"+label1+"], ["+exp2.getRegister()+", %"+label2+"]\n");
        return new RegisterSet("i1",reg); // return the register that the boolean result is stored to
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public RegisterSet visit(CompareExpression n, Scope argu) throws Exception {
        isExpression=true;
        RegisterSet exp1 = n.f0.accept(this, argu);   // visit the expression and get the register that the result is stored to
        n.f1.accept(this, argu);
        isExpression=true;
        RegisterSet exp2 = n.f2.accept(this, argu); // visit the expression and get the register that the result is stored to
        isExpression=false;
        String newReg = getRegister();
        // compare the 2 expression results and store the boolean result to newReg
        emit("  "+newReg+" = icmp slt i32 "+exp1.getRegister()+", "+exp2.getRegister()+"\n");
        return new RegisterSet("i1",newReg);  // return the register that the boolean result is stored to
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public RegisterSet visit(PlusExpression n, Scope argu) throws Exception {
        String className = argu.getClassName();
        String functionName = argu.getFunctionName();
        isExpression=true;
        RegisterSet exp1 = n.f0.accept(this, argu); // visit the expression and get the register that the result is stored to
        n.f1.accept(this, argu);
        isExpression=true;
        RegisterSet exp2 = n.f2.accept(this, argu); // visit the expression and get the register that the result is stored to
        isExpression=false;
        String newReg = getRegister();
        // add the 2 expression results and store the boolean result to newReg
        emit("  "+newReg+" = add i32 "+exp1.getRegister()+", "+exp2.getRegister()+"\n");
        return new RegisterSet("i32",newReg); // return the register that the int result is stored to
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public RegisterSet visit(MinusExpression n, Scope argu) throws Exception {
        isExpression=true;
        RegisterSet exp1 = n.f0.accept(this, argu); // visit the expression and get the register that the result is stored to
        n.f1.accept(this, argu);
        isExpression=true;
        RegisterSet exp2 = n.f2.accept(this, argu); // visit the expression and get the register that the result is stored to
        isExpression=false;
        String newReg = getRegister();
        // subtract the 2 expression results and store the boolean result to newReg
        emit("  "+newReg+" = sub i32 "+exp1.getRegister()+", "+exp2.getRegister()+"\n");
        return new RegisterSet("i32",newReg); // return the register that the int result is stored to
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public RegisterSet visit(TimesExpression n, Scope argu) throws Exception {
        isExpression=true;
        RegisterSet exp1 = n.f0.accept(this, argu); // visit the expression and get the register that the result is stored to
        n.f1.accept(this, argu);
        isExpression=true;
        RegisterSet exp2 = n.f2.accept(this, argu); // visit the expression and get the register that the result is stored to
        isExpression=false;
        String newReg = getRegister();
        // multiply the 2 expression results and store the boolean result to newReg
        emit("  "+newReg+" = mul i32 "+exp1.getRegister()+", "+exp2.getRegister()+"\n");
        return new RegisterSet("i32",newReg); // return the register that the int result is stored to
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public RegisterSet visit(ArrayLookup n, Scope argu) throws Exception {
        isExpression=true;
        RegisterSet arraySet = n.f0.accept(this, argu); // visit the expression
        isExpression=true;
        RegisterSet indexSet = n.f2.accept(this, argu); // visit the expression
        isExpression=false;
        String arrayRegister = arraySet.getRegister();  // get the register that the result of f0 is stored to
        String indexRegister = indexSet.getRegister();  // get the register that the result of f2 is stored to
        String regArraySize = getRegister();
        emit("  "+regArraySize+" = load i32, i32* "+arrayRegister+"\n");  // get the size of the array from the first index of the array
        String regValidIndex = getRegister();
        emit("  "+regValidIndex+" = icmp ule i32 "+regArraySize+", "+indexRegister+"\n"); // check if the index of the array is valid (by comparing it with the array size)
        String label1 = getLabel("oob");
        String label2 = getLabel("oob");
        emit("  br i1 "+regValidIndex+", label %"+label1+", label %"+label2+"\n");  // depending on if the index is valid go to the corresponding label
        emit(label1+":\n"); // if the index is not valid
        emit("  call void @throw_oob()\n");
        emit("  br label %"+label2+"\n");   // this is not actually executed
        emit(label2+":\n"); // if the index is valid
        String newIndex = getRegister();
        emit("  "+newIndex+" = add i32 "+indexRegister+", 1\n");    //add 1 to the given index (as at the first index the size of the array is stored)
        String regArrayIndexPointer = getRegister();
        emit("  "+regArrayIndexPointer+" = getelementptr i32, i32* "+arrayRegister+", i32 "+newIndex+"\n"); // get a pointer to the given index of the array

        String resultReg = getRegister();
        // get the element of the array index that was given and store it to resultReg
        emit("  "+resultReg+" = load i32, i32* "+regArrayIndexPointer+"\n");
        return new RegisterSet("i32",resultReg);  // return the value of the array in the given index
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public RegisterSet visit(ArrayLength n, Scope argu) throws Exception {
        isExpression=true;
        RegisterSet arraySet = n.f0.accept(this, argu);
        isExpression=false;

        String lengthReg = getRegister();
        // get the array size from the first index of the array and store it to lengthReg
        emit("  "+lengthReg+" = load i32, i32* "+arraySet.getRegister()+"\n");

        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return new RegisterSet("i32",lengthReg);  // return the register that the size of the array was stored
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public RegisterSet visit(MessageSend n, Scope argu) throws Exception {
        isExpression=true;
        RegisterSet callerSet = n.f0.accept(this, argu);  // visit the expression and get the register that the result is stored to
        isExpression=false;
        String className = currentExprType; // get the class name of the caller object
        n.f1.accept(this, argu);
        RegisterSet funSet = n.f2.accept(this, null);
        String functionName = funSet.getId(); // get the function name
        n.f3.accept(this, argu);
        List<RegisterSet> tempForNestedCalls = functionCallParams;  // kept for nested calls
        functionCallParams = new ArrayList<>(); // list that stores all the arguments type/registers
        if(n.f4.present()){
            n.f4.accept(this, argu);  // after visiting, all the arguments are stored in functionCallParams list
        }
        n.f5.accept(this, argu);
        String unconvertedReturnType = VTable.getFunReturnType(functionName,className); // get the return type of the function
        String returnType = VTable.typeConvert(unconvertedReturnType);  // convert the return type to LLVM type
        int functionOffset = VTable.getFunctionOffset(className,functionName);  // get the offset of the function
        String callerReg = callerSet.getRegister(); // get the register that the result of f0 is stored to
        String regBit1 = getRegister();
        String regLoad1 = getRegister();
        emit("  "+regBit1+" = bitcast i8* "+callerReg+" to i8***\n"); // cast the class object pointer so we can access vtable
        emit("  "+regLoad1+" = load i8**, i8*** "+regBit1+"\n");  // load vtable pointer
        String regGetEl = getRegister();
        emit("  "+regGetEl+" = getelementptr i8*, i8** "+regLoad1+", i32 "+functionOffset+"\n");  // get a pointer to the function using the fucntion offset
        String regLoad2 = getRegister();
        emit("  "+regLoad2+" = load i8*, i8** "+regGetEl+"\n"); // get the actual function pointer
        String regBit2 = getRegister();
        emit("  "+regBit2+" = bitcast i8* "+regLoad2+" to "+returnType+" (i8*");  // cast the function pointer to a pointer the same as the function signature
        for(RegisterSet param: functionCallParams){ // for every argument of the call
            emit(", "+param.getId()); // write the type of each of the arguments
        }
        emit(")*\n");
        String regFunCall = getRegister();
        // perform the call
        emit("  "+regFunCall+" = call "+returnType+" "+regBit2+"(i8* "+callerReg);  // the first argument is the object itself
        for(RegisterSet param: functionCallParams){ // for every argument of the call
            emit(", "+param.getId()+" "+param.getRegister()); // write the type and the register of each of the arguments
        }
        emit(")\n");
        functionCallParams = tempForNestedCalls;  // reset the argument list (for nested calls)
        currentExprType = unconvertedReturnType;  // set the return type based on the return type of the function
        return new RegisterSet(returnType,regFunCall);  // return the register that the result of the call is stored to
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public RegisterSet visit(ExpressionList n, Scope argu) throws Exception {
        isExpression=true;
        RegisterSet firstParam = n.f0.accept(this, argu); // returns the type and the register of the expression
        isExpression=false;
        functionCallParams.add(firstParam); // add the type and the register of each argument to the list
        if (n.f1 != null) {
            n.f1.accept(this, argu);
        }
        return null;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public RegisterSet visit(ExpressionTail n, Scope argu) throws Exception {
        for ( Node node: n.f0.nodes) {
            RegisterSet argSet = node.accept(this, argu);
            functionCallParams.add(argSet);
        }
        return null;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public RegisterSet visit(ExpressionTerm n, Scope argu) throws Exception {
        isExpression=true;
        RegisterSet parameter = n.f1.accept(this, argu);  // returns the type and the register of the expression
        isExpression=false;
        return parameter;
    }


    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public RegisterSet visit(ArrayAllocationExpression n, Scope argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        isExpression=true;
        RegisterSet indexSet = n.f3.accept(this, argu); // visit the expression and get the register that the result is stored to
        isExpression=false;
        n.f4.accept(this, argu);
        String label1 = getLabel("ArrayAlloc");
        String label2 = getLabel("ArrayAlloc");
        String regIsNeg = getRegister();
        emit("  "+regIsNeg+" = icmp slt i32 "+indexSet.getRegister()+", 0\n");  // check if the index is negative
        emit("  br i1 "+regIsNeg+", label %"+label1+", label %"+label2+"\n"); // based on if the index is negative go to the corresponding label
        emit(label1+":\n"); // if the index is negative
        emit("  call void @throw_oob()\n");
        emit("  br label %"+label2+"\n");
        emit(label2+":\n"); // if the index is not negative
        String newSize = getRegister();
        emit("  "+newSize+" = add i32 "+indexSet.getRegister()+", 1\n");  // add 1 to the given index as we need one more int to store the size of the array
        String reg1 = getRegister();
        emit("  "+reg1+" = call i8* @calloc(i32 4, i32 "+newSize+")\n");  // allocate index + 1 integers
        String reg2 = getRegister();
        emit("  "+reg2+" = bitcast i8* "+reg1+" to i32*\n");  // cast the returned pointer
        emit("  store i32 "+indexSet.getRegister()+", i32* "+reg2+"\n");  // store the size of the array to the first position of the array
        return new RegisterSet("i32*",reg2);
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public RegisterSet visit(AllocationExpression n, Scope argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);

        RegisterSet classSet = n.f1.accept(this, null);
        String className = classSet.getId();  // class name
        int classFieldsSize = getClassSize(className);  // get the size of the class fields using the offsets of the fields
        String reg1 = getRegister();
        emit("  "+reg1+" = call i8* @calloc(i32 1, i32 "+classFieldsSize+")\n");  // allocate one object
        String reg2 = getRegister();
        emit("  "+reg2+" = bitcast i8* "+reg1+" to i8***\n"); // cast the object in order to get it to point to the correct vtable
        String reg3 = getRegister();
        int vtableSize = VTable.MethodCount.get(className); // get the vtable size
        emit("  "+reg3+" = getelementptr ["+vtableSize+" x i8*], ["+vtableSize+" x i8*]* @."+className+"_vtable, i32 0, i32 0\n");  // get the address of the first element of the vtable
        emit("  store i8** "+reg3+", i8*** "+reg2+"\n");  // set the vtable to the correct address

        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        currentExprType = className;  // set the class name as the return type
        return new RegisterSet("i8*",reg1); // return the register with the address of the new object
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public RegisterSet visit(NotExpression n, Scope argu) throws Exception {
        String _ret=null;
        isExpression=true;
        RegisterSet expr = n.f1.accept(this, argu); // visit the expression and get the register that the result is stored to
        isExpression=false;
        String newReg = getRegister();
        // use xor with 1 to return the opposite result
        emit("  "+newReg+" = xor i1 1, "+expr.getRegister()+"\n");
        return new RegisterSet("i1",newReg);
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public RegisterSet visit(BracketExpression n, Scope argu) throws Exception {
        RegisterSet _ret=null;
        n.f0.accept(this, argu);
        isExpression=true;
        _ret = n.f1.accept(this, argu);
        isExpression=false;
        n.f2.accept(this, argu);
        return _ret;
    }


    public RegisterSet visit(Identifier n, Scope argu) throws Exception {
        if(argu==null){ // if null is given just return the name of the identifier
            return new RegisterSet(n.f0.toString(),null);
        }
        if(isExpression){ // if the identifier is inside an expression
            String id = n.f0.toString();  // the name of the id
            String idRegister = RegisterMap.get(id);  // the register of the id
            DefinedVariable varDecl = SymbolTable.getClosestVar(id,argu.getClassName(),argu.getFunctionName());
            String normalType = SymbolTable.ClassVariables.get(varDecl);  // get the type of the identifier
            String convertedType = VTable.typeConvert(normalType);  // convert the type to LLVM type
            String newRegister="";
            if(idRegister==null){ // if the identifier is a class field
                // class field
                int offset = SymbolTable.getVariableOffset(argu.getClassName(),id) + 8; // get the field offset
                String tempIdRegister = getRegister();
                emit("  "+tempIdRegister+" = getelementptr i8, i8* %_0, i32 "+offset+"\n"); // get a pointer to the field using the offset
                idRegister = getRegister();
                emit("  "+idRegister+" = bitcast i8* "+tempIdRegister+" to "+convertedType+"*\n");  // cast it to the correct type
            }
            newRegister= getRegister();
            // finally load the value of the identifier to a new register in order to be used in an expression
            emit("  "+newRegister+" = load "+convertedType+", "+convertedType+"* "+idRegister+"\n");
            currentExprType = normalType;
            return new RegisterSet(convertedType,newRegister);  // return the type of the id and the new register that contains it's value

        }
        if(argu.getClassName().equals(argu.getFunctionName())){
            return null;  // never actually gets here
        }else{
            String id = n.f0.toString();
            if(SymbolTable.ClassInherits.containsKey(id)){  // if it is a class name
                return new RegisterSet(VTable.typeConvert(id),getRegister()); // return the converted type and a new register
            }
            return new RegisterSet(id,getRegister()); // return the name of the identifier and a new register
        }
    }

    public RegisterSet visit(ArrayType n, Scope argu) {
        if(argu==null)
            return new RegisterSet("int[]",null);
        return new RegisterSet("i32*",getRegister()); // return the LLVM type and a new register
    }

    public RegisterSet visit(BooleanType n, Scope argu) {
        if(argu==null)
            return new RegisterSet("boolean",null);
        return new RegisterSet("i1",getRegister()); // return the LLVM type and a new register
    }

    public RegisterSet visit(IntegerType n, Scope argu) {
        if(argu==null)
            return new RegisterSet("int",null);
        return new RegisterSet("i32",getRegister()); // return the LLVM type and a new register
    }

    public RegisterSet visit(TrueLiteral n, Scope argu) {
        return new RegisterSet("i1","1"); // return the LLVM type and 1 (=true)
    }

    public RegisterSet visit(FalseLiteral n, Scope argu) {
        return new RegisterSet("i1","0"); // return the LLVM type and 0 (=false)
    }

    public RegisterSet visit(IntegerLiteral n, Scope argu) throws Exception {
        return new RegisterSet("i32",n.f0.toString());  // return the LLVM type and the number
    }
    public RegisterSet visit(ThisExpression n, Scope argu) throws Exception {
        currentExprType = argu.getClassName();  // set the class name as the return type
        return new RegisterSet("i8*","%_0");  // return the LLVM type and the register of the object itself (always %_0 inside functions)
    }
}
