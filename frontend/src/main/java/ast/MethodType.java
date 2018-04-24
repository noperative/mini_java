package ast;

import util.ImpTable;
import visitor.Visitor;

public class MethodType extends Type {

    public ImpTable<Type> params = new ImpTable<Type>();
    public ImpTable<Type> locals = new ImpTable<Type>();
    public NodeList<VarDecl> formals;
    public Type returnType;

    @Override
    public boolean equals(Object other) {
        return this.getClass() == other.getClass();
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return null;
    }
}
