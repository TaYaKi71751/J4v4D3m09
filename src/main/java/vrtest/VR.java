package vrtest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.Dotenv.Filter;
import io.github.cdimascio.dotenv.DotenvEntry;

class VR {
    private String vr_uri = "https://cp.esafetykorea.or.kr";
    private static String firefox = "Mozilla/5.0 (X11; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0";
    protected Map<String, String> cookies;
    protected HashMap<String, String> postData = new HashMap<String, String>();
    protected String g4_lms_plug, username, password;
    protected Document doc, classDoc;

    HashMap<String, String> data = new HashMap<>();
    private static String subjectTitle;

    void getLogInPostData() throws IOException {
        Elements elements = doc.getElementsByAttribute("name");
        for (Element e : elements) {
            if (e.toString().indexOf("<input") != 0)
                continue;
            postData.put(e.attr("name"),
                    e.attr("name").indexOf("mb_") != -1
                            ? (e.attr("name").contains("ass") ? (password != null ? password : "")
                                    : (username != null ? username : ""))
                            : e.attr("value"));
        }
    }

    void vrCookies() throws IOException {
        Response res = Jsoup.connect(vr_uri).header("User-Agent", firefox).header("Cache-control", "no-cache")
                .header("Cache-store", "no-store").maxBodySize(0).method(Method.GET).execute();
        cookies = res.cookies();
        doc = Jsoup.parse(res.body());
    }

    void vrLoggedCookies() throws IOException {
        if (cookies == null)
            return;
        Response res = Jsoup
                .connect(vr_uri + Arrays
                        .asList(doc.getElementsByAttributeValueMatching("type", "^((?![a-z|A-Z]).)*$").first()
                                .toString().split("\'"))
                        .stream().filter(i -> i.contains("php")).collect(Collectors.toList()).get(0).replace("./", "/"))
                .header("User-Agent", firefox).header("Cache-control", "no-cache").data(postData).method(Method.POST)
                .cookies(cookies).execute();
        cookies = res.cookies();
        if (res.toString().contains("alert")
                || !Jsoup.connect(vr_uri).cookies(cookies).maxBodySize(0).header("User-Agent", firefox)
                        .header("Cache-control", "no-cache").get().toString().contains("./bbs/logout.php"))
            vrLoggedCookies();
    }

    void locRepClass() throws IOException {
        String str = "";
        doc = Jsoup.connect(vr_uri
                + (("/" + (str = doc.toString().replace("\"", "").split("\'")[1].replace("..", "").replace("//", "")))
                        .indexOf("//") == -1 ? "/" + str : str.replace("./", "/")))
                .header("User-Agent", firefox).cookies(cookies).get();
    }

    void vrClassPage() throws IOException {
        if (classDoc != null || doc.body().childNodeSize() < 2) {
            vrCookies();
            getLogInPostData();
            vrLoggedCookies();
        }
        doc = Jsoup.connect(vr_uri + doc.select("a:contains(강의)").first().attr("href").replace("./", "/"))
                .header("User-Agent", firefox).header("Referer", vr_uri + "/").cookies(cookies).get();
        locRepClass();
        subjectTitle = doc.select("[class=subject]").last().text();
        doc = Jsoup.connect(vr_uri + doc.select("a[href]").last().attr("href").replace("..", "").replace("//", ""))
                .header("User-Agent", firefox).cookies(cookies).get();
        locRepClass();

        g4_lms_plug = Arrays.asList(doc.select("script:containsData(g4_lms_plug)").last().toString().split(";"))
                .stream().filter(i -> i.contains("g4_lms_plug")).collect(Collectors.toList()).get(0).replace("./", "/")
                .split("=")[1].replace("\"", "").replace("..", "").replace("//", "");

    }

