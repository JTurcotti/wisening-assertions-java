package supervisor;

public class Config {
    //how many branches to remember into the past
    static final int BRANCH_MONITORING_WINDOW_SIZE = 10;

    //when to decide a value is fresh enough to be worth recomputing dependers
    static final float COMPUTATION_CELL_FRESH_VAL_TRESHOLD = 0.001f;

    //max size for a given computation cell
    static final int COMPUTATION_CELL_GROUP_MAX_CELL_SIZE = 1000;

    //cold values for various computation cells
    static final float PI_COLD_VALUE = 0.5f;
    static final float PHI_COLD_VALUE = 0.0f;
    static final float BETA_COLD_VALUE = 0.0f;
    static final float ETA_COLD_VALUE = 0.0f;
    static final float ALPHA_COLD_VALUE = 0.0f;
    static final float OMEGA_COLD_VALUE = 0.0f;

    //this is probably the single most important configurable parameter of all!
    static final float LINE_CORRECTNESS_COLD_VALUE = 0.5f;
    static final float ASSERTION_CORRECTNESS_COLD_VALUE = 0.0f;


    //when printing internal state, the number of bins to display for statistics
    static final int BINS_FOR_DISPLAY = 10;

    //to get the computation network primed, perform a fixed number of warmup rounds. This is that number.
    static final int WARMUP_ROUNDS = 7;
}
