package com.movies;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Demo {
    public static void main(String[] args) {
        // String str = "Raging Bull";
        // String regex = "[\u4e00-\u9fa5]";
        // Pattern pattern = Pattern.compile(regex);
        // Matcher matcher = pattern.matcher(str);
        // if (matcher.find()) {
        //     System.out.println(str);
        // } else {
        //     str = AliTranslate.translate(str);
        //     System.out.println(str);
        // }
        String file1Path = "C:\\Users\\emma\\Desktop\\a.txt"; // 替换为第一个 IMDb 名单文件的路径
        String file2Path = "C:\\Users\\emma\\Desktop\\b.txt"; // 替换为第二个 IMDb 名单文件的路径

        Set<String> list1 = readFileToList(file1Path);
        Set<String> list2 = readFileToList(file2Path);

        // 找出第一个列表中没有的电影
        System.out.println("在第一个列表中但不在第二个列表中的电影：");
        for (String movie : list1) {
            if (!list2.contains(movie)) {
                System.out.println(movie);
            }
        }

        // 找出第二个列表中没有的电影
        System.out.println("\n在第二个列表中但不在第一个列表中的电影：");
        for (String movie : list2) {
            if (!list1.contains(movie)) {
                System.out.println(movie);
            }
        }
    }

    // 读取文件并将内容存储到集合中
    private static Set<String> readFileToList(String filePath) {
        Set<String> movieSet = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // 假设每一行就是一部电影
                movieSet.add(line.split("\\.")[1].trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return movieSet;
    }
}