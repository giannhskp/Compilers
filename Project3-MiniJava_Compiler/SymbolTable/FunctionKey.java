package SymbolTable;

public class FunctionKey {
    // used as a key in the FunctionDeclarations Map
    // a function is recognized by its name and the name of the class that this function is declared in
    String functionName;
    String className;

    public FunctionKey(String fn,String cn){
        this.functionName = fn;
        this.className = cn;
    }

    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        if(o == null)
            return false;
        if(o.getClass() != o.getClass())
            return false;
        FunctionKey defVar = (FunctionKey) o;
        return this.functionName.equals(defVar.functionName) && this.className.equals(defVar.className);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash*31 + this.functionName.hashCode();
        hash = hash*31 + this.className.hashCode();
        return hash;
    }

    @Override
    public String toString(){
        return "<"+functionName+","+className+">";
    }
}
