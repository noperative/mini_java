package typechecker.implementation;

import ast.*;
import typechecker.ErrorReport;
import util.ImpTable;
import util.ImpTable.DuplicateException;
import util.Pair;
import visitor.DefaultVisitor;

import java.util.Iterator;
import java.util.Map;

/**
 * This visitor implements Phase 1 of the TypeChecker. It constructs the symboltable.
 *
 * @author norm
 */
public class BuildSymbolTableVisitor extends DefaultVisitor<Pair<ImpTable<Type>, ImpTable<Type>>> {

    private final ImpTable<Type> globals = new ImpTable<Type>();
    private final ImpTable<Type> functions = new ImpTable<Type>();
    private ImpTable<Type> thisFunction = null;


    ////// MiniJava-related constructs
    private final ErrorReport errors;
    private ImpTable<Type> main = new ImpTable<Type>();
    private ImpTable<Type> classes = new ImpTable<Type>();
    private String thisClass = null;
    private ClassType thisSuperClass = null;
    private ImpTable<Type> thisFields = null;
    private ImpTable<Type> thisMethods = null;
    private ImpTable<Type> thisFormals = null;
    private ImpTable<Type> thisLocals = null;

    public BuildSymbolTableVisitor(ErrorReport errors) {
        this.errors = errors;
    }

    /////////////////// Phase 1 ///////////////////////////////////////////////////////
    // In our implementation, Phase 1 builds up a symbol table containing all the
    // global identifiers defined in a Functions program, as well as symbol tables
    // for each of the function declarations encountered.
    //
    // We also check for duplicate identifier definitions in each symbol table

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(Program n) {
        n.mainClass.accept(this); // process main class
        n.classes.accept(this); // process all the "normal" classes.
        return new Pair(main,classes);
    }

