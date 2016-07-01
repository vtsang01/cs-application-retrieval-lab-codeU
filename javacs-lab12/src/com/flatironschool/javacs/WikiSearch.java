package com.flatironschool.javacs;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Iterator; 

import redis.clients.jedis.Jedis;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {
	
	// map from URLs that contain the term(s) to relevance score
	private Map<String, Integer> map;

	/**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Integer> map) {
		this.map = map;
	}
	
	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Integer getRelevance(String url) {
		Integer relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}
	
	/**
	 * Prints the contents in order of term frequency.
	 * 
	 * @param map
	 */
	private void print() {
		List<Entry<String, Integer>> entries = sort();
		for (Entry<String, Integer> entry: entries) {
			System.out.println(entry);
		}
	}
	
	/**
	 * Computes the union of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
        	Map<String, Integer> temp_map = new HashMap<String, Integer>(); 
        	temp_map.putAll(map);

        	Set<String> keys = that.map.keySet(); 

        	for (String key : keys){
	        	if (temp_map.containsKey(key)){
				Integer value1 = temp_map.get(key); 
				Integer value2 = that.map.get(key); 
				Integer total = totalRelevance(value1, value2);
				temp_map.put(key, total); 
				continue; 
			}
			Integer val = that.map.get(key);
			temp_map.put(key, val);
        	}

        	WikiSearch union = new WikiSearch(temp_map); 
		return union;
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
        	Map<String, Integer> temp_map = new HashMap<String, Integer>();
        	Set<String> keys = that.map.keySet(); 

        	for (String key : keys){
        		if (map.containsKey(key)){
        			Integer value1 = map.get(key); 
        			Integer value2 = that.map.get(key); 
        			Integer total = totalRelevance(value1, value2);
        			temp_map.put(key, total); 
        		}
        	}
        	WikiSearch intersection = new WikiSearch(temp_map);
		return intersection;
	}
	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
        	Map<String, Integer> temp_map = new HashMap<String, Integer>();;
        	Set<String> keys = map.keySet();  

        	for (String key : keys){
        		if (that.map.containsKey(key)){
        			continue; 
        		}
        		Integer value = map.get(key); 
        		temp_map.put(key, value); 
        	}
        	WikiSearch diff = new WikiSearch(temp_map);
		return diff;
	}
	
	/**
	 * Computes the relevance of a search with multiple terms.
	 * 
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Integer>> sort() {
        	Comparator<Entry<String, Integer>> comparator = new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> one, Entry<String, Integer> two) {
				if (one.getValue() < two.getValue()) {
			    		return -1;
				}
				if (one.getValue() > two.getValue()) {
				    return 1;
				}
				return 0;
			}
	        };

		List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>();  
		 // Get a set of the entries
      		Set set = map.entrySet();
      		// Get an iterator
      		Iterator i = set.iterator();
      		// Display elements
      		while(i.hasNext()) {
         		Entry<String, Integer> entry = (Entry<String, Integer>)i.next();
         		list.add(entry); 
      		}
      		Collections.sort(list, comparator);
      		return list;
	}

	/**
	 * Performs a search and makes a WikiSearch object.
	 * 
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index) {
		Map<String, Integer> map = index.getCounts(term);
		return new WikiSearch(map);
	}

	public static void main(String[] args) throws IOException {
		
		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		
		// search for the first term
		String term1 = "java";
		System.out.println("Query: " + term1);
		WikiSearch search1 = search(term1, index);
		search1.print();
		
		// search for the second term
		String term2 = "programming";
		System.out.println("Query: " + term2);
		WikiSearch search2 = search(term2, index);
		search2.print();
		
		// compute the intersection of the searches
		System.out.println("Query: " + term1 + " AND " + term2);
		WikiSearch intersection = search1.and(search2);
		intersection.print();
	}
}
