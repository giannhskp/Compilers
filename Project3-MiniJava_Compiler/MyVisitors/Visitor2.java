package MyVisitors;

import syntaxtree.*;
import visitor.*;
import SymbolTable.SymbolTable;
import SymbolTable.DefinedVariable;
// every visit function gets an argument of the current scope
// scope = current class & current function
// if the scope is not inside a function, Scope variable contains the class name in both fields (class and function)
// Scope is defined in Scope.java
public class Visitor2 extends GJDepthFirst<String, Scope>{
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
        n.f11.accept(this, null);
        if(n.f14.present()) {
            n.f14.accept(this, argu);
        }
        if(n.f15.present()) {
            Scope newScope = new Scope(classname,"main");
            n.f15.accept(this, newScope);
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
        if(n.f3.present()) {
            n.f3.accept(this, argu);
        }
        if(n.f4.present()) {
            Scope newScope = new Scope(classname,classname);
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
        if(n.f5.present()) {
            n.f5.accept(this, argu);
        }
        if(n.f6.present()) {
            Scope newScope = new Scope(className,className);
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

        if(n.f4.present()){
            n.f4.accept(this, null);
        }
        if(n.f7.present()) {
            n.f7.accept(this, argu);
        }
        Scope newScope = new Scope(argu.getClassName(),myName);
        if(n.f8.present()) {
            n.f8.accept(this, newScope);
        }
        String returnExpr = n.f10.accept(this,newScope);
        SymbolTable.checkExpressionType(returnExpr,myType,argu.getClassName(),myName);

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */

    public String visit(VarDeclaration n, Scope argu) throws Exception {
        String myType = n.f0.accept(this, null);
        SymbolTable.checkDeclarationType(myType);
        n.f1.accept(this, null);
        n.f2.accept(this, argu);

        return null;
    }


    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, Scope argu) throws Exception {
        String ident = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String rightExpr = n.f2.accept(this, argu);
        n.f3.accept(this, argu);

        SymbolTable.checkAssignmentTypes(ident,rightExpr,argu.getClassName(),argu.getFunctionName());
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
    public String visit(ArrayAssignmentStatement n, Scope argu) throws Exception {
        String className = argu.getClassName();
        String functionName = argu.getFunctionName();
        String arrayName = n.f0.accept(this, argu);
        SymbolTable.checkExpressionType(arrayName,"int[]",className,functionName);
        n.f1.accept(this, argu);
        String index = n.f2.accept(this, argu);
        SymbolTable.checkExpressionType(index,"int",className,functionName);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        String assignExpr = n.f5.accept(this, argu);
        SymbolTable.checkExpressionType(assignExpr,"int",className,functionName);
        n.f6.accept(this, argu);
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
    public String visit(IfStatement n, Scope argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String condition = n.f2.accept(this, argu);
        SymbolTable.checkExpressionType(condition,"boolean",argu.getClassName(),argu.functionName);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return null;
    }


    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, Scope argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String condition = n.f2.accept(this, argu);
        SymbolTable.checkExpressionType(condition,"boolean",argu.getClassName(),argu.functionName);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return null;
    }


    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, Scope argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String printVar = n.f2.accept(this, argu);
        SymbolTable.checkExpressionType(printVar,"int",argu.getClassName(),argu.getFunctionName());
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return null;
    }


    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, Scope argu) throws Exception {
        String className = argu.getClassName();
        String functionName = argu.getFunctionName();
        String clause1 = n.f0.accept(this, argu);
        SymbolTable.checkExpressionType(clause1,"boolean",className,functionName);
        n.f1.accept(this, argu);
        String clause2 =  n.f2.accept(this, argu);
        SymbolTable.checkExpressionType(clause2,"boolean",className,functionName);
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, Scope argu) throws Exception {
        String className = argu.getClassName();
        String functionName = argu.getFunctionName();
        String exp1 = n.f0.accept(this, argu);
        SymbolTable.checkExpressionType(exp1,"int",className,functionName);
        n.f1.accept(this, argu);
        String exp2 = n.f2.accept(this, argu);
        SymbolTable.checkExpressionType(exp2,"int",className,functionName);
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, Scope argu) throws Exception {
        String className = argu.getClassName();
        String functionName = argu.getFunctionName();
        String exp1 = n.f0.accept(this, argu);
        SymbolTable.checkExpressionType(exp1,"int",className,functionName);
        n.f1.accept(this, argu);
        String exp2 = n.f2.accept(this, argu);
        SymbolTable.checkExpressionType(exp2,"int",className,functionName);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, Scope argu) throws Exception {
        String className = argu.getClassName();
        String functionName = argu.getFunctionName();
        String exp1 = n.f0.accept(this, argu);
        SymbolTable.checkExpressionType(exp1,"int",className,functionName);
        n.f1.accept(this, argu);
        String exp2 = n.f2.accept(this, argu);
        SymbolTable.checkExpressionType(exp2,"int",className,functionName);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, Scope argu) throws Exception {
        String className = argu.getClassName();
        String functionName = argu.getFunctionName();
        String exp1 = n.f0.accept(this, argu);
        SymbolTable.checkExpressionType(exp1,"int",className,functionName);
        n.f1.accept(this, argu);
        String exp2 = n.f2.accept(this, argu);
        SymbolTable.checkExpressionType(exp2,"int",className,functionName);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, Scope argu) throws Exception {
        String className = argu.getClassName();
        String functionName = argu.getFunctionName();
        String arrayName = n.f0.accept(this, argu);
        SymbolTable.checkExpressionType(arrayName,"int[]",className,functionName);
        n.f1.accept(this, argu);
        String index = n.f2.accept(this, argu);
        SymbolTable.checkExpressionType(index,"int",className,functionName);
        n.f3.accept(this, argu);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, Scope argu) throws Exception {
        String className = argu.getClassName();
        String functionName = argu.getFunctionName();
        String arrayName = n.f0.accept(this, argu);
        SymbolTable.checkExpressionType(arrayName,"int[]",className,functionName);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return "int";
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, Scope argu) throws Exception {
        String caller = n.f0.accept(this, argu);
        String callerType = SymbolTable.getFunCallerType(caller,argu.getClassName(),argu.getFunctionName());
        n.f1.accept(this, argu);
        String functionName = n.f2.accept(this, null);
        n.f3.accept(this, argu);
        String argumentTypes = "";
        if(n.f4.present()){
            argumentTypes = n.f4.accept(this, argu);
        }
        String returnType = SymbolTable.checkFunctionCall(callerType,functionName,argumentTypes);
        n.f5.accept(this, argu);
        return returnType;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, Scope argu) throws Exception {
        String parameter = n.f0.accept(this, argu);
        String ret = SymbolTable.getArgumentType(parameter,argu.getClassName(),argu.getFunctionName());
        if (n.f1 != null) {
            ret += n.f1.accept(this, argu);
        }
        return ret;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, Scope argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += "," + node.accept(this, argu);
        }

        return ret;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, Scope argu) throws Exception {
        String parameter = n.f1.accept(this, argu);
        return SymbolTable.getArgumentType(parameter,argu.getClassName(),argu.getFunctionName());
    }




