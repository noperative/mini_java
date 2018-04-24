package driver;

import ir.frame.Frame;
import ir.frame.x86_64.X86_64Frame;

import java.io.File;

import translate.Fragments;
import translate.Translator;
import x86_64sim.Sim;

import codegen.Assembly;
import codegen.CodeGenerator;

public class DSimulate {

    public static final Frame architecture = X86_64Frame.factory;
	
    /**
     * Given a source file, compile it and write the parse tree to System.out.
     * 
     * @param program  program to compile.
     */
    public static void compile(File program) throws Exception {
	Fragments translated = Translator.translate(architecture, program);
	CodeGenerator cogen = new CodeGenerator();
	Assembly assembly = cogen.apply(translated);
	String sProgram = assembly.toString();
	// System.out.println("Program:\n" + sProgram);
	String output = Sim.ulate(sProgram, false).result;
	System.out.println("Program output:\n" + output);
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
