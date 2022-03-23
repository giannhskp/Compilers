package MyVisitors;

import syntaxtree.*;
import visitor.*;
import SymbolTable.SymbolTable;
// every visit function gets an argument of the current scope
// scope = current class & current function
// if the scope is not inside a function, Scope variable contains the class name in both fields (class and function)
// Scope is defined in Scope.java
public class Visitor1 extends GJDepthFirst<String, Scope>{
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

    public String visit(MainClass n, Scope argu) throws Exception {
        String classname = n.f1.accept(this, null);
        SymbolTable.classDec(classname,classname);
        SymbolTable.setMainClassName(classname);
        if(n.f14.present()) {
            Scope newScope = new Scope(classname,"main");
            n.f14.accept(this, newScope);
        }

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

    public String visit(ClassDeclaration n, Scope argu) throws Exception {
        String classname = n.f1.accept(this, null);
        SymbolTable.classDec(classname,classname);
        Scope newScope = new Scope(classname,classname);
        if(n.f3.present()) {
            n.f3.accept(this, newScope);
        }
        if(n.f4.present()) {
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

    public String visit(ClassExtendsDeclaration n, Scope argu) throws Exception {
        String className = n.f1.accept(this, null);
        String superClassMame = n.f3.accept(this, null);
        SymbolTable.classDec(className,superClassMame);
        Scope newScope = new Scope(className,className);
        if(n.f5.present()) {
            n.f5.accept(this, newScope);
        }
        if(n.f6.present()) {
            n.f6.accept(this, newScope);
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

    public String visit(MethodDeclaration n, Scope argu) throws Exception {

        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);
        Scope newScope = new Scope(argu.getClassName(),myName);
        String argumentList = n.f4.present() ? n.f4.accept(this, newScope) : "";
        SymbolTable.funDec(myName,argu.getClassName(),myType,argumentList);
        if(n.f7.present()) {
            n.f7.accept(this, newScope);
        }

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */

    public String visit(VarDeclaration n, Scope argu) throws Exception {

        String myType = n.f0.accept(this, null);
        String myName = n.f1.accept(this, null);
        SymbolTable.varDec(myName, argu.getClassName(), argu.getFunctionName(),myType);
        n.f2.accept(this, null);

        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */

    public String visit(FormalParameterList n, Scope argu) throws Exception {
        String ret = n.f0.accept(this, argu);

        if (n.f1 != null) {
            ret += n.f1.accept(this, argu);
        }

        return ret;
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public String visit(FormalParameterTail n, Scope argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += "," + node.accept(this, argu);
        }

        return ret;
    }
    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public String visit(FormalParameterTerm n, Scope argu) throws Exception {
        return n.f1.accept(this, argu);
    }


    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */

    public String visit(FormalParameter n, Scope argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        SymbolTable.varDec(name, argu.getClassName(), argu.getFunctionName(),type);
        return type;
    }


    public String visit(ArrayType n, Scope argu) {
        return "int[]";
    }

    public String visit(BooleanType n, Scope argu) {
        return "boolean";
    }

    public String visit(IntegerType n, Scope argu) {
        return "int";
    }


    public String visit(Identifier n, Scope argu) {
        return n.f0.toString();
    }
}
