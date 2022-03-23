package SymbolTable;

public class OffsetSet {
    // used in order to keep every variable/function and its offset
    String name;    // variable/functrion name
    int offset;
    int size;

    OffsetSet(String name,int of,int sz){
        this.name = name;
        this.offset = of;
        this.size = sz;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o){
        if(this == o)
            return true;
        if(o == null)
            return false;
        if(o.getClass() != o.getClass())
            return false;
        OffsetSet newObj = (OffsetSet) o;
        return this.name.equals(newObj.name) && this.offset== newObj.offset && this.size== newObj.size;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash*31 + this.name.hashCode();
        hash = hash*31 + this.offset;
        hash = hash*31 + this.size;
        return hash;
    }

    @Override
    public String toString(){
        return "<"+name+","+offset+","+size+">";
    }
}