    void vrClass() throws IOException, InterruptedException {
        vrClassPage();
        List<Element> lecList = doc.select("a[href]").stream().filter(a -> a.toString().contains("go_lecview"))
                .collect(Collectors.toList());// lecview(vars)
        for (int a = 0; a < lecList.size(); a++) {
            try {
                go_lecview(Arrays.asList(lecList.get(a).attr("href").split("\'")).stream()
                        .filter(b -> b.matches("^[a-zA-Z0-9|']*$")).collect(Collectors.toList())
                        .toArray(new String[] {}));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IndexOutOfBoundsException e) {
                if (doc.body().text().isBlank())
                    vrClassPage();
            }
        }
    }

    void locRepLec() throws IOException {
        String str = "";
        doc = Jsoup.connect(vr_uri + "/" + g4_lms_plug.replace("./", "").replace(" ", "")
                + (("/" + (str = doc.toString().replace("\"", "").split("\'")[1].replace("..", "").replace("//", "")))
                        .indexOf("//") == -1 ? "/" + str : str.replace("./", "/")).replace("./", "player/"))
                .header("User-Agent", firefox).cookies(cookies).get();
    }

    void go_lecview(String... params) throws IOException, InterruptedException {
        data = new HashMap<>();
        String lecTitle = doc.select("td[class]").stream().filter(a -> a.attr("class").toString().contains("title"))
                .collect(Collectors.toList()).get((Integer.parseInt(params[2]) - 1)).text().replace(" ", "+");
        Integer maxPage = Integer.parseInt(
                doc.select("td[class]").stream().filter(a -> a.toString().contains(" / ")).collect(Collectors.toList())
                        .get(Integer.parseInt(params[2]) - 1).text().toString().split("/")[1].trim());
        classDoc = doc;
        lecview(lecTitle, maxPage, params);
        doc = classDoc;
    }

    void lecview(String lecTitle, Integer maxPage, String... params) throws IOException, InterruptedException {
        // for (int i = Integer.parseInt(params[3]); i <= maxPage; i++) {
        for (int i = 1; i <= maxPage; i++) {
            doc = Jsoup
                    .connect(vr_uri + "/" + g4_lms_plug.replace("./", "").replace(".", "").replace(" ", "")
                            + "/player/index.php?p_id=" + params[0] + "&s_id=" + params[1] + "&wr_order=" + params[2]
                            + "&wr_page=" + /* params[3] */i + "&bid=" + params[4])
                    .header("User-Agent", firefox).cookies(cookies).get();
            locRepLec();
            getUpdate();
            if (!(doc.select("iframe[src]").size() > 0))
                continue;

            doc = Jsoup.connect(doc.select("iframe[src]").last().attr("src")).header("User-Agent", firefox).get();
            if (doc.select("video[src]").size() != 0) {
                System.out.println("./" + subjectTitle + "/" + lecTitle + "/" + (1000 + i) + ".mp4");
                tryToGetVideo(doc.location().substring(0, doc.location().lastIndexOf("/")) + "/mp4/"
                        + doc.location().split("/")[5].replaceAll("\\D", "") + ".mp4", lecTitle, 1000 + i);
                continue;
            }
            if (doc.toString().contains("bg")) {
                System.out.println("./" + subjectTitle + "/" + lecTitle + "/" + (1000 + i) + ".xml");
                tryToGetXml(doc.location().replace(".html", ".xml"), lecTitle, 1000 + i);
                continue;
            }
        }
    }

    private static File mkdir(String path) throws InterruptedException, IOException {
        Runtime.getRuntime().exec("mkdir -p " + path).waitFor();
        return new File(path);
    }

