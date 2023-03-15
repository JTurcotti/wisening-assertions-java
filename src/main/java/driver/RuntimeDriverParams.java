package driver;
@driver.RuntimeDriverParams.IsRuntimeDriverParamClass
public class RuntimeDriverParams {
    static final java.lang.String pathToThisFile = "src/main/java/driver/RuntimeDriverParams.java";

    public @interface IsRuntimeDriverParamClass {}

    public @interface IsRuntimeDriverParam {}

    @IsRuntimeDriverParam
    static final java.lang.String serialFormulasPath = "";

    @IsRuntimeDriverParam
    static final java.lang.String precedentResultsPath = "";

    @IsRuntimeDriverParam
    static final boolean precedentResultsPresent = false;

    @IsRuntimeDriverParam
    static final java.lang.String outputPath = "";

    @IsRuntimeDriverParam
    static final boolean active = false;
}