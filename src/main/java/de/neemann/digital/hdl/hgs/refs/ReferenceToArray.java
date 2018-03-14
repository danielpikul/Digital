/*
 * Copyright (c) 2018 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.hdl.hgs.refs;

import de.neemann.digital.hdl.hgs.*;

/**
 * Handles the access to arrays
 */
public class ReferenceToArray implements Reference {

    private final Reference parent;
    private final Expression index;

    /**
     * Creates a new Array access
     *
     * @param parent the parent reference
     * @param index  the index to access
     */
    public ReferenceToArray(Reference parent, Expression index) {
        this.parent = parent;
        this.index = index;
    }

    @Override
    public void set(Context context, Object value) throws HGSEvalException {
        final int i = Value.toInt(index.value(context));
        if (i < 0)
            throw new HGSEvalException("index out of bounds: " + i);

        Value.toArray(parent.get(context)).hgsArraySet(i, value);
    }

    @Override
    public Object get(Context context) throws HGSEvalException {
        final int i = Value.toInt(index.value(context));
        final HGSArray array = Value.toArray(parent.get(context));
        if (i < 0 || i >= array.hgsArraySize())
            throw new HGSEvalException("index out of bounds: " + i);
        return array.hgsArrayGet(i);
    }
}
