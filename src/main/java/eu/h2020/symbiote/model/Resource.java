package eu.h2020.symbiote.model;

import java.util.List;

/**
 * POJO describing a resource.
 */
public class Resource {
    private String id;
    private String name;
    private String owner;
    private String description;
    private List<String> observedProperties;
    private String resourceURL;
    private Location location;
    private String featureOfInterest;
    private String platformId;

    /**
     * Default empty constructor.
     */
    public Resource() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getObservedProperties() {
        return observedProperties;
    }

    public void setObservedProperties(List<String> observedProperties) {
        this.observedProperties = observedProperties;
    }

    public String getResourceURL() {
        return resourceURL;
    }

    public void setResourceURL(String resourceURL) {
        this.resourceURL = resourceURL;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getFeatureOfInterest() {
        return featureOfInterest;
    }

    public void setFeatureOfInterest(String featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", description='" + description + '\'' +
                ", observedProperties=" + observedProperties +
                ", resourceURL='" + resourceURL + '\'' +
                ", location=" + location +
                ", featureOfInterest='" + featureOfInterest + '\'' +
                ", platformId='" + platformId + '\'' +
                '}';
    }
}
