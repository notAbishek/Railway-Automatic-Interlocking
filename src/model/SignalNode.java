package model;

import enums.SignalType;

public class SignalNode extends Node {
    private SignalState state = SignalState.RED;
    private SignalType signalType;
    private String protectsTrackId;   // set by GraphBuilder, never by user
    private String stationCode; // null if not a station-boundary signal
    private int platformCount; // 0 if not applicable

    public SignalNode(String id, String name) {
        super(id, name);
        this.signalType = SignalType.GENERIC;
        this.stationCode = null;
        this.platformCount = 0;
    }

    public SignalNode(String id, String name, SignalType signalType) {
        super(id, name);
        this.state = SignalState.RED;
        this.signalType = signalType;
        this.stationCode = null;
        this.platformCount = 0;
    }

    public SignalNode(String id, String name,
            SignalType signalType, String stationCode, int platformCount) {
        super(id, name);
        this.state = SignalState.RED;
        this.signalType = signalType;
        this.stationCode = stationCode;
        this.platformCount = platformCount;
    }

    @Override
    public String getType() {
        return "SIGNAL";
    }

    public SignalState getState() {
        return this.state;
    }

    public SignalType getSignalType() {
        return signalType;
    }

    public String getProtectsTrackId() {
        return protectsTrackId;
    }

    public void setProtectsTrackId(String trackId) {
        this.protectsTrackId = trackId;
    }

    public String getStationCode() {
        return stationCode;
    }

    public int getPlatformCount() {
        return platformCount;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public void setPlatformCount(int platformCount) {
        this.platformCount = platformCount;
    }

    public void setState(SignalState state) {
        this.state = state;
    }

}
