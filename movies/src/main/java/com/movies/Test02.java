package com.movies;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test02 {

    public static void main(String[] args) {
        List<String> seriesList = new ArrayList<>();
        seriesList.add("Foyle's War (2002–2015)");
        seriesList.add("Rick and Morty (2013–)");
        seriesList.add("L'amica geniale (2018–)");
        seriesList.add("Twin Peaks: The Return (2017)");
        seriesList.add("Yes, Prime Minister (1986–1987)");
        seriesList.add("The Venture Bros. (2003–2018)");
        seriesList.add("Pose (2018–2021)");
        seriesList.add("The Last Dance (2020)");

        for (String series : seriesList) {
            extractSeriesInfo(series);
        }
    }

    private static void extractSeriesInfo(String series) {
        Pattern pattern = Pattern.compile("^(.*?) \\((\\d{4})(?:[–-](\\d{4}))?.*\\)$");
        Matcher matcher = pattern.matcher(series);

        if (matcher.find()) {
            String name = matcher.group(1);
            String startYear = matcher.group(2);
            String endYear = matcher.group(3); // This might be null if there is no end year

            System.out.println("Name: " + name);
            System.out.println("Start Year: " + startYear);
            if (endYear != null && !endYear.isEmpty()) {
                System.out.println("End Year: " + endYear);
            }
            System.out.println();
        }
    }
}
