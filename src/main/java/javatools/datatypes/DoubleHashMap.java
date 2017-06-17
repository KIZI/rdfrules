package javatools.datatypes;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javatools.administrative.D;

/**
 * 
 * This class is part of the Java Tools (see
 * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
 * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
 * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
 *
 * This class implements a HashMap with double values.
 * 
 * @author Fabian M. Suchanek
 * 
 * @param <K>
 */
public class DoubleHashMap<K> extends AbstractSet<K>{

  /** Holds the keys */
  protected Object[] keys;

  /** Holds the values */
  protected double[] values;

  /** Holds size */
  protected int size;

  /** Constructor*/
  public DoubleHashMap() {
    clear();
  }

  /** Creates a DoubleHashMap with these keys set to 1*/
  public DoubleHashMap(K... keys) {
	  this();
	  for(K k : keys) add(k);
  }

  /** Returns an index where to store the object*/
  protected int index(Object key, int len) {
    return (Math.abs(key.hashCode()) % len);
  }

  /** Returns an index where to store the object*/
  protected int index(Object key) {
    return (index(key, keys.length));
  }

  /** Retrieves a value */
  public double get(K key) {
    return (get(key, -1));
  }

  /** Finds a key, keys[find] will be NULL if non-existent */
  protected int find(Object key) {
    int i = index(key);
    while (true) {
      if (keys[i] == null) return (i);
      if (keys[i].equals(key)) return (i);
      i++;
      if (i == keys.length) i = 0;
    }
  }

  /** Retrieves a value */
  public double get(K key, int defaultValue) {
    int pos = find(key);
    if (keys[pos] == null) return (defaultValue);
    else return (values[pos]);
  }

  /** True if value is there */
  public boolean containsKey(Object key) {
    return (keys[find(key)] != null);
  }

  /** Increases a value, true for 'added new key with delta as value', false for 'increased existing value' */
  public boolean add(K key, double delta) {
    int pos = find(key);
    if (keys[pos] == null) {
      keys[pos] = key;
      values[pos] = delta;
      size++;
      if (size > keys.length * 3 / 4) rehash();
      return (true);
    }
    values[pos]+=delta;
    return (false);
  }
  
  /** Increases a value, true for  'added new key with value 1', false for 'increased existing value' */
  public boolean increase(K key) {
    return(add(key,1.0));
  }

  /** Returns keys. Can be used only once. */
  public PeekIterator<K> keys() {
    final Object[] e = keys;
    return (new PeekIterator<K>() {

      int pos = -1;

      @SuppressWarnings("unchecked")
      @Override
      protected K internalNext() throws Exception {
        pos++;
        for (; pos < keys.length; pos++) {
          if (e[pos] != null) {
            return ((K) e[pos]);
          }
        }
        return (null);
      }

    });
  }

  /** Adds a key, true for 'added the key as new', false for 'overwrote existing value' */
  public boolean put(K key, double value) {
    if (put(keys, values, key, value)) {
      size++;
      if (size > keys.length * 3 / 4) rehash();
      return (true);
    }
    return (false);
  }

  /** Adds a key, true for 'added the key as new', false for 'overwrote existing value' */
  protected boolean put(Object[] keys, double[] values, Object key, double value) {
    int i = index(key, keys.length);
    while (true) {
      if (keys[i] == null) {
        keys[i] = key;
        values[i] = value;
        return (true);
      }
      if (keys[i].equals(key)) {
        values[i] = value;
        return (false);
      }
      i++;
      if (i == keys.length) i = 0;
    }
  }

  /** Rehashes */
  protected void rehash() {
    Object[] newKeys = new Object[keys.length * 2];
    double[] newValues = new double[keys.length * 2];
    for (int i = 0; i < keys.length; i++) {
      if (keys[i] != null) put(newKeys, newValues, keys[i], values[i]);
    }
    keys = newKeys;
    values = newValues;
  }

  /** Test*/
  public static void main(String[] args) throws Exception {
    DoubleHashMap<String> m = new DoubleHashMap<String>();
    for (double i = 1; i <3000; i *= 2)
      m.put("#" + i, i);
    m.put("#0", 17);
    for (String key : m.keys())
      D.p(key, m.get(key));
  }

  @Override
  public Iterator<K> iterator() {    
    return keys().iterator();
  }

  @Override
  public int size() {
    return size;
  }
  
  @Override
  public boolean add(K e) {
    return(increase(e));
  };
  
  @Override
  public void clear() {
    size=0;
    keys = new Object[10];
    values = new double[10];
  }
  
  @Override
  public boolean contains(Object o) {
    return containsKey(o);
  }

  /** Adds all integer values up*/
  public void add(DoubleHashMap<K> countBindings) {
    for(K key : countBindings.keys()) {
      add(key,countBindings.get(key));
    }
  }

  /** increases the counters*/
  public void add(Collection<K> set) {
    for(K k : set) add(k);    
  }
    
  @Override
  public String toString() {   
    if(isEmpty()) return("{}");
    StringBuilder b=new StringBuilder("{");
    int counter=20;
    for(K key : keys()) {
      if(counter--==0) {
        b.append("..., ");
        break;
      }
      b.append(key).append('=').append(get(key)).append(", ");
    }
    b.setLength(b.length()-2);
    return(b.append("}").toString());
  }
  
  /** returns the keys in increasing order*/
  public List<K> increasingKeys() {
    List<K> result=keys().asList();    
    Collections.sort(result, new Comparator<K>(){

      @Override
      public int compare(K o1, K o2) {
        double i1=get(o1);
        double i2=get(o2);
        return(i1<i2?-1:i1>i2?1:0);
      }});
    return(result);
  }
  
  /** returns the keys in decreasing order*/
  public List<K> decreasingKeys() {
    List<K> result=keys().asList();    
    Collections.sort(result, new Comparator<K>(){

      @Override
      public int compare(K o1, K o2) {
        double i1=get(o1);
        double i2=get(o2);
        return(i1<i2?1:i1>i2?-1:0);
      }});
    return(result);
  }

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IntHashMap<?>))
			return (false);
		IntHashMap<?> other = (IntHashMap<?>) o;
		if (other.size() != this.size())
			return (false);
		for(int i=0;i<keys.length;i++) {
			if(keys[i]!=null && values[i]!=other.get(keys[i])) return(false);
		}
		return(true);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}

	/** Finds the maximum value*/
	public double findMax() {
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null && values[i] > max)
				max = values[i];
		}
		return (max);
	}

	/** Computes the sum*/
	public double computeSum() {
		double sum=0;
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null) sum+=values[i];
		}
		return (sum);
	}

}
