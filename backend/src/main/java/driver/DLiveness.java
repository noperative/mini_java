package driver;

import ir.frame.Frame;
import ir.frame.x86_64.X86_64Frame;
import ir.interp.InterpMode;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;


import translate.Fragments;
import translate.Translator;
import util.List;

import codegen.AssemFragment;
import codegen.AssemProc;
import codegen.CodeGenerator;
import codegen.assem.Instr;

import analysis.FlowGraph;
import analysis.Liveness;
import analysis.implementation.LivenessImplementation;


public class DLiveness {

    public static final Frame architecture = X86_64Frame.factory;
	
    protected static InterpMode getSimulationMode() {
	// return InterpMode.LINEARIZED_IR;
	return InterpMode.BASIC_BLOCKS;
    }

    private static <T> boolean sameset(List<T> l1, List<T> l2) {
	HashSet<T> h1, h2;
	h1 = new HashSet<T>();
	h2 = new HashSet<T>();
	for (T t : l1) 
	    h1.add(t);
	for (T t : l2)
	    h2.add(t);
	return h1.equals(h2);
    }

    /**
     * Given a source file, compile it and write the liveness information
     * to System.out.
     * 
     * @param program  program to compile.
     */
    public static void compile(File program) throws Exception {
	Fragments translated = Translator.translate(architecture, program);

	CodeGenerator cogen = new CodeGenerator();
	
	for (AssemFragment frag : cogen.apply(translated)) {
	    if (frag instanceof AssemProc) {
		AssemProc proc = (AssemProc) frag;
		System.out.println("liveness information for : "
				   + proc.getLabel());
		FlowGraph<Instr> flowGraph = FlowGraph.build(proc.getBody());
		Liveness<Instr> live =
		    new LivenessImplementation<Instr>(flowGraph);
		System.out.println(live);
		
		File out = new File("live-" + proc.getLabel() + ".dot");
		try {
		    PrintStream outb = new PrintStream(out);
		    outb.print(live.dotString(proc.getLabel().toString()));
		    outb.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
	String program = args[0];
	try {
	    compile(new File(program));
	} catch (Exception e) {
	    System.out.println("Compilation problem");
	    e.printStackTrace();
	}
    }

}