    /**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    public String visit(Clause n, Scope argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, Scope argu) throws Exception {
        return "this";
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(ArrayAllocationExpression n, Scope argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        String expression = n.f3.accept(this, argu);
        SymbolTable.checkExpressionType(expression,"int",argu.getClassName(),argu.getFunctionName());
        n.f4.accept(this, argu);
        return "int[]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, Scope argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        String className = n.f1.accept(this, null);
        SymbolTable.checkClassAllocation(className);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        return className;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, Scope argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        String expr = n.f1.accept(this, argu);
        SymbolTable.checkExpressionType(expr,"boolean",argu.getClassName(),argu.getFunctionName());
        return "boolean";
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, Scope argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        _ret = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
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


    public String visit(Identifier n, Scope argu) throws Exception {
        if(argu==null)  // if the identifier is a className
            return n.f0.toString(); //return the name of the class
        DefinedVariable variable = SymbolTable.getClosestVar(n.f0.toString(),argu.getClassName(),argu.getFunctionName());  // if a variable/identifier was given
        // find the definition of the variable
        if(variable != null){   // if the variable is defined
            return SymbolTable.ClassVariables.get(variable);    // return   the variable's type
        }
        // if the variable is not defined
        throw new Exception("Reference to undefined Identifier: " + n.f0.toString());
    }

    public String visit(TrueLiteral n, Scope argu) {
        return "true";
    }

    public String visit(FalseLiteral n, Scope argu) {
        return "false";
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, Scope argu) throws Exception {
        return n.f0.toString();
    }

}
