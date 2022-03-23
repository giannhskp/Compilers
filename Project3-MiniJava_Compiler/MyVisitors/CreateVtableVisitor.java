package MyVisitors;

import VTable.VTable;
import syntaxtree.*;
import visitor.GJDepthFirst;

import java.io.IOException;
import java.io.Writer;

public class CreateVtableVisitor extends GJDepthFirst<String, Scope>{ // this visitor emits the helper methods and the vtables
    Writer writer;
    public CreateVtableVisitor(Writer wr){
        writer=wr;
    }

    public void emit(String s) throws IOException {
        writer.write(s);
    }

    public String helperMethods(){  // create the helper functions
        String declares="\ndeclare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n";
        String constants="@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n";
        String print_int="define void @print_int(i32 %i) {\n" +
                "    %_str = bitcast [4 x i8]* @_cint to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                "    ret void\n" +
                "}\n";
        String throw_oob="define void @throw_oob() {\n" +
                "    %_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str)\n" +
                "    call void @exit(i32 1)\n" +
                "    ret void\n" +
                "}\n";
        return declares+constants+print_int+throw_oob;
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, Scope argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        emit(helperMethods());
        return null;
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

    public String visit(MainClass n, Scope argu) throws Exception {
        String classname = n.f1.accept(this, null);
        VTable.MethodCount.put(classname,0);
        String emitString = "@."+classname+ "_vtable = global [" + 0 + " x i8*] [] \n"; // emit the vtable of the main class
        emit(emitString);
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
        VTable.MethodCount.put(classname,0);
        VTable.initializeFunctions(classname,null);
        Scope newScope = new Scope(classname,classname);
        if(n.f3.present()) {
            n.f3.accept(this, newScope);
        }
        if(n.f4.present()) {
            n.f4.accept(this, newScope);
        }
        String emitString = "@."+classname+ "_vtable = global [" + VTable.MethodCount.get(classname) + " x i8*]";
        emit(emitString);
        emit(VTable.getVTableEmmit(classname)); // emits the actual prototypes of the functions of the vtable
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
        String classname = n.f1.accept(this, null);
        String superClassName = n.f3.accept(this, null);
        int superClassCount = VTable.MethodCount.get(superClassName);
        VTable.MethodCount.put(classname,superClassCount);
        VTable.initializeFunctions(classname,superClassName);
        Scope newScope = new Scope(classname,classname);
        if(n.f5.present()) {
            n.f5.accept(this, newScope);
        }
        if(n.f6.present()) {
            n.f6.accept(this, newScope);
        }
        String emitString = "@."+classname+ "_vtable = global [" + VTable.MethodCount.get(classname) + " x i8*]\n";
        emit(emitString);
        emit(VTable.getVTableEmmit(classname)); // emits the actual prototypes of the functions of the vtable
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
        String className = argu.getClassName();
        String functionName = n.f2.accept(this, null);
        boolean isOverride = VTable.addFunction(className,functionName);  // add the function to ta vtable
        if(!isOverride){  // if this function does not override another function of a superClass
            int currCount = VTable.MethodCount.get(className);
            VTable.MethodCount.put(className,currCount+1);  // increase the size of the vtable of the class by 1
        }
        return null;
    }


    public String visit(Identifier n, Scope argu) {
        return n.f0.toString();
    }
}
