/**
 * ﻿Copyright (C) 2013
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.svalbard.encode.uvf;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNot;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.n52.schetland.uvf.UVFConstants;
import org.n52.sos.exception.ows.NoApplicableCodeException;
import org.n52.sos.exception.ows.concrete.UnsupportedEncoderInputException;
import org.n52.sos.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.gml.CodeType;
import org.n52.sos.ogc.gml.CodeWithAuthority;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.om.AbstractPhenomenon;
import org.n52.sos.ogc.om.MultiObservationValues;
import org.n52.sos.ogc.om.ObservationValue;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.StreamingValue;
import org.n52.sos.ogc.om.TimeLocationValueTriple;
import org.n52.sos.ogc.om.TimeValuePair;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.sos.ogc.om.values.CountValue;
import org.n52.sos.ogc.om.values.MultiValue;
import org.n52.sos.ogc.om.values.QuantityValue;
import org.n52.sos.ogc.om.values.TLVTValue;
import org.n52.sos.ogc.om.values.TVPValue;
import org.n52.sos.ogc.om.values.Value;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.swe.SweDataArray;
import org.n52.sos.ogc.swe.SweDataRecord;
import org.n52.sos.ogc.swe.SweField;
import org.n52.sos.ogc.swe.simpleType.SweBoolean;
import org.n52.sos.ogc.swe.simpleType.SweQuantity;
import org.n52.sos.ogc.swe.simpleType.SweTime;
import org.n52.sos.response.AbstractObservationResponse.GlobalGetObservationValues;
import org.n52.sos.response.BinaryAttachmentResponse;
import org.n52.sos.response.GetObservationResponse;
import org.n52.sos.util.CollectionHelper;
import org.n52.sos.util.JTSHelper;
import org.n52.sos.util.builder.SweDataArrayBuilder;
import org.n52.sos.util.builder.SweDataArrayValueBuilder;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author <a href="mailto:e.h.juerrens@52north.org">Eike Hinderk
 *         J&uuml;rrens</a>
 *
 */
public class UVFEncoderTest {

    @Rule
    public ExpectedException exp = ExpectedException.none();

    private static final long UTC_TIMESTAMP_1 = 43200000l;
    private static final long UTC_TIMESTAMP_0 = -UTC_TIMESTAMP_1;
    private UVFEncoder encoder;
    private GetObservationResponse responseToEncode;
    private String obsPropIdentifier = "test-obs-prop-identifier";
    private String foiIdentifier = "test-foi-identifier";
    private String unit = "test-unit";

    @Before
    public void initObjects() throws OwsExceptionReport {
        encoder = new UVFEncoder();

        final OmObservation omObservation = new OmObservation();
        OmObservationConstellation observationConstellation = new OmObservationConstellation();

        // Observed Property
        String valueType = "test-obs-prop-value-type";
        String description = "test-obs-prop-description";
        AbstractPhenomenon observableProperty = new OmObservableProperty(
                obsPropIdentifier,
                description,
                unit,
                valueType);
        observationConstellation.setObservableProperty(observableProperty);

        // Feature Of Interest
        CodeWithAuthority featureIdentifier = new CodeWithAuthority(foiIdentifier);
        AbstractFeature featureOfInterest = new SamplingFeature(featureIdentifier);
        int srid = 4326;
        String geomWKT = "POINT(51.9350382 7.6521225)";
        final Geometry point = JTSHelper.createGeometryFromWKT(geomWKT, srid);
        ((SamplingFeature) featureOfInterest).setGeometry(point);
        observationConstellation.setFeatureOfInterest(featureOfInterest);

        // value
        final String uomId = "test-uom";
        final double testValue = 52.0;
        Value<?> measuredValue = new QuantityValue(testValue, uomId);

        // timestamps
        Time phenomenonTime = new TimeInstant(new Date(UTC_TIMESTAMP_1));

        // observation value
        ObservationValue<?> value = new SingleObservationValue<>(phenomenonTime, measuredValue);
        omObservation.setValue(value);

        // observation type
        observationConstellation.setObservationType(OmConstants.OBS_TYPE_MEASUREMENT);

        // Final package
        omObservation.setObservationConstellation(observationConstellation);
        List<OmObservation> observationCollection = CollectionHelper.list(omObservation);
        responseToEncode = new GetObservationResponse();
        responseToEncode.setObservationCollection(observationCollection);
    }

