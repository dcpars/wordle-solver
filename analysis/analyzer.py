from lxml import html
import re
import requests

# analyzer.py
# Randomly scrape Wikipedia articles and analyzer words for popularity

RANDOM_ARTICLE_URL = 'https://en.wikipedia.org/wiki/Special:Random'
REQUEST_INTERVAL_SEC = 10
WORD_REGEX = r"\b([a-zA-Z]{5})\b"


def fetch_html(url):
    response = requests.get(url)
    if response.status_code == 200:
        # The Random Article page returns a redirect to a random page
        # TODO: Check if redirect URL matches an article we've already seen
        redirect_url = response.url

        print(redirect_url)
        return response.text
    else:
        print("Request to {} failed with status: {}".format(url, response.status_code))
        return None


# Given a paragraph of text, parse an array of valid five-letter words.
def parse_words_from_paragraph(paragraph):
    valid_words = []
    word_matches = re.findall(WORD_REGEX, paragraph)
    if word_matches and len(word_matches) > 0:
        formatted_words = list(map(lambda w: w.lower(), word_matches))
        valid_words.extend(formatted_words)
    return valid_words


# Given raw HTML, parse into a list of valid five-letter words.
def parse_content_for_words(page):
    dom = html.fromstring(page)
    paragraphs = dom.xpath('//div[@class="mw-parser-output"]/p/text()')
    valid_words = []
    for paragraph in paragraphs:
        paragraph_words = parse_words_from_paragraph(paragraph)
        valid_words.extend(paragraph_words)
    return valid_words


raw_html = fetch_html(RANDOM_ARTICLE_URL)
words = parse_content_for_words(raw_html)
print(words)
