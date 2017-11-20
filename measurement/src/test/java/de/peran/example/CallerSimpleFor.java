package de.peran.example;

import java.io.File;

import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.writer.filesystem.ChangeableFolderSyncFsWriter;

public class CallerSimpleFor {

    int x = 0;

    public static void main(final String[] args) {
		try {
			final File tmpFolder = new File("target/kieker_results_test/");
			ChangeableFolderSyncFsWriter.getInstance(MonitoringController.getInstance()).setFolder(tmpFolder);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        System.out.println("Here it starts");
        final CallerSimpleFor c = new CallerSimpleFor();
        c.method1();
        c.method3();
    }

    public void method1() {
        x++;
        method2();
        method2(x+1);
        main(3);
        main("asd");
    }
    
    private void main(final int z){
    	System.out.println("Zweite main!");
    }
    
    private void main(final String z){
    	System.out.println("Dritte main!");
    }
    
    private void method2() {
        System.out.println("X has been incremented: " + x);
    }

    private void method2(final int z) {
        System.out.println("X has been incremented and z has been defined " + z);
    }

    public void method3() {
        System.out.println("Lets try another class");
        final CalleeSimpleFor c = new CalleeSimpleFor();
        c.callMe();
    }

    public void methodNever() {
        System.out.println("I  am never called");
    }
}