    ////// Types
    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(BooleanType n) {
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(IntegerType n) {
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(UnknownType n) {
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(IntArrayType n){ return null; }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(ObjectType n) { return null; }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(FunctionType n) { return null; }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(ClassType n) { return null; }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(MethodType n) { return null; }
    ////////


    ///////// Statements
    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(Assign n) {
        //System.out.println("Found an assign with type: " + n.value);
        n.value.accept(this);
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(Print n) {
        n.exp.accept(this);
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(ArrayAssign n){
        n.index.accept(this);
        n.value.accept(this);
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(Block n){
        n.statements.accept(this);
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(While n) {
        n.tst.accept(this);
        n.body.accept(this);
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(If n){
        n.tst.accept(this);
        n.thn.accept(this);
        n.els.accept(this);
        return null;
    }
    /////////////////

    //////////////// Expressions
    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(And n){
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(ArrayLength n){
        n.array.accept(this);
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(ArrayLookup n){
        n.array.accept(this);
        n.index.accept(this);
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(BooleanLiteral n){ return null; }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(Call n) {
        n.receiver.accept(this);
        n.rands.accept(this);
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(Conditional n) {
        n.e1.accept(this);
        n.e2.accept(this);
        n.e3.accept(this);
        return null;
    }


    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(IdentifierExp n) {
        //lookup(n.name);
        return null;
    }


    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(LessThan n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(Plus n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(Minus n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>>visit(Times n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(Not not) {
        not.e.accept(this);
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(This n){
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(NewArray n){
        return null;
    }

    @Override
    public  Pair<ImpTable<Type>, ImpTable<Type>> visit(NewObject n){
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(IntegerLiteral n) {
        return null;
    }
    ////////////////////


    /////////////////// The rest of the AST

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(FunctionDecl n) {
        FunctionType ft = new FunctionType();
        thisFunction = new ImpTable<Type>();
        ft.locals = thisFunction;
        ft.formals = n.formals;
        ft.returnType = n.returnType;
        n.formals.accept(this);
        n.statements.accept(this);
        n.returnExp.accept(this);
        n.type = ft;
        def(functions, n.name, ft);
        thisFunction = null;
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(MainClass n) {
        // TODO: Does this even do anything?
        // Main class can only have statements so no special scope is created
        // I think we only need to typecheck the statements in phase 2
        ClassType ct = new ClassType();
        n.type = ct;
        def(main, n.className, ct);
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(ClassDecl n) {
        ClassType ct = new ClassType();
        thisClass = n.name;
        thisFields = new ImpTable<Type>();
        thisMethods = new ImpTable<Type>();

        //Scope of the class
        ct.superClass = n.superName;
        ct.name = n.name;

        // Take the scope of the super class as well
        if (ct.superClass != null) {
            ClassType st = (ClassType) classes.lookup(ct.superClass);
            if (st == null) errors.undefinedId(n.superName);
            else {
                thisSuperClass = st;
                ct.superClassType = st;
                Iterator<Map.Entry<String, Type>> fieldsiter = st.fields.iterator();
                Iterator<Map.Entry<String, Type>> methodsiter = st.methods.iterator();

                while (fieldsiter.hasNext()){
                    Map.Entry<String, Type> entry = fieldsiter.next();
                    def(thisFields, entry.getKey(), entry.getValue());
                }
                while (methodsiter.hasNext()){
                    Map.Entry<String, Type> entry = methodsiter.next();
                    def(thisMethods, entry.getKey(), entry.getValue());
                }
            }
        }

        n.vars.accept(this);
        //System.out.println("Calling methods accept from class: " + n.name);
        n.methods.accept(this);

        // Add class scope to ImplTable
        ct.fields = thisFields;
        ct.methods = thisMethods;
        n.type = ct;
        def(classes, n.name, ct);

        thisClass = null;
        thisFields = null;
        thisMethods = null;
        return null;
    }

    @Override
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(MethodDecl n) {
        //System.out.println("methods declaration: " +n.name);
        MethodType mt = new MethodType();
        thisFormals = new ImpTable<Type>();
        thisLocals = new ImpTable<Type>();


        mt.returnType = n.returnType;
        mt.formals = n.formals;

        n.formals.accept(this);
        n.vars.accept(this);
        n.statements.accept(this);
        n.returnExp.accept(this);

        // Add method scope to implTable
        mt.params = thisFormals;
        mt.locals = thisLocals;
        n.type = mt;

        //Check if super method exists, otherwise overwrite
        Type methodSuperExists = null;
        if (thisSuperClass != null) {
            methodSuperExists = thisSuperClass.methods.lookup(n.name);
        }
        if (methodSuperExists != null){
            override(thisMethods, n.name, mt);
        } else {
            def(thisMethods, n.name, mt);
        }

        thisFormals = null;
        thisLocals = null;
        return null;
    }


    @Override
    // This is a formal parameter to the current function
    public Pair<ImpTable<Type>, ImpTable<Type>> visit(VarDecl n) {
        switch(n.kind){
            case FIELD:
                def(thisFields, n.name, n.type);
                break;
            case LOCAL:
                def(thisLocals, n.name, n.type);
                break;
            case FORMAL:
                def(thisFormals, n.name, n.type);
                break;
            default:
                    errors.undefinedId(n.name);
        }
        return null;
    }

    @Override
    public <T extends AST> Pair<ImpTable<Type>, ImpTable<Type>> visit(NodeList<T> ns) {
        for (int i = 0; i < ns.size(); i++)
            ns.elementAt(i).accept(this);
        return null;
    }

    ///////////////////// Helpers ///////////////////////////////////////////////
    // Lookup a name in the two symbol tables that it might be in
    private Type lookup(String name) {
        Type t;
        if (thisFunction != null) {
            t = thisFunction.lookup(name);
            if (t != null)
                return t;
        }
        t = globals.lookup(name);
        if (t == null)
            errors.undefinedId(name);
        return t;
    }


    /**
     * Add an entry to a table, and check whether the name already existed.
     * If the name already existed before, the new definition is ignored and
     * an error is sent to the error report.
     */
    private <V> void def(ImpTable<V> tab, String name, V value) {
        try {
            tab.put(name, value);
        } catch (DuplicateException e) {
            errors.duplicateDefinition(name);
        }
    }
    /**
     * Add an entry to a table
     */
    private <V> void override(ImpTable<V> tab, String name, V value) {
            tab.set(name, value);
    }
}
