package org.nagoya.model.dataitem;

public class Actor extends Person {
    private static final long serialVersionUID = -872388290176035829L;
    private String role;

    public Actor(String name, String role, FxThumb thumb) {
        super(name, thumb);
        this.setRole(role);
    }

    public Actor() {
        super();
        this.role = "";
    }

    public String getThumbUrl()
    {
        if(getThumb() == null) return "";
        if(getThumb().getThumbURL() == null) return "";
        return getThumb().getThumbURL().toString();
    }


    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = sanitizeString(role);
    }


    @Override
    public String toString() {
        return "Actor [role=\"" + this.role + "\", " + super.toString() + this.dataItemSourceToString() + "]";
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.role == null) ? 0 : this.role.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        Actor other = (Actor) obj;
        if (this.role == null) {
            if (other.role != null) {
                return false;
            }
        } else if (!this.role.equals(other.role)) {
            return false;
        }
        return true;
    }

}
