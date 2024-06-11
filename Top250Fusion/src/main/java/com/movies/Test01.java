package com.movies;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test01 {
    public static void main(String[] args) {
        String[] series = {
                "Foyle's War (2002–2015)",
                "L'amica geniale (2018–)",
                "Twin Peaks: The Return (2017)",
                "Yes, Prime Minister (1986–1987)",
                "The Venture Bros. (2003–2018)",
                "Pose (2018–2021)",
                "The Last Dance (2020)"
        };

        Pattern pattern = Pattern.compile("(.+)\\s\\((.+)\\)");

        for (String s : series) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                String name = matcher.group(1);
                String years = matcher.group(2);
                System.out.println("名称: " + name + ", 年份: " + years);
            }
        }
    }
}
