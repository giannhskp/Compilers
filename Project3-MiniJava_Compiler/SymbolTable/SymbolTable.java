package SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class SymbolTable {
    // ClassInherits stores all the defined classes as keys, and as values the class that it extends (it's "father" class)
    // if a class does not extends another class, then it has it's own name both as key and value.
    public static Map<String,String> ClassInherits =  new HashMap<String,String>();
    // ClassVariables contains all the defined variables. The key is the DefinedVariable and as value the variable's type
    public static Map<DefinedVariable,String> ClassVariables =  new HashMap<DefinedVariable,String>();
    // FunctionDeclarations contains all the function declarations. The key is type FunctionKey and it uniquely identifies a function
    // As item, every function has a list that has the return type as the first element followed by the types of the arguments (if it has any)
    public static Map<FunctionKey,List<String>> FunctionDeclarations =  new HashMap<FunctionKey,List<String>>();

    // VariableOffsets is used in order to print the class variables offsets. Every key of the Map corresponds to a class.
    // The list contains the variables of this class and the corresponding offset of each variable
    public static Map<String, List<OffsetSet>> VariableOffsets = new HashMap<String, List<OffsetSet>>();
    // FunctionOffsets is used in order to print the function offsets.
    public static Map<String, List<OffsetSet>> FunctionOffsets = new HashMap<String, List<OffsetSet>>();
    public static List<String> ClassNames = new ArrayList<String>(); // Stores all the class names. Used in order to print the classes offsets, in the order that are defined in the input file
    public static String MainClassName; // stores the name of the class that contains the main. (Used in order to not print this class in the offset printing)

    public static void classDec(String className,String superClass) throws Exception {  // used by the first visitor for the defined classes
        if (ClassInherits.containsKey(className)){  // if a class with tha same name is already defined
            throw new Exception("Class: " + className + " already defined");
        }else{
            if(className.equals(superClass))    // if the class does not extend another class
                ClassInherits.put(className,superClass);
            else if(ClassInherits.containsKey(superClass))   // if the superClass is already defined
                ClassInherits.put(className,superClass);
            else    // if the superClass is not defined. (super classes must always defined first)
                throw new Exception("Child class: "+ className +" defined before super class");

        }
        ClassNames.add(className);
    }

    public static void varDec(String variableName,String className,String functionName, String type) throws Exception {
        DefinedVariable newVar = new DefinedVariable(variableName,className,functionName);  // create a variable "key" for the Map
        if (ClassVariables.containsKey(newVar)){    // check if a variable with the same name in the same scope is already defined
            throw new Exception("Variable: " + variableName + " already defined in the same scope");
        }else{
            ClassVariables.put(newVar,type);    // add the variable to the symbol table
        }
        // Find the offset of this variable
        if(!className.equals(functionName)) // if the variable is defined inside a function, dont find the offset
            return;
        if(!VariableOffsets.containsKey(className)){  // if it is the first variable of this class, initialize the variable list
            VariableOffsets.put(className,new ArrayList<OffsetSet>());
        }

        int startOffsetFrom = 0;
        String currentClass = className;
        while(true) {   // check superclasses
            String superClass = ClassInherits.get(currentClass);
            if (!superClass.equals(currentClass)) {  //check if the class extends another class
                // if it extends another class, find the offset of the last variable
                List<OffsetSet> superClassVariables = VariableOffsets.get(superClass);  // get the variables/offsets of the superclass
                if (superClassVariables != null) {    // if the superclass has at least one variable
                    OffsetSet lastVar = superClassVariables.get(superClassVariables.size() - 1);  // get the last/variable offset
                    startOffsetFrom = lastVar.getOffset() + lastVar.getSize();
                    break;
                }else{  // if the superclass has no variables, continue searching to it's superclass (if it exists)
                    currentClass = superClass;
                }
            }else{  // if it does not extend another class
                break;
            }
        }


        List<OffsetSet> variableList = VariableOffsets.get(className);  // get the already defined variables of the current class
        int size = getTypeSize(type);   // get the size of the current variable
        if(variableList.isEmpty()){ // if it the the first variable that is defined in this class
            variableList.add(new OffsetSet(variableName,startOffsetFrom,size)); // add the variable to the list of this class
        }else{
            OffsetSet lastVar = variableList.get(variableList.size()-1);    // get the last variable/offset of this class
            int newVarStart = lastVar.getOffset() + lastVar.getSize();  // compute the offset
            variableList.add(new OffsetSet(variableName,newVarStart,size)); // add the variable/offset to the list
        }
    }

    public static void funDec(String functionName,String className,String returnType, String ArgumentTypes) throws Exception {
        FunctionKey funKey = new FunctionKey(functionName,className);   // create the function "key" for the Map
        if(FunctionDeclarations.containsKey(funKey)){   // check if a function with the same name is already defined in the same class
            throw new Exception("Function: " + functionName +" already defined in this class. Overloading is not supported.");
        }else{
            List<String> types = new ArrayList<String>();   // list that stores the return type of the function and it's arguments types
            types.add(returnType);  // at the first index of the list store the return type
            if(!ArgumentTypes.equals("")) { // if the function "takes" at least one argument
                String[] splittedArgTypes = ArgumentTypes.split(",");  // split the arguments as we take them in one string
                for (String t : splittedArgTypes) { // add every argument type to the list
                    types.add(t);
                }
            }
            String currentClass = className;
            while(true){    // search all the superclasses in order to check if it exists a fucntion with the same name (override)
                if(ClassInherits.containsKey(currentClass)){  // if the class exists
                    String superClass = ClassInherits.get(currentClass);    // get the super class
                    if(currentClass.equals(superClass))    // we reached the root class
                        break;
                    FunctionKey superClassKey = new FunctionKey(functionName,superClass);
                    if(FunctionDeclarations.containsKey(superClassKey)){    // check if the function exists in the superclass
                        // if it exists, check if they have the same return type and argument types
                        if(!types.equals(FunctionDeclarations.get(superClassKey))){ // if the don't match
                            throw new Exception("Function: "+functionName+" override with different argument/return types.");
                        }
                        // if the match, override is correct
                        break;
                    }else{  // if the function does not exist in the superclass, check the "next" superclass (if it exists)
                        currentClass = superClass;
                    }
                }else{
                    throw new Exception("Undefined class name");
                }

            }
            // finally add the function to the function declaration symbol table
            FunctionDeclarations.put(funKey,types);
        }
        // Compute the function offset
        if(functionName.equals("main")) // skip main function
            return;
        if(!FunctionOffsets.containsKey(className)){    // check if it is the first function of this class
            // if it is, put the class to the map and initialize the function list
            FunctionOffsets.put(className,new ArrayList<OffsetSet>());
        }
        String currentClass = className;
        while(true){    // check all the superclasses, in order to check if this function overrides a function from a "father" class
            String superClass = ClassInherits.get(currentClass);
            if(superClass.equals(currentClass))
                break;
            FunctionKey superFun = new FunctionKey(functionName,superClass);
            if(FunctionDeclarations.containsKey(superFun))  // if the same function exists to a super class
                return; // dont compute the offset of this function
            currentClass = superClass;
        }

        int startOffsetFrom = 0;
        currentClass = className;
        while(true) {   // check the super classed in order to find the last declared function
            String superClass = ClassInherits.get(currentClass);
            if (!superClass.equals(currentClass)) { // if the current class has a superclass
                List<OffsetSet> superClassFunctions = FunctionOffsets.get(superClass);  // get the functions of the superclass
                if (superClassFunctions!=null && !superClassFunctions.isEmpty()) {  // if it has at least one function
                    OffsetSet lastVar = superClassFunctions.get(superClassFunctions.size() - 1);
                    startOffsetFrom = lastVar.getOffset() + lastVar.getSize();  // the new function's offset will start from here
                    break;
                }else{  // if the super class has no functions, continue searching to it's superclass (if it exists)
                    currentClass = superClass;
                }
            }else{  // if there are no more superclasses
                break;
            }
        }

        List<OffsetSet> functionList = FunctionOffsets.get(className);
        int size = 8;   // every function is a "pointer", so their size is 8
        if(functionList.isEmpty()){ // if it is the first function of the class
            functionList.add(new OffsetSet(functionName,startOffsetFrom,size)); // add the function to the list, with the offset that was computed
        }else{  // if there are already functions in this class
            OffsetSet lastVar = functionList.get(functionList.size()-1);    // get the last function with it's offset
            int newVarStart = lastVar.getOffset() + lastVar.getSize();  // the new function's offset will start from here
            functionList.add(new OffsetSet(functionName,newVarStart,size)); // add the function to the list
        }
    }

    public static DefinedVariable getClosestVar(String expr,String className,String functionName){
        // returns the variable's definition with the given name that is "closer" to the given scope
        // in other words, returns the definition of the variable that the given identifier of the given scope is referring to
        DefinedVariable variable = new DefinedVariable(expr,className,functionName);    // if it is defined inside the function
        if(!ClassVariables.containsKey(variable)){
            // if it is not defined inside the function, check it is a field of the given class
            variable = new DefinedVariable(expr,className,className);
            if(!ClassVariables.containsKey(variable)){
                // if it is not a field of the given class, check if it is a field of the "parent" classes
                String superClass = className;
                while(true){    // start searching in superclasses
                    String newSuperClass = ClassInherits.get(superClass);
                    if(newSuperClass == null || newSuperClass.equals(superClass)){ // if we reached the root class
                        return null;
                    }
                    variable = new DefinedVariable(expr,newSuperClass,newSuperClass);
                    if(ClassVariables.containsKey(variable)){   // check if it is a field of the super class
                        break;
                    }
                    superClass = newSuperClass;
                }
            }
        }
        return variable;
    }

    public static void checkExpressionType(String expr,String validType,String className,String functionName) throws Exception {
        // check if the given variable/type (expr) is the same as the validType
        if("int".equals(expr) || "boolean".equals(expr) || "int[]".equals(expr)){   // if a naive type was given
            if(!validType.equals(expr)){    // check if it is the same as the validType
                throw new Exception("Wrong type of: " + expr + " . Must be of type: " + validType);
            }
            return;
        }
        if("true".equals(expr) || "false".equals(expr)){    // if true or false was given
            if(!validType.equals("boolean"))    // check if validType is boolean
                throw new Exception("Wrong type of: " + expr + " . Must be of type: " + validType);
            return;
        }
        if(isNum(expr)){    // check if a number was given
            if("int".equals(validType)){    // check if the valid type is int
                return;
            }
            throw new Exception("Wrong type of: " + expr + " (int). Must be of type: " + validType);
        }
        if("int".equals(validType) && isNum(expr)){
            return;
        }
        if(expr.equals("this")) // if the keyword this was given
            expr = className;   // set the type as the class name

        String type;
      if(ClassInherits.containsKey(expr)){    // check if the given type is a class name
        type = expr;    // assing the type to be the class name
      }else{
        throw new Exception("Undefined variable: " + expr);
      }

        if(!type.equals(validType)){
            // if the given type is not the same as the valid, check if validType is a superClass of the given type
            while(true){
                String superClass = ClassInherits.get(type);    // get superclass
                if(superClass == null || superClass.equals(type)) break;
                if(superClass.equals(validType)) return;    // valid type was a superclass of the type
                type = superClass;  // continue with the next superclass
            }

            throw new Exception("Wrong type of: " + expr + " . Must be of type: " + validType);
        }

    }

    public static void checkAssignmentTypes(String exprLeft,String exprRight,String className,String functionName) throws Exception {
        // check if the two "operands" of an assignment are the same type
        // left operands can only be a variable
        // call checkExpressionType in order to check if the left operands is the same type as the type of the right that we just found
        checkExpressionType(exprRight,exprLeft,className,functionName);
    }

    public static String checkFunctionCall(String identifier,String functionName,String arguments) throws Exception {
        // identifier is the expression type that tha function is called to
        // so it must match the class that the function is defined in
        FunctionKey fun = new FunctionKey(functionName,identifier); // create a function key for the given function name and the given class/identifier
        if(!FunctionDeclarations.containsKey(fun)){ // if the function is not defined in this class
            while(true){    // check if it is defined in a parent class
                String superclass = ClassInherits.get(identifier);  // get super class
                if(superclass == null || superclass.equals(identifier)){    // if we have reached the base class
                    throw new Exception("Function: " + functionName + " is not defined");
                }
                fun = new FunctionKey(functionName,superclass); //check if the function is defined in the super class
                if(FunctionDeclarations.containsKey(fun))   // if it is defined in the superclass, stop searching
                    break;
                identifier = superclass;    // continue searching in the superclass of the current class
            }
        }
        // if we found the definition of the function, we must check if the arguments of the call are the same type as the arguments of the definition
        List<String> functionTypes = FunctionDeclarations.get(fun); // get the return type and argument types of the function definition
        String funReturnType = getReturnType(functionTypes);    // get the return type
        List<String> funArgumentTypes = getArgumentTypes(functionTypes);    // get the argument types

        if(funArgumentTypes.isEmpty()){ // if the definition has no arguments
            if("".equals(arguments))    // check if the call also has no arguments
                return funReturnType;
            throw new Exception("Function: " + functionName + " takes 0 arguments, but arguments were given");
        }else{  // if the definition of the function has at least one argument
            String []givenArguments = arguments.split(","); // get the call argument types
            if(funArgumentTypes.size() != givenArguments.length){   // check if the call has the same number of arguments as the definition
                throw new Exception("Missmatched number of arguments. More/Less arguments we given in function: "+ functionName);
            }
            for(int i=0;i<funArgumentTypes.size();i++){ // compare one by one the types of the call arguments with the types of the definition arguments
                if(!funArgumentTypes.get(i).equals(givenArguments[i])){
                    // if the are not the same type, check if the definition argument type is a superclass of the call argument type
                    String givenArg = givenArguments[i];
                    if(ClassInherits.containsKey(givenArg)){    // if the type is a class name
                        String givenClass = givenArg;
                        while(true){    // check all the superclasses
                            String superclass = ClassInherits.get(givenClass);  // get the superclass
                            if(superclass == null || superclass.equals(givenClass)){    // if we reached the base class
                                throw new Exception("Argument ("+ (i+1) +") of function: " + functionName + " is type: "+ givenArg + " instead of type: "+ funArgumentTypes.get(i));
                            }
                            if(superclass.equals(funArgumentTypes.get(i)))  // if we found the same type/class
                                break;
                            givenClass = superclass;
                        }
                    }else{
                        throw new Exception("Argument ("+ (i+1) +") of function: " + functionName + " is type: "+ givenArg + " instead of type: "+ funArgumentTypes.get(i));
                    }
                }
            }
            // if all the arguments are the same type, return the type that the function returns
            return funReturnType;
        }
    }

    public static  String getFunCallerType(String expr, String className,String functionName) throws Exception {
        // finds the type of the expression that a function is called to (for example: expr.someFunction() )
        // the expression can either be the keyword this, or an identifier that is defined as a class object
        if("this".equals(expr)) // if the keyword this was given, return the class name
            return className;
        // if the expression was an identifier of a class object, then the class name is given as argument to this function by the visit function of the Visitor2
        if(ClassInherits.containsKey(expr)) // if the given expr is a class name, return the given class name
            return expr;
        throw new Exception("Message send: the base is not a user defined type, "+expr);
    }

    public static  String getArgumentType(String expr, String className,String functionName) throws Exception {
        // returns the type of a given expression
        if("int".equals(expr) || isNum(expr))   // if "int" was given
            return "int";
        if("int[]".equals(expr))    // if "int[]" was given
            return "int[]";
        if("boolean".equals(expr) || "true".equals(expr) || "false".equals(expr)) // if boolean type or "true" or "false" were given
            return "boolean";
        if("this".equals(expr)) // if the keyword this was given
            return className;
        if(ClassInherits.containsKey(expr)) // if a class name was given
            return expr;
        throw new Exception("Unknown type of function argument: " + expr);
    }

    private static boolean isNum(String x){
        // check if the given string is a number
        try{
            Integer.parseInt(x);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public static void checkClassAllocation(String className) throws Exception {
        // check if the given class name, is actually a defined class
        if(!ClassInherits.containsKey(className)){
            throw new Exception("Reference to undefined class: " + className);
        }
    }

    public static void checkDeclarationType(String type) throws Exception {
        // check if a type of a declaration is valid
        // mostly used in order to check Class objects that are defined, and the first Visitor cannot check as some
        //   of the classes are not yet defined at the moment that the declaration is seen
        if("int".equals(type) || "boolean".equals(type) || "int[]".equals(type))
            return;
        if(ClassInherits.containsKey(type)) // check if it a defined class name
            return;
        throw new Exception("Type/Class: "+ type +" is not defined");
    }

    public static void deleteData(){
        ClassInherits.clear();
        ClassVariables.clear();
        FunctionDeclarations.clear();
        VariableOffsets.clear();
        FunctionOffsets.clear();
        ClassNames = new ArrayList<String>();
    }

    public static void printOffsets(){
        for (String className: ClassNames){   // for every defined class
            if(className.equals(MainClassName)) // skip the class that contains main, so as to not be printed
                continue;
            System.out.println("    ------------------- Class: "+className+" ---------------------");
            System.out.println("    -------------- Variables ----------------");
            if(VariableOffsets.containsKey(className))  // if the class has at least one variable definition
                for(OffsetSet var: VariableOffsets.get(className)){ // print the variable and it's offset
                    System.out.println("    "+className+"."+var.getName()+" : "+var.getOffset());
                }
            System.out.println("    -------------- Functions ----------------");
            if(FunctionOffsets .containsKey(className)) // if the class has at least one function definition
                for(OffsetSet var: FunctionOffsets.get(className)){ // print the function and it's offset
                    System.out.println("    "+className+"."+var.getName()+" : "+var.getOffset());
                }
            System.out.println();
        }
    }

    public static int getOffset(String className,String functionName){
        while(true){
            if(FunctionOffsets.containsKey(className)){
                for(OffsetSet var: FunctionOffsets.get(className)){
                    if(var.getName().equals(functionName))
                        return var.getOffset();
                }
            }
            String superClass = ClassInherits.get(className);
            if(superClass.equals(className)){
                System.err.println("Should never get here");
                return -1;
            }
            className=superClass;
        }
    }

    public static int getVariableOffset(String className,String id){
        while(true){
            if(VariableOffsets.containsKey(className)){
                for(OffsetSet var: VariableOffsets.get(className)){
                    if(var.getName().equals(id))
                        return var.getOffset();
                }
            }
            String superClass = ClassInherits.get(className);
            if(superClass.equals(className)){
                System.out.println("Should never get here");
                return -1;
            }
            className=superClass;
        }
    }

    public static String getDefinedClassField(String className,String id){
        while(true){
            if(SymbolTable.ClassVariables.containsKey(new DefinedVariable(id,className,className))){
                return className;
            }else{
                String superClass = ClassInherits.get(className);
                if(superClass.equals(className)){
                    System.out.println("Should never get here");
                    return null;
                }
                className=superClass;
            }
        }
    }

    private static String getReturnType(List<String> list){ // only called by checkFunctionCall
        return list.get(0); // get the first item of the list that corresponds to the return type of the function
    }
    private static List<String> getArgumentTypes(List<String> list){    // only called by checkFunctionCall
        // get all the elements of the list except the first one. These correspond to the types of the arguments of a function definition
        return list.subList(1,list.size());
    }

    private static int getTypeSize(String type){
        // get the size of a type (used in order to compute the offsets)
        if("int".equals(type))  // int has size of 4
            return 4;
        if("boolean".equals(type))  // boolean has size of 1
            return 1;
        return 8;   // every other type has size of 8
    }

    public static void setMainClassName(String mainName){
        MainClassName=mainName;
    }
}
