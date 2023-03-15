package driver;


@RuntimeDriverParams.IsRuntimeDriverParamClass
public class RuntimeDriverParams {
    public @interface IsRuntimeDriverParamClass {}

    public @interface IsRuntimeDriverParam {}
    @IsRuntimeDriverParam
    static final String serialFormulasPath = "to be replaced by spoon Transformer";

    @IsRuntimeDriverParam
    static final String precedentResultsPath = "to be replaced by spoon Transformer";
    @IsRuntimeDriverParam
    static final boolean precedentResultsPresent = false;
    @IsRuntimeDriverParam
    static final String outputPath = "to be replaced by spoon Transformer";
    @IsRuntimeDriverParam
    static final boolean active = false;

}
