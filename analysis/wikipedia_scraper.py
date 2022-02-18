from lxml import html
import re
import requests


class WikipediaScraper:
    RANDOM_ARTICLE_URL = "https://en.wikipedia.org/wiki/Special:Random"
    WORD_REGEX = r"\b([a-zA-Z]{5})\b"

    # Find a random article from Wikipedia and scrape it for five-letter words.
    # Return a Tuple containing the URL of the article and the word counts.
    def scrape_article_words(self):
        url, html_content = self.__fetch_html(self.RANDOM_ARTICLE_URL)
        words = self.__parse_content_for_words(html_content)
        return url, self.__count_words(words)

    @staticmethod
    def __fetch_html(url):
        response = requests.get(url)
        if response.status_code == 200:
            # The Random Article page returns a redirect to a random page.
            # Identify the article we were redirected to to avoid repeatedly
            # scraping the same article.
            redirect_url = response.url
            return redirect_url, response.text
        else:
            print("Request to {} failed with status: {}".format(url, response.status_code))
            return None

    # Given a paragraph of text, parse an array of valid five-letter words.
    def __parse_words_from_paragraph(self, paragraph):
        valid_words = []
        word_matches = re.findall(self.WORD_REGEX, paragraph)
        if word_matches and len(word_matches) > 0:
            formatted_words = list(map(lambda w: w.lower(), word_matches))
            valid_words.extend(formatted_words)
        return valid_words

    # Given raw HTML, parse into a list of valid five-letter words.
    def __parse_content_for_words(self, page):
        dom = html.fromstring(page)
        paragraphs = dom.xpath('//div[@class="mw-parser-output"]/p/text()')
        # Wikipedia often embeds hyperlinks in articles, linking to other articles.
        # The text of the hyperlink is valuable to parse as it often contains
        # words eligible for Wordle.
        hyperlinks = dom.xpath('//div[@class="mw-parser-output"]/p/a/text()')
        valid_words = []
        for text in paragraphs + hyperlinks:
            words = self.__parse_words_from_paragraph(text)
            valid_words.extend(words)
        return valid_words

    # Given an array of words, count the number of times each word occurs
    # and return a dictionary of word/count pairs.
    @staticmethod
    def __count_words(words):
        article_word_counts = {}
        for word in words:
            new_count = 1 if word not in article_word_counts else article_word_counts[word] + 1
            article_word_counts[word] = new_count
        return article_word_counts
