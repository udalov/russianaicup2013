import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashSet;

public class LocalRunner {
    private static void run(boolean vis, boolean sync, int teamSize, boolean smartGuy, boolean keyboardPlayer) throws IOException {
        ProcessBuilder b = new ProcessBuilder("java", "-jar", "lib/local-runner.jar",
                String.valueOf(vis),
                String.valueOf(sync),
                String.valueOf(teamSize),
                "out/log.txt",
                String.valueOf(smartGuy),
                String.valueOf(keyboardPlayer)
        );
        b.start();
    }

    /**
     * @param args -vis, -sync, -smartGuy, -keyboard
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        HashSet<String> set = new HashSet<>(Arrays.asList(args));
        run(set.contains("-vis"), set.contains("-sync"), 3, set.contains("smartGuy"), set.contains("-keyboard"));

        while (true) {
            try {
                Runner.main(args);
            } catch (ConnectException e) {
                if ("Connection refused".equals(e.getMessage())) {
                    Thread.sleep(500);
                    continue;
                }
            }
            break;
        }
    }
}
