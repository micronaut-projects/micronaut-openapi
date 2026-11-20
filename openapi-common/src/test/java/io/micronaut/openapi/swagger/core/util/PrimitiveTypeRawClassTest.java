package io.micronaut.openapi.swagger.core.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimitiveTypeRawClassTest {

    @SuppressWarnings("unused")
    private List<String> list;
    @SuppressWarnings("unused")
    private List<String>[] genericArray;
    @SuppressWarnings("unused")
    private Map<String, ? extends Number> wildcard;

    @SuppressWarnings("unused")
    private <T extends CharSequence> T typeVariable() {
        return null;
    }

    @Test
    void resolvesRawClasses() throws Exception {
        assertEquals(String.class, PrimitiveType.rawClass(String.class));
        assertEquals(List.class, PrimitiveType.rawClass(field("list")));
        assertEquals(List[].class, PrimitiveType.rawClass(field("genericArray")));
        Type wildcardArg = ((java.lang.reflect.ParameterizedType) field("wildcard")).getActualTypeArguments()[1];
        assertEquals(Number.class, PrimitiveType.rawClass(wildcardArg));
        assertEquals(CharSequence.class, PrimitiveType.rawClass(getClass().getDeclaredMethod("typeVariable").getGenericReturnType()));
        assertEquals(Object.class, PrimitiveType.rawClass(null));
    }

    @Test
    void resolvesPrimitiveTypesFromGenericTypes() throws Exception {
        assertEquals(PrimitiveType.DECIMAL, PrimitiveType.fromType(BigDecimal.class));
        assertEquals(PrimitiveType.STRING, PrimitiveType.fromTypeAndFormat(String.class, null));
        assertEquals(null, PrimitiveType.fromType(field("list")));
    }

    private Type field(String name) throws NoSuchFieldException {
        return getClass().getDeclaredField(name).getGenericType();
    }
}
