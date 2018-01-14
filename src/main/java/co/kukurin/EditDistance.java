package co.kukurin;

import java.util.stream.IntStream;

public class EditDistance {

  public static int calculate(String s1, String s2) {
    int[] count = new int[s1.length() + 1];
    int[] newCount = new int[s1.length() + 1];

    for (int i = 0; i <= s1.length(); i++) {
      // s2 empty => s1.length() insertions
      count[i] = i;
    }

    for (int i = 1; i <= s2.length(); i++) {
      // s1 empty => s2.length() insertions
      newCount[0] = i;

      for (int j = 1; j <= s1.length(); j++) {
        int matching = s1.charAt(j - 1) != s2.charAt(i - 1) ? 1 : 0;
        newCount[j] = IntStream.of(count[j - 1] + matching, // swap
            newCount[j - 1] + 1,     // move "right" = insertion in s2
            count[j] + 1             // move "down" = deletion from s2
        ).min().getAsInt();
      }

      int[] tmp = count;
      count = newCount;
      newCount = tmp;
    }

    return count[count.length - 1];
  }

}
