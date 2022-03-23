package VTable;

import SymbolTable.FunctionKey;
import SymbolTable.SymbolTable;

import java.util.*;

public class VTable {
    public static Map<String, Map<String,String>> ClassFunctions = new LinkedHashMap<>();
    public static Map<String,Integer> MethodCount = new HashMap<>();

    public static boolean addFunction(String className,String functionName){  // add a function to the ClassFunctions map
        boolean override=false;
        if(ClassFunctions.get(className).containsKey(functionName)){  // if the class already contains a class with the same name (from it's superClass)
            override=true;
        }
        // for the given class add to the map the function and the class tha is defined to
        // if the class already exist from the superClass, the classname is overwritten
        ClassFunctions.get(className).put(functionName,className);
        return override;
    }

    public static void initializeFunctions(String className,String superClassName){
        if(superClassName==null || superClassName.equals(SymbolTable.MainClassName)){ // if the class does not extend another class
            ClassFunctions.put(className,new LinkedHashMap<>());  // then initialize it with an empty map
        }else{  // if the class extends another class
            ClassFunctions.put(className,new LinkedHashMap<>(ClassFunctions.get(superClassName))); // add the functions of it's superClass as it inherts them
        }
    }
    public static void printFunctions(){  // only for debuggind
        for(String className: ClassFunctions.keySet()){
            System.out.println("Class: "+className+" - Functions: "+ClassFunctions.get(className));
        }
    }

    public static String getFunReturnType(String funName,String funClassName){  // get the return type of a function
        String definedClassName = ClassFunctions.get(funClassName).get(funName);  // get the name of the class that the function is defined to
        FunctionKey fun = new FunctionKey(funName,definedClassName);
        List<String> functionTypes = SymbolTable.FunctionDeclarations.get(fun); // get the return type and argument types of the function definition
        return functionTypes.get(0);    // get the return type
    }

    public static String getVTableEmmit(String className){
        if(ClassFunctions.get(className).isEmpty()){  // if the class has no functions
            return " []";
        }
        String returnString = " [\n";
        Map<String,String> funcMap = ClassFunctions.get(className); // get the functions of the class
        int count=0;
        for(String funName: funcMap.keySet()){  // for every function of the class
            if(count>0){
                returnString += ",\n";
            }
            count++;
            String funClassName = funcMap.get(funName); // get the name of the class that the function is defined to
            FunctionKey fun = new FunctionKey(funName,funClassName);
            List<String> functionTypes = SymbolTable.FunctionDeclarations.get(fun); // get the return type and argument types of the function definition
            String funReturnType = functionTypes.get(0);    // get the return type
            List<String> funArgumentTypes = functionTypes.subList(1,functionTypes.size());    // get the argument types
            String convertedReturn = typeConvert(funReturnType);  // convert the return type to LLVM type
            String convertedArgs = "i8*"; // first argument is always a pointer to the object
            for(String arg: funArgumentTypes){  // for every argument
                convertedArgs += ", " + typeConvert(arg); // convert the argument type to LLVM type
            }
            returnString += "   i8* bitcast ("+convertedReturn+" ("+convertedArgs+")* @"+funClassName+"."+funName+" to i8*)"; // create the function declaration
        }

        return returnString+"\n]\n";  // return all the function declaration
    }

    public static String typeConvert(String type){  // convert a miniJava type to LLVM type
        switch (type){
            case "int":
                return "i32";
            case "int[]":
                return "i32*";
            case "boolean":
                return "i1";
            case "i32":
            case "i32*":
            case "i1":
                return type;
            default:
                return "i8*";
        }
    }
    public static String getInitialValues(String type){ // get the initial values depending on the type, in order to initialize variables/objects
        switch (type){
            case "i32":
                return "0";
            case "i1":
                return "false";
            default:
                return "null";
        }
    }

    public static int getFunctionOffset(String className,String functionName){
        Map<String,String> funcMap = ClassFunctions.get(className); // get the functions of the class
        String DefinedClassName = funcMap.get(functionName);   // get the name of the class that the function is defined to
        int off = SymbolTable.getOffset(DefinedClassName,functionName); // get the offset of the class
        return off/8; // devide it by 8 to get the index of the function in the vtable
    }

    public static void deleteData(){
        ClassFunctions.clear();
        MethodCount.clear();
    }

}
