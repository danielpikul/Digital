/*
 * Copyright (c) 2019 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.draw.library;

import de.neemann.digital.analyse.SubstituteLibrary;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementFactory;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.draw.elements.Circuit;
import de.neemann.digital.draw.elements.PinException;
import de.neemann.digital.draw.elements.VisualElement;
import de.neemann.digital.draw.model.ModelCreator;
import de.neemann.digital.draw.model.NetList;
import de.neemann.digital.hdl.hgs.*;
import de.neemann.digital.lang.Lang;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * The description of a nested element.
 * This is a complete circuit which is used as a element.
 */
public final class ElementTypeDescriptionCustom extends ElementTypeDescription {
    private static final int MAX_DEPTH = 30;
    private final File file;
    private final Circuit circuit;
    private final HashMap<String, Statement> map;
    private String description;
    private NetList netList;

    /**
     * Creates a new element
     *
     * @param file    the file which is loaded
     * @param circuit the circuit
     * @throws PinException PinException
     */
    ElementTypeDescriptionCustom(File file, Circuit circuit) throws PinException {
        super(file.getName(), (ElementFactory) null, circuit.getInputNames());
        this.file = file;
        this.circuit = circuit;
        map = new HashMap<>();
        setShortName(file.getName());
        addAttribute(Keys.ROTATE);
        addAttribute(Keys.LABEL);
        addAttribute(Keys.SHAPE_TYPE);
        if (circuit.getAttributes().get(Keys.IS_GENERIC))
            addAttribute(Keys.GENERIC);
    }

    /**
     * Returns the filename
     * The returned file is opened if the user wants to modify the element
     *
     * @return the filename
     */
    public File getFile() {
        return file;
    }

    /**
     * @return the elements attributes
     */
    public ElementAttributes getAttributes() {
        return circuit.getAttributes();
    }

    /**
     * @return the circuit
     */
    public Circuit getCircuit() {
        return circuit;
    }

    /**
     * Sets a custom description for this field
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDescription(ElementAttributes elementAttributes) {
        if (description != null)
            return description;
        else
            return super.getDescription(elementAttributes);
    }

    /**
     * Gets a {@link ModelCreator} of this circuit.
     * Every time this method is called a new {@link ModelCreator} is created.
     *
     * @param subName                 name of the circuit, used to name unique elements
     * @param depth                   recursion depth, used to detect a circuit which contains itself
     * @param containingVisualElement the containing visual element
     * @param library                 the library used
     * @return the {@link ModelCreator}
     * @throws PinException             PinException
     * @throws NodeException            NodeException
     * @throws ElementNotFoundException ElementNotFoundException
     */
    ModelCreator getModelCreator(String subName, int depth, VisualElement errorVisualElement, VisualElement containingVisualElement, ElementLibrary library) throws PinException, NodeException, ElementNotFoundException {
        if (netList == null)
            netList = new NetList(circuit);

        if (depth > MAX_DEPTH)
            throw new NodeException(Lang.get("err_recursiveNestingAt_N0", circuit.getOrigin()));

        if (circuit.getAttributes().get(Keys.IS_GENERIC)) {
            try {
                Context args;
                if (containingVisualElement != null) {
                    args = containingVisualElement.getGenericArgs();
                    if (args==null) {
                        String argsCode = containingVisualElement.getElementAttributes().get(Keys.GENERIC);
                        Statement s = getStatement(argsCode);
                        args = new Context();
                        if (containingVisualElement.getGenericArgs() != null)
                            args.declareVar("args", containingVisualElement.getGenericArgs());
                        s.execute(args);
                    }
                } else
                    args = new Context();

                Circuit c = circuit.createDeepCopy();
                for (VisualElement ve : c.getElements()) {
                    String gen = ve.getElementAttributes().get(Keys.GENERIC).trim();
                    if (!gen.isEmpty()) {
                        boolean isCustom = library.getElementType(ve.getElementName()) instanceof ElementTypeDescriptionCustom;
                        Statement genS = getStatement(gen);
                        if (isCustom) {
                            Context mod = new Context()
                                    .declareVar("args", args);
                            genS.execute(mod);
                            ve.setGenericArgs(mod);
                        } else {
                            Context mod = new Context()
                                    .declareVar("args", args)
                                    .declareVar("this", new SubstituteLibrary.AllowSetAttributes(ve.getElementAttributes()));
                            genS.execute(mod);
                        }
                    }
                }

                return new ModelCreator(c, library, true, new NetList(netList, errorVisualElement), subName, depth, errorVisualElement);
            } catch (IOException | ParserException | HGSEvalException e) {
                throw new NodeException(Lang.get("err_parsingGenericsCode"), e);
            }
        } else
            return new ModelCreator(circuit, library, true, new NetList(netList, errorVisualElement), subName, depth, errorVisualElement);
    }

    private Statement getStatement(String code) throws IOException, ParserException {
        Statement genS = map.get(code);
        if (genS == null) {
            genS = new Parser(code).parse(false);
            map.put(code, genS);
        }
        return genS;
    }

}
