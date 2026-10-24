package android.os;

/**
 * Compile-time stub for the hidden android.os.ServiceManager class.
 * This class is omitted from the public Android SDK, so we define it here
 * to satisfy the compiler. At runtime, the Android OS bootclasspath will
 * load the real system class instead of this stub.
 */
public class ServiceManager {
    public static IBinder getService(String name) {
        throw new RuntimeException("Stub!");
    }
}
