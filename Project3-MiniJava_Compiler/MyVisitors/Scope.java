package MyVisitors;

public class Scope {
    // used when traversing the parse tree with the visitors
    // every time the current scope is stored in a Scope object
    // a scope is defined by the class name and the function name
    // if a cuurent scope is not inside a function, functionName also contains the class name
    String className;
    String functionName;

    Scope(String cn,String fn){
        this.className = cn;
        this.functionName = fn;
    }

    public String getClassName(){
        return this.className;
    }

    public String getFunctionName(){
        return this.functionName;
    }

    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        if(o == null)
            return false;
        if(o.getClass() != o.getClass())
            return false;
        Scope defVar = (Scope) o;
        return this.functionName.equals(defVar.functionName) && this.className.equals(defVar.className);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash*31 + this.className.hashCode();
        hash = hash*31 + this.functionName.hashCode();
        return hash;
    }

    @Override
    public String toString(){
        return "<"+className+","+functionName+">";
    }
}
