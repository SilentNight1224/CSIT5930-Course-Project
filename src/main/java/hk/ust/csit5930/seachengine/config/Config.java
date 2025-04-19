package hk.ust.csit5930.seachengine.config;

public class Config {
    public static final String CRAWLER_ROOT_URL = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
    public static final int CRAWLER_MAX_PAGES = 300;
    public static final String CRAWLER_PAGE_COUNT_KEY = "PageCount";

    public static final String STOP_WORDS_FILE = "stopwords.txt";

    public static final int INDEX_HEAD_TOKEN_WEIGHT = 2;

    public static final String AI_SUMMARY_PROMPT = """
          Summarize the following webpage content into **one to two paragraphs** and format the result using **valid HTML**.\s
          Output **only plain text** without Markdown syntax, code blocks, or additional explanations.\s
          """;
}
