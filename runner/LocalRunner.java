import java.io.IOException;
import java.net.ConnectException;

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

    public static void main(String[] args) throws IOException, InterruptedException {
        run(true, true, 3, false, false);
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
