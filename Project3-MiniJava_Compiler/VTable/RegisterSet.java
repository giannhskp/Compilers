package VTable;

public class RegisterSet {
    String id;
    String register;

    public RegisterSet(String i,String r){
        this.id = i;
        this.register = r;
    }

    public String getId() {
        return id;
    }

    public String getRegister() {
        return register;
    }

    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        if(o == null)
            return false;
        if(o.getClass() != o.getClass())
            return false;
        RegisterSet defVar = (RegisterSet) o;
        return this.id.equals(defVar.id) && this.register.equals(defVar.register);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash*31 + this.id.hashCode();
        hash = hash*31 + this.register.hashCode();
        return hash;
    }

    @Override
    public String toString(){
        return "<"+id+","+register+">";
    }
}
