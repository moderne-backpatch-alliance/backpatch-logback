package ch.qos.logback.core.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.EnvUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HardenedObjectInputStreamTest {

    ByteArrayOutputStream bos;
    ObjectOutputStream oos;
    HardenedObjectInputStream inputStream;
    String[] whitelist = new String[] {Innocent.class.getName()};
    
    @Before
    public void setUp() throws Exception {
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void smoke() throws ClassNotFoundException, IOException {
        Innocent innocent = new Innocent();
        innocent.setAnInt(1);
        innocent.setAnInteger(2);
        innocent.setaString("smoke");
        Innocent back = writeAndRead(innocent);
        assertEquals(innocent, back);
    }



    private Innocent writeAndRead(Innocent innocent) throws IOException, ClassNotFoundException {
        writeObject(oos, innocent);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        inputStream = new HardenedObjectInputStream(bis, whitelist);
        Innocent fooBack = (Innocent) inputStream.readObject();
        inputStream.close();
        return fooBack;
    }
    
    private void writeObject(ObjectOutputStream oos, Object o) throws IOException {
        oos.writeObject(o);
        oos.flush();
        oos.close();
    }

    @Test
    public void denialOfService() throws ClassNotFoundException, IOException {

        if(!EnvUtil.isJDK9OrHigher()) {
            return;
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(payload());
        inputStream = new HardenedObjectInputStream(bis, whitelist);
        try {
            inputStream.readObject();
            fail("InvalidClassException expected");
        } catch(InvalidClassException e) {
        }
        finally {
            inputStream.close();
        }
    }

    private byte[] payload() throws IOException {
        Set root = buildEvilHashset();
        writeObject(oos, root);
        return bos.toByteArray();
    }

    private Set buildEvilHashset() {
        Set root = new HashSet();
        Set s1 = root;
        Set s2 = new HashSet();
        for (int i = 0; i < 100; i++) {
            Set t1 = new HashSet();
            Set t2 = new HashSet();
            t1.add("foo"); // make it not equal to t2
            s1.add(t1);
            s1.add(t2);
            s2.add(t1);
            s2.add(t2);
            s1 = t1;
            s2 = t2;
        }
        return root;
    }

    // tests written by us: upstream's 12cf2c5a15 only adapts existing tests to the new constructors
    @Test
    public void listedJavaClassesAreRead() throws Exception {
        ArrayList<Object> list = new ArrayList<Object>();
        list.add("a");
        list.add(Integer.valueOf(1));
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("list", list);
        map.put("long", Long.valueOf(2));
        assertEquals(map, readWith(map, whitelist));
    }

    @Test
    public void unlistedJavaUtilClassIsRefused() throws Exception {
        assertRefused(new HashSet<String>());
        assertRefused(new LinkedList<String>());
        assertRefused(new Date());
    }

    @Test
    public void unlistedJavaLangClassIsRefused() throws Exception {
        assertRefused(new StackTraceElement("c", "m", "f", 1));
    }

    @Test
    public void refusalIsReportedToContext() throws Exception {
        Context context = new ContextBase();
        ByteArrayInputStream bis = new ByteArrayInputStream(serialize(new HashSet<String>()));
        // reflection: under a revert of the fix this fails at run time, not at compile time
        HardenedObjectInputStream ois = HardenedObjectInputStream.class.getConstructor(Context.class, InputStream.class, String[].class).newInstance(context,
                        bis, whitelist);
        try {
            ois.readObject();
            fail("InvalidClassException expected");
        } catch (InvalidClassException expected) {
        } finally {
            ois.close();
        }
        new StatusChecker(context).assertContainsMatch(Status.ERROR, "Unauthorized deserialization attempt for class \\[java.util.HashSet\\]");
    }

    private void assertRefused(Serializable o) throws Exception {
        try {
            readWith(o, whitelist);
            fail("InvalidClassException expected for " + o.getClass().getName());
        } catch (InvalidClassException expected) {
        }
    }

    private Object readWith(Serializable o, String[] allowed) throws Exception {
        HardenedObjectInputStream ois = new HardenedObjectInputStream(new ByteArrayInputStream(serialize(o)), allowed);
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
    }

    private byte[] serialize(Serializable o) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(out);
        objectOut.writeObject(o);
        objectOut.close();
        return out.toByteArray();
    }

    /**
     * Demonstrates that HardenedObjectInputStream.resolveProxyClass works correctly
     * by rejecting deserialization of dynamic proxy classes, even when the interfaces
     * they implement are whitelisted.
     */
    @Test
    public void resolveProxyClassRejectsDynamicProxies() throws Exception {
        // We deliberately whitelist both the interface and the invocation handler.
        // Despite this, deserialization must fail because resolveProxyClass always
        // throws InvalidClassException for proxy classes.
        assertProxyIsRejected(new String[] { ProxyInterface.class.getName(), TestInvocationHandler.class.getName() });
    }

    // test written by us: the check holds on its own, also with Proxy itself whitelisted
    @Test
    public void resolveProxyClassRejectsDynamicProxiesWhenProxyIsWhitelisted() throws Exception {
        assertProxyIsRejected(new String[] { ProxyInterface.class.getName(), TestInvocationHandler.class.getName(), Proxy.class.getName() });
    }

    private void assertProxyIsRejected(String[] proxyWhitelist) throws Exception {
        ProxyInterface proxy = (ProxyInterface) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ProxyInterface.class },
                        new TestInvocationHandler());

        // Serialize the proxy instance
        ByteArrayOutputStream proxyBos = new ByteArrayOutputStream();
        ObjectOutputStream proxyOos = new ObjectOutputStream(proxyBos);
        proxyOos.writeObject(proxy);
        proxyOos.close();

        ByteArrayInputStream bis = new ByteArrayInputStream(proxyBos.toByteArray());
        HardenedObjectInputStream hardenedOis = new HardenedObjectInputStream(bis, proxyWhitelist);
        try {
            hardenedOis.readObject();
            fail("InvalidClassException expected");
        } catch (InvalidClassException expected) {
        } finally {
            hardenedOis.close();
        }
    }

    /**
     * A marker interface for the dynamic proxy used in the resolveProxyClass test.
     */
    interface ProxyInterface extends Serializable {
        String getMessage();
    }

    /**
     * A serializable InvocationHandler used to create the test dynamic proxy.
     */
    static class TestInvocationHandler implements InvocationHandler, Serializable {
        private static final long serialVersionUID = 1L;

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getMessage".equals(method.getName())) {
                return "hello from proxy";
            }
            return null;
        }
    }
}
