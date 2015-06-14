package org.dalquist.photos.survey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * See http://www.sentex.ca/~mec1995/tutorial/7seg/7nums.gif
 */
public class SevenSegmentUniqueTest {
  private static Set<Character> SEGMENTS = ImmutableSet.of('a', 'b', 'c', 'd', 'e', 'f', 'g');
  
  private static Set<Character> ZERO    = ImmutableSet.of('a', 'b', 'c', 'd', 'e', 'f');
  private static Set<Character> ONE     = ImmutableSet.of('b', 'c');
  private static Set<Character> TWO     = ImmutableSet.of('a', 'b', 'd', 'e', 'g');
  private static Set<Character> THREE   = ImmutableSet.of('a', 'b', 'c', 'd', 'g');
  private static Set<Character> FOUR    = ImmutableSet.of('b', 'c', 'f', 'g');
  private static Set<Character> FIVE    = ImmutableSet.of('a', 'c', 'd', 'f', 'g');
  private static Set<Character> SIX     = ImmutableSet.of('a', 'c', 'd', 'e', 'f', 'g');
  private static Set<Character> SEVEN   = ImmutableSet.of('a', 'b', 'c');
  private static Set<Character> EIGHT   = ImmutableSet.of('a', 'b', 'c', 'd', 'e', 'f', 'g');
  private static Set<Character> NINE    = ImmutableSet.of('a', 'b', 'c', 'd', 'f', 'g');
  
  private static List<Set<Character>> ALL_CHARS = ImmutableList.of(ZERO, ONE, TWO, THREE, FOUR,
      FIVE, SIX, SEVEN, EIGHT, NINE);

  private static List<Set<Character>> TENS = ImmutableList.of(SIX, SEVEN);
  private static List<Set<Character>> ONES = ImmutableList.of(EIGHT, NINE, ZERO, ONE, TWO);

  @Test
  public void findUniqueCount() throws Exception {
    // subset -> subCharacterSets
    List<Result> results = new ArrayList<>();

    for (Set<Character> subset : Sets.powerSet(SEGMENTS)) {
      Set<Set<Character>> subChararacters = new LinkedHashSet<>();
      boolean unique = true;
      for (Set<Character> character : TENS) {
        character = new HashSet<>(character);
        character.retainAll(subset);
        unique = unique && subChararacters.add(character);
        if (!unique) {
          break;
        }
      }
      
      if (unique) {
        results.add(new Result(subset, subChararacters));
      }
    }
    
    Collections.sort(results);
    
    // Print results
    for (Result result : results) {
      System.out.println(result.subset.size() + "> " + result.subset + " : "
          + result.characters);
    }
  }
  
  private static class Result implements Comparable<Result> {
    private final Set<Character> subset;
    private final Set<Set<Character>> characters;
    
    public Result(Set<Character> subset, Set<Set<Character>> characters) {
      this.subset = subset;
      this.characters = characters;
    }

    @Override
    public int compareTo(Result o) {
      return ComparisonChain.start().compare(subset.size(), o.subset.size()).result();
    }
  }
}
