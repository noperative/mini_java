package typechecker.implementation;

import ast.*;
import typechecker.ErrorReport;
import util.ImpTable;
import util.Pair;
import visitor.Visitor;

import java.util.ArrayList;
import java.util.List;


/**
 * This class implements Phase 2 of the Type Checker. This phase
 * assumes that we have already constructed the program's symbol table in
 * Phase1.
 * <p>
 * Phase 2 checks for the use of undefined identifiers and type errors.
 * <p>
 * Visitors may return a Type as a result. Generally, only visiting
 * an expression or a type actually returns a type.
 * <p>
 * Visiting other nodes just returns null.
 *
 * @author kdvolder
 */
public class TypeCheckVisitor implements Visitor<Type> {

    /**
     * The place to send error messages to.
     */
    private ErrorReport errors;

    /**
     * The symbol table from Phase 1.
     */
    private ImpTable<Type> globals;
    private ImpTable<Type> functions;
    private ImpTable<Type> thisFunction;

    // The MiniJava symbol table
    private ImpTable<Type> main;
    private ImpTable<Type> classes;
    private String thisClass = null;
    private String thisSuperClass = null;
    private ImpTable<Type> thisFields = null;
    private ImpTable<Type> thisMethods = null;
    private ImpTable<Type> thisSuperFields = null;
    private ImpTable<Type> thisSuperMethods = null;
    private ImpTable<Type> thisFormals = null;
    private ImpTable<Type> thisLocals = null;

    private Type lookup(String name) {
        Type t = null;
        if (thisLocals != null) {
            t = thisLocals.lookup(name);
            if (t != null) {
                return t;
            }
        }

        if (thisFormals != null) {
            t = thisFormals.lookup(name);
            if (t != null) {
                return t;
            }
        }

        if (thisFields != null) {
            t = thisFields.lookup(name);
            if (t != null) {
                return t;
            }
        }

        if (thisSuperClass != null) {
            Type superType = classes.lookup(thisSuperClass);
            while (superType != null) {
                ClassType superClassType = (ClassType) superType;
                t = superClassType.fields.lookup(name);
                if (t != null) {
                    return t;
                }

                String nextSuper = superClassType.superClass;
                if (nextSuper != null) {
                    superType = classes.lookup(nextSuper);
                } else {
                    break;
                }
            }
        }

        errors.undefinedId(name);
        return t;
    }

    public TypeCheckVisitor(Pair<ImpTable<Type>, ImpTable<Type>> variables, ErrorReport errors) {
        this.main = variables.first;
        this.classes = variables.second;
        this.errors = errors;
    }

    //// Helpers /////////////////////

    /**
     * Check whether the type of a particular expression is as expected.
     */
    private void check(Expression exp, Type expected) {
        Type actual = exp.accept(this);

        if (expected instanceof ObjectType && actual instanceof ObjectType) {
            Type classType = classes.lookup(((ObjectType) actual).name);
            while (classType != null && classType instanceof ClassType) {
                if (((ClassType) classType).name.equals(((ObjectType) expected).name)) {
                    return;
                }
                classType = classes.lookup(((ClassType) classType).superClass);
            }
        }


        if (!assignableFrom(expected, actual)) {
            errors.typeError(exp, expected, actual);
        }
    }

    /**
     * Check whether two types in an expression are the same
     */
    private void check(Expression exp, Type t1, Type t2) {
        //System.out.println("t1: " + t1.getClass() + " and t2: " + t2.getClass());

        if (t1 instanceof ObjectType && t2 instanceof ObjectType) {
            Type classType = classes.lookup(((ObjectType) t2).name);
            while (classType != null && classType instanceof ClassType) {
                if (((ClassType) classType).name.equals(((ObjectType) t1).name)) {
                    return;
                }
                classType = classes.lookup(((ClassType) classType).superClass);
            }
        }
        if (!t1.equals(t2))
            errors.typeError(exp, t1, t2);
    }

    private boolean assignableFrom(Type varType, Type valueType) {
        return varType.equals(valueType);
    }

    ///////// Visitor implementation //////////////////////////////////////

    @Override
    public <T extends AST> Type visit(NodeList<T> ns) {
        for (int i = 0; i < ns.size(); i++) {
            ns.elementAt(i).accept(this);
        }
        return null;
    }

    @Override
    public Type visit(Program n) {
        n.mainClass.accept(this);
        n.classes.accept(this);
        return null;
    }

    @Override
    public Type visit(BooleanType n) {
        return n;
    }

    @Override
    public Type visit(IntegerType n) {
        return n;
    }

    @Override
    public Type visit(UnknownType n) {
        return n;
    }

    /**
     * Can't use check, because print allows either Integer or Boolean types
     */
    @Override
    public Type visit(Print n) {
        Type actual = n.exp.accept(this);
        if (!assignableFrom(new IntegerType(), actual) && !assignableFrom(new BooleanType(), actual)) {
            List<Type> l = new ArrayList<Type>();
            l.add(new IntegerType());
            l.add(new BooleanType());
            errors.typeError(n.exp, l, actual);
        }
        return null;
    }

    @Override
    public Type visit(Assign n) {
        //System.out.println("Assigning: " + n.name + " to: " + n.value.toString());
        Type toAssign = lookup(n.name);
        if (toAssign != null) {
            Type exp = n.value.accept(this);
            check(n.value, toAssign, exp);
        }
        //System.out.println("Done");
        return null;
    }

