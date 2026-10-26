package com.smartflow.model;

/**
 * Encapsulated Resource model.
 * Represents enterprise hardware, software, or lab resources.
 */
public class Resource {
    private int id;
    private String name;
    private ResourceType type;
    private int totalCapacity;
    private int allocatedQuantity;
    private double hourlyCost;
    private String location;

    public Resource() {
    }

    public Resource(int id, String name, ResourceType type, int totalCapacity, int allocatedQuantity, double hourlyCost, String location) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.totalCapacity = totalCapacity;
        this.allocatedQuantity = allocatedQuantity;
        this.hourlyCost = hourlyCost;
        this.location = location;
    }

    public boolean isAvailable(int required) {
        return (totalCapacity - allocatedQuantity) >= required;
    }

    public int getAvailableQuantity() {
        return Math.max(0, totalCapacity - allocatedQuantity);
    }

    public double getUtilizationPercentage() {
        if (totalCapacity <= 0) return 0.0;
        return (allocatedQuantity * 100.0) / totalCapacity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public int getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(int allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public double getHourlyCost() {
        return hourlyCost;
    }

    public void setHourlyCost(double hourlyCost) {
        this.hourlyCost = hourlyCost;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return name + " (" + getAvailableQuantity() + "/" + totalCapacity + " avail)";
    }
}
