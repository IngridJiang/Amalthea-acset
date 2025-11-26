package tools.vitruv.methodologisttemplate.vsum;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 
 */
public class VSUMRunner {

    public static void main(String[] args) {
      
        Path workDir = Paths.get("galette-output-0");

        System.out.println("Running VSUM in " + workDir.toAbsolutePath());
        new Test().insertTask(workDir, 0);
    }
}
