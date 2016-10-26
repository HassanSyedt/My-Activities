package cs.umass.edu.myactivitiestoolkit.util;

import java.lang.reflect.Array;
import java.util.LinkedList;

/**
 * Created by Hassan on 10/26/16.
 */
public class Converter<T,J> {


    public T[] convertToPrimitive(J... values){
        LinkedList<T> gimick = new LinkedList<>();
        for(J a: values){
            gimick.add((T) a);
        }
        return (T[]) gimick.toArray();
    }

    public T convert(J value){
        return (T) value;
    }

}