    @Test
    public void shouldThrowExceptionOnWrongInput() throws UnsupportedEncoderInputException, OwsExceptionReport {
        final Object objToEncode = new Object();

        exp.expect(UnsupportedEncoderInputException.class);
        exp.expectMessage(objToEncode.getClass().getName() + " can not be encoded by Encoder "
                + encoder.getClass().getName() + " because it is not yet implemented!");

        encoder.encode(objToEncode);
    }

    @Test
    public void shouldEncodeGetObservationResponse() throws UnsupportedEncoderInputException, OwsExceptionReport {
        BinaryAttachmentResponse encodedResponse = encoder.encode(responseToEncode);

        Assert.assertThat(encodedResponse, IsNot.not(CoreMatchers.nullValue()));
        final String[] split = new String(encodedResponse.getBytes()).split("\n");
        Assert.assertTrue("Expected >= 10 elements in array, got " + split.length, split.length >= 10);
    }

    @Test
    public void shouldEncodeFunctionInterpretationLine() throws UnsupportedEncoderInputException, OwsExceptionReport {
        Assert.assertThat(new String(encoder.encode(responseToEncode).getBytes()).split("\n")[0],
                Is.is("$ib Funktion-Interpretation: Linie"));
    }

    @Test
    public void shouldEncodeIndexUnitTime() throws UnsupportedEncoderInputException, OwsExceptionReport {
        Assert.assertThat(new String(encoder.encode(responseToEncode).getBytes()).split("\n")[1],
                Is.is("$sb Index-Einheit: *** Zeit ***"));
    }

    @Test
    public void shouldEncodeMeasurementIdentifier() throws UnsupportedEncoderInputException, OwsExceptionReport {
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[2];
        final String expected = "$sb Mess-Groesse: " + obsPropIdentifier
                .substring(obsPropIdentifier.length() - UVFConstants.MAX_IDENTIFIER_LENGTH, obsPropIdentifier.length());

        Assert.assertThat(actual, Is.is(expected));
        Assert.assertThat(actual.length(), Is.is(33));
    }
    
    @Test
    public void shouldEncodeUnitOfMeasurement() throws UnsupportedEncoderInputException, OwsExceptionReport {
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[3];
        final String expected = "$sb Mess-Einheit: " + unit;

        Assert.assertThat(actual, Is.is(expected));
    }

    @Test
    public void shouldEncodeMeasurementLocationIdentifier()
            throws UnsupportedEncoderInputException, OwsExceptionReport {
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[4];
        final String expected = "$sb Mess-Stellennummer: " + foiIdentifier
                .substring(foiIdentifier.length() - UVFConstants.MAX_IDENTIFIER_LENGTH, foiIdentifier.length());

        Assert.assertThat(actual, Is.is(expected));
        Assert.assertThat(actual.length(), Is.is(39));
    }
    
    @Test
    public void shouldEncodeTimeseriesTypeIdentifierTimebased()
            throws UnsupportedEncoderInputException, OwsExceptionReport {
        Assert.assertThat(new String(encoder.encode(responseToEncode).getBytes()).split("\n")[5], Is.is("*Z"));
    }

    @Test
    public void shouldEncodeTimeseriesIdentifierAndCenturies() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[6];
        final String expected = obsPropIdentifier.substring(
            obsPropIdentifier.length() - UVFConstants.MAX_IDENTIFIER_LENGTH,
            obsPropIdentifier.length()) +
            " " + unit + "     " + 
            "1970 1970";