    private void getUpdate() throws IOException {
        HashMap<String, String> data = new HashMap<String, String>();
        for (Element e : doc.select("input[type=hidden]")) {
            data.put(e.attr("name"), e.attr("value"));
        }
        data.put("_", Long.toString(System.currentTimeMillis()));
        String updateURL = (doc.location().substring(0, doc.location().lastIndexOf("/")))
                + (updateURL = Arrays.asList(Jsoup
                        .connect((doc.location().substring(0, doc.location().lastIndexOf("/")))
                                + (doc.head().select("script[type]").stream().filter(a -> a.toString().contains("lay"))
                                        .collect(Collectors.toList()).get(0).attr("src").replace("./", "/")))
                        .ignoreContentType(true).maxBodySize(0).header("User-Agent", firefox)
                        .header("Cache-Control", "no-Cache").cookies(cookies).get().body().childNode(0).toString()
                        .split(",")).stream().filter(a -> a.contains("php")).collect(Collectors.toList()).get(0))
                                .split("\"")[updateURL.split("\"").length >> 1].replace("./", "/");
        try {
            if (Jsoup.connect(updateURL).data(data).ignoreContentType(true).maxBodySize(0).header("User-Agent", firefox)
                    .header("Cache-Control", "no-Cache").cookies(cookies).get().text().contains("died")) {
                System.out.println("studied");
                return;
            }
            throw new Exception();
        } catch (Exception e) {
            getUpdate();
        }
        return;
    }

    private void tryToGetXml(String src, String lecTitle, Integer pageNum) throws IOException, InterruptedException {
        try {
            getXml(src, lecTitle, pageNum);
        } catch (FileNotFoundException e) {
            if (!e.toString().contains(src))
                return;
            tryToGetXml(src, lecTitle, pageNum);
        }
    }

    private void tryToGetVideo(String src, String lecTitle, Integer pageNum) throws IOException, InterruptedException {
        try {
            getVideo(src, lecTitle, pageNum);
        } catch (FileNotFoundException e) {
            if (!e.toString().contains(src))
                return;
            tryToGetVideo(src, lecTitle, pageNum);
        }
    }

    // https://contents.esafetykorea.or.kr/[content|seoul]/^*[0-9]*$/js/[arrange|video|quiz].js
    private void getXml(String src, String lecTitle, Integer pageNum) throws IOException, InterruptedException {
        System.out.println(src.substring(src.lastIndexOf("/"), src.length()));
        String a = src.substring(src.lastIndexOf("/"), src.length()), str = null,
                tagName = Jsoup
                        .parse(str = Jsoup.connect(src.replace(a, a.replace("/", "/xml/").replace(".html", ".xml")))
                                .header("User-Agent", firefox).get().children().last().toString())
                        .body().children().last().tagName();
        File dir = mkdir("./dl/" + subjectTitle + "/" + lecTitle
                + (new URL(doc.location()).getPath().substring(0, new URL(doc.location()).getPath().lastIndexOf("/")))
                + "/xml");
        try (PrintWriter out = new PrintWriter(dir.getPath() + "/" + pageNum + "-" + tagName + ".xml")) {
            out.println(str);
        }
        // https://stackoverflow.com/questions/1053467/how-do-i-save-a-string-to-a-text-file-using-java
    }

    private void getVideo(String src, String lecTitle, Integer pageNum) throws IOException, InterruptedException {
        File dir = mkdir("./dl/" + subjectTitle + "/" + lecTitle
                + (new URL(doc.location()).getPath().substring(0, new URL(doc.location()).getPath().lastIndexOf("/")))
                + "/mp4");
        System.out.println(src.substring(src.lastIndexOf("/"), src.length()));
        InputStream in = new java.net.URL(src).openStream();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(dir.getPath() + "/" + pageNum + ".mp4"));
        for (int b; (b = in.read()) != -1;) {
            out.write(b);
        }
        out.close();
        in.close();
        // https://stackoverflow.com/questions/59623263/downloading-a-mp4-file-in-java
        ///// https://examples.javacodegeeks.com/enterprise-java/html/download-images-from-a-website-using-jsoup/
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        HashMap<String, String> dotenv = new HashMap<>();
        for (DotenvEntry e : Dotenv.configure().load().entries(Filter.DECLARED_IN_ENV_FILE)) {
            dotenv.put(e.getKey(), e.getValue());
        }
        VR vr = new VR() {
            {
                username = dotenv.get("username");
                password = dotenv.get("password");
            }
        };
        vr.vrCookies();
        vr.getLogInPostData();
        vr.vrLoggedCookies();
        // vr.vrApply();
        vr.vrClass();
    }
}