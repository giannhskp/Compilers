package SymbolTable;

public class DefinedVariable {
    // DefinedVariable is used as a key in ClassVariables Map
    // used in order to uniquely identify a defined variable
    // a variable is uniquely identified by its name, the class name that is declared in and the function name that is declared in
    // if a variable is a class field (is not defined in a function) the the functionName also contains the className
    String idName;
    String className;
    String functionName;

    public DefinedVariable(String n,String c, String f){
        this.idName = n;
        this.className = c;
        this.functionName = f;
    }

    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        if(o == null)
            return false;
        if(o.getClass() != o.getClass())
            return false;
        DefinedVariable defVar = (DefinedVariable) o;
        return this.idName.equals(defVar.idName) && this.className.equals(defVar.className) && this.functionName.equals(defVar.functionName);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash*31 + this.idName.hashCode();
        hash = hash*31 + this.className.hashCode();
        hash = hash*31 + this.functionName.hashCode();
        return hash;
    }

    @Override
    public String toString(){
        return "<"+idName+","+className+","+functionName+">";
    }
}
