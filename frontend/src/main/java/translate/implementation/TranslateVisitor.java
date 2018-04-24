package translate.implementation;

import ast.*;
import ir.frame.Access;
import ir.frame.Frame;
import ir.temp.Label;
import ir.temp.Temp;
import ir.tree.BINOP.Op;
import ir.tree.*;
import ir.tree.CJUMP.RelOp;
import translate.DataFragment;
import translate.Fragments;
import translate.ProcFragment;
import translate.implementation.Cx;
import translate.implementation.Ex;
import translate.implementation.Nx;
import translate.implementation.TRExp;
import util.*;
import visitor.Visitor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static ir.tree.IR.*;
import static translate.TranslatorLabels.*;


/**
 * This visitor builds up a collection of IRTree code fragments for the body
 * of methods in a Functions program.
 * <p>
 * Methods that visit statements and expression return a TRExp, other methods
 * just return null, but they may add Fragments to the collection by means
 * of a side effect.
 *
 * @author kdvolder
 */
public class TranslateVisitor implements Visitor<TRExp> {
    /**
     * We build up a list of Fragment (pieces of stuff to be converted into
     * assembly) here.
     */
    private Fragments frags;

    /**
     * We use this factory to create Frame's, without making our code dependent
     * on the target architecture.
     */
    private Frame frameFactory;

    /**
     * The symbol table may be needed to find information about classes being
     * instantiated, or methods being called etc.
     */
    private Frame frame;

    private Lookup<Type> classTable;

    private FunTable<IRExp> currentEnv;

    private Map<String, Map<String, Integer>> offsetTable;

    private final static String THIS = "this";

    private ImpTable<ClassDecl> classDeclLookup = new ImpTable<ClassDecl>();

    //Error Checking
    private boolean checkErrors = true;
    private String thisClass = null;
    private ClassType thisClassType = null;
    private MethodType thisMethodType = null;

    public TranslateVisitor(Pair<Lookup<Type>, Lookup<Type>> table, Frame frameFactory) {
        this.frags = new Fragments(frameFactory);
        this.frameFactory = frameFactory;
        this.classTable = table.second;
        this.offsetTable = new HashMap<>();
    }

    /////// Helpers //////////////////////////////////////////////

    private boolean atGlobalScope() {
        return frame.getLabel().equals(L_MAIN);
    }

    /**
     * Create a frame with a given number of formals. We assume that
     * no formals escape, which is the case in MiniJava.
     */
    private Frame newFrame(Label name, int nFormals) {
        return frameFactory.newFrame(name, nFormals);
    }

    /**
     * Creates a label for a function (used by calls to that method).
     * The label name is simply the function name.
     */
    private Label functionLabel(String functionName) {
        return Label.get("_" + functionName);
    }

    private String getClassName(Call n) {
        if (n.receiver instanceof NewObject) {
            return ((NewObject) n.receiver).typeName;
        }

        if (n.receiver instanceof Call) {
            return getClassName((Call) n.receiver);
        }

        if (n.receiver instanceof This) {
            return thisClass;
        }

        if (n.receiver instanceof IdentifierExp) {
            String receiverName = ((IdentifierExp) n.receiver).name;

            Type ct = thisMethodType.locals.lookup(((IdentifierExp) n.receiver).name);
            if (ct == null){
                ct = thisMethodType.params.lookup(((IdentifierExp) n.receiver).name);
            }
            if (ct == null){
                ct = thisClassType.fields.lookup(((IdentifierExp) n.receiver).name);
            }
            //System.out.println(((IdentifierExp) n.receiver).name + " is an instance of: " + ct.toString());
            return ct.toString();
        }

        // Should not get here if our type checking works properly
        return null;
    }



    private void putEnv(String name, Access access) {
        currentEnv = currentEnv.insert(name, access.exp(frame.FP()));
    }

    private void putEnv(String name, IRExp exp) {
        currentEnv = currentEnv.insert(name, exp);
    }

    ////// Visitor ///////////////////////////////////////////////

    @Override
    public <T extends AST> TRExp visit(NodeList<T> ns) {
        IRStm result = IR.NOP;
        for (int i = 0; i < ns.size(); i++) {
            AST nextStm = ns.elementAt(i);
            TRExp e = nextStm.accept(this);
            // e will be null if the statement was in fact a function declaration
            // just ignore these as they generate Fragments
            if (e != null)
                result = IR.SEQ(result, e.unNx());
        }
        return new Nx(result);
    }

