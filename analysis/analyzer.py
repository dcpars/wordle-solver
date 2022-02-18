from wikipedia_scraper import WikipediaScraper
from wordle_db import WordleDb
import time


class Analyzer:

    def __init__(self):
        self.scraper = WikipediaScraper()
        self.wordle_db = WordleDb()

    def analyze_article(self):
        url, word_counts = self.scraper.scrape_article_words()
        updated_word_counts = self.__store_url_and_word_count(url, word_counts)
        return url, updated_word_counts

    # Store the URL and word counts in the database. For the MVP, if
    # the URL has already been scraped, don't update anything. Consider
    # keeping this behavior in the long term.
    def __store_url_and_word_count(self, url, word_counts):
        if self.wordle_db.url_hasnt_been_scraped(url):
            self.wordle_db.store_url(url, len(word_counts))
            if word_counts is not None and len(word_counts) > 0:
                return self.wordle_db.store_word_counts(word_counts)
        return {}


REQUEST_INTERVAL_SEC = 5
analyzer = Analyzer()
while True:
    u, wc = analyzer.analyze_article()
    print("{}: {} words found.".format(u, len(wc)))
    time.sleep(REQUEST_INTERVAL_SEC)
