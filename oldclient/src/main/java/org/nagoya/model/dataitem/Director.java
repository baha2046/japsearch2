package org.nagoya.model.dataitem;


public class Director extends Person {

    public static Director of(String name) {
        return new Director(name, null);
    }

    private static final long serialVersionUID = 6191933432944186334L;

    private Director(String name, FxThumb thumb) {
        super(name, thumb);
    }

    private Director() {
        super();
    }


    @Override
    public String toString() {
        return "Director [toString()=" + super.toString() + " ,\"" + this.dataItemSourceToString() + "\"]";
    }

}
