package model;

import java.util.ArrayList;
import java.util.List;

public class Track {
    private String id;
    private String name;
    private String startNode;
    private String endNode;
    private int distance;
    private boolean inUse;
    private List<String> usedBy;

    public Track() {
        this.inUse = false;
        this.usedBy = new ArrayList<>();
    }

    public Track(String id, String name, String startNode, String endNode, int distance) {
        this.id = id;
        this.name = name;
        this.startNode = startNode;
        this.endNode = endNode;
        this.distance = distance;
        this.inUse = false;
        this.usedBy = new ArrayList<>();
    }
}
