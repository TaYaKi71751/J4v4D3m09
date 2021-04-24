package vrtest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Document;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import io.github.cdimascio.dotenv.Dotenv.Filter;

class VR {
    private String vr_uri = "https://vroongfriends.esafetykorea.or.kr";
    private String firefox = "Mozilla/5.0 (X11; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0";
    protected Map<String, String> cookies;
    protected HashMap<String, String> postData = new HashMap<String, String>();
    protected String username, password;

    void getPostData() throws IOException {
        Elements elements = Jsoup.connect(vr_uri).header("User-Agent", firefox).get().getElementsByAttribute("name");
        for (Element e : elements) {
            if (e.toString().indexOf("<input") != 0)
                continue;
            this.postData
                    .put(e.attr("name"),
                            e.attr("name").indexOf("mb_") != -1
                                    ? (e.attr("name").contains("ass") ? (this.password != null ? this.password : "")
                                            : (this.username != null ? this.username : ""))
                                    : e.attr("value"));
        }
    }

    void vrCookies() throws IOException {
        Response res = Jsoup.connect(vr_uri).header("User-Agent", firefox).method(Method.GET).execute();
        this.cookies = res.cookies();
    }

    void vrLoggedCookies() throws IOException {
        if (cookies == null)
            return;
        Response res = Jsoup.connect(vr_uri + "/bbs/login_check.php").header("User-Agent", firefox).data(this.postData)
                .method(Method.POST).cookies(cookies).execute();
        this.cookies = res.cookies();
        res = Jsoup.connect(vr_uri + "/bbs/login_check.php").header("User-Agent", firefox).data(this.postData)
                .method(Method.POST).cookies(cookies).execute();
        this.cookies = res.cookies();
        Boolean loggedIn = Jsoup.connect(vr_uri).cookies(this.cookies).userAgent(firefox).get().toString()
                .contains("./bbs/logout.php");
    }

    void vrApply() throws IOException {
        Document doc = Jsoup.connect(vr_uri + "/lms/plugin/apply/apply.php").header("User-Agent", firefox)
                .header("Referer", vr_uri+"/").cookies(this.cookies).get();
    }

    

    public static void main(String[] args) throws IOException {
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
        vr.getPostData();
        vr.vrCookies();
        vr.vrLoggedCookies();
        vr.vrApply();
    }
}