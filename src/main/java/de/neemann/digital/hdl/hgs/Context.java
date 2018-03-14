/*
 * Copyright (c) 2018 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.hdl.hgs;

import de.neemann.digital.hdl.hgs.function.Func;
import de.neemann.digital.hdl.hgs.function.Function;
import de.neemann.digital.hdl.hgs.function.FunctionFormat;
import de.neemann.digital.hdl.hgs.function.FunctionIsPresent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The evaluation context
 */
public class Context {
    private final Context parent;
    private final StringBuilder code;
    private HashMap<String, Object> map;

    /**
     * Creates a new context
     */
    public Context() {
        this(null, true);
    }

    /**
     * Creates a new context
     *
     * @param parent the parent context
     */
    public Context(Context parent) {
        this(parent, true);
    }

    /**
     * Creates a new context
     *
     * @param parent      the parent context
     * @param enablePrint enables the print, if false, the printing goes to the parent of this context
     */
    public Context(Context parent, boolean enablePrint) {
        this.parent = parent;
        if (enablePrint)
            this.code = new StringBuilder();
        else
            this.code = null;
        map = new HashMap<>();

        // some function which are always present
        addFunc("format", new FunctionFormat());
        addFunc("isPresent", new FunctionIsPresent());
        addFunc("sizeOf", new Func(1, args -> Value.toArray(args[0]).hgsArraySize()));
        addFunc("newMap", new Func(0, args -> new HashMap()));
        addFunc("newList", new Func(0, args -> new ArrayList()));
    }

    /**
     * Returns true if this context contains a mapping for the specified key.
     *
     * @param name the key
     * @return true if value is present
     */
    public boolean contains(String name) {
        if (map.containsKey(name))
            return true;
        else {
            if (parent != null)
                return parent.contains(name);
            else
                return false;
        }
    }

    /**
     * Get a variable
     *
     * @param name the name
     * @return the value
     * @throws HGSEvalException HGSEvalException
     */
    public Object getVar(String name) throws HGSEvalException {
        Object v = map.get(name);
        if (v == null) {

            if (name.equals("output"))
                return code.toString();

            if (parent == null)
                throw new HGSEvalException("variable not found: " + name);
            else
                return parent.getVar(name);
        } else
            return v;
    }

    /**
     * Set a variable
     *
     * @param name name
     * @param val  value
     * @return this for chained calls
     */
    public Context setVar(String name, Object val) {
        map.put(name, val);
        return this;
    }

    /**
     * Adds a function to the context.
     * Only needed for type checking. Calls setVar().
     *
     * @param name the name
     * @param func the function
     * @return this for chained calls
     */
    public Context addFunc(String name, Function func) {
        return setVar(name, func);
    }

    /**
     * Prints code to the context
     *
     * @param str the string to print
     * @return this for chained calls
     */
    public Context print(String str) {
        if (code != null)
            code.append(str);
        else
            parent.print(str);
        return this;
    }

    @Override
    public String toString() {
        if (code != null)
            return code.toString();
        else
            return parent.toString();
    }

    /**
     * @return the output length
     */
    public int length() {
        return code.length();
    }

}
