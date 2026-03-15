package enums;

public enum SignalType {
    DISTANT,              // warns of stop signal ahead
    OUTER_HOME,           // optional approach control
    HOME,                 // entry to station limits
    ROUTING_HOME,         // indicates which route is set
    STARTER,              // exit from station limits
    ADVANCED_STARTER,     // outermost exit boundary
    INTERMEDIATE_STARTER, // between platforms
    BLOCK,                // between stations on single track
    SHUNT,                // shunting movements only
    CALLING_ON,           // subsidiary signal, used after stop
    REPEATING,            // repeats the signal ahead
    GENERIC               // default when type not specified
}
