package org.dalquist.photos.survey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * See http://www.sentex.ca/~mec1995/tutorial/7seg/7nums.gif
 */
public class SevenSegmentUniqueTest {
  private static Set<String> SEGMENTS = ImmutableSet.of("a", "b", "c", "d", "e", "f", "g");
  private static Set<String> SEGMENTS_2D = merge(tens(SEGMENTS), ones(SEGMENTS));

  private static Set<String> ZERO = ImmutableSet.of("a", "b", "c", "d", "e", "f");
  private static Set<String> ONE = ImmutableSet.of("b", "c");
  private static Set<String> TWO = ImmutableSet.of("a", "b", "d", "e", "g");
  private static Set<String> THREE = ImmutableSet.of("a", "b", "c", "d", "g");
  private static Set<String> FOUR = ImmutableSet.of("b", "c", "f", "g");
  private static Set<String> FIVE = ImmutableSet.of("a", "c", "d", "f", "g");
  private static Set<String> SIX = ImmutableSet.of("a", "c", "d", "e", "f", "g");
  private static Set<String> SEVEN = ImmutableSet.of("a", "b", "c");
  private static Set<String> EIGHT = ImmutableSet.of("a", "b", "c", "d", "e", "f", "g");
  private static Set<String> NINE = ImmutableSet.of("a", "b", "c", "d", "f", "g");

  private static List<Set<String>> ALL_CHARS = ImmutableList.of(ZERO, ONE, TWO, THREE, FOUR, FIVE,
      SIX, SEVEN, EIGHT, NINE);

  private static List<Set<String>> TEMPS;
//  = ImmutableList.of(
//      merge(tens(SIX), ones(SIX)),
//      merge(tens(SIX), ones(SEVEN)),
//      merge(tens(SIX), ones(EIGHT)),
//      merge(tens(SIX), ones(NINE)),
//      merge(tens(SEVEN), ones(ZERO)),
//      merge(tens(SEVEN), ones(ONE)),
//      merge(tens(SEVEN), ones(TWO)),
//      merge(tens(SEVEN), ones(THREE)),
//      merge(tens(SEVEN), tens(FOUR)));
  
  static {
    ImmutableList.Builder<Set<String>> tempsBuilder = ImmutableList.builder();
    for (int t = 60; t <= 90; t++) {
      int tens = t/10;
      tempsBuilder.add(merge(tens(ALL_CHARS.get(tens)), ones(ALL_CHARS.get(t - (tens * 10)))));
    }
    TEMPS = tempsBuilder.build();
    System.out.println("Generated " + TEMPS.size() + " possible temps");
  }

  @Test
  public void findUniqueCount2d() throws Exception {
    // subset -> subCharacterSets
    List<Result> results = new ArrayList<>();

    for (Set<String> subset : Sets.powerSet(SEGMENTS_2D)) {
      Set<Set<String>> subChararacters = new LinkedHashSet<>();
      boolean unique = true;
      for (Set<String> character : TEMPS) {
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
      System.out.println(result.subset.size() + "> " + result.subset + " : " + result.characters);
    }
  }

  @Test
  public void findUniqueCount() throws Exception {
    // subset -> subCharacterSets
    List<Result> results = new ArrayList<>();

    for (Set<String> subset : Sets.powerSet(SEGMENTS)) {
      Set<Set<String>> subChararacters = new LinkedHashSet<>();
      boolean unique = true;
      for (Set<String> character : ALL_CHARS) {
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
      System.out.println(result.subset.size() + "> " + result.subset + " : " + result.characters);
    }
  }

  private static class Result implements Comparable<Result> {
    private final Set<String> subset;
    private final Set<Set<String>> characters;

    public Result(Set<String> subset, Set<Set<String>> characters) {
      this.subset = subset;
      this.characters = characters;
    }

    @Override
    public int compareTo(Result o) {
      return ComparisonChain.start().compare(subset.size(), o.subset.size()).result();
    }
  }

  @SafeVarargs
  private static Set<String> merge(Set<String>... sets) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (Set<String> s : sets) {
      builder.addAll(s);
    }
    return builder.build();
  }

  private static Set<String> tens(Set<String> s) {
    return prefix('1', s);
  }

  private static Set<String> ones(Set<String> s) {
    return prefix('0', s);
  }

  private static Set<String> prefix(char prefix, Set<String> s) {
    return ImmutableSet.copyOf(Iterables.transform(s, new Function<String, String>() {
      @Override
      public String apply(String input) {
        return prefix + input;
      }
    }));
  }
}