        Assert.assertThat(actual, Is.is(expected));
    }
    
    @Test
    public void shouldEncodeTimeseriesIdentifierAndCenturiesFromStreamingValues() throws
            UnsupportedEncoderInputException, OwsExceptionReport {
        GlobalGetObservationValues globalValues = responseToEncode.new GlobalGetObservationValues();
        DateTime end = new DateTime(0);
        DateTime start = new DateTime(0);
        Time phenomenonTime = new TimePeriod(start, end);
        globalValues.addPhenomenonTime(phenomenonTime);
        responseToEncode.setGlobalValues(globalValues);
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[6];
        final String expected = obsPropIdentifier.substring(
            obsPropIdentifier.length() - UVFConstants.MAX_IDENTIFIER_LENGTH,
            obsPropIdentifier.length()) +
            " " + unit + "     " + 
            "1970 1970";

        Assert.assertThat(actual, Is.is(expected));
    }
    
    @Test
    public void shouldEncodeMeasurementLocationName() throws UnsupportedEncoderInputException, OwsExceptionReport {
        final String foiName = "test-foi-name";
        CodeType name = new CodeType(foiName);
        responseToEncode.getObservationCollection().get(0).getObservationConstellation().getFeatureOfInterest().
            setName(CollectionHelper.list(name));
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[5];
        final String expected = "$sb Mess-Stellenname: " + foiName;

        Assert.assertThat(actual, Is.is(expected));
    }
    
    @Test
    public void shouldEncodeMeasurementLocationIdAndCoordinates() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[7];
        final String expected = "-foi-identifier51.93503827.6521225 0.000     ";
        
        Assert.assertThat(actual, Is.is(expected));
    }
    
    @Test
    public void shouldEncodeTemporalBoundingBox() throws UnsupportedEncoderInputException, OwsExceptionReport {
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[8];
        final String expected = "70010112007001011200Zeit    ";

        Assert.assertThat(actual, Is.is(expected));
    }
    
    @Test
    public void shouldEncodeSingleObservationValueAndTimestamp() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[9];
        final String expected = "700101120052.0      ";
        
        Assert.assertThat(actual, Is.is(expected));
    }
    
    @Test
    public void shouldEncodeShortenedSingleObservationValueAndTimestamp() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        ((QuantityValue)responseToEncode.getObservationCollection().get(0).getValue().getValue()).
            setValue(52.1234567890);
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[9];
        final String expected = "700101120052.1234567";

        Assert.assertThat(actual, Is.is(expected));
    }
    
    @Test
    public void shouldEncodeSingleObservationValueAndEndOfTimePeriodPhenomenonTime() throws
            UnsupportedEncoderInputException, OwsExceptionReport {
        Time phenomenonTime = new TimePeriod(new Date(UTC_TIMESTAMP_0), new Date(UTC_TIMESTAMP_1));
        responseToEncode.getObservationCollection().get(0).getValue().setPhenomenonTime(phenomenonTime );
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[9];
        final String expected = "700101120052.0      ";
        
        Assert.assertThat(actual, Is.is(expected));
    }

    @Test
    public void shouldEncodeMultiObservationValueTimeValuePair() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        ObservationValue<MultiValue<List<TimeValuePair>>> mv = new MultiObservationValues<>();
        MultiValue<List<TimeValuePair>> value = new TVPValue();
        value.setUnit(unit);
        TimeValuePair tvp1 = new TimeValuePair(new TimeInstant(new Date(UTC_TIMESTAMP_0)),
                new QuantityValue(52.1234567890));
        TimeValuePair tvp2 = new TimeValuePair(new TimeInstant(new Date(UTC_TIMESTAMP_1)),
                new QuantityValue(52.1234567890));
        List<TimeValuePair> valueList = CollectionHelper.list(tvp1, tvp2);
        value.setValue(valueList);
        mv.setValue(value);
        responseToEncode.getObservationCollection().get(0).setValue(mv);

        final String[] encodedLines = new String(encoder.encode(responseToEncode).getBytes()).split("\n");

        Assert.assertThat(encodedLines[8], Is.is("69123112007001011200Zeit    "));
        Assert.assertThat(encodedLines[9], Is.is("691231120052.1234567"));
        Assert.assertThat(encodedLines[10], Is.is("700101120052.1234567"));
    }

    @Test
    public void shouldThrowExceptionOnWrongInputTLVTValue() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        ObservationValue<MultiValue<List<TimeLocationValueTriple>>> mv = new MultiObservationValues<>();
        MultiValue<List<TimeLocationValueTriple>> value = new TLVTValue();
        Time time = new TimeInstant(new Date(UTC_TIMESTAMP_1));
        TimeLocationValueTriple tlvt = new TimeLocationValueTriple(time , null , null);
        List<TimeLocationValueTriple> valueList = CollectionHelper.list(tlvt);
        value.setValue(valueList);
        mv.setValue(value);
        responseToEncode.getObservationCollection().get(0).setValue(mv);
        
        exp.expect(NoApplicableCodeException.class);
        exp.expectMessage("Encoding of Observations with values of type "
                + "'org.n52.sos.ogc.om.values.TLVTValue' not supported.");

        encoder.encode(responseToEncode);
    }

    @Test
    public void shouldEncodeMultiObservationValueSweDataArrayValue() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        SweDataRecord elementType = new SweDataRecord();
        SweField valueField = new SweField(obsPropIdentifier, new SweQuantity());
        final SweTime sweTime = new SweTime();
        sweTime.setDefinition(OmConstants.PHENOMENON_TIME);
        SweField timestampField = new SweField("phenomenonTime", sweTime);
        List<SweField> fields = CollectionHelper.list(timestampField, valueField);
        elementType.setFields(fields );
        SweDataArray dataArray = SweDataArrayBuilder.aSweDataArray()
                .setElementType(elementType)
                .setEncoding("text", "@", ";", ".")
                .addBlock("1969-12-31T12:00:00+00:00", "52.1234567890")
                .addBlock("1970-01-01T12:00:00+00:00", "42.1234567890")
                .build();
        
        ObservationValue<MultiValue<SweDataArray>> value = SweDataArrayValueBuilder.aSweDataArrayValue()
                .setSweDataArray(dataArray)
                .build();
        responseToEncode.getObservationCollection().get(0).setValue(value);

        final String[] encodedLines = new String(encoder.encode(responseToEncode).getBytes()).split("\n");

        Assert.assertThat(encodedLines[8], Is.is("69123112007001011200Zeit    "));
        Assert.assertThat(encodedLines[9], Is.is("691231120052.1234567"));
        Assert.assertThat(encodedLines[10], Is.is("700101120042.1234567"));
    }

    @Test
    public void shouldThrowExceptionOnWrongInputSweDataArrayWithBooleans() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        SweDataRecord elementType = new SweDataRecord();
        SweField valueField = new SweField(obsPropIdentifier, new SweBoolean());
        final SweTime sweTime = new SweTime();
        sweTime.setDefinition(OmConstants.PHENOMENON_TIME);
        SweField timestampField = new SweField("phenomenonTime", sweTime);
        List<SweField> fields = CollectionHelper.list(timestampField, valueField);
        elementType.setFields(fields );
        SweDataArray dataArray = SweDataArrayBuilder.aSweDataArray()
                .setElementType(elementType)
                .setEncoding("text", "@", ";", ".")
                .addBlock("1969-12-31T12:00:00+00:00", "52.1234567890")
                .addBlock("1970-01-01T12:00:00+00:00", "42.1234567890")
                .build();
        
        ObservationValue<MultiValue<SweDataArray>> value = SweDataArrayValueBuilder.aSweDataArrayValue()
                .setSweDataArray(dataArray)
                .build();

        responseToEncode.getObservationCollection().get(0).setValue(value);

        exp.expect(NoApplicableCodeException.class);
        exp.expectMessage("Encoding of SweArrayObservations with values of type "
                + "'org.n52.sos.ogc.swe.simpleType.SweBoolean' not supported.");

        encoder.encode(responseToEncode);
    }

    @Test
    public void shouldEncodeSingleObservationWithNoDataValue() throws
            UnsupportedEncoderInputException, OwsExceptionReport {
        responseToEncode.getObservationCollection().get(0).getValue().setValue(null);;
        final String actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n")[9];
        final String expected = "7001011200-777      ";

        Assert.assertThat(actual, Is.is(expected));
    }

    @Test
    public void shouldEncodeMultiObservationValueSweDataArrayValueWithNoDataValue() throws
            UnsupportedEncoderInputException, OwsExceptionReport {
        SweDataRecord elementType = new SweDataRecord();
        SweField valueField = new SweField(obsPropIdentifier, new SweQuantity());
        final SweTime sweTime = new SweTime();
        sweTime.setDefinition(OmConstants.PHENOMENON_TIME);
        SweField timestampField = new SweField("phenomenonTime", sweTime);
        List<SweField> fields = CollectionHelper.list(timestampField, valueField);
        elementType.setFields(fields );
        SweDataArray dataArray = SweDataArrayBuilder.aSweDataArray()
                .setElementType(elementType)
                .setEncoding("text", "@", ";", ".")
                .addBlock("1969-12-31T12:00:00+00:00", null)
                .addBlock("1970-01-01T12:00:00+00:00", "42.1234567890")
                .build();

        ObservationValue<MultiValue<SweDataArray>> value = SweDataArrayValueBuilder.aSweDataArrayValue()
                .setSweDataArray(dataArray)
                .build();
        responseToEncode.getObservationCollection().get(0).setValue(value);

        final String[] encodedLines = new String(encoder.encode(responseToEncode).getBytes()).split("\n");

        Assert.assertThat(encodedLines[8], Is.is("69123112007001011200Zeit    "));
        Assert.assertThat(encodedLines[9], Is.is("6912311200-777.0    "));
        Assert.assertThat(encodedLines[10], Is.is("700101120042.1234567"));
    }
    
    @Test
    public void shouldThrowNoApplicableCodeExceptionWhenReceivingNotMeasurementObservations() throws 
            UnsupportedEncoderInputException, OwsExceptionReport {
        String[] notSupportedTypes = {
                OmConstants.OBS_TYPE_CATEGORY_OBSERVATION,
                OmConstants.OBS_TYPE_COMPLEX_OBSERVATION,
                OmConstants.OBS_TYPE_DISCRETE_COVERAGE_OBSERVATION,
                OmConstants.OBS_TYPE_GEOMETRY_OBSERVATION,
                OmConstants.OBS_TYPE_OBSERVATION,
                OmConstants.OBS_TYPE_POINT_COVERAGE_OBSERVATION,
                OmConstants.OBS_TYPE_SWE_ARRAY_OBSERVATION,
                OmConstants.OBS_TYPE_TEXT_OBSERVATION,
                OmConstants.OBS_TYPE_TIME_SERIES_OBSERVATION,
                OmConstants.OBS_TYPE_TRUTH_OBSERVATION,
                OmConstants.OBS_TYPE_UNKNOWN
        };
        for (String notSupportedType : notSupportedTypes) {
            responseToEncode.getObservationCollection().get(0).getObservationConstellation()
                .setObservationType(notSupportedType);
            
            exp.expect(NoApplicableCodeException.class);
            exp.expectMessage("Observation Type '" + notSupportedType + "' not supported by this encoder '" + 
                    encoder.getClass().getName() + "'.");

            encoder.encode(responseToEncode);
        }
    }

    @Test
    public void shouldNotEncodeUnitOfMeasurementForCountObservations() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        responseToEncode.getObservationCollection().get(0).getObservationConstellation().
                setObservationType(OmConstants.OBS_TYPE_COUNT_OBSERVATION);
        final String[] actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n");
        final String expected = "$sb Mess-Einheit: " + unit;

        Assert.assertThat(Arrays.asList(actual), IsNot.not(CoreMatchers.hasItems(expected)));
    }

    @Test
    public void shouldEncodeTimeseriesIdentifierAndCenturiesWithoutUnitForCountObservations() throws
    UnsupportedEncoderInputException,
            OwsExceptionReport {
        responseToEncode.getObservationCollection().get(0).getObservationConstellation().
            setObservationType(OmConstants.OBS_TYPE_COUNT_OBSERVATION);
        Time phenTime = new TimeInstant(new Date(UTC_TIMESTAMP_1));
        responseToEncode.getObservationCollection().get(0).setValue(new SingleObservationValue<>(phenTime, 
                new CountValue(52)));
        ((OmObservableProperty)responseToEncode.getObservationCollection().get(0).getObservationConstellation()
                .getObservableProperty()).setUnit(null);
        final String[] actual = new String(encoder.encode(responseToEncode).getBytes()).split("\n");
        final String expected = obsPropIdentifier.substring(
            obsPropIdentifier.length() - UVFConstants.MAX_IDENTIFIER_LENGTH,
            obsPropIdentifier.length()) +
            " " + unit + "     " + 
            "1970 1970";

        Assert.assertThat(Arrays.asList(actual), IsNot.not(CoreMatchers.hasItem(expected)));
    }

    @Test
    public void shouldThrowExceptionOnStreamingValue() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        ObservationValue<?> mv = new StreamingValue<Object>() {

            private static final long serialVersionUID = 42L;

            public Object nextEntity() throws OwsExceptionReport { return null; }

            protected void queryTimes() {
                setPhenomenonTime(new TimeInstant(new Date(UTC_TIMESTAMP_1)));
            }

            protected void queryUnit() {}

            public TimeValuePair nextValue() throws OwsExceptionReport { return null; }

            public void mergeValue(StreamingValue<Object> streamingValue) {}

            public boolean hasNextValue() throws OwsExceptionReport { return false; }

            public OmObservation nextSingleObservation(boolean withIdentifierNameDesription) throws OwsExceptionReport {
                return null;
            }
        };
        responseToEncode.getObservationCollection().get(0).setValue(mv);
        
        exp.expect(NoApplicableCodeException.class);
        exp.expectMessage("Support for 'org.n52.sos.ogc.om.StreamingValue' not yet implemented.");

        encoder.encode(responseToEncode);
    }
    
    @Test
    public void shouldReturnEmptyFileWhenObservationCollectionIsEmpty() throws UnsupportedEncoderInputException,
            OwsExceptionReport {
        List<OmObservation> observationCollection = Collections.emptyList();
        responseToEncode.setObservationCollection(observationCollection);
        
        BinaryAttachmentResponse encodedResponse = encoder.encode(responseToEncode);
        Assert.assertThat(encodedResponse.getSize(), Is.is(-1));
    }
}