package org.intocps.maestro.framework.fmi2.api.mabl;

public class MablToMablAPI {

    private final MablApiBuilder mablApiBuilder;
    private BooleanBuilderFmi2Api booleanBuilderApi;
    private DataWriter dataWriter;
    private MathBuilderFmi2Api mathBuilderFmi2Api;

    public MablToMablAPI(MablApiBuilder mablApiBuilder) {
        this.mablApiBuilder = mablApiBuilder;
    }

    public DataWriter getDataWriter() {
        if (this.dataWriter == null) {
            this.dataWriter = new DataWriter(this.mablApiBuilder.dynamicScope, this.mablApiBuilder);
        }
        return this.dataWriter;
    }

    public BooleanBuilderFmi2Api getBooleanBuilder() {
        if (this.booleanBuilderApi == null) {
            this.booleanBuilderApi = new BooleanBuilderFmi2Api(this.mablApiBuilder.dynamicScope, this.mablApiBuilder);
        }
        return this.booleanBuilderApi;
    }

    public MathBuilderFmi2Api getMathBuilder() {
        if (this.mathBuilderFmi2Api == null) {
            this.mathBuilderFmi2Api = new MathBuilderFmi2Api(this.mablApiBuilder.dynamicScope, this.mablApiBuilder);
        }
        return this.mathBuilderFmi2Api;
    }
}