    @Override
    public TRExp visit(Program n) {
//        frame = newFrame(L_MAIN, 0);
        currentEnv = FunTable.theEmpty();
//        TRExp statements = n.statements.accept(this);
//        TRExp print = n.print.accept(this);
//        IRStm body = IR.SEQ(
//                statements.unNx(),
//                print.unNx());
//        body = frame.procEntryExit1(body);
//        frags.add(new ProcFragment(frame, body));

        n.mainClass.accept(this);
        n.classes.accept(this);

        return null;
    }

    @Override
    public TRExp visit(BooleanType n) {
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(IntegerType n) {
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(UnknownType n) {
        throw new Error("Not implemented");
    }

    private TRExp visitStatements(NodeList<Statement> statements) {
        IRStm result = IR.NOP;
        for (int i = 0; i < statements.size(); i++) {
            Statement nextStm = statements.elementAt(i);
            result = IR.SEQ(result, nextStm.accept(this).unNx());
        }
        return new Nx(result);
    }

    @Override
    public TRExp visit(Conditional n) {
        Temp temp = new Temp();
        Label t = Label.gen();
        Label f = Label.gen();
        Label join = Label.gen();
        IRStm tst = n.e1.accept(this).unCx(t, f);
        IRStm thn = IR.SEQ(
                IR.LABEL(t),
                new Nx(IR.MOVE(temp, n.e2.accept(this).unEx())).unNx(),
                IR.JUMP(join));
        IRStm els = IR.SEQ(
                IR.LABEL(f),
                new Nx(IR.MOVE(temp, n.e3.accept(this).unEx())).unNx());

        return new Ex(IR.ESEQ(IR.SEQ(tst, thn, els, IR.LABEL(join)), IR.TEMP(temp)));
    }

    @Override
    public TRExp visit(ClassType n) {
        return null;
    }

    @Override
    public TRExp visit(MethodType n) {
        return null;
    }

    @Override
    public TRExp visit(Print n) {
        TRExp arg = n.exp.accept(this);
        return new Ex(IR.CALL(L_PRINT, arg.unEx()));
    }

    @Override
    public TRExp visit(Assign n) {
        TRExp val = n.value.accept(this);

        IRExp lhs = currentEnv.lookup(n.name);
        if (lhs != null) {
            return new Nx(IR.MOVE(lhs, val.unEx()));
        }

        return new Nx(
                IR.MOVE(
                        IR.MEM(
                                IR.BINOP(
                                        Op.PLUS,
                                        currentEnv.lookup(THIS),
                                        IR.CONST(offsetTable.get(thisClass).get(n.name))
                                )
                        ),
                        val.unEx()
                )
        );
    }

    private TRExp relOp(final CJUMP.RelOp op, AST ltree, AST rtree) {
        final TRExp l = ltree.accept(this);
        final TRExp r = rtree.accept(this);
        return new TRExp() {
            @Override
            public IRStm unCx(Label t, Label f) {
                return IR.CJUMP(op, l.unEx(), r.unEx(), t, f);
            }

            @Override
            public IRStm unCx(IRExp dst, IRExp src) {
                return IR.CMOVE(op, l.unEx(), r.unEx(), dst, src);
            }

            @Override
            public IRExp unEx() {
                TEMP v = TEMP(new Temp());
                return ESEQ(SEQ(
                        MOVE(v, FALSE),
                        CMOVE(op, l.unEx(), r.unEx(), v, TRUE)),
                        v);
            }

            @Override
            public IRStm unNx() {
                Label end = Label.gen();
                return SEQ(unCx(end, end),
                        LABEL(end));
            }

        };

    }

    @Override
    public TRExp visit(LessThan n) {
        return relOp(RelOp.LT, n.e1, n.e2);
    }

    //////////////////////////////////////////////////////////////

    private TRExp numericOp(Op op, Expression e1, Expression e2) {
        TRExp l = e1.accept(this);
        TRExp r = e2.accept(this);
        return new Ex(IR.BINOP(op, l.unEx(), r.unEx()));
    }

    @Override
    public TRExp visit(Plus n) {
        return numericOp(Op.PLUS, n.e1, n.e2);
    }

    @Override
    public TRExp visit(Minus n) {
        return numericOp(Op.MINUS, n.e1, n.e2);
    }

    @Override
    public TRExp visit(Times n) {
        return numericOp(Op.MUL, n.e1, n.e2);
    }

    //////////////////////////////////////////////////////////////////

    @Override
    public TRExp visit(IntegerLiteral n) {
        return new Ex(IR.CONST(n.value));
    }

    @Override
    public TRExp visit(IdentifierExp n) {
        IRExp var = currentEnv.lookup(n.name);
        if (var != null) {
            return new Ex(var);
        }

        return new Ex(
            IR.MEM(
                    IR.BINOP(
                            Op.PLUS,
                            currentEnv.lookup(THIS),
                            IR.CONST(offsetTable.get(thisClass).get(n.name))
                    )
            )
        );
    }

    @Override
    public TRExp visit(Not n) {
        final TRExp negated = n.e.accept(this);
        return new Cx() {
            @Override
            public IRStm unCx(Label ifTrue, Label ifFalse) {
                return negated.unCx(ifFalse, ifTrue);
            }

            @Override
            public IRStm unCx(IRExp dst, IRExp src) {
                return new Ex(IR.BINOP(Op.MINUS, IR.CONST(1), negated.unEx())).unCx(dst, src);
            }

            @Override
            public IRExp unEx() {
                return new Ex(IR.BINOP(Op.MINUS, IR.CONST(1), negated.unEx())).unEx();
            }
        };
    }

    @Override
    public TRExp visit(FunctionDecl n) {
        Frame oldframe = frame;
        frame = newFrame(functionLabel(n.name), n.formals.size());
        FunTable<IRExp> saveEnv = currentEnv;

        //Get the access information for each regular formal and add it to the environment.
        for (int i = 0; i < n.formals.size(); i++) {
            putEnv(n.formals.elementAt(i).name, frame.getFormal(i));
        }

        TRExp stats = visitStatements(n.statements);
        TRExp exp = n.returnExp.accept(this);

        IRStm body = IR.SEQ(
                stats.unNx(),
                IR.MOVE(frame.RV(), exp.unEx()));
        body = frame.procEntryExit1(body);
        frags.add(new ProcFragment(frame, body));

        frame = oldframe;
        currentEnv = saveEnv;

        return null;
    }


    @Override
    public TRExp visit(VarDecl n) {
        switch (n.kind){
            case LOCAL:
            Access var = frame.allocLocal(false);
            putEnv(n.name, var);
            break;

            case FORMAL:
                break;

            case FIELD:
                Label label = Label.get(thisClass+"_"+n.name);
                IRData data = new IRData(label,List.list(IR.CONST(0)));
                DataFragment dt = new DataFragment(frame,data);
                frags.add(dt);
                break;
        }
        return null;
    }


    @Override
    public TRExp visit(Call n) {
        String className = getClassName(n);
        String methodName = n.name;
        TRExp ths = n.receiver.accept(this);
        List<IRExp> args = List.list();
        args.add(ths.unEx());
        for (int i = 0; i < n.rands.size(); i++) {
            TRExp arg = n.rands.elementAt(i).accept(this);
            args.add(arg.unEx());
        }
        return new Ex(IR.CALL(functionLabel(className + "$" + methodName), args));
    }




    @Override
    public TRExp visit(FunctionType n) {
        throw new Error("Not implemented");
    }

    /**
     * After the visitor successfully traversed the program,
     * retrieve the built-up list of Fragments with this method.
     */
    public Fragments getResult() {
        return frags;
    }

    @Override
    public TRExp visit(MainClass n) {
        frame = newFrame(L_MAIN, 0);
        TRExp statement = n.statement.accept(this);
        IRStm body = frame.procEntryExit1(statement.unNx());
        // Is this even necessary?
        frame.procEntryExit1(body);
        frags.add(new ProcFragment(frame, body));
        return null;
    }

    @Override
    public TRExp visit(ClassDecl n) {
        thisClass = n.name;
        thisClassType = (ClassType) classTable.lookup(n.name);

        if (!offsetTable.containsKey(n.name)) {
            offsetTable.put(n.name, new HashMap<>());

            int offset = 0;
            for (Map.Entry<String, Type> field : thisClassType.fields) {
                offsetTable.get(n.name).put(field.getKey(), offset);
                offset += frame.wordSize();
            }
        }
        // Find the corresponding method
        Iterator<Map.Entry<String, Type>> iter = thisClassType.methods.iterator();
        // Iterate over the classtype methods in some dumb way
        while(iter.hasNext()) {
            Map.Entry<String, Type> entry = iter.next();
            MethodType method = (MethodType) entry.getValue();

            boolean methodExists = false;
            //System.out.println("Test, iterating over methods in "+ thisClass+ ":" + entry.getKey());
            //Check if n.methods already has this method
            for (int i = 0; i < n.methods.size(); i++){
                if (n.methods.elementAt(i).name.equals(entry.getKey())){
                    //System.out.println("Method is declared in class");
                    methodExists = true;
                    break;
                }
            }
            if (!methodExists) {
                //System.out.println("Method NOT FOUND in class, need to handle");
                ClassDecl superClass = classDeclLookup.lookup(n.superName);
                for (int i = 0; i < superClass.methods.size(); i++){
                    if (superClass.methods.elementAt(i).name.equals(entry.getKey())) {
                        n.methods.add(superClass.methods.elementAt(i));
                    }
                }
            }
        }

        //Insert this class decl so we can copy the methods in case of inheritance
        try {
            classDeclLookup.put(n.name, n);
        } catch (ImpTable.DuplicateException e) {
            e.printStackTrace();
        }

        n.methods.accept(this);
        thisClass = null;
        thisClassType = null;
        return null;
    }

    @Override
    public TRExp visit(MethodDecl n) {
        Frame oldFrame = frame;
        Label methodLabel = functionLabel(thisClass + "$" + n.name);
        frame = newFrame(methodLabel, n.formals.size() + 1);
        FunTable<IRExp> saveEnv = currentEnv;

        thisMethodType = (MethodType) ((ClassType) classTable.lookup(thisClass)).methods.lookup(n.name);

        // Put "this" into the env somehow -- do we even need to?
        putEnv(THIS,  frame.getFormal(0));
        //Get the access information for each regular formal and add it to the environment.
        for (int i = 1; i < n.formals.size()+1; i++) {
            putEnv(n.formals.elementAt(i-1).name, frame.getFormal(i));
        }

        for (int i = 0; i < n.vars.size(); i++) {
            Access a = frame.allocLocal(false);
            putEnv(n.vars.elementAt(i).name, a);
        }

        //System.out.println("current env:" + currentEnv);
        // Do we need to do this?
        //TRExp varDecls = n.vars.accept(this);
        TRExp stats = visitStatements(n.statements);
        TRExp exp = n.returnExp.accept(this);

        IRStm body = IR.SEQ(
                //varDecls.unNx(),
                stats.unNx(),
                IR.MOVE(frame.RV(), exp.unEx()));
        body = frame.procEntryExit1(body);
        frags.add(new ProcFragment(frame, body));

        frame = oldFrame;
        currentEnv = saveEnv;
        thisMethodType = null;
        return null;
    }

    @Override
    public TRExp visit(IntArrayType n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TRExp visit(ObjectType n) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TRExp visit(Block n) {
        return n.statements.accept(this);
    }

    @Override
    public TRExp visit(If n) {

        Label t = Label.gen();
        Label f = Label.gen();
        Label end = Label.gen();

        IRStm tst = n.tst.accept(this).unCx(t,f);
        IRStm thn = n.thn.accept(this).unNx();
        IRStm els = n.els.accept(this).unNx();

        TRExp value = new Nx(IR.SEQ(
                tst,
                IR.LABEL(t),
                thn,
                IR.JUMP(end),
                IR.LABEL(f),
                els,
                IR.LABEL(end)));

        return value;
    }

    @Override
    public TRExp visit(While n) {
        Label begin = Label.gen();
        Label check = Label.gen();
        Label end = Label.gen();

        // should call relOp which we unCx(begin, end) to make the CJUMP
        IRStm tst = n.tst.accept(this).unCx(begin, end);

        //System.out.println("currentEnv when we evaluate body:" + currentEnv);
        IRStm body = n.body.accept(this).unNx();

        TRExp value = new Nx(IR.SEQ(
                IR.LABEL(check),
                tst,
                IR.LABEL(begin),
                body,
                IR.JUMP(check),
                IR.LABEL(end)
        ));

        return value;
    }

    @Override
    public TRExp visit(ArrayAssign n) {
        IRExp index = n.index.accept(this).unEx();
        IRExp value = n.value.accept(this).unEx();
        // TODO: David please confirm we can just lookup the array here
        IRExp array = currentEnv.lookup(n.name);
        if (array == null) {
            array = IR.MEM(
                    IR.BINOP(
                            Op.PLUS,
                            currentEnv.lookup(THIS),
                            IR.CONST(offsetTable.get(thisClass).get(n.name))
                    )
            );
        }
        IRExp assign = IR.MEM(IR.BINOP(Op.PLUS, array, IR.BINOP(Op.MUL, index, IR.CONST(frame.wordSize()))));

        TRExp result = new Nx(IR.MOVE(assign, value));

        if (checkErrors){
            // just use code from below with some variable renames and hope it works
            Label check = Label.gen();
            Label err = Label.gen();
            Label pass = Label.gen();

            TEMP res = TEMP(new Temp());

            IRExp length = new Ex(IR.MEM(IR.BINOP(Op.MINUS, array, IR.CONST(frame.wordSize())))).unEx();
            // Check that 0 <= index < array length
            result = new Nx(
                            IR.SEQ(
                                    IR.CJUMP(RelOp.GE, index, IR.CONST(0), check, err), //Check >= 0
                                    LABEL(check),
                                    IR.CJUMP(RelOp.LT, index, length, pass, err), // Check < array.size
                                    LABEL(err),
                                    IR.EXP(IR.CALL(L_ERROR, IR.CONST(1))),
                                    LABEL(pass),
                                    IR.MOVE(assign, value)
                            ));
        }

        return result;
    }

    @Override
    public TRExp visit(And n) {
        IRExp e1 = n.e1.accept(this).unEx();
        IRExp e2 = n.e2.accept(this).unEx();

        Label check1 = Label.gen();
        Label check2 = Label.gen();
        Label pass = Label.gen();

        TEMP result = TEMP(new Temp());

        TRExp value = new Ex(IR.ESEQ(
                SEQ(
                    MOVE(result, IR.FALSE),
                    IR.CJUMP(RelOp.EQ, e1, IR.CONST(1), check1, pass),
                    LABEL(check1),
                    IR.CJUMP(RelOp.EQ, e2, IR.CONST(1), check2, pass),
                    LABEL(check2),
                    MOVE(result, IR.TRUE),
                    LABEL(pass)
                ),
                result));

        return value;
    }

    @Override
    public TRExp visit(ArrayLookup n) {
        IRExp array = n.array.accept(this).unEx();
        IRExp index = n.index.accept(this).unEx();


        TRExp value = new Ex(IR.MEM(IR.BINOP(Op.PLUS, array, IR.BINOP(Op.MUL, index, IR.CONST(frame.wordSize())))));

        if (checkErrors) {
            Label check = Label.gen();
            Label err = Label.gen();
            Label pass = Label.gen();

            TEMP result = TEMP(new Temp());

            IRExp length = new Ex(IR.MEM(IR.BINOP(Op.MINUS, array, IR.CONST(frame.wordSize())))).unEx();
            // Check that 0 <= index < array length
            value = new Ex(
                    IR.ESEQ(
                            IR.SEQ(
                                    IR.CJUMP(RelOp.GE, index, IR.CONST(0), check, err), //Check >= 0
                                    LABEL(check),
                                    IR.CJUMP(RelOp.LT, index, length, pass, err), // Check < array.size
                                    LABEL(err),
                                    IR.EXP(IR.CALL(L_ERROR, IR.CONST(1))),
                                    LABEL(pass),
                                    IR.MOVE(result, value.unEx())
                            ),
                            result));
        }

        return value;
    }

    @Override
    public TRExp visit(ArrayLength n) {
        //Array length was stored at a[-1]
        TRExp array = n.array.accept(this);
        TRExp length = new Ex(IR.MEM(IR.BINOP(Op.MINUS, array.unEx(), IR.CONST(frame.wordSize()))));
        return length;
    }

    @Override
    public TRExp visit(BooleanLiteral n) {
        if (n.value) return new Ex(IR.TRUE);
        else return new Ex(IR.FALSE);
    }

    @Override
    public TRExp visit(This n) {
        Access t = frame.getFormal(0);
        return new Ex(t.exp(frame.FP()));
    }

    @Override
    public TRExp visit(NewArray n) {
        TRExp size = n.size.accept(this);
        TRExp pointer = new Ex(IR.CALL(L_NEW_ARRAY, size.unEx()));
        return pointer;
    }

    @Override
    public TRExp visit(NewObject n) {
        ClassType ct = (ClassType) this.classTable.lookup(n.typeName);

//        if (!offsetTable.containsKey(n.typeName)) {
//            offsetTable.put(n.typeName, new HashMap<>());
//
//            int offset = 0;
//            for (Map.Entry<String, Type> field : ct.fields) {
//                offsetTable.get(n.typeName).put(field.getKey(), offset);
//                offset += frame.wordSize();
//            }
//        }
        //System.out.println(n.typeName + "is of class: " + ct.name + " and has " + ct.fields + " fields");
        TRExp pointer = new Ex(IR.CALL(L_NEW_OBJECT,IR.CONST(ct.fields.size()*frame.wordSize())));
        return pointer;
    }
}
