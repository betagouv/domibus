package eu.domibus.core.util;

import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassUtilImplTest {
    @Tested
    ClassUtilImpl classUtil;

    @Test
    public void isMethodDefined() {
        boolean res = classUtil.isMethodDefined(this, "isMethodDefined", new Class[]{});
        Assertions.assertTrue(res);

        res = classUtil.isMethodDefined(this, "isMethodDefined2", new Class[]{});
        Assertions.assertFalse(res);

        res = classUtil.isMethodDefined(this, "isMethodDefined", new Class[]{String.class});
        Assertions.assertFalse(res);
    }
}
