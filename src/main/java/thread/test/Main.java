package thread.test;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

/**
 *
 * @author Laszlo Kishalmi
 */
public class Main implements Runnable {

    private final Object TIMER = new Object();
    
    @Override
    public void run() {
        GradleConnector gconn = GradleConnector.newConnector().useGradleVersion("2.13");
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        CancellationTokenSource cancellationTokenSource = GradleConnector.newCancellationTokenSource();
        ProjectConnection pconn = null;
        try {
            System.out.println("Active Threads [pre conn]: " + tg.activeCount());
            pconn = gconn.forProjectDirectory(new File(System.getProperty("user.dir"), "../services-clubs-service")).connect();
            System.out.println("Active Threads [pre build]: " + tg.activeCount());
            pconn.newBuild().withArguments("clean", "build", "-x", "check")
                    .withCancellationToken(cancellationTokenSource.token())
                    .setStandardOutput(System.out)
                    .run();
            System.out.println("Active Threads [post build]: " + tg.activeCount());
        } finally {
            if (pconn != null) {
                pconn.close();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("Active Threads [post close]: " + tg.activeCount());
            }
        }
        // A very raw mimic what NetBeans do for waiting for all the created threads end in the TG
        synchronized(TIMER) {
            try {
                while (tg.activeCount() > 1) {
                    TIMER.wait(1000);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                TIMER.notifyAll();
                System.out.println("Active Threads [final]: " + tg.activeCount());
            }
        }
    }

    public static void main(String[] args) {
        ThreadGroup tg = new ThreadGroup("Gradle Build");
        Thread build = new Thread(tg, new Main());
        build.start();
    }
}
