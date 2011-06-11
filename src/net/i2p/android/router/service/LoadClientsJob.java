package net.i2p.android.router.service;

import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.router.Job;
import net.i2p.router.JobImpl;
import net.i2p.router.RouterContext;
import net.i2p.util.I2PThread;

/**
 * Load the clients we want.
 *
 * We can't use LoadClientAppsJob (reading in clients.config) directly
 * because Class.forName() needs a PathClassLoader argument -
 * http://doandroids.com/blogs/2010/6/10/android-classloader-dynamic-loading-of/
 * ClassLoader cl = new PathClassLoader(_apkPath, ClassLoader.getSystemClassLoader());
 *
 * We can't extend LoadClientAppsJob to specify a class loader,
 * even if we use it only for Class.forName() and not for
 * setContextClassLoader(), because I2PTunnel still
 * can't find the existing static RouterContext due to the new class loader.
 *
 * Also, if we load them that way, we can't register a shutdown hook.
 *
 * So fire off the ones we want here, without a clients.config file and
 * without using Class.forName().
 *
 */
class LoadClientsJob extends JobImpl {
    
    /** this is the delay to load the clients. There are additional delays e.g. in i2ptunnel.config */
    private static final long LOAD_DELAY = 10*1000;


    public LoadClientsJob(RouterContext ctx) {
        super(ctx);
        getTiming().setStartAfter(getContext().clock().now() + LOAD_DELAY);
    }

    public String getName() { return "Start Clients"; };

    public void runJob() {
        Job j = new RunI2PTunnel(getContext());
        getContext().jobQueue().addJob(j);
        // add other clients here
    }

    private static class RunI2PTunnel extends JobImpl {

        public RunI2PTunnel(RouterContext ctx) {
            super(ctx);
        }

        public String getName() { return "Start I2P Tunnel"; };

        public void runJob() {
            System.err.println("Starting i2ptunnel");
            TunnelControllerGroup.main(null);
            System.err.println("i2ptunnel started");
            getContext().addShutdownTask(new I2PTunnelShutdownHook());

        }
    }

    private static class I2PTunnelShutdownHook implements Runnable {
        public void run() {
            System.err.println("i2ptunnel shutdown hook");
            TunnelControllerGroup.getInstance().unloadControllers();
        }
    }
}
