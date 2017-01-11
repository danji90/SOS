/*
 * Copyright (C) 2012-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.sos.decode;

import java.util.Collections;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;
import org.isotc211.x2005.gco.CodeListValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.shetland.iso.GcoConstants;
import org.n52.shetland.ogc.sensorML.Role;
import org.n52.sos.util.CodingHelper;
import org.n52.svalbard.decode.Decoder;
import org.n52.svalbard.decode.DecoderKey;
import org.n52.svalbard.decode.exception.DecodingException;
import org.n52.sos.exception.ows.concrete.UnsupportedDecoderXmlInputException;

import com.google.common.base.Joiner;

/**
 * {@link Decoder} class to decode ISO TC211 Geographic COmmon (GCO) extensible
 * markup language.
 *
 * @author <a href="mailto:c.hollmann@52north.org">Carsten Hollmann</a>
 * @since 4.2.0
 *
 */
public class Iso19139GcoDecoder implements Decoder<Object, XmlObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iso19139GcoDecoder.class);

    private static final Set<DecoderKey> DECODER_KEYS = CodingHelper.decoderKeysForElements(GcoConstants.NS_GCO,
            CodeListValueType.class);

    public Iso19139GcoDecoder() {
        LOGGER.debug("Decoder for the following keys initialized successfully: {}!", Joiner.on(", ")
                .join(DECODER_KEYS));
    }

    @Override
    public Set<DecoderKey> getKeys() {
        return Collections.unmodifiableSet(DECODER_KEYS);
    }

    @Override
    public Object decode(XmlObject element) throws DecodingException {
        if (element instanceof CodeListValueType) {
            return encodeCodeListValue((CodeListValueType) element);
        } else {
            throw new UnsupportedDecoderXmlInputException(this, element);
        }
    }

    private Role encodeCodeListValue(CodeListValueType circ) {
        Role role = new Role(circ.getStringValue());
        role.setCodeList(circ.getCodeList());
        role.setCodeListValue(circ.getCodeListValue());
        return role;
    }

}
