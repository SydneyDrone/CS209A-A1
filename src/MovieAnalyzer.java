import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovieAnalyzer {
    private final List<Movie> movieList;

    public static void main(String[] args) {
        List<List<String>> data = new ArrayList<>();
        try (Scanner scanner = new Scanner(Path.of("resources/test.csv"), StandardCharsets.UTF_8)) {
            while (scanner.hasNext()) {
                data.add(parseLine(scanner.nextLine()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (List<String> list : data) {
            for (String s : list) {
                System.out.println(s);
            }
        }
    }

    public MovieAnalyzer(String dataset_path) {
        movieList = new ArrayList<>();
        List<List<String>> data = new ArrayList<>();
        try (Scanner scanner = new Scanner(Path.of(dataset_path), StandardCharsets.UTF_8)) {
            while (scanner.hasNext()) {
                data.add(parseLine(scanner.nextLine()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 1; i < data.size(); i++) {
            movieList.add(new Movie(data.get(i)));
        }
    }

    public static List<String> parseLine(String str) {
        List<String> result = new ArrayList<>();

        if (str == null || str.isEmpty()) {
            return result;
        }

        StringBuilder sb = new StringBuilder();
        char separator = ',';
        char quote = '"';
        int quoteNum = 0;
        boolean isQuotedAttribute = false;
        char[] chars = str.toCharArray();

        for (char ch : chars) {
            if (sb.length() == 0 && !isQuotedAttribute && ch == quote) {
                isQuotedAttribute = true;
            } else if (ch == separator) {
                if (quoteNum % 2 ==(isQuotedAttribute ? 1 : 0)) {
                    if (isQuotedAttribute) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    result.add(sb.toString());
                    sb = new StringBuilder();
                    //clear flags
                    quoteNum = 0;
                    isQuotedAttribute = false;
                } else {
                    sb.append(ch);
                }
            } else if (ch == quote) {
                quoteNum++;
                sb.append(ch);
            } else {
                sb.append(ch);
            }
        }

        result.add(sb.toString());
        return result;
    }


    public Map<Integer, Integer> getMovieCountByYear() {
        Map<Integer, Integer> map = new TreeMap<>(Collections.reverseOrder());
        for (Movie movie : movieList) {
            if (movie.releasedYear == -1) continue;
            map.put(movie.releasedYear, map.getOrDefault(movie.releasedYear, 0) + 1);
        }
        return map;
    }

    public Map<String, Integer> getMovieCountByGenre() {
        Map<String, Integer> map = new HashMap<>();
        for (Movie movie : movieList) {
            if (movie.genre.isEmpty()) continue;
            for (String genre : movie.genre) {
                map.put(genre, map.getOrDefault(genre, 0) + 1);
            }
        }
        return map.entrySet().stream()
                .sorted((o1, o2) -> o1.getValue().equals(o2.getValue()) ? o1.getKey().compareTo(o2.getKey()) : o2.getValue().compareTo(o1.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    public Map<List<String>, Integer> getCoStarCount() {
        Map<List<String>, Integer> map = new HashMap<>();
        Map<List<String>, Integer> ans = new LinkedHashMap<>();
        for (Movie movie : movieList) {
            if (movie.stars.isEmpty()) continue;
            for (int i = 0; i < movie.stars.size(); i++) {
                for (int j = i + 1; j < movie.stars.size(); j++) {
                    List<String> pair = new ArrayList<>();
                    pair.add(movie.stars.get(i));
                    pair.add(movie.stars.get(j));
                    Collections.sort(pair);
                    map.put(pair, map.getOrDefault(pair, 0) + 1);
                }
            }
        }
        map.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(entry -> ans.put(entry.getKey(), entry.getValue()));
        return ans;
    }

    public List<String> getTopMovies(int top_k, String by) {
        if (by.equals("runtime")) {
            return movieList.stream()
                    .filter(movie -> movie.runtime != -1)
                    .sorted((o1, o2) -> o1.runtime == o2.runtime ? o1.name.compareTo(o2.name) : o2.runtime - o1.runtime)
                    .limit(top_k)
                    .flatMap(movie -> Stream.of(movie.name))
                    .toList();
        } else if (by.equals("overview")) {
            return movieList.stream()
                    .filter(movie -> !movie.overview.isEmpty())
                    .sorted((o1, o2) -> o1.overview.length() == o2.overview.length() ? o1.name.compareTo(o2.name) : o2.overview.length() - o1.overview.length())
                    .limit(top_k)
                    .flatMap(movie -> Stream.of(movie.name))
                    .toList();
        } else {
            return new ArrayList<>();
        }
    }

    public List<String> getTopStars(int top_k, String by) {
        Map<String, Double> map = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();
        if (by.equals("rating")) {
            for (Movie movie : movieList) {
                if (movie.stars.isEmpty() || movie.rating == -1) continue;
                for (String star : movie.stars) {
                    map.put(star, map.getOrDefault(star, 0d) + movie.rating);
                    countMap.put(star, countMap.getOrDefault(star, 0) + 1);
                }
            }
        } else if (by.equals("gross")) {
            for (Movie movie : movieList) {
                if (movie.stars.isEmpty() || movie.gross == -1) continue;
                for (String star : movie.stars) {
                    map.put(star, map.getOrDefault(star, 0d) + movie.gross);
                    countMap.put(star, countMap.getOrDefault(star, 0) + 1);
                }
            }
        }
        if (map.isEmpty()) {
            return new ArrayList<>();
        } else {
            map.replaceAll((s, v) -> map.get(s) / countMap.get(s));
            return map.entrySet().stream()
                    .sorted((o1, o2) -> o1.getValue().equals(o2.getValue()) ? o1.getKey().compareTo(o2.getKey()) : o2.getValue().compareTo(o1.getValue()))
                    .limit(top_k)
                    .flatMap(entry -> Stream.of(entry.getKey()))
                    .toList();
        }
    }

    public List<String> searchMovies(String genre, float min_rating, int max_runtime) {
        return movieList.stream()
                .filter(movie -> movie.genre.contains(genre) && movie.rating >= min_rating && movie.runtime <= max_runtime)
                .sorted(Comparator.comparing(o -> o.name))
                .flatMap(movie -> Stream.of(movie.name))
                .toList();
    }
}

class Movie {
    String name;
    int releasedYear;
    String certificate;
    int runtime; //needs formatting
    List<String> genre;
    float rating;
    String overview;
    int score;
    String director;
    List<String> stars;
    int voteNum;
    int gross; //needs formatting

    public Movie(List<String> list) {
        setName(list.get(1));
        setReleasedYear(list.get(2));
        setCertificate(list.get(3));
        setRuntime(list.get(4));
        setGenre(list.get(5));
        setRating(list.get(6));
        setOverview(list.get(7));
        setScore(list.get(8));
        setDirector(list.get(9));
        setStars(list.subList(10, 14));
        setVoteNum(list.get(14));
        setGross(list.get(15));
    }

    public void setName(String name) {
        this.name = Objects.requireNonNullElse(name, "");
    }

    public void setReleasedYear(String releasedYearStr) {
        if (releasedYearStr == null || releasedYearStr.isEmpty()) {
            this.releasedYear = -1;
        } else {
            this.releasedYear = Integer.parseInt(releasedYearStr);
        }
    }

    public void setCertificate(String certificate) {
        this.certificate = Objects.requireNonNullElse(certificate, "");
    }

    public void setRuntime(String runtimeStr) {
        if (runtimeStr == null || runtimeStr.isEmpty()) {
            this.runtime = -1;
        } else {
            this.runtime = Integer.parseInt(runtimeStr.split(" ")[0]);
        }
    }

    public void setGenre(String genreStr) {
        genreStr = Objects.requireNonNullElse(genreStr, "");
        this.genre = List.of(genreStr.split(", "));
    }

    public void setRating(String ratingStr) {
        if (ratingStr == null || ratingStr.isEmpty()) {
            this.rating = -1;
        } else {
            this.rating = Float.parseFloat(ratingStr);
        }
    }

    public void setOverview(String overview) {
        this.overview = Objects.requireNonNullElse(overview, "");
    }

    public void setScore(String scoreStr) {
        if (scoreStr == null || scoreStr.isEmpty()) {
            this.score = -1;
        } else {
            this.score = Integer.parseInt(scoreStr);
        }
    }

    public void setDirector(String director) {
        this.director = Objects.requireNonNullElse(director, "");
    }

    public void setStars(List<String> stars) {
        this.stars = stars.stream()
                .filter(Objects::nonNull)
                .filter(star -> !star.isEmpty())
                .toList();
    }

    public void setVoteNum(String voteNumStr) {
        if (voteNumStr == null || voteNumStr.isEmpty()) {
            this.voteNum = -1;
        } else {
            this.voteNum = Integer.parseInt(voteNumStr);
        }
    }

    public void setGross(String grossStr) {
        if (grossStr == null || grossStr.isEmpty()) {
            this.gross = -1;
        } else {
            this.gross = Integer.parseInt(grossStr.replace("\"", "").replace(",", ""));
        }
    }
}

