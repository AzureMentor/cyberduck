package ch.cyberduck.core.editor;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.NullLocal;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class LaunchServicesApplicationFinderTest extends AbstractTestCase {

    @BeforeClass
    public static void register() {
        LaunchServicesApplicationFinder.register();
    }

    @Test
    public void testFindAll() throws Exception {
        ApplicationFinder f = ApplicationFinderFactory.get();
        final List<Application> applications = f.findAll(new NullLocal(null, "file.txt"));
        assertFalse(applications.isEmpty());
        assertTrue(applications.contains(new Application("com.apple.TextEdit", "Preview")));
        assertTrue(applications.contains(new Application("com.macromates.textmate", "TextMate")));
    }

    @Test
    public void testFind() throws Exception {
        ApplicationFinder f = ApplicationFinderFactory.get();
        assertEquals(new Application("com.apple.Preview", "Preview"), f.find(new NullLocal(null, "file.png")));
    }

    @Test
    public void testIsOpen() throws Exception {
        final ApplicationFinder f = ApplicationFinderFactory.get();
        assertTrue(f.isOpen(new Application("com.apple.finder", null)));
        assertFalse(f.isOpen(new Application("com.apple.Finder", null)));
    }

    @Test
    public void testIsOpenRepeated() throws Exception {
        final ApplicationFinder f = ApplicationFinderFactory.get();
        this.repeat(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                assertTrue(f.isOpen(new Application("com.apple.finder", null)));
                assertFalse(f.isOpen(new Application("com.apple.Finder", null)));
                return null;
            }
        }, 5);
    }
}
