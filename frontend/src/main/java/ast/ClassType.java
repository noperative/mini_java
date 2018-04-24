package ast;

import visitor.Visitor;
import util.ImpTable;

public class ClassType extends Type{

    public ClassType superClassType;
    public String superClass;
    public String name;
    public ImpTable<Type> fields = new ImpTable<Type>();
    public ImpTable<Type> methods = new ImpTable<Type>();

    @Override
    public boolean equals(Object other) {
        System.out.println("Checking equality");
        if (superClassType != null){
            return (this.getClass() == other.getClass() || this.superClassType.getClass() == other.getClass());
        }
        return this.getClass() == other.getClass();
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return null;
    }
}
