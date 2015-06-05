package se.inera.webcert.hsa.model;

import java.io.Serializable;

/**
 * Created by Magnus Ekstrand on 03/06/15.
 */
public class Befattning implements Comparable<Befattning>, Serializable {

    private static final long serialVersionUID = -749264516311105908L;

    private String code;
    private String description;

    // -- Getters and setters

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    // -- Public scope

    @Override
    public int compareTo(Befattning comparable) {
        return getCode().compareTo(comparable.getCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Befattning that = (Befattning) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Befattning{" +
                "code='" + code + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

}
