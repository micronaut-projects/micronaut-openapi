package io.micronaut.openapi;

import java.io.Serializable;

import javax.xml.namespace.QName;

/**
 * This class is copy of jakarta.xml.bind.api.JAXBElement
 */
public class JAXBElement<T> implements Serializable {

    /**
     * xml element tag name
     */
    final protected QName name;

    /**
     * Java datatype binding for xml element declaration's type.
     */
    final protected Class<T> declaredType;

    final protected Class<?> scope;

    /**
     * xml element value.
     * Represents content model and attributes of an xml element instance.
     */
    protected T value;

    /**
     * true iff the xml element instance has xsi:nil="true".
     */
    protected boolean nil = false;

    /**
     * Designates global scope for an xml element.
     */
    public static final class GlobalScope {

        private GlobalScope() {
        }
    }

    /**
     * <p>Construct an xml element instance.</p>
     *
     * @param name Java binding of xml element tag name
     * @param declaredType Java binding of xml element declaration's type
     * @param scope Java binding of scope of xml element declaration.
     *     Passing null is the same as passing {@code GlobalScope.class}
     * @param value Java instance representing xml element's value.
     *
     * @see #getScope()
     * @see #isTypeSubstituted()
     */
    public JAXBElement(QName name,
                       Class<T> declaredType,
                       Class<?> scope,
                       T value) {
        if (declaredType == null || name == null) {
            throw new IllegalArgumentException();
        }
        this.declaredType = declaredType;
        if (scope == null) {
            scope = GlobalScope.class;
        }
        this.scope = scope;
        this.name = name;
        this.value = value;
    }

    /**
     * Construct a xml element instance.
     * <p>
     * This is just a convenience method for {@code new JAXBElement(name,declaredType,GlobalScope.class,value)}
     */
    public JAXBElement(QName name, Class<T> declaredType, T value) {
        this(name, declaredType, GlobalScope.class, value);
    }

    /**
     * Returns the Java binding of the xml element declaration's type attribute.
     */
    public Class<T> getDeclaredType() {
        return declaredType;
    }

    /**
     * Returns the xml element tag name.
     */
    public QName getName() {
        return name;
    }

    /**
     * <p>Set the content model and attributes of this xml element.</p>
     *
     * <p>When this property is set to {@code null}, {@code isNil()} must by {@code true}.
     * Details of constraint are described at {@link #isNil()}.</p>
     *
     * @see #isTypeSubstituted()
     */
    public void setValue(T t) {
        value = t;
    }

    /**
     * <p>Return the content model and attribute values for this element.</p>
     *
     * <p>See {@link #isNil()} for a description of a property constraint when
     * this value is {@code null}</p>
     */
    public T getValue() {
        return value;
    }

    /**
     * Returns scope of xml element declaration.
     *
     * @return {@code GlobalScope.class} if this element is of global scope.
     *
     * @see #isGlobalScope()
     */
    public Class<?> getScope() {
        return scope;
    }

    /**
     * <p>Returns {@code true} iff this element instance content model
     * is nil.</p>
     *
     * <p>This property always returns {@code true} when {@link #getValue()} is null.
     * Note that the converse is not true, when this property is {@code true},
     * {@link #getValue()} can contain a non-null value for attribute(s). It is
     * valid for a nil xml element to have attribute(s).</p>
     */
    public boolean isNil() {
        return (value == null) || nil;
    }

    /**
     * <p>Set whether this element has nil content.</p>
     *
     * @see #isNil()
     */
    public void setNil(boolean value) {
        nil = value;
    }

    /* Convenience methods
     * (Not necessary but they do unambiguously conceptualize
     *  the rationale behind this class' fields.)
     */

    /**
     * Returns true iff this xml element declaration is global.
     */
    public boolean isGlobalScope() {
        return scope == GlobalScope.class;
    }

    /**
     * Returns true iff this xml element instance's value has a different
     * type than xml element declaration's declared type.
     */
    public boolean isTypeSubstituted() {
        if (value == null) {
            return false;
        }
        return value.getClass() != declaredType;
    }

    private static final long serialVersionUID = 1L;
}