    @Override
    public Type visit(LessThan n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new BooleanType());
        return n.getType();
    }

    @Override
    public Type visit(Conditional n) {
        check(n.e1, new BooleanType());
        Type t2 = n.e2.accept(this);
        Type t3 = n.e3.accept(this);
        check(n, t2, t3);
        return t2;
    }

    @Override
    public Type visit(ClassType n) {
        return null;
    }

    @Override
    public Type visit(MethodType n) {
        return null;
    }

    @Override
    public Type visit(Plus n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(Minus n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(Times n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(IntegerLiteral n) {
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(IdentifierExp n) {
        Type identifierType = lookup(n.name);
        if (identifierType == null) {
            return new UnknownType();
        }

        return identifierType;
    }

    @Override
    public Type visit(Not n) {
        check(n.e, new BooleanType());
        n.setType(new BooleanType());
        return n.getType();
    }

    @Override
    public Type visit(FunctionDecl n) {
        thisFunction = n.type.locals;
        n.statements.accept(this);
        check(n.returnExp, n.returnType);
        thisFunction = null;
        return null;
    }

    @Override
    public Type visit(VarDecl n) {
        return null;
    }

    @Override
    public Type visit(Call n) {
        Type receiver = n.receiver.accept(this);
        //if (n.name.equals("ainit")) {
            //System.out.println(((ClassType) classes.lookup("B")).methods);
            //System.out.println(n.receiver);
        //}

        if (receiver instanceof ObjectType) {
            Type classType = classes.lookup(((ObjectType) receiver).name);

            if (classType != null && classType instanceof ClassType) {
                Type methodType = ((ClassType) classType).methods.lookup(n.name);

                if (methodType != null && methodType instanceof MethodType) {
                    MethodType mt = (MethodType) methodType;
                    n.setType(mt.returnType);

                    if (n.rands.size() != mt.formals.size()) {
                        errors.wrongNumberOfArguments(mt.formals.size(), n.rands.size());
                    }
                    for (int i = 0; i < n.rands.size(); i++) {
                        if (i < mt.formals.size()) {
                            Expression actual = n.rands.elementAt(i);
                            Type formal = mt.formals.elementAt(i).type;
                            check(actual, formal);
                        }
                    }
                    return n.getType();
                } else {
                    errors.undefinedId(n.name);
                }
            } else {
                errors.undefinedId(((ObjectType) receiver).name);
            }
        }

        n.setType(new UnknownType());
        return n.getType();
    }

    @Override
    public Type visit(FunctionType n) {
        return n;
    }

    @Override
    public Type visit(MainClass n) {
        // TODO: I think there's more to it than this
        thisClass = n.className;
        n.statement.accept(this);
        thisClass = null;
        return null;
    }

    @Override
    public Type visit(ClassDecl n) {
        thisClass = n.name;
        thisFields = n.type.fields;
        thisMethods = n.type.methods;

        if (n.superName != null) {
            ClassType superClass = (ClassType) classes.lookup(n.type.superClass);
            if (superClass == null) {
                errors.undefinedId(n.superName);
            } else {
                thisSuperClass = n.superName;
                thisSuperFields = superClass.fields;
                thisSuperMethods = superClass.methods;
            }
        }

        n.methods.accept(this);

        thisClass = null;
        thisFields = null;
        thisMethods = null;
        thisSuperFields = null;
        thisSuperMethods = null;
        return null;
    }

    @Override
    public Type visit(MethodDecl n) {
        thisFormals = n.type.params;
        thisLocals = n.type.locals;

        n.statements.accept(this);
        check(n.returnExp, n.returnType);

        thisFormals = null;
        thisLocals = null;

        return null;
    }

    @Override
    public Type visit(IntArrayType n) {
        return n;
    }

    @Override
    public Type visit(ObjectType n) {
        return n;
    }

    @Override
    public Type visit(Block n) {
        n.statements.accept(this);
        return null;
    }

    @Override
    public Type visit(If n) {
        check(n.tst, new BooleanType());
        n.thn.accept(this);
        n.els.accept(this);
        return null;
    }

    @Override
    public Type visit(While n) {
        check(n.tst, new BooleanType());
        n.body.accept(this);
        return null;
    }

    @Override
    public Type visit(ArrayAssign n) {
        Type arrayType = lookup(n.name);
        if (arrayType != null) {
            check(n.index, new IntegerType());
            check(n.value, new IntegerType());
        }
        return null;
    }

    @Override
    public Type visit(And n) {
        check(n.e1, new BooleanType());
        check(n.e2, new BooleanType());
        n.setType(new BooleanType());
        return n.getType();
    }

    @Override
    public Type visit(ArrayLookup n) {
        check(n.array, new IntArrayType());
        check(n.index, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(ArrayLength n) {
        check(n.array, new IntArrayType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(BooleanLiteral n) {
        n.setType(new BooleanType());
        return n.getType();
    }

    @Override
    public Type visit(This n) {
        if (thisClass == null) {
            errors.undefinedId("this");
            return new UnknownType();
        }

        n.setType(new ObjectType(thisClass));
        return n.getType();
    }

    @Override
    public Type visit(NewArray n) {
        check(n.size, new IntegerType());
        n.setType(new IntArrayType());
        return n.getType();
    }

    @Override
    public Type visit(NewObject n) {
        n.setType(new ObjectType(n.typeName));
        return n.getType();
    }
}
