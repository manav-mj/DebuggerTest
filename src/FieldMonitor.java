import com.sun.jdi.Field;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ModificationWatchpointRequest;
import exec.*;

import java.io.IOException;
import java.util.List;

public class FieldMonitor {

    public static final String PKG_NAME = "processing.test.androidsketch";
    public static final String CLASS_NAME = "androidsketch";
    public static final String FIELD_NAME = "mouseX";
    public static final int TCP_PORT = 7777;
    private static Process deviceId;
    private static int pId;

    public static void main(String[] args)
            throws IOException, InterruptedException {
        // start app in debug mode
        String[] cmd = {
                "shell", "am", "start",
                "-e", "debug", "true",
                "-a", "android.intent.action.MAIN",
                "-c", "android.intent.category.LAUNCHER",
                "-n", PKG_NAME + "/.MainActivity"
        };
        adb(cmd);
        // fetch details
        adb("devices");
        // find jdwp pid
        final String[] jdwpcmd = generateAdbCommand("jdwp");
        deviceId = Runtime.getRuntime().exec(jdwpcmd);
        new StreamPump(deviceId.getInputStream(), "jdwp: ").addTarget(
                new JDWPProcessor()).start();
        new StreamPump(deviceId.getErrorStream(), "jdwperr: ").addTarget(
                System.err).start();

        Thread.sleep(1000);
        // forward to tcp port
        adb("forward", "tcp:" + TCP_PORT, "jdwp:" + pId);
        // connect
        System.out.println(":debugger:Attaching Debugger");
        VirtualMachine vm = new VMAcquirer().connect(TCP_PORT);
        // wait to connect
        Thread.sleep(3000);
        // set watch field on already loaded classes
        List<ReferenceType> referenceTypes = vm.classesByName(PKG_NAME + "." + CLASS_NAME);

        for (ReferenceType refType : referenceTypes) {
            addFieldWatch(vm, refType);
        }
        // watch for loaded classes
        addClassWatch(vm);

        // resume the vm
        vm.resume();

        // process events
        EventQueue eventQueue = vm.eventQueue();
        while (true) {
            EventSet eventSet = eventQueue.remove();
            for (Event event : eventSet) {
                if (event instanceof VMDeathEvent
                        || event instanceof VMDisconnectEvent) {
                    // exit
                    System.out.println(":debugger:app killed");
                    return;
                } else if (event instanceof ClassPrepareEvent) {
                    // watch field on loaded class
                    ClassPrepareEvent classPrepEvent = (ClassPrepareEvent) event;
                    ReferenceType refType = classPrepEvent
                            .referenceType();
                    addFieldWatch(vm, refType);
                } else if (event instanceof ModificationWatchpointEvent) {
                    // a Test.foo has changed
                    ModificationWatchpointEvent modEvent = (ModificationWatchpointEvent) event;
                    System.out.println("watching mouseX:");
                    System.out.println("old="
                            + modEvent.valueCurrent());
                    System.out.println("new=" + modEvent.valueToBe());
                    System.out.println();
                }
            }
            eventSet.resume();
        }
    }

    /**
     * Watch all classes of name "androidsketch"
     */
    private static void addClassWatch(VirtualMachine vm) {
        EventRequestManager erm = vm.eventRequestManager();
        ClassPrepareRequest classPrepareRequest = erm.createClassPrepareRequest();
        classPrepareRequest.addClassFilter(CLASS_NAME);
        classPrepareRequest.setEnabled(true);
    }

    /**
     * Watch field of name "mouseX"
     */
    private static void addFieldWatch(VirtualMachine vm,
                                      ReferenceType refType) {
        EventRequestManager erm = vm.eventRequestManager();
        Field field = refType.fieldByName(FIELD_NAME);
        ModificationWatchpointRequest modificationWatchpointRequest = erm.createModificationWatchpointRequest(field);
        modificationWatchpointRequest.setEnabled(true);
    }

    private static ProcessResult adb(final String... cmd) throws InterruptedException, IOException {
        final String[] adbCmd = generateAdbCommand(cmd);
        return runADB(adbCmd);
    }

    private static String[] generateAdbCommand(final String... cmd) throws IOException {
        return concat(new String[]{"adb"}, cmd);
    }

    private static final String ADB_DAEMON_MSG_1 = "daemon not running";
    private static final String ADB_DAEMON_MSG_2 = "daemon started successfully";
    public static boolean adbDisabled = false;


    public static ProcessResult runADB(final String... cmd)
            throws InterruptedException, IOException {

        if (adbDisabled) {
            throw new IOException("adb is currently disabled");
        }

        final String[] adbCmd;
        if (!cmd[0].contains("adb")) {
            adbCmd = splice(cmd, "adb", 0);
        } else {
            adbCmd = cmd;
        }
        // printing this here to see if anyone else is killing the adb server
//        if (processing.app.Base.DEBUG) {
//            PApplet.printArray(adbCmd);
//        }
        try {
            ProcessResult adbResult = new ProcessHelper(adbCmd).execute();
            // Ignore messages about starting up an adb daemon
            String out = adbResult.getStdout();
            if (out.contains(ADB_DAEMON_MSG_1) && out.contains(ADB_DAEMON_MSG_2)) {
                StringBuilder sb = new StringBuilder();
                for (String line : out.split("\n")) {
                    if (!out.contains(ADB_DAEMON_MSG_1) &&
                            !out.contains(ADB_DAEMON_MSG_2)) {
                        sb.append(line).append("\n");
                    }
                }
                return new ProcessResult(adbResult.getCmd(),
                        adbResult.getResult(),
                        sb.toString(),
                        adbResult.getStderr(),
                        adbResult.getTime());
            }
            return adbResult;
        } catch (IOException ioe) {
            if (-1 < ioe.getMessage().indexOf("Permission denied")) {
                System.out.println("Trouble with adb! : " +
                        "Could not run the adb tool from the Android SDK.\n" +
                        "One possibility is that its executable permission\n" +
                        "is not properly set. You can try setting this\n" +
                        "permission manually, or re-installing the SDK.\n\n" +
                        "The mode will be disabled until this problem is fixed.\n");
                adbDisabled = true;
            }
            throw ioe;
        }
    }

    static public String[] concat(String a[], String b[]) {
        String c[] = new String[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    static final public String[] splice(String list[],
                                        String value, int index) {
        String outgoing[] = new String[list.length + 1];
        System.arraycopy(list, 0, outgoing, 0, index);
        outgoing[index] = value;
        System.arraycopy(list, index, outgoing, index + 1,
                list.length - index);
        return outgoing;
    }

    private static class JDWPProcessor implements LineProcessor {
        public void processLine(final String line) {
            pId = Integer.parseInt(line);
        }
    }
}
