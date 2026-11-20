package Test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import model.ClientAgent;
import utils.Transition;

public class Test {
	public static void main(String[] args) {
		String name = "P1";
		String ipAddress = "localhost";
		int timeout = 60;
		
		
		Process proc =  null;
		/*
		try
        {
            // Command to create an external process
            String command1 = "java -jar C:\\Users\\Alez0\\OneDrive\\Desktop\\TablutCompetition\\Tablut\\Executables\\Server.jar";

            // Running the above command
            Runtime run  = Runtime.getRuntime();
            proc = run.exec(command1);
            
            try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
            
            String command2 = "java -jar C:\\Users\\Alez0\\OneDrive\\Desktop\\TablutCompetition\\Tablut\\Executables\\RandomPlayer.jar WHITE";

            // Running the above command
            Runtime run1  = Runtime.getRuntime();
            Process proc1 = run.exec(command2);
        }

        catch (IOException e)
        {
            e.printStackTrace();
        }
		*/
		
		/*
		ClientAgent m = null;
		try {
			m = new ClientAgent("Black", name, timeout, ipAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		m.run();
		*/

	

		
		
		TrainingEnvironment env = new TrainingEnvironment();
		env.runTraining(1000000);

	}
}
	



